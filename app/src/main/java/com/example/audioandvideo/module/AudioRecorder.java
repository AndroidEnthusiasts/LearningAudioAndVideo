package com.example.audioandvideo.module;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioRecorder {
    private static final String TAG = "AudioRecorder";

    public static final int DEFAULT_SAMPLE_RATE = 44100;
    public static final int DEFAULT_PCM_DATA_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int DEFAULT_CHANNELS = 1;

    private ExecutorService mExecutor = Executors.newCachedThreadPool();
    private AudioRecord mAudioRecord;
    private int mBufferSize;
    private int mSampleRate = DEFAULT_SAMPLE_RATE;
    private int mPcmFormat = DEFAULT_PCM_DATA_FORMAT;
    private int mChannels = DEFAULT_CHANNELS;

    //    private AudioRecordCallback mRecordCallback;
    //    private Handler mHandler;
    private FileOutputStream mFileOutputStream;
    private boolean mIsRecording = false;

    public void setSampleRate(int sampleRate) {
        mSampleRate = sampleRate;
    }

    public int getSampleRate() {
        return mSampleRate;
    }

    public void setPcmFormat(int pcmFormat) {
        mPcmFormat = pcmFormat;
    }


    public void setChannels(int channels) {
        mChannels = channels;
    }

    public int getChannels() {
        return mChannels;
    }

    public boolean start(File filePath) {
        try {
            mFileOutputStream = new FileOutputStream(filePath);

            int channelConfig = mChannels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_OUT_STEREO;
//            mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, channelConfig, mPcmFormat);
            mBufferSize = getAudioBufferSize(channelConfig, mPcmFormat);
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRate,
                    channelConfig, mPcmFormat, mBufferSize);
        } catch (Exception e) {
            Log.e(TAG, "init AudioRecord exception: " + e.getLocalizedMessage());
            return false;
        }

        if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "cannot init AudioRecord");
            return false;
        }
        mIsRecording = true;
        mExecutor.execute(this::record);

        return true;
    }

    // 16BIT 格式兼容性更好
    // 单声道效率更高
    private int getAudioBufferSize(int channelLayout, int pcmFormat) {
        int bufferSize = 1024;

        switch (channelLayout) {
            case AudioFormat.CHANNEL_IN_MONO:
                bufferSize *= 1;
                break;
            case AudioFormat.CHANNEL_IN_STEREO:
                bufferSize *= 2;
                break;
        }

        switch (pcmFormat) {
            case AudioFormat.ENCODING_PCM_8BIT:
                bufferSize *= 1;
                break;
            case AudioFormat.ENCODING_PCM_16BIT:
                bufferSize *= 2;
                break;
        }

        return bufferSize;
    }

    private void record() {
        //设置进程优先级， THREAD_PRIORITY_URGENT_AUDIO标准较重要音频播放优先级
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        if (mAudioRecord == null || mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            return;
        }

        ByteBuffer audioBuffer = ByteBuffer.allocate(mBufferSize);
        mAudioRecord.startRecording();
        Log.d(TAG, "AudioRecorder started");

        int readResult;
        while (mIsRecording) {
            readResult = mAudioRecord.read(audioBuffer.array(), 0, mBufferSize);
//            Log.d("录制数据长度", readResult + "");
            if (readResult > 0) {
                byte[] data = new byte[readResult];
                audioBuffer.position(0);
                audioBuffer.limit(readResult);
                audioBuffer.get(data, 0, readResult);
//                mHandler.post(() -> mRecordCallback.onRecordSample(data));
                try {
                    mFileOutputStream.write(data);
                } catch (IOException e) {
                    Log.e(TAG, "onRecordSample write data failed: " + e.getMessage());
                }
            }
        }

        release();
        Log.d(TAG, "AudioRecorder finished");
    }

    public void stop() {
        mIsRecording = false;
        try {
            mFileOutputStream.flush();
            mFileOutputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "onRecordEnded exception occur: " + e.getMessage());
        }
    }

    private void release() {
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }

//    public interface AudioRecordCallback {
//        // start 在哪个线程调用，就运行在哪个线程
//        void onRecordSample(byte[] data);
//    }
}
