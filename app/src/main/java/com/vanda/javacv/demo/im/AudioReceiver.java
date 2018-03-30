package com.vanda.javacv.demo.im;

import com.vanda.javacv.demo.utils.Logger;

import java.io.IOException;
import java.net.DatagramPacket;

/**
 * Date    27/03/2018
 * Author  WestWang
 * 音频接收者
 */

public class AudioReceiver extends MediaReceiver {

    private static final String TAG = AudioReceiver.class.getSimpleName();
    private IMediaReceiver mAudioReceiver;

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
        return IMConstants.AUDIO_RECEIVE_PORT;
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
        while (!mShutdown) {
            // 所以单帧需要由多个udp报文进行合并
            DatagramPacket dp = new DatagramPacket(buf, packetLength);
            mSocket.receive(dp);
            byte[] dpData = dp.getData();
            int dpLen = dp.getLength();
            Logger.e(TAG, "audio data received, length: " + dpLen + ", port: " + getPort());
            // 解udp包
            byte[] header = new byte[headerLength];
            System.arraycopy(dpData, 0, header, 0, headerLength);
            byte[] json = new byte[header[8]];
            System.arraycopy(header, 9, json, 0, header[8]);
            Logger.e(TAG, "Json data: " + new String(json));
            // 音频数据
            byte[] content = new byte[dpLen - headerLength];
            System.arraycopy(dpData, headerLength, content, 0, content.length);
            // 开始合并
            byte[] destData = new byte[matAudio.length + content.length];
            System.arraycopy(matAudio, 0, destData, 0, matAudio.length);
            System.arraycopy(content, 0, destData, matAudio.length, content.length);
            matAudio = destData;
            if (header[0] == 0x01 << 4) {
                // 音频、不拆包
                try {
                    if (mAudioReceiver != null) {
                        mAudioReceiver.onReceive(matAudio);
                    }
                } catch (Exception e) {
                    Logger.e(TAG, "deal data error. " + e.getLocalizedMessage());
                } finally {
                    // 复位
                    matAudio = new byte[0];
                }
            } else if (header[0] == (0x01 << 4 | 0x01)) {
                // 音频、需要拆包
                if (header[1] >> 4 == 0x01) {
                    // 当前最后一个包
                    try {
                        if (mAudioReceiver != null) {
                            mAudioReceiver.onReceive(matAudio);
                        }
                    } catch (Exception e) {
                        Logger.e(TAG, "deal data error. " + e.getLocalizedMessage());
                    } finally {
                        // 复位
                        matAudio = new byte[0];
                    }
                }
            }
        }
    }

    public void setAudioReceiver(IMediaReceiver receiver) {
        mAudioReceiver = receiver;
    }
}
