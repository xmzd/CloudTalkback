package com.vanda.javacv.demo.im;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.vanda.javacv.demo.im.talkback.ITalkbackReceiver;
import com.vanda.javacv.demo.utils.Logger;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;

/**
 * Date    28/03/2018
 * Author  WestWang
 * 图片接收者
 */

public class ImageReceiver extends MediaReceiver {

    private static final String TAG = ImageReceiver.class.getSimpleName();
    private ITalkbackReceiver mImageReceiver;
    private static final int WHAT_SUCCESS = 1;
    private static final String ARGS_DATA = "data";
    private ReceiveHandler mHandler;

    private static class ReceiveHandler extends Handler {

        private WeakReference<ImageReceiver> mRef;

        ReceiveHandler(ImageReceiver instance) {
            mRef = new WeakReference<ImageReceiver>(instance);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mRef == null) {
                return;
            }
            ImageReceiver instance = mRef.get();
            if (instance == null) {
                return;
            }
            switch (msg.what) {
                case WHAT_SUCCESS:
                    Bundle bundle = msg.getData();
                    if (bundle != null) {
                        byte[] data = bundle.getByteArray(ARGS_DATA);
                        if (instance.mImageReceiver != null && data != null && data.length > 0) {
                            instance.mImageReceiver.onReceive(data);
                        }
                    }
                    break;
            }
        }
    }

    public ImageReceiver() {
        super();
        mHandler = new ReceiveHandler(this);
    }

    /**
     * 接收数据，运行在子线程
     */
    @Override
    protected void receiveData() throws IOException {
        // 最终方案，一个udp包长度为5000
        byte buf[] = new byte[packetLength];
        // 存放一帧完整的图片数据
        byte[] matImage = new byte[0];
        while (!mShutdown) {
            // 所以单帧需要由多个udp报文进行合并
            DatagramPacket dp = new DatagramPacket(buf, packetLength);
            mSocket.receive(dp);
            byte[] dpData = dp.getData();
            int dpLen = dp.getLength();
            Logger.e(TAG, "Image data received, length: " + dpLen + ", port: " + getPort());
            // 解udp包
            byte[] header = new byte[headerLength];
            System.arraycopy(dpData, 0, header, 0, headerLength);
            // json数据
            byte[] json = new byte[header[8]];
            System.arraycopy(header, 9, json, 0, header[8]);
            Logger.e(TAG, "Json data: " + new String(json));
            // 帧数据
            byte[] content = new byte[dpLen - headerLength];
            System.arraycopy(dpData, headerLength, content, 0, content.length);
            // 开始合并
            byte[] destData = new byte[matImage.length + content.length];
            System.arraycopy(matImage, 0, destData, 0, matImage.length);
            System.arraycopy(content, 0, destData, matImage.length, content.length);
            matImage = destData;
            if (header[0] == 0x00) {
                // 视频、不拆包
                try {
                    sendMsgToTarget(matImage);
                } catch (Exception e) {
                    Logger.e(TAG, "deal image data error. " + e.getLocalizedMessage());
                } finally {
                    // 复位
                    matImage = new byte[0];
                }
            } else if (header[0] == 0x01) {
                // 视频、需要拆包
                if (header[1] >> 4 == 0x01) {
                    // 当前最后一个包
                    try {
                        sendMsgToTarget(matImage);
                    } catch (Exception e) {
                        Logger.e(TAG, "deal image data error. " + e.getLocalizedMessage());
                    } finally {
                        // 复位
                        matImage = new byte[0];
                    }
                }
            }
        }
    }

    /**
     * 获取InetAddress
     *
     * @return InetAddress
     */
    @Override
    public String getHost() {
        return IMConstants.HOST;
    }

    /**
     * 获取MulticastSocket
     *
     * @return MulticastSocket
     */
    @Override
    public int getPort() {
        return IMConstants.IMAGE_RECEIVE_PORT;
    }

    public void setImageReceiver(ITalkbackReceiver receiver) {
        mImageReceiver = receiver;
    }

    /**
     * 通过Handler发送消息
     *
     * @param data byte[]
     */
    private void sendMsgToTarget(byte[] data) {
        Message msg = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putByteArray(ARGS_DATA, data);
        msg.setData(bundle);
        msg.what = WHAT_SUCCESS;
        msg.sendToTarget();
    }
}
