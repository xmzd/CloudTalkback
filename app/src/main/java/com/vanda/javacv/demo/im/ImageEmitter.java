package com.vanda.javacv.demo.im;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;

import com.vanda.javacv.demo.im.utils.PacketUtil;
import com.vanda.javacv.demo.utils.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Date    28/03/2018
 * Author  WestWang
 * 图片发射器
 */

public class ImageEmitter extends MediaEmitter {

    private static final String TAG = ImageEmitter.class.getSimpleName();
    private int mWidth = 320;
    private int mHeight = 240;
    private LinkedBlockingDeque<byte[]> mDeque;

    public ImageEmitter(LinkedBlockingDeque<byte[]> deque) {
        super();
        mDeque = deque;
    }

    public void setWidth(int width) {
        this.mWidth = width;
    }

    public void setHeight(int height) {
        this.mHeight = height;
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
        return IMConstants.IMAGE_EMIT_PORT;
    }

    /**
     * 发射数据，运行在子线程
     */
    @Override
    protected void emitData() throws IOException {
        while (!mShutdown) {
            try {
                byte[] data = mDeque.take();
//                int[] pixels = new int[mWidth * mHeight];
//                decodeYUV420(pixels, data, mWidth, mHeight);
//                Bitmap bitmap = Bitmap.createBitmap(pixels, mWidth, mHeight, Bitmap.Config.ARGB_4444);
                // 转换成YuvImage
                YuvImage image = new YuvImage(data, ImageFormat.NV21, mWidth, mHeight, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // 压缩jpg
                image.compressToJpeg(new Rect(0, 0, mWidth, mHeight), 50, baos);
//                packetImage(baos.toByteArray());
                Bitmap bitmap = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size());
                rotateBitmap(bitmap);
                baos.close();
            } catch (InterruptedException e) {
                Logger.e(TAG, "error, take image data from deque. " + e.getLocalizedMessage());
            }
        }
    }

    private void decodeYUV420(int[] rgb, byte[] yuv420, int width, int height) {
        final int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420[uvp++]) - 128;
                    u = (0xff & yuv420[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0;
                else if (r > 262143) r = 262143;
                if (g < 0) g = 0;
                else if (g > 262143) g = 262143;
                if (b < 0) b = 0;
                else if (b > 262143) b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
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
}
