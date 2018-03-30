package com.vanda.javacv.demo.audio;

/**
 * Date    20/03/2018
 * Author  WestWang
 */

public interface IAudioPlayer {

    /**
     * 设置参数
     * @param param
     */
    void setAudioParam(AudioParam param);

    /**
     * 设置数据源
     * @param data
     */
    void setDataSource(byte[] data);

    /**
     * 准备
     * @return
     */
    boolean prepare();

    /**
     * 设置播放状态
     * @param state
     */
    void setPlayState(int state);

    /**
     * 播放
     */
    void onStart();

    /**
     * 暂停
     */
    void onPause();

    /**
     * 播放完成
     */
    void onComplete();

    /**
     * 释放资源
     */
    void release();
}
