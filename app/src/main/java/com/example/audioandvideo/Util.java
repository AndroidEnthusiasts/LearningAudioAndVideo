package com.example.audioandvideo;

/**
 * create by yx
 * on 2021/3/20
 */
public class Util {
    public static int toInt(byte[] bytes) {
        int number = 0;
        for (int i = 0; i < bytes.length; i++) {
            number += (bytes[i] & 0xFF) << i * 8;
        }
        return number;
    }
}
