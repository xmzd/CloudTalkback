package com.vanda.javacv.demo.im;

/**
 * Date    26/03/2018
 * Author  WestWang
 * IM 相关常量
 */

public final class IMConstants {

    /**
     * 主播地址
     */
//    public static final String HOST = "192.168.3.103";
    public static final String HOST = "47.92.50.248";
//    public static final String HOST = "239.0.0.100";
    /**
     * 图片接收端口
     */
    public static final int IMAGE_RECEIVE_PORT = 9990;
    /**
     * 音频接收端口
     */
    public static final int AUDIO_RECEIVE_PORT = 9990;
    /**
     * 图片发送端口
     */
    public static final int IMAGE_EMIT_PORT = 9990;
    /**
     * 音频发送端口
     */
    public static final int AUDIO_EMIT_PORT = 9990;

    /**
     * 本地Socket端口
     */
    public static final int LOCAL_PORT = 8888;

    /**
     * 远程服务端Socket端口
     */
    public static final int REMOTE_PORT = 9990;

    public static final String SOURCE_PERSON = "user1";
    public static final String SOURCE_DEVICE = "android";
    public static final String TARGET_PERSON = "user2";
    public static final String TARGET_DEVICE = "ios2";

    public static final String KEY_SOURCE_PERSON = "rp";
    public static final String KEY_SOURCE_DEVICE = "rd";
    public static final String KEY_TARGET_PERSON = "tp";
    public static final String KEY_TARGET_DEVICE = "td";

    public static final String CHARSET = "utf-8";
}
