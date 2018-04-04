package com.vanda.javacv.demo.im.talkback;

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
    private LinkedBlockingDeque<byte[]> mDeque;

    public AudioRecorder() {
        init();
    }

    public AudioRecorder(int sampleRate, int channelIn, int format) {
        mSampleRate = sampleRate;
        mChannelIn = channelIn;
        mFormat = format;
        init();
    }

    private void init() {
        mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelIn, mFormat);
        mDeque = new LinkedBlockingDeque<>();
    }

    /**
     * 实例化AudioRecord
     */
    private void createAudioRecord() {
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                mSampleRate, mChannelIn, mFormat, mBufferSize);
    }

    public LinkedBlockingDeque<byte[]> getDeque() {
        return mDeque;
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
        mDeque.clear();
        mAudioRecord.startRecording();
        mIsRecording = true;
        // 启动线程开始采集
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] data = new byte[mBufferSize];
                    while (mIsRecording) {
                        if (mAudioRecord != null && mDeque != null) {
                            int flag = mAudioRecord.read(data, 0, mBufferSize);
                            if (AudioRecord.ERROR_INVALID_OPERATION != flag) {
                                mDeque.put(data);
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.e(TAG, "error, recording audio. " + e.getLocalizedMessage());
                }
            }
        }).start();
    }

    /**
     * 停止采集，释放资源
     */
    public void stopAndRelease() {
        mIsRecording = false;
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;
        mDeque.clear();
        mDeque = null;
    }
}
