package com.vanda.javacv.demo.im.talkback;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.util.SparseArray;

import com.vanda.javacv.demo.im.IMConstants;
import com.vanda.javacv.demo.im.utils.PacketUtil;
import com.vanda.javacv.demo.utils.Logger;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Date    02/04/2018
 * Author  WestWang
 * 音视频udp包发送、接收
 */

public class TalkbackTransfer {

    private static final String TAG = TalkbackTransfer.class.getSimpleName();
    private InetAddress mInetAddress;
    private MulticastSocket mSocket;
    private boolean mShutdown = false;
    private boolean mIsInitialized;
    private ImageSize mImageSize;
    private DataEntity mDataEntity;
    private static final int packetLength = 2000;
    private static final int headerLength = 200;
    private static final int contentLength = 1800;
    private static final int frameLength = 8;
    private static int mImageWidth;
    private static int mImageHeight;

    private LinkedBlockingDeque<byte[]> mImageDeque;
    private LinkedBlockingDeque<byte[]> mAudioDeque;
    private long mFrameImageCounter = 0;
    private long mFrameAudioCounter = 0;

    private ITalkbackReceiver mImageReceiver;
    private ITalkbackReceiver mAudioReceiver;
    private static final int WHAT_IMAGE_DATA = 1;
    private ReceiveHandler mHandler;
    private SparseArray<byte[]> mImageArray;
    private SparseArray<byte[]> mAudioArray;
    private long mCurrentImageFrameNum = 1;
    private int mCurrentImageFramePacketSum;
    private long mCurrentAudioFrameNum = 1;
    private int mCurrentAudioFramePacketSum;

    /**
     * Handler
     */
    private static class ReceiveHandler extends Handler {

        private WeakReference<TalkbackTransfer> mRef;

        ReceiveHandler(TalkbackTransfer instance) {
            mRef = new WeakReference<TalkbackTransfer>(instance);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            if (mRef == null) {
                return;
            }
            TalkbackTransfer instance = mRef.get();
            if (instance == null) {
                return;
            }
            switch (msg.what) {
                case WHAT_IMAGE_DATA:
                    byte[] data = (byte[]) msg.obj;
                    if (instance.mImageReceiver != null && data != null && data.length > 0) {
                        instance.mImageReceiver.onReceive(data);
                    }
                    break;
            }
        }

    }

    public TalkbackTransfer() {
        init();
        mHandler = new ReceiveHandler(this);
        mImageArray = new SparseArray<>();
        mAudioArray = new SparseArray<>();
    }

    private void init() {
        try {
            mInetAddress = InetAddress.getByName(getHost());
            mSocket = new MulticastSocket(getLocalPort());
            mIsInitialized = true;
        } catch (Exception e) {
            mIsInitialized = false;
            Logger.e(TAG, "init error. " + e.getLocalizedMessage());
        }
    }

