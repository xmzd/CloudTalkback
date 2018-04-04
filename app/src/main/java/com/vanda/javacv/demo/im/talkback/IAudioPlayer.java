package com.vanda.javacv.demo.im.talkback;

/**
 * Date    20/03/2018
 * Author  WestWang
 */

public interface IAudioPlayer {

    /**
     * 设置参数
     *
     * @param param AudioParam
     */
    void setAudioParam(AudioParam param);

    /**
     * 设置数据源
     *
     * @param data byte[]
     */
    void setDataSource(byte[] data);

    /**
     * 设置播放状态
     *
     * @param state int
     */
    void setPlayState(int state);

    /**
     * 开始
     */
    void play();

    /**
     * 暂停
     */
    void pause();

    /**
     * 停止
     */
    void stop();

    /**
     * 释放资源
     */
    void release();
}
