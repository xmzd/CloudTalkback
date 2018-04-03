package com.vanda.javacv.demo.im.socket;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;

import com.vanda.javacv.demo.im.IMConstants;
import com.vanda.javacv.demo.im.PacketUtil;
import com.vanda.javacv.demo.utils.Logger;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    private static final int packetLength = 5000;
    private static final int headerLength = 200;
    private static final int contentLength = 4800;

    private LinkedBlockingDeque<byte[]> mImageDeque;
    private long mFrameCounter = 0;

    public void setImageDataSource(LinkedBlockingDeque<byte[]> deque) {
        mImageDeque = deque;
    }

    public TalkbackTransfer() {
        init();
    }

    private void init() {
        try {
            mInetAddress = InetAddress.getByName(IMConstants.HOST);
            mSocket = new MulticastSocket(IMConstants.LOCAL_PORT);
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

    private void emitImage() throws IOException {
        while (!mShutdown) {
            try {
                byte[] data = mImageDeque.take();
                // 转换成YuvImage
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
        packetImage(data);
    }

    private void packetImage(byte[] data) throws IOException {
        /**
         * 第1个字符，高4位 0000：1-->语音，0-->视频
         * 第1个字符，低4位 0000：1-->拆包，0-->不拆包
         *
         * 第2个字符 至 第9个字符，这8个byte（长整型）表示本帧编号，编号从1开始，每一个后续帧的编号+1
         *
         * 第10个字符，高4位 0000：如果拆包，这里记录了拆包的总数，如果不拆包，则没有意义
         * 第10个字符，低4位 0000：如果拆包，则记录当前包在整个拆包中的序号，从1开始；如果不拆包，则没有意义
         *
         *
         * 第11个字符，共八位，0000 0000 记录包中从第12个字符开始，json数据的长度，最长188
         * 第12个字符～第200个字符：json数据-->rp：发送人，rd：发送设备，tp：接收人，td接收设备
         */
        int length = data.length;
        Logger.d(TAG, "image data original length: " + length);
        // 构造json数据
        byte[] jsonBytes = getJsonBytes();
        if (jsonBytes == null) {
            return;
        }
        // 第11个字符，json长度
        byte byte10 = (byte) (jsonBytes.length & 0xFF);
        // 构建第二个字符至第9个字符
        // 帧计数器+1
        mFrameCounter++;
        byte[] frameCount = PacketUtil.longToByte(mFrameCounter);
        // 构建第一个字符、第10个字符
        byte byte0 = 0;
        byte byte9 = 0;
        if (length > contentLength) {
            // 需要拆包
            int count = (length % contentLength == 0) ? (length / contentLength) : ((length / contentLength) + 1);
            // 高4位
            byte9 = (byte) ((count & 0xFF) << 4);
            for (int i = 0; i < count; i++) {
                byte0 = 0x01;
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
                send(jsonBytes, byte10, frameCount, byte0, result, content);
            }
        } else {
            // 不需要拆包
            byte0 = 0x00;
            byte9 = 0x00;
            // 发送
            send(jsonBytes, byte10, frameCount, byte0, byte9, data);
        }
    }

    /**
     * 发送数据
     */
    private void send(byte[] jsonBytes, byte byte10, byte[] frameCount, byte byte0, byte byte9, byte[] content) throws IOException {
        byte[] header = getHeaderBytes(jsonBytes, byte10, frameCount, byte0, byte9);
        // 拼接一个完整的udp包
        byte[] result = new byte[200 + content.length];
        System.arraycopy(header, 0, result, 0, 200);
        System.arraycopy(content, 0, result, 200, content.length);
        // 打包发送
        DatagramPacket dataPacket = new DatagramPacket(result, result.length, mInetAddress, getPort());
        Logger.d(TAG, "emit image data, length: " + dataPacket.getLength() + ", port: " + getPort());
        mSocket.send(dataPacket);
    }

    /**
     * 构造Json数据(rp：发送人，rd：发送设备，tp：接收人，td接收设备)
     *
     * @return byte[]
     */
    byte[] getJsonBytes() {
        try {
            JSONObject object = new JSONObject();
            object.put("rp", mDataEntity.sourcePerson);
            object.put("rd", mDataEntity.sourceDevice);
            object.put("tp", mDataEntity.targetPerson);
            object.put("td", mDataEntity.targetDevice);
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

    private void emitAudio() throws IOException {

    }

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

    private void receive() throws IOException {

    }

    public void startEmitImage() {
        if (!mIsInitialized) {
            Logger.e(TAG, "startEmitImage, init error.");
            return;
        }
        mShutdown = false;
        new Thread(mEmitImageRunnable).start();
    }

    public void startEmitAudio() {
        if (!mIsInitialized) {
            Logger.e(TAG, "startEmitAudio, init error.");
            return;
        }
        mShutdown = false;
        new Thread(mEmitAudioRunnable).start();
    }

    public void startReceive() {
        if (!mIsInitialized) {
            Logger.e(TAG, "startReceive, init error.");
            return;
        }
        mShutdown = false;
        new Thread(mReceiveRunnable).start();
    }

    public void stop() {
        if (!mIsInitialized) {
            Logger.e(TAG, "stop, init error.");
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
     * 获取MulticastSocket
     *
     * @return MulticastSocket
     */
    public int getPort() {
        return IMConstants.IMAGE_EMIT_PORT;
    }

    public void setImageSize(ImageSize imageSize) {
        mImageSize = imageSize;
    }

    public void setDataEntity(DataEntity dataEntity) {
        mDataEntity = dataEntity;
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
