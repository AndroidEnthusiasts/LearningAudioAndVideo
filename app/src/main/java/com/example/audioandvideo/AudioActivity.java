package com.example.audioandvideo;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.audioandvideo.module.AudioRecorder;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;


/**
 * 在路哥原例子中学习改写练习
 * 作业：
 * 完成使用AudioRecord录制pcm格式音频，支持暂停和恢复。
 * 完成PCM和WAV的互相转换。
 * 完成多段pcm音频文件拼接，wav应该差不多，读取拼接的head把长度记录下来，然后把data数据写入文件尾部，在修改head的长度数值加上刚才记录的长度
 * 完成录制的wav音频播放
 *
 */
public class AudioActivity extends Activity {
    private static String TAG = "AudioActivity";

    @BindView(R.id.audio_btn_start_record)
    public Button mBtnStartRecord;
    @BindView(R.id.audio_btn_stop_record)
    public Button mBtnStopRecord;
    @BindView(R.id.audio_btn_start_play)
    public Button mBtnStartPlay;
    @BindView(R.id.audio_btn_stop_play)
    public Button mBtnStopPlay;
    @BindView(R.id.audio_btn_pause_recover_record)
    public Button mBtnPause;
//    @BindView(R.id.audio_btn_start_wav_play)
//    public Button mBtnWavPlay;

    private AudioPlayer mAudioPlayer = new AudioPlayer();
    private AudioRecorder mAudioRecorder = new AudioRecorder();
    private File mPcmFile;

