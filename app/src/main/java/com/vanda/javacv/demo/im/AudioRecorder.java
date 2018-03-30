package com.vanda.javacv.demo.im;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.vanda.javacv.demo.utils.Logger;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * Date    28/03/2018
 * Author  WestWang
 * 音频采集
 */

public class AudioRecorder {

    private static final String TAG = AudioRecorder.class.getSimpleName();
    private int mSampleRate = 22050;
    private int mChannelIn = AudioFormat.CHANNEL_IN_MONO;
    private int mFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int mBufferSize;
    private AudioRecord mAudioRecord;
    private boolean mIsRecording = false;
    private LinkedBlockingDeque<byte[]> mQueue;

    public AudioRecorder() {
        mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelIn, mFormat);
        mQueue = new LinkedBlockingDeque<>();
    }

    public AudioRecorder(int sampleRate, int channelIn, int format) {
        mSampleRate = sampleRate;
        mChannelIn = channelIn;
        mFormat = format;
        mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelIn, mFormat);
        mQueue = new LinkedBlockingDeque<>();
    }

    /**
     * 实例化AudioRecord
     */
    private void createAudioRecord() {
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRate, mChannelIn, mFormat, mBufferSize);
    }

    public LinkedBlockingDeque<byte[]> getDeque() {
        return mQueue;
    }

    /**
     * 采集
     */
    public void startRecording() {
        if (mAudioRecord == null) {
            createAudioRecord();
        }
        if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            mAudioRecord.stop();
        }
        mQueue.clear();
        mAudioRecord.startRecording();
        mIsRecording = true;
        // 启动线程开始采集
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] data = new byte[mBufferSize];
                    while (mIsRecording) {
                        int flag = mAudioRecord.read(data, 0, mBufferSize);
                        if (AudioRecord.ERROR_INVALID_OPERATION != flag) {
                            mQueue.put(data);
                        }
                    }
                } catch (Exception e) {
                    Logger.e(TAG, "error, recording audio. " + e.getLocalizedMessage());
                }
            }
        }).start();
    }

    public void stop() {
        mIsRecording = false;
        mAudioRecord.stop();
        mAudioRecord.release();
    }
}
