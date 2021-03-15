//
// Created by Admin on 2021/3/15.
//

#ifndef AUDIOANDVIDEO_LOG_H
#define AUDIOANDVIDEO_LOG_H


#include <jni.h>
#include <android/log.h>

#define LOG_TAG "JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

const static int SUCCEED = 1;
const static int FAILED = -1;

#endif //AUDIOANDVIDEO_LOG_H
