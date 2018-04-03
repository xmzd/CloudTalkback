package com.vanda.javacv.demo.im;

import com.vanda.javacv.demo.utils.Logger;

import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Date    28/03/2018
 * Author  WestWang
 * 音视频发射器基类
 */

public abstract class MediaEmitter extends MediaTransfer {

    private static final String TAG = MediaEmitter.class.getSimpleName();
    private String mSrcName;
    private String mSrcDevice;
    private String mDestName;
    private String mDestDevice;
    long mFrameCounter = 0;

    public MediaEmitter() {
        init();
    }

    private void init() {
        try {
            mInetAddress = InetAddress.getByName(getHost());
            mSocket = new MulticastSocket(IMConstants.LOCAL_PORT);
//            mSocket = new MulticastSocket();
//            mSocket.joinGroup(mInetAddress);
            mIsInitialized = true;
        } catch (Exception e) {
            mIsInitialized = false;
            Logger.e(TAG, "init error. " + e.getLocalizedMessage());
        }
    }

    @Override
    protected void transfer() throws IOException {
        emitData();
    }

    /**
     * 发射数据，运行在子线程
     */
    protected abstract void emitData() throws IOException;

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

    /**
     * 构造Json数据(rp：发送人，rd：发送设备，tp：接收人，td接收设备)
     *
     * @return byte[]
     */
    byte[] getJsonBytes() {
        try {
            JSONObject object = new JSONObject();
            object.put("rp", mSrcName);
            object.put("rd", mSrcDevice);
            object.put("tp", mDestName);
            object.put("td", mDestDevice);
            byte[] jsonBytes = object.toString().getBytes("utf-8");
            if (jsonBytes.length > 188) {
                Logger.e(TAG, "error, json data length is more than 190");
                return null;
            }
            return jsonBytes;
        } catch (Exception e) {
            Logger.e(TAG, "error, getJsonBytes. " + e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * 构建数据包的头部
     */
    byte[] getHeaderBytes(byte[] jsonBytes, byte byte10, byte[] frameCount, byte byte0, byte byte9) {
        byte[] header = new byte[200];
        header[0] = byte0;
        System.arraycopy(frameCount, 0, header, 1, 8);
        header[9] = byte9;
        header[10] = byte10;
        for (int index = 0; index < jsonBytes.length; index++) {
            header[11 + index] = jsonBytes[index];
        }
        return header;
    }

    public void setSrcName(String srcName) {
        this.mSrcName = srcName;
    }

    public void setSrcDevice(String srcDevice) {
        this.mSrcDevice = srcDevice;
    }

    public void setDestName(String destName) {
        this.mDestName = destName;
    }

    public void setDestDevice(String destDevice) {
        this.mDestDevice = destDevice;
    }
}
