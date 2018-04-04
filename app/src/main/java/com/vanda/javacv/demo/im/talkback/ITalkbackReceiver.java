package com.vanda.javacv.demo.im.talkback;

/**
 * Date    26/03/2018
 * Author  WestWang
 * 对讲接收者接口
 */

public interface ITalkbackReceiver {

    /**
     * 接收数据
     *
     * @param data byte[]
     */
    void onReceive(byte[] data);

}