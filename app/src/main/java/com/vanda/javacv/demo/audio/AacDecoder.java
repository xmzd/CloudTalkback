package com.vanda.javacv.demo.audio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Date    21/03/2018
 * Author  WestWang
 * AAC-->PCM
 */

public class AacDecoder {

    private MediaCodec mMediaCodec;

    private ByteBuffer[] mBuffIn;
    private ByteBuffer[] mBuffOut;
    private int mCount = 0;

    private MediaCodec.BufferInfo mBufferInfo;
    // 时间基数
    private int mPresentationTimeUs = 0;
    private ByteArrayOutputStream mBaos = new ByteArrayOutputStream();

    public AacDecoder() {
        try {
            mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
        final int kSampleRates[] = {8000, 11025, 22050, 44100, 48000};
        // 比特率, 声音中的比特率是指将模拟声音信号转换成数字声音信号后，单位时间内的二进制数据量，是间接衡量音频质量的一个指标
        final int kBitRates[] = {64000, 96000, 128000};
        // 初始化, 此格式使用的音频编码技术、音频采样率、使用此格式的音频信道数（单声道为 1，立体声为 2）
        MediaFormat mediaFormat = new MediaFormat();
        mediaFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, kSampleRates[3]);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, kBitRates[1]);

        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        // 用来标记AAC是否有adts头，1->有
        mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
        // ByteBuffer key（暂时不了解该参数的含义，但必须设置）
        byte[] data = new byte[]{(byte) 0x11, (byte) 0x90};
        ByteBuffer csd_0 = ByteBuffer.wrap(data);
        mediaFormat.setByteBuffer("csd-0", csd_0);
        // 设置相关参数
        mMediaCodec.configure(mediaFormat, null, null, 0);
        // 开始
        mMediaCodec.start();

        mBuffIn = mMediaCodec.getInputBuffers();
        mBuffOut = mMediaCodec.getOutputBuffers();
        mBufferInfo = new MediaCodec.BufferInfo();
    }

    public byte[] decode(byte[] inputData) throws IOException {
        long kTimeOutUs = 0;
        // 返回一个包含有效数据的input buffer的index,-1->不存在
        int inputBufIndex = mMediaCodec.dequeueInputBuffer(kTimeOutUs);
        if (inputBufIndex >= 0) {
            // 获取当前的ByteBuffer
            ByteBuffer dstBuf = mBuffIn[inputBufIndex];
            // 清空ByteBuffer
            dstBuf.clear();
            // 填充数据
            dstBuf.put(inputData);
            dstBuf.limit(inputData.length);

            // 计算pts
            long pts = computePresentationTime(mPresentationTimeUs);

            // 将指定index的input buffer提交给解码器
            mMediaCodec.queueInputBuffer(inputBufIndex, 0, inputData.length, 0, 0);
            mPresentationTimeUs += 1;
        }
        // 返回一个output buffer的index，-1->不存在
        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, kTimeOutUs);
        if (outputBufferIndex < 0) {
            // 记录解码失败的次数
            mCount++;
        }
        ByteBuffer outputBuffer;
        while (outputBufferIndex >= 0) {
            // 获取解码后的ByteBuffer
            outputBuffer = mBuffOut[outputBufferIndex];
            // 用来保存解码后的数据
            byte[] outData = new byte[mBufferInfo.size];
            outputBuffer.get(outData);
            // 清空缓存
            outputBuffer.clear();
            mBaos.write(outData);
            // 释放已经解码的buffer
            mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            // 解码未解完的数据
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, kTimeOutUs);
        }
        byte[] out = mBaos.toByteArray();

        // 写完以后重置输出流，否则数据会重复
        mBaos.flush();
        mBaos.reset();

        // 返回
        return out;
    }

    /**
     * 计算PTS，实际上这个pts对应音频来说作用并不大，设置成0也是没有问题的
     */
    private long computePresentationTime(long frameIndex) {
        return frameIndex * 90000 * 1024 / 44100;
    }
}
