package com.vanda.javacv.demo.audio;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;

import com.vanda.javacv.demo.im.AVReceiver;
import com.vanda.javacv.demo.im.AudioReceiver;
import com.vanda.javacv.demo.im.IMediaReceiver;

/**
 * Date    20/03/2018
 * Author  WestWang
 */

public class AudioPlayer implements IAudioPlayer {

    private static final String TAG = AudioPlayer.class.getSimpleName();
    private AudioParam mAudioParam;
    private byte[] mData;
    private long mLength;
    private AudioTrack mAudioTrack;
    private Handler mHandler;

    private int mState = PlayState.STATE_UNINITIALIZED;
    private int mPrimePlaySize = 0;
    private int mPlayOffset = 0;

    private AVReceiver mAVReceiver;
    private AudioReceiver mAudioReceiver;

    public AudioPlayer() {
        mAudioParam = new AudioParam();
    }

    public AudioPlayer(Handler handler) {
        mHandler = handler;
    }

    public AudioPlayer(Handler handler, AudioParam param) {
        mHandler = handler;
        mAudioParam = param;
    }

    /**
     * 设置参数
     *
     * @param param
     */
    @Override
    public void setAudioParam(AudioParam param) {
        mAudioParam = param;
    }

    /**
     * 设置数据源
     *
     * @param data
     */
    @Override
    public void setDataSource(byte[] data) {
        if (data != null) {
            mData = data;
            mLength += data.length;
        }
    }

    /**
     * 准备
     *
     * @return
     */
    @Override
    public boolean prepare() {
        setPlayState(PlayState.STATE_PREPARE);
        createAudioTrack();
        return true;
    }

    /**
     * 设置播放状态
     *
     * @param state
     */
    @Override
    public void setPlayState(int state) {
        mState = state;
    }

    /**
     * 开始播放
     */
    @Override
    public void onStart() {

    }

    /**
     * 暂停
     */
    @Override
    public void onPause() {

    }

    /**
     * 播放完成
     */
    @Override
    public void onComplete() {

    }

    /**
     * 释放资源
     */
    @Override
    public void release() {
        if (mAVReceiver != null) {
            mAVReceiver.stop();
            mAVReceiver = null;
        }
        if (mAudioReceiver != null) {
            mAudioReceiver.stop();
            mAudioReceiver = null;
        }
        if (mAudioTrack != null) {
            if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                mAudioTrack.stop();
            }
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }

    /**
     * 创建AudioTrack
     */
    private void createAudioTrack() {
        // 最小缓冲区
        int minBuffSize = AudioTrack.getMinBufferSize(mAudioParam.sampleRate, mAudioParam.channels, mAudioParam.bit);
        mPrimePlaySize = minBuffSize * 2;
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                mAudioParam.sampleRate,
                mAudioParam.channels,
                mAudioParam.bit,
                mPrimePlaySize,
                AudioTrack.MODE_STREAM);
        mAudioTrack.setVolume(1.0f);
        // 自动开始播放
        startReceive();
    }

    public void setAudioReceiver(AVReceiver receiver) {
        mAVReceiver = receiver;
    }

    public void setAudioReceiver(AudioReceiver receiver) {
        mAudioReceiver = receiver;
    }

    /**
     * 接收PCM音频数据并写入到AudioTrack
     */
    private void startReceive() {
        mAudioTrack.play();
        if (mAVReceiver != null) {
            mAVReceiver.setAudioReceiver(new IMediaReceiver() {
                @Override
                public void onReceive(byte[] data) {
                    mAudioTrack.write(data, 0, data.length);
                }
            });
        }
        if (mAudioReceiver != null) {
            mAudioReceiver.setAudioReceiver(new IMediaReceiver() {
                @Override
                public void onReceive(byte[] data) {
                    mAudioTrack.write(data, 0, data.length);
                }
            });
        }
    }

}
