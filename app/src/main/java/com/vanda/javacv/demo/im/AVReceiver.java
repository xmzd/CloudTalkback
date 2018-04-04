package com.vanda.javacv.demo.im;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.vanda.javacv.demo.im.talkback.ITalkbackReceiver;
import com.vanda.javacv.demo.im.utils.PacketUtil;
import com.vanda.javacv.demo.utils.Logger;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Date    26/03/2018
 * Author  WestWang
 * 视频接收者
 */

public class AVReceiver extends MediaReceiver {

    private static final String TAG = AVReceiver.class.getSimpleName();
    private ITalkbackReceiver mAudioReceiver;
    private ITalkbackReceiver mImageReceiver;
    private static final int WHAT_SUCCESS = 1;
    private static final String ARGS_DATA = "data";
    private ReceiveHandler mHandler;
    private Map<Byte, byte[]> mImageMap = new HashMap<>();
    private long mCurrentImageFrameNum = 1;
    private int mCurrentImageFramePacketSum;

    private static class ReceiveHandler extends Handler {

        private WeakReference<AVReceiver> mRef;

        ReceiveHandler(AVReceiver instance) {
            mRef = new WeakReference<AVReceiver>(instance);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mRef == null) {
                return;
            }
            AVReceiver instance = mRef.get();
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

    public AVReceiver() {
        super();
        mHandler = new ReceiveHandler(this);
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
        return IMConstants.LOCAL_PORT;
    }

    /**
     * 接收数据，运行在子线程
     */
    @Override
    protected void receiveData() throws IOException {
        // 最终方案，一个udp包长度为5000
        byte buf[] = new byte[packetLength];
        // 存放一帧完整的音频数据
        byte[] matAudio = new byte[0];
        // 存放一帧完整的图片数据
        byte[] matImage = new byte[0];
        while (!mShutdown) {
            // 所以单帧需要由多个udp报文进行合并
            DatagramPacket dp = new DatagramPacket(buf, packetLength);
            mSocket.receive(dp);
            byte[] dpData = dp.getData();
            int dpLen = dp.getLength();
            Logger.d(TAG, "AV data received, length: " + dpLen + ", port: " + getPort());
            // 解udp包
            byte[] header = new byte[headerLength];
            System.arraycopy(dpData, 0, header, 0, headerLength);
            // json数据
            byte[] json = new byte[header[10]];
            System.arraycopy(header, 11, json, 0, header[10]);
            Logger.d(TAG, "Json data: " + new String(json));
            // 帧数据
            byte[] content = new byte[dpLen - headerLength];
            System.arraycopy(dpData, headerLength, content, 0, content.length);
            byte first = header[0];

            if (isAudioData(first)) {
                // 音频
                Logger.d(TAG, "audio data dispatching...");
                // 开始合并
                byte[] destData = new byte[matAudio.length + content.length];
                System.arraycopy(matAudio, 0, destData, 0, matAudio.length);
                System.arraycopy(content, 0, destData, matAudio.length, content.length);
                matAudio = destData;
                if (first == 0x01 << 4) {
                    // 音频、不拆包
                    try {
                        if (mAudioReceiver != null) {
                            mAudioReceiver.onReceive(matAudio);
                        }
                    } catch (Exception e) {
                        Logger.e(TAG, "deal audio data error. " + e.getLocalizedMessage());
                    } finally {
                        // 复位
                        matAudio = new byte[0];
                    }
                } else if (first == (0x01 << 4 | 0x01)) {
                    // 音频、需要拆包
                    // 当前最后一个包
                    try {
                        if (mAudioReceiver != null) {
                            mAudioReceiver.onReceive(matAudio);
                        }
                    } catch (Exception e) {
                        Logger.e(TAG, "deal audio data error. " + e.getLocalizedMessage());
                    } finally {
                        // 复位
                        matAudio = new byte[0];
                    }
                }
            } else if (isVideoData(first)) {
                // 视频
                Logger.d(TAG, "image data dispatching...");
                // 帧序号
                byte[] frameCount = new byte[8];
                System.arraycopy(header, 1, frameCount, 0, 8);
                long frame = PacketUtil.bytesToLong(frameCount);
                Logger.e(TAG, "frame number (帧编号) : " + frame);
                // 拆包信息
                byte packet = header[9];
                // 高4位，拆包总数
                byte packetSum = (byte) ((packet & 0xF0) >> 4);
                // 低4位，本次拆包序号
                byte packetNum = (byte) (packet & 0x0F);
                Logger.e(TAG, "packet info (拆包信息...) sum(本次拆包总数): " + packetSum + ", num(本次拆包序号): " + packetNum);
                if (first == 0x00) {
                    // 视频、不拆包
                    byte[] destData = new byte[matImage.length + content.length];
                    System.arraycopy(matImage, 0, destData, 0, matImage.length);
                    System.arraycopy(content, 0, destData, matImage.length, content.length);
                    matImage = destData;
                    // 显示本帧数据
                    try {
                        sendMsgToTarget(matImage);
                    } catch (Exception e) {
                        Logger.e(TAG, "deal image data error. " + e.getLocalizedMessage());
                    } finally {
                        // 复位
                        matImage = new byte[0];
                    }
                } else if (first == 0x01) {
                    if (frame > mCurrentImageFrameNum) {
                        // 下一帧数据到达，合并上一帧数据并显示
                        if (mCurrentImageFramePacketSum != mImageMap.size()) {
                            // 当前帧未能完整接收，舍弃
                        } else {
                            // 当前帧完整接收，显示
                            Byte[] keys = new Byte[mImageMap.size()];
                            Iterator<Byte> iterator = mImageMap.keySet().iterator();
                            int index = 0;
                            while (iterator.hasNext()) {
                                keys[index] = iterator.next();
                                index++;
                            }
                            Arrays.sort(keys);
                            for (Byte b : keys) {
                                Logger.e(TAG, "排序后的包结果..." + b);
                                byte[] imagePacket = mImageMap.get(b);
                                byte[] destData = new byte[matImage.length + imagePacket.length];
                                System.arraycopy(matImage, 0, destData, 0, matImage.length);
                                System.arraycopy(imagePacket, 0, destData, matImage.length, imagePacket.length);
                                matImage = destData;
                            }
                            // 显示本帧数据
                            try {
                                sendMsgToTarget(matImage);
                            } catch (Exception e) {
                                Logger.e(TAG, "deal image data error. " + e.getLocalizedMessage());
                            } finally {
                                // 复位
                                matImage = new byte[0];
                            }
                        }
                        // 清空map
                        mImageMap.clear();
                        // 放入本包
                        mImageMap.put(packetNum, content);
                    } else if (frame == mCurrentImageFrameNum) {
                        // 当前帧
                        mImageMap.put(packetNum, content);
                    } else {
                        // 网络原因导致该包到达较晚，舍弃
                    }
                    // 当前帧
                    mCurrentImageFrameNum = frame;
                    mCurrentImageFramePacketSum = packetSum;
                }
            }
        }
    }

    public void setImageReceiver(ITalkbackReceiver receiver) {
        mImageReceiver = receiver;
    }

    public void setAudioReceiver(ITalkbackReceiver receiver) {
        mAudioReceiver = receiver;
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

    /**
     * 判断音频数据包
     */
    private boolean isAudioData(byte data) {
        return data == 0x01 << 4 || data == (0x01 << 4 | 0x01);
    }

    /**
     * 判断视频数据包
     */
    private boolean isVideoData(byte data) {
        return data == 0x00 || data == 0x01;
    }
}
