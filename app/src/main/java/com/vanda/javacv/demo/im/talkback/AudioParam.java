package com.vanda.javacv.demo.im.talkback;

import android.media.AudioFormat;

/**
 * Date    20/03/2018
 * Author  WestWang
 * Audio 参数
 */

public class AudioParam {

    // 采样率
    public int sampleRate = 22050;
    // 单次采样的存储位数 PCM 16 bit per sample
    public int bit = AudioFormat.ENCODING_PCM_16BIT;
    // 通道数
    public int channels = AudioFormat.CHANNEL_OUT_MONO;
    // 帧率
    public int frame = 2;

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getBit() {
        return bit;
    }

    public void setBit(int bit) {
        this.bit = bit;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public int getFrame() {
        return frame;
    }

    public void setFrame(int frame) {
        this.frame = frame;
    }
}
