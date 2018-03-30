package com.vanda.javacv.demo.audio;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Date    21/03/2018
 * Author  WestWang
 * PCM-->AAC
 */

public class AacEncoder {

    private MediaCodec mMediaCodec;
    private String mMimeType = "OMX.google.aac.encoder";

    private ByteBuffer[] mBuffIn;
    private ByteBuffer[] mBuffOut;

    private MediaCodec.BufferInfo mBufferInfo;
    // 时间基数
    private int mPresentationTimeUs = 0;
    private ByteArrayOutputStream mBaos = new ByteArrayOutputStream();

    public AacEncoder() {
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
        final int kSampleRates[] = {8000, 11025, 22050, 44100, 48000};
        // 比特率, 声音中的比特率是指将模拟声音信号转换成数字声音信号后，单位时间内的二进制数据量，是间接衡量音频质量的一个指标
        final int kBitRates[] = {64000, 96000, 128000};
        // 初始化, 此格式使用的音频编码技术、音频采样率、使用此格式的音频信道数（单声道为 1，立体声为 2）
        MediaFormat mediaFormat = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC, kSampleRates[3], 1);
        mediaFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, kBitRates[1]);
        // 传入的数据大小
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 1024);
        // 设置相关参数
        mMediaCodec.configure(mediaFormat, null, null,
                MediaCodec.CONFIGURE_FLAG_ENCODE);
        // 开始
        mMediaCodec.start();

        mBuffIn = mMediaCodec.getInputBuffers();
        mBuffOut = mMediaCodec.getOutputBuffers();
        mBufferInfo = new MediaCodec.BufferInfo();
    }

    /**
     * 开始编码
     **/
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public byte[] encode(byte[] input) throws Exception {
        Log.e("encode", input.length + " is coming");
        // 其中需要注意的有dequeueInputBuffer（-1），参数表示需要得到的毫秒数，-1表示一直等，
        // 0表示不需要等，传0的话程序不会等待，但是有可能会丢帧。
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mBuffIn[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(input);
            inputBuffer.limit(input.length);

            // 计算pts
            long pts = computePresentationTime(mPresentationTimeUs);

            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
            mPresentationTimeUs += 1;
        }

        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);

        while (outputBufferIndex >= 0) {
            int outBitsSize = mBufferInfo.size;
            // 7 is ADTS size
            int outPacketSize = outBitsSize + 7;
            ByteBuffer outputBuffer = mBuffOut[outputBufferIndex];

            outputBuffer.position(mBufferInfo.offset);
            outputBuffer.limit(mBufferInfo.offset + outBitsSize);

            // 添加ADTS头
            byte[] outData = new byte[outPacketSize];
            addADTStoPacket(outData, outPacketSize);

            outputBuffer.get(outData, 7, outBitsSize);
            outputBuffer.position(mBufferInfo.offset);

            // 写到输出流里
            mBaos.write(outData);

            // Log.e("AudioEncoder", outData.length + " bytes written");

            mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
        }

        // 输出流的数据转成byte[]
        byte[] out = mBaos.toByteArray();

        // 写完以后重置输出流，否则数据会重复
        mBaos.flush();
        mBaos.reset();

        // 返回
        return out;
    }

    /**
     * 给编码出的aac裸流添加adts头字段
     *
     * @param packet    要空出前7个字节，否则会搞乱数据
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        // AAC LC
        int profile = 2;
        // 44.1KHz
        int freqIdx = 4;
        // CPE
        int chanCfg = 2;
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    /**
     * 计算PTS，实际上这个pts对应音频来说作用并不大，设置成0也是没有问题的
     */
    private long computePresentationTime(long frameIndex) {
        return frameIndex * 90000 * 1024 / 44100;
    }

    /**
     * 关闭释放资源
     **/
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void close() {
        try {
            mMediaCodec.stop();
            mMediaCodec.release();
            mBaos.flush();
            mBaos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