    //    private boolean mIsRecording = false;
    private boolean mIsPause = false;
    List<String> files = new ArrayList<>();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        ButterKnife.bind(this);
        mPcmFile = new File(Common.APP_DIR);
        try {
            if (!mPcmFile.exists()) {
                mPcmFile.mkdirs();
            }
            mPcmFile = new File(Common.AUDIO_OUTPUT_PCM);
            if (!mPcmFile.exists()) {
                mPcmFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mAudioRecorder.setSampleRate(AudioRecorder.DEFAULT_SAMPLE_RATE);
        mAudioRecorder.setPcmFormat(AudioRecorder.DEFAULT_PCM_DATA_FORMAT);
        mAudioRecorder.setChannels(AudioRecorder.DEFAULT_CHANNELS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAudioPlayer.stop();
        mAudioRecorder.stop();
        mIsPause = false;
    }

    @OnClick(R.id.audio_btn_pause_recover_record)
    public void pauseAndRecover() {
        if (mIsPause) {
            Log.d(TAG, "恢复录制");
            mBtnPause.setText(getResources().getString(R.string.audio_btn_pause_record));
            //恢复录制
            files.add(mPcmFile.getParent() + File.separator + mPcmFile.getName() + files.size());
            startRecord(new File(files.get(files.size() - 1)));
        } else {
            Log.d(TAG, "暂停录制");
            mBtnPause.setText(getResources().getString(R.string.audio_btn_recover_record));
            //暂停录制
            mAudioRecorder.stop();
        }
        mIsPause = !mIsPause;
    }

    @OnClick(R.id.audio_btn_start_record)
    public void setBtnStartRecord(View view) {
        startRecord(mPcmFile);
    }

    private void startRecord(File file) {
        Log.e(TAG, "开始录制");
        if (mAudioRecorder.start(file)) {
            disableButtons();
            new Handler().postDelayed(() -> mBtnStopRecord.setEnabled(true), 3000);
        }
    }

    @OnClick(R.id.audio_btn_stop_record)
    public void stopRecord() {
        Log.e(TAG, "停止录制");
        if (mIsPause) {
            mIsPause = false;
            mBtnPause.setText(getResources().getString(R.string.audio_btn_pause_record));
        }
        mAudioRecorder.stop();
        resetButtons();
        if (files.size() > 0) {
            merge();
        }
        pcmToWav(mPcmFile, new File(Common.AUDIO_OUTPUT_WAV));
    }

    /**
     * 合并多个pcm文件
     */
    private void merge() {
        try {
            BufferedSink sink = Okio.buffer(Okio.sink(mPcmFile, true));
            for (String name : files) {
                BufferedSource source = Okio.buffer(Okio.source(new File(name)));
                sink.writeAll(source);
                sink.flush();
                source.close();
            }
            sink.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //3. 合并后记得删除缓存文件并清除list
        for (int i = 0; i < files.size(); i++) {
            new File((files.get(i))).delete();
        }
        files.clear();
    }

    private void pcmToWav(File pcmFile, File wavFile) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(pcmFile);
            fos = new FileOutputStream(wavFile);

            int sampleFormat = AudioRecorder.DEFAULT_PCM_DATA_FORMAT == AudioFormat.ENCODING_PCM_16BIT ? 16 : 8;
            writeWavHeader(fos, fis.getChannel().size(), sampleFormat, AudioRecorder.DEFAULT_SAMPLE_RATE, AudioRecorder.DEFAULT_CHANNELS);

            int channelConfig = AudioRecorder.DEFAULT_CHANNELS == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
            int bufferSize = AudioRecord.getMinBufferSize(AudioRecorder.DEFAULT_SAMPLE_RATE, channelConfig, AudioRecorder.DEFAULT_PCM_DATA_FORMAT);

            byte[] data = new byte[bufferSize];
            while (fis.read(data) != -1) {
                fos.write(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }

                if (fos != null) {
                    fos.flush();
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeWavHeader(@NonNull FileOutputStream fos, long pcmDataLength, int sampleFormat,
                                int sampleRate, int channels) throws IOException {
        long audioDataLength = pcmDataLength + 36;
        long bitRate = sampleRate * channels * sampleFormat / 8;
        byte[] header = new byte[44];
        // RIFF
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        // 是整个文件的长度减去ID和Size的长度
        header[4] = (byte) (audioDataLength & 0xff);
        header[5] = (byte) ((audioDataLength >> 8) & 0xff);
        header[6] = (byte) ((audioDataLength >> 16) & 0xff);
        header[7] = (byte) ((audioDataLength >> 24) & 0xff);
        // WAVE
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        // 'fmt '
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        // 1(PCM)
        header[20] = 1;
        header[21] = 0;
        // channels
        header[22] = (byte) channels;
        header[23] = 0;
        // sample rate
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        // bit rate
        header[28] = (byte) (bitRate & 0xff);
        header[29] = (byte) ((bitRate >> 8) & 0xff);
        header[30] = (byte) ((bitRate >> 16) & 0xff);
        header[31] = (byte) ((bitRate >> 24) & 0xff);
        header[32] = 4;
        header[33] = 0;
        // 采样精度
        header[34] = (byte) sampleFormat;
        header[35] = 0;
        // data
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        // data length
        header[40] = (byte) (pcmDataLength & 0xff);
        header[41] = (byte) ((pcmDataLength >> 8) & 0xff);
        header[42] = (byte) ((pcmDataLength >> 16) & 0xff);
        header[43] = (byte) ((pcmDataLength >> 24) & 0xff);
        fos.write(header);
    }

    @OnClick(R.id.audio_btn_start_wav_play)
    public void startWavPlay() {
        Log.e(TAG, "开始播放wav");
        File file = new File(Common.AUDIO_OUTPUT_WAV);
        if (!file.exists()) {
            Toast.makeText(getBaseContext(), R.string.audio_msg_no_audio_file, Toast.LENGTH_LONG).show();
            return;
        }
        byte[] head = new byte[44];
        try {
            BufferedSource source = Okio.buffer(Okio.source(file));
            int res = source.read(head);
            if (res != 44) {
                return;
            }
            byte[] byte4 = new byte[4];
//            System.arraycopy(head, 40, byte4, 0, 4);
//            int pcmSize = Util.toInt(byte4);
            byte[] byte2 = new byte[2];
            System.arraycopy(head, 22, byte2, 0, 2);
            int channels = Util.toInt(byte2);
            System.arraycopy(head, 34, byte2, 0, 2);
            int sampleFormat = Util.toInt(byte2);
            System.arraycopy(head, 24, byte4, 0, 4);
            int sampleRate = Util.toInt(byte4);
            source.close();
            if (sampleFormat == 16) {
                disablePlayButtons();
                mAudioPlayer.start(sampleRate,
                        channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                        AudioRecorder.DEFAULT_PCM_DATA_FORMAT);
            } else {
                Log.e(TAG, "sampleFormat 不是16位");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.audio_btn_start_play)
    public void startPlay() {
        Log.e(TAG, "开始播放");
        mAudioPlayer.start();
        disablePlayButtons();
    }

    @OnClick(R.id.audio_btn_stop_play)
    public void stopPlay() {
        Log.e(TAG, "停止播放");
        mAudioPlayer.stop();
        resetPlayButtons();
    }

    private void disableButtons() {
        mBtnStartPlay.setEnabled(false);
        mBtnStopPlay.setEnabled(false);
        mBtnStartRecord.setEnabled(false);
        mBtnStopRecord.setEnabled(false);
        mBtnPause.setEnabled(true);
    }

    private void resetButtons() {
        mBtnPause.setEnabled(false);
        mBtnStartPlay.setEnabled(true);
        mBtnStopPlay.setEnabled(false);
        mBtnStartRecord.setEnabled(true);
        mBtnStopRecord.setEnabled(false);
    }

    private void disablePlayButtons() {
        mBtnStartPlay.setEnabled(false);
        mBtnStartRecord.setEnabled(false);
        mBtnStopRecord.setEnabled(false);
        mBtnPause.setEnabled(false);
        mBtnStopPlay.setEnabled(true);
    }

    private void resetPlayButtons() {
        mBtnStartPlay.setEnabled(true);
        mBtnStopPlay.setEnabled(false);
        mBtnStartRecord.setEnabled(false);
        mBtnStopRecord.setEnabled(false);
        mBtnPause.setEnabled(false);
    }

//    @Override
//    public void onRecordSample(byte[] data) {
//    }

    private class AudioPlayer {

        private AudioTrack mAudioTrack;
        private volatile boolean mIsPlaying = false;
        private int mBufferSize;
        private ExecutorService mExecutor;

        private void start() {
            if (!mPcmFile.exists()) {
                Toast.makeText(getBaseContext(), R.string.audio_msg_no_audio_file, Toast.LENGTH_LONG).show();
                return;
            }
            if (mIsPlaying) {
                Toast.makeText(getBaseContext(), R.string.audio_msg_playing_now, Toast.LENGTH_LONG).show();
                return;
            }
            release();
            int channelConfig = AudioRecorder.DEFAULT_CHANNELS == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
            mBufferSize = AudioTrack.getMinBufferSize(AudioRecorder.DEFAULT_SAMPLE_RATE, channelConfig, AudioRecorder.DEFAULT_PCM_DATA_FORMAT);
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, AudioRecorder.DEFAULT_SAMPLE_RATE, channelConfig,
                    AudioRecorder.DEFAULT_PCM_DATA_FORMAT, mBufferSize, AudioTrack.MODE_STREAM);
            mIsPlaying = true;
            mExecutor = Executors.newSingleThreadExecutor();
            mExecutor.execute(this::play);
        }

        public void start(int sampleRate, int channelConfig, int dataFormat) {
            if (mIsPlaying) {
                Toast.makeText(getBaseContext(), R.string.audio_msg_playing_now, Toast.LENGTH_LONG).show();
                return;
            }
            release();
            mBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, dataFormat);
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig,
                    dataFormat, mBufferSize, AudioTrack.MODE_STREAM);
            mIsPlaying = true;
            mExecutor = Executors.newSingleThreadExecutor();
            mExecutor.execute(this::playWav);
        }

        private void playWav() {
            Log.d(TAG, "AudioPlayer started");
            DataInputStream dis = null;
            try {
                byte[] buffer = new byte[mBufferSize];
                int readCount;
                dis = new DataInputStream(new FileInputStream(new File(Common.AUDIO_OUTPUT_WAV)));
                dis.skipBytes(44);
                while (dis.available() > 0 && mIsPlaying) {
                    readCount = dis.read(buffer);
                    if (readCount < 0) {
                        continue;
                    }
                    mAudioTrack.play();
                    mAudioTrack.write(buffer, 0, readCount);
                }
            } catch (Exception e) {
                Log.e(TAG, "play audio failed: " + e.getMessage());
            } finally {
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            stop();
            Log.d(TAG, "AudioPlayer stopped");
        }

        private void play() {
            Log.d(TAG, "AudioPlayer started");
            DataInputStream dis = null;
            try {
                byte[] buffer = new byte[mBufferSize];
                int readCount;
                dis = new DataInputStream(new FileInputStream(mPcmFile));
                while (dis.available() > 0 && mIsPlaying) {
                    readCount = dis.read(buffer);
                    if (readCount < 0) {
                        continue;
                    }
                    mAudioTrack.play();
                    mAudioTrack.write(buffer, 0, readCount);
                }
            } catch (Exception e) {
                Log.e(TAG, "play audio failed: " + e.getMessage());
            } finally {
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            stop();
            Log.d(TAG, "AudioPlayer stopped");
        }

        private void stop() {
            mIsPlaying = false;
            if (mExecutor != null) {
                try {
                    mExecutor.shutdown();
                    mExecutor.awaitTermination(50, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "stop play faild");
                }
            }
            release();
            if (Looper.myLooper() == Looper.getMainLooper()) {
                new Runnable() {
                    @Override
                    public void run() {
                        resetButtons();
                    }
                };
            } else {
                new Handler(Looper.getMainLooper()).post(() -> resetButtons());
            }
        }

        private void release() {
            if (mAudioTrack != null) {
                mAudioTrack.stop();
                mAudioTrack.release();
                mAudioTrack = null;
            }
        }

    }
}
