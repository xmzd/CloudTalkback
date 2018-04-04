package com.vanda.javacv.demo.im.talkback;

/**
 * Date    30/03/2018
 * Author  WestWang
 * 音视频会话接口
 */

public interface ITalkbackConversation {

    /**
     * 开始会话
     */
    void openConversation();

    /**
     * 结束会话
     */
    void closeConversation();
}
