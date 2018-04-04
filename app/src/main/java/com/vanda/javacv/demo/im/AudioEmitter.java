package com.vanda.javacv.demo.im;

import com.vanda.javacv.demo.im.utils.PacketUtil;
import com.vanda.javacv.demo.utils.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Date    28/03/2018
 * Author  WestWang
 * 音频发射器
 */

public class AudioEmitter extends MediaEmitter {

    private static final String TAG = AudioEmitter.class.getSimpleName();
    private LinkedBlockingDeque<byte[]> mQueue;

    public AudioEmitter(LinkedBlockingDeque<byte[]> queue) {
        super();
        mQueue = queue;
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
        return IMConstants.AUDIO_EMIT_PORT;
    }

    /**
     * 发射数据，运行在子线程
     */
    @Override
    protected void emitData() throws IOException {
        while (!mShutdown) {
            try {
                byte[] data = mQueue.take();
                int length = data.length;
                /**
                 * 第1个字符，高4位：1-->语音，0-->视频
                 * 第1个字符，低4位：1-->拆包，0-->不拆包
                 * 第2个字符，高4位：1-->当前帧为最后一个包，0-->当前帧不是最后一个包
                 * 第2个字符低4位～第8个字符低4位：保留位
                 * 第9个字符，共八位，记录包中从第11个字符开始，json数据的长度，最长190
                 * 第10个字符～第200个字符：json数据-->rp：发送人，rd：发送设备，tp：接收人，td接收设备
                 */
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
                        byte0 = 0x01 << 4 | 0x01;
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
                        byte result = (byte) (byte9 & ((i + 1) & 0xFF));
                        // 发送
                        send(jsonBytes, byte10, frameCount, byte0, result, content);
                    }
                } else {
                    // 不需要拆包
                    byte0 = 0x01 << 4;
                    byte9 = 0x00;
                    // 发送
                    send(jsonBytes, byte10, frameCount, byte0, byte9, data);
                }
            } catch (InterruptedException e) {
                Logger.e(TAG, "error, take audio data from deque. " + e.getLocalizedMessage());
            }
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
        Logger.d(TAG, "emit audio data, length: " + dataPacket.getLength() + ", port: " + getPort());
        mSocket.send(dataPacket);
    }

}