    /**
     * 发送图片
     */
    private Runnable mEmitImageRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                emitImage();
            } catch (IOException e) {
                Logger.e(TAG, "Emit image data error. " + e.getLocalizedMessage());
            }
        }
    };

    /**
     * 发送音频
     */
    private Runnable mEmitAudioRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                emitAudio();
            } catch (IOException e) {
                Logger.e(TAG, "Emit audio data error. " + e.getLocalizedMessage());
            }
        }
    };

    /**
     * 接收
     */
    private Runnable mReceiveRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                receive();
            } catch (IOException e) {
                Logger.e(TAG, "Receive data error. " + e.getLocalizedMessage());
            }
        }
    };

    /**
     * 发送图片
     */
    private void emitImage() throws IOException {
        while (!mShutdown) {
            try {
                // YUV data from queue
                byte[] data = mImageDeque.take();
                // 转换成YuvImage
                if (mImageSize == null) {
                    mImageSize = new ImageSize(mImageWidth, mImageHeight);
                }
                YuvImage image = new YuvImage(data, ImageFormat.NV21, mImageSize.width, mImageSize.height, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // 压缩jpg
                image.compressToJpeg(new Rect(0, 0, mImageSize.width, mImageSize.height), 50, baos);
                Bitmap bitmap = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size());
                rotateBitmap(bitmap);
                baos.close();
            } catch (InterruptedException e) {
                Logger.e(TAG, "error, take image data from deque. " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * 发送音频
     */
    private void emitAudio() throws IOException {
        while (!mShutdown) {
            try {
                byte[] data = mAudioDeque.take();
                packetData(data, true);
            } catch (InterruptedException e) {
                Logger.e(TAG, "error, take audio data from deque. " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * 接收数据
     */
    private void receive() throws IOException {
        byte buff[] = new byte[packetLength];
        // 存放一帧完整的音频数据
        byte[] matAudio = new byte[0];
        // 存放一帧完整的图片数据
        byte[] matImage = new byte[0];
        while (!mShutdown) {
            // 接收数据
            DatagramPacket dp = new DatagramPacket(buff, packetLength);
            mSocket.receive(dp);
            byte[] dpData = dp.getData();
            int dpLen = dp.getLength();
            Logger.e(TAG, "data received, length: " + dpLen + ", port: " + getLocalPort());

            // 解udp包
            byte[] header = new byte[headerLength];
            System.arraycopy(dpData, 0, header, 0, headerLength);

            // 解json数据
            byte[] json = new byte[header[10]];
            System.arraycopy(header, 11, json, 0, header[10]);
            Logger.e(TAG, "Json data: " + new String(json));

            // 解帧编号
            byte[] frameCount = new byte[frameLength];
            System.arraycopy(header, 1, frameCount, 0, frameLength);
            long frameNum = PacketUtil.bytesToLong(frameCount);
            Logger.e(TAG, "frame number (帧编号) : " + frameNum);

            // 解包信息
            byte packet = header[9];
            // 高4位，拆包总数
            byte packetSum = (byte) ((packet & 0xF0) >> 4);
            // 低4位，本次拆包序号
            byte packetNum = (byte) (packet & 0x0F);
            Logger.e(TAG, "packet info sum(本次拆包总数): " + packetSum + ", num(本次拆包序号): " + packetNum);

            // 帧数据
            byte[] content = new byte[dpLen - headerLength];
            System.arraycopy(dpData, headerLength, content, 0, content.length);

            // 第1个字符，用于判断是图片数据还是音频数据
            byte first = header[0];
            if (isAudioData(first)) {
                // 音频
                Logger.d(TAG, "audio data dispatching...");
                if (first == 0x01 << 4) {
                    // 音频、不拆包
                    byte[] destData = new byte[matAudio.length + content.length];
                    System.arraycopy(matAudio, 0, destData, 0, matAudio.length);
                    System.arraycopy(content, 0, destData, matAudio.length, content.length);
                    matAudio = destData;
                    // 播放音频
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
                    if (frameNum > mCurrentAudioFrameNum) {
                        // 下一帧数据到达，合并上一帧数据并显示
                        if (mCurrentAudioFramePacketSum != mAudioArray.size()) {
                            // 当前帧未能完整接收，舍弃
                        } else {
                            // 当前帧完整接收，显示
                            for (int i = 0; i < mAudioArray.size(); i++) {
                                byte[] audioPacket = mAudioArray.valueAt(i);
                                byte[] destData = new byte[matAudio.length + audioPacket.length];
                                System.arraycopy(matAudio, 0, destData, 0, matAudio.length);
                                System.arraycopy(audioPacket, 0, destData, matAudio.length, audioPacket.length);
                                matAudio = destData;
                            }
                            // 播放音频
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
                        // 清空map
                        mAudioArray.clear();
                        // 放入本包
                        mAudioArray.put(packetNum, content);
                    } else if (frameNum == mCurrentAudioFrameNum) {
                        // 当前帧
                        mAudioArray.put(packetNum, content);
                    } else {
                        // 网络原因导致该包到达较晚，舍弃
                    }
                    // 当前帧
                    mCurrentAudioFrameNum = frameNum;
                    mCurrentAudioFramePacketSum = packetSum;
                }
            } else if (isImageData(first)) {
                // 视频
                Logger.d(TAG, "image data dispatching...");
                if (first == 0x00) {
                    // 视频、没有拆包
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
                    // 视频、拆过包
                    if (frameNum > mCurrentImageFrameNum) {
                        // 下一帧数据到达，合并上一帧数据并显示
                        if (mCurrentImageFramePacketSum != mImageArray.size()) {
                            // 当前帧未能完整接收，舍弃
                        } else {
                            // 当前帧完整接收，显示
                            for (int i = 0; i < mImageArray.size(); i++) {
                                byte[] imagePacket = mImageArray.valueAt(i);
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
                        mImageArray.clear();
                        // 放入本包
                        mImageArray.put(packetNum, content);
                    } else if (frameNum == mCurrentImageFrameNum) {
                        // 当前帧
                        mImageArray.put(packetNum, content);
                    } else {
                        // 网络原因导致该包到达较晚，舍弃
                    }
                    // 当前帧
                    mCurrentImageFrameNum = frameNum;
                    mCurrentImageFramePacketSum = packetSum;
                }
            }
        }
    }

    /**
     * 旋转、镜像处理
     */
    private void rotateBitmap(Bitmap bitmap) throws IOException {
        Matrix matrix = new Matrix();
        // 旋转
        matrix.postRotate(-90);
        // 镜像水平翻转
        matrix.postScale(-1, 1);
        Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();
        bitmap = null;
        // 发送
        ByteArrayOutputStream baso = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 50, baso);
        byte[] data = baso.toByteArray();
        baso.close();
        // 打包发送
        packetData(data, false);
    }

    /**
     * 根据策略拆包发送
     * <p>
     * 第1个字符，高4位 0000：1-->语音，0-->视频
     * 第1个字符，低4位 0000：1-->拆包，0-->不拆包
     * <p>
     * 第2个字符 至 第9个字符，这8个byte（长整型）表示本帧编号，编号从1开始，每一个后续帧的编号+1
     * <p>
     * 第10个字符，高4位 0000：如果拆包，这里记录了拆包的总数，如果不拆包，则没有意义
     * 第10个字符，低4位 0000：如果拆包，则记录当前包在整个拆包中的序号，从1开始；如果不拆包，则没有意义
     * <p>
     * 第11个字符，共八位，0000 0000 记录包中从第12个字符开始，json数据的长度，最长188
     * 第12个字符～第200个字符：json数据-->rp：发送人，rd：发送设备，tp：接收人，td接收设备
     */
    private void packetData(byte[] data, boolean isAudio) throws IOException {
        int length = data.length;
        if (isAudio) {
            Logger.d(TAG, "Audio data original length: " + length);
        } else {
            Logger.d(TAG, "Image data original length: " + length);
        }
        // 构造json数据
        byte[] jsonBytes = getJsonBytes();
        if (jsonBytes == null) {
            return;
        }
        // 第11个字符，json长度
        byte byte10 = (byte) (jsonBytes.length & 0xFF);
        // 构建第2个字符至第9个字符
        // 帧计数器+1
        long frameCounter = 0;
        if (isAudio) {
            mFrameAudioCounter++;
            frameCounter = mFrameAudioCounter;
        } else {
            mFrameImageCounter++;
            frameCounter = mFrameImageCounter;
        }
        byte[] frameCount = PacketUtil.longToByte(frameCounter);
        // 构建第1个字符、第10个字符
        byte byte0 = 0;
        byte byte9 = 0;
        if (length > contentLength) {
            // 需要拆包
            int count = (length % contentLength == 0) ? (length / contentLength) : ((length / contentLength) + 1);
            // 高4位
            byte9 = (byte) ((count & 0xFF) << 4);
            for (int i = 0; i < count; i++) {
                if (isAudio) {
                    byte0 = 0x01 << 4 | 0x01;
                } else {
                    byte0 = 0x01;
                }
                byte[] content;
                if (i == count - 1) {
                    // 最后一个包
                    content = new byte[length - i * contentLength];
                } else {
                    // 不是最后一个包
                    content = new byte[contentLength];
                }
                System.arraycopy(data, i * contentLength, content, 0, content.length);
                // 低4位
                byte result = (byte) (byte9 | ((i + 1) & 0xFF));
                // 发送
                send(getHeaderBytes(byte0, frameCount, result, byte10, jsonBytes), content);
            }
        } else {
            // 不需要拆包
            if (isAudio) {
                byte0 = 0x01 << 4;
            } else {
                byte0 = 0x00;
            }
            byte9 = 0x00;
            // 发送
            send(getHeaderBytes(byte0, frameCount, byte9, byte10, jsonBytes), data);
        }
    }

    /**
     * 发送数据
     */
    private void send(byte[] header, byte[] content) throws IOException {
        // 拼接一个完整的udp包
        byte[] result = new byte[headerLength + content.length];
        System.arraycopy(header, 0, result, 0, headerLength);
        System.arraycopy(content, 0, result, headerLength, content.length);
        // 打包发送
        DatagramPacket dataPacket = new DatagramPacket(result, result.length, mInetAddress, getRemotePort());
        Logger.d(TAG, "send data length: " + dataPacket.getLength() + ", port: " + getRemotePort());
        mSocket.send(dataPacket);
    }

    /**
     * 构造Json数据
     * <p>
     * rp：发送人
     * rd：发送设备
     * tp：接收人
     * td接收设备
     */
    private byte[] getJsonBytes() {
        try {
            JSONObject object = new JSONObject();
            object.put(IMConstants.KEY_SOURCE_PERSON, mDataEntity.sourcePerson);
            object.put(IMConstants.KEY_SOURCE_DEVICE, mDataEntity.sourceDevice);
            object.put(IMConstants.KEY_TARGET_PERSON, mDataEntity.targetPerson);
            object.put(IMConstants.KEY_TARGET_DEVICE, mDataEntity.targetDevice);
            byte[] jsonBytes = object.toString().getBytes(IMConstants.CHARSET);
            if (jsonBytes.length > 188) {
                Logger.e(TAG, "error, the length of json data is more than 190");
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
    private byte[] getHeaderBytes(byte byte0, byte[] frameCount, byte byte9, byte byte10, byte[] jsonBytes) {
        // 包头长度200
        byte[] header = new byte[headerLength];
        // 第1个字符
        header[0] = byte0;
        // 第2到第9个字符
        System.arraycopy(frameCount, 0, header, 1, 8);
        // 第10个字符
        header[9] = byte9;
        // 第11个字符
        header[10] = byte10;
        // 第12到第200个字符
        for (int index = 0; index < jsonBytes.length; index++) {
            header[11 + index] = jsonBytes[index];
        }
        return header;
    }

    /**
     * 判断音频数据包
     */
    private boolean isAudioData(byte data) {
        return data == 0x01 << 4 || data == (0x01 << 4 | 0x01);
    }

    /**
     * 判断图片数据包
     */
    private boolean isImageData(byte data) {
        return data == 0x00 || data == 0x01;
    }

    /**
     * 通过Handler发送消息
     *
     * @param data byte[]
     */
    private void sendMsgToTarget(byte[] data) {
        if (mHandler != null) {
            android.os.Message msg = mHandler.obtainMessage();
            msg.what = WHAT_IMAGE_DATA;
            msg.obj = data;
            msg.sendToTarget();
        }
    }

    /**
     * 是否初始化成功
     */
    private boolean isInitialized(String method) {
        if (!mIsInitialized) {
            Logger.e(TAG, method + ", init error.");
            return false;
        }
        return true;
    }

    /**
     * 检测ImageSize
     */
    private boolean checkImageSize() {
        if (mImageSize == null) {
            Logger.e(TAG, "error, ImageSize is null");
            return false;
        }
        return true;
    }

    /**
     * 检测DataEntity
     */
    private boolean checkDataEntity() {
        if (mDataEntity == null) {
            Logger.e(TAG, "error, DataEntity is null");
            return false;
        }
        return true;
    }

    /**
     * 检测ImageDeque
     */
    private boolean checkImageDeque() {
        if (mImageDeque == null) {
            Logger.e(TAG, "error, ImageDeque is null");
            return false;
        }
        return true;
    }

    /**
     * 检测AudioDeque
     */
    private boolean checkAudioDeque() {
        if (mAudioDeque == null) {
            Logger.e(TAG, "error, AudioDeque is null");
            return false;
        }
        return true;
    }

    /**
     * 开始发射图片数据
     */
    public void startEmitImage() {
        if (!isInitialized("startEmitImage") || !checkImageSize() ||
                !checkDataEntity() || !checkImageDeque()) {
            return;
        }
        mShutdown = false;
        new Thread(mEmitImageRunnable).start();
    }

    /**
     * 开始发射音频数据
     */
    public void startEmitAudio() {
        if (!isInitialized("startEmitAudio") || !checkDataEntity() ||
                !checkAudioDeque()) {
            return;
        }
        mShutdown = false;
        new Thread(mEmitAudioRunnable).start();
    }

    /**
     * 开始接收数据
     */
    public void startReceive() {
        if (!isInitialized("startReceive")) {
            return;
        }
        mShutdown = false;
        new Thread(mReceiveRunnable).start();
    }

    /**
     * 关闭，释放资源
     */
    public void stopAndRelease() {
        if (!isInitialized("stop")) {
            return;
        }
        mShutdown = true;
        try {
            mSocket.close();
            Logger.d(TAG, "socket closed.");
        } catch (Exception e) {
            Logger.e(TAG, "stop error. " + e.getLocalizedMessage());
        }
        mIsInitialized = false;

        mInetAddress = null;
        mSocket = null;

        mImageSize = null;
        mDataEntity = null;

        mImageArray.clear();
        mImageArray = null;
        mAudioArray.clear();
        mAudioArray = null;

        mImageReceiver = null;
        mAudioReceiver = null;

        mHandler.removeMessages(WHAT_IMAGE_DATA);
        mHandler = null;

        mImageDeque.clear();
        mAudioDeque.clear();
    }

    /**
     * 主机地址
     *
     * @return String
     */
    private String getHost() {
        return IMConstants.HOST;
    }

    /**
     * 本地端口，发送接收
     *
     * @return int
     */
    private int getLocalPort() {
        return IMConstants.LOCAL_PORT;
    }

    /**
     * 远程端口，发送
     *
     * @return int
     */
    private int getRemotePort() {
        return IMConstants.REMOTE_PORT;
    }

    /**
     * 设置ImageSize
     */
    public void setImageSize(int width, int height) {
        mImageWidth = width;
        mImageHeight = height;
        mImageSize = new ImageSize(mImageWidth, mImageHeight);
    }

    /**
     * 设置DataEntity
     */
    public void setDataEntity(String sourcePerson, String sourceDevice, String targetPerson, String targetDevice) {
        mDataEntity = new DataEntity(sourcePerson, sourceDevice, targetPerson, targetDevice);
    }

    /**
     * 设置图片数据源
     *
     * @param deque LinkedBlockingDeque<byte[]>
     */
    public void setImageDataSource(LinkedBlockingDeque<byte[]> deque) {
        mImageDeque = deque;
    }

    /**
     * 设置音频数据源
     *
     * @param deque LinkedBlockingDeque<byte[]>
     */
    public void setAudioDataSource(LinkedBlockingDeque<byte[]> deque) {
        mAudioDeque = deque;
    }

    /**
     * 设置图片接收监听
     *
     * @param receiver ITalkbackReceiver
     */
    public void setImageReceiver(ITalkbackReceiver receiver) {
        mImageReceiver = receiver;
    }

    public void setAudioReceiver(ITalkbackReceiver receiver) {
        mAudioReceiver = receiver;
    }

    /**
     * 图片分辨率
     */
    public class ImageSize {

        private int width;
        private int height;

        ImageSize(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Json数据信息
     */
    public class DataEntity {

        private String sourcePerson;
        private String sourceDevice;
        private String targetPerson;
        private String targetDevice;

        DataEntity(String sourcePerson, String sourceDevice, String targetPerson, String targetDevice) {
            this.sourcePerson = sourcePerson;
            this.sourceDevice = sourceDevice;
            this.targetPerson = targetPerson;
            this.targetDevice = targetDevice;
        }
    }
}
