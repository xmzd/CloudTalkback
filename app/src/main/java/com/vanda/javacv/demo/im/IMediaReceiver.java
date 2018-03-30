package com.vanda.javacv.demo.im;

/**
 * Date    26/03/2018
 * Author  WestWang
 * 音视频接收者接口
 */

public interface IMediaReceiver {

    /**
     * 视频数据
     *
     * @param data byte[]
     */
    void onReceive(byte[] data);

}