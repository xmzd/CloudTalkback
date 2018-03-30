package com.vanda.javacv.demo.im;

import com.vanda.javacv.demo.utils.Logger;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Date    26/03/2018
 * Author  WestWang
 * 音视频接收者基类
 */

public abstract class MediaReceiver extends MediaTransfer {

    private static final String TAG = MediaReceiver.class.getSimpleName();

    MediaReceiver() {
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        try {
            mInetAddress = InetAddress.getByName(getHost());
            mSocket = new DatagramSocket(getPort());
            mIsInitialized = true;
        } catch (Exception e) {
            mIsInitialized = false;
            Logger.e(TAG, "init error. " + e.getLocalizedMessage());
        }
    }

    @Override
    protected void transfer() throws IOException {
        receiveData();
    }

    /**
     * 接收数据，运行在子线程
     */
    protected abstract void receiveData() throws IOException;

    @Override
    public void stop() {
        if (!mIsInitialized) {
            Logger.e(TAG, "init error.");
            return;
        }
        mShutdown = true;
        try {
            mSocket.close();
            Logger.d(TAG, "socket closed.");
        } catch (Exception e) {
            Logger.e(TAG, "stop error. " + e.getLocalizedMessage());
        }
    }
}
