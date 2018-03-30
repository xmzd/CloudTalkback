package com.vanda.javacv.demo.im;

import com.vanda.javacv.demo.utils.Logger;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Date    28/03/2018
 * Author  WestWang
 * 音视频传输基类
 */

public abstract class MediaTransfer {

    private static final String TAG = MediaTransfer.class.getSimpleName();
    InetAddress mInetAddress;
    DatagramSocket mSocket;
    boolean mShutdown = false;
    boolean mIsInitialized;
    static final int packetLength = 5000;
    static final int headerLength = 200;
    static final int contentLength = 4800;

    /**
     * 接收数据子线程
     */
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                transfer();
            } catch (IOException e) {
                Logger.e(TAG, "transfer data error. " + e.getLocalizedMessage());
            }
        }
    };

    /**
     * 获取InetAddress
     *
     * @return InetAddress
     */
    public abstract String getHost();

    /**
     * 获取MulticastSocket
     *
     * @return MulticastSocket
     */
    public abstract int getPort();

    /**
     * 传输数据
     */
    protected abstract void transfer() throws IOException;

    /**
     * 开始，启动线程
     */
    public void start() {
        if (!mIsInitialized) {
            Logger.e(TAG, "init error.");
            return;
        }
        mShutdown = false;
        new Thread(mRunnable).start();
    }

    /**
     * 停止，结束线程，释放资源
     */
    public void stop() {
        // 子类实现
    }

}
