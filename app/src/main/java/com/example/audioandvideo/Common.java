package com.example.audioandvideo;

import android.os.Environment;

public class Common {
    public static final String SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final String APP_DIR = SDCARD + "/demo";
    public static final String AUDIO_OUTPUT_WAV = APP_DIR + "/audio_record.wav";
    public static final String AUDIO_OUTPUT_PCM = APP_DIR + "/audio_record.pcm";
}
