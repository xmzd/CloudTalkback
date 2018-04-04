package com.vanda.javacv.demo.im.talkback;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

/**
 * Date    20/03/2018
 * Author  WestWang
 * 音频播放器
 */

public class AudioPlayer implements IAudioPlayer, ITalkbackReceiver {

    private static final String TAG = AudioPlayer.class.getSimpleName();

    private int mPlayState = PlayState.STATE_UNINITIALIZED;

    private static final int WHAT_DATA = 1;
    private static final int WHAT_PLAY = 2;
    private static final int WHAT_PAUSE = 3;
    private static final int WHAT_STOP = 4;

    private AudioParam mAudioParam;
    private AudioTrack mAudioTrack;
    private PlayerHandler mPlayerHandler;
    private PlayerHandlerThread mHandlerThread;

    private final class PlayerHandler extends Handler {

        PlayerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WHAT_DATA:
                    byte[] data = (byte[]) msg.obj;
                    mAudioTrack.write(data, 0, data.length);
                    break;
                case WHAT_PLAY:
                    mAudioTrack.play();
                    break;
                case WHAT_PAUSE:
                    mAudioTrack.pause();
                    break;
                case WHAT_STOP:
                    mAudioTrack.stop();
                    break;
            }
        }
    }

    private final class PlayerHandlerThread extends HandlerThread {

        PlayerHandlerThread(String name) {
            super(name);
        }

        @Override
        protected void onLooperPrepared() {
            createAudioTrack();
        }
    }

    public AudioPlayer() {
        mAudioParam = new AudioParam();
        mHandlerThread = new PlayerHandlerThread(TAG);
        mHandlerThread.start();
        mPlayerHandler = new PlayerHandler(mHandlerThread.getLooper());
    }

    /**
     * 设置参数
     *
     * @param param AudioParam
     */
    @Override
    public void setAudioParam(AudioParam param) {
        mAudioParam = param;
    }

    /**
     * 设置数据源
     *
     * @param data byte[]
     */
    @Override
    public void setDataSource(byte[] data) {
        if (mPlayerHandler != null) {
            Message msg = mPlayerHandler.obtainMessage();
            msg.what = WHAT_DATA;
            msg.obj = data;
            msg.sendToTarget();
        }
    }


    /**
     * 设置播放状态
     *
     * @param state int
     */
    @Override
    public void setPlayState(int state) {
        mPlayState = state;
    }

    /**
     * 开始
     */
    @Override
    public void play() {
        if (mPlayerHandler != null) {
            mPlayerHandler.sendEmptyMessage(WHAT_PLAY);
        }
    }

    /**
     * 暂停
     */
    @Override
    public void pause() {
        if (mPlayerHandler != null) {
            mPlayerHandler.sendEmptyMessage(WHAT_PAUSE);
        }
    }

    /**
     * 停止
     */
    @Override
    public void stop() {
        if (mPlayerHandler != null) {
            mPlayerHandler.sendEmptyMessage(WHAT_STOP);
        }
    }


    /**
     * 释放资源
     */
    @Override
    public void release() {
        // 退出Thread
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
        if (mAudioTrack != null) {
            if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                mAudioTrack.stop();
            }
            mAudioTrack.release();
            mAudioTrack = null;
        }
        mPlayerHandler = null;
        mAudioParam = null;
    }

    /**
     * 音频数据
     *
     * @param data byte[]
     */
    @Override
    public void onReceive(byte[] data) {
        setDataSource(data);
    }

    /**
     * 创建AudioTrack
     */
    private void createAudioTrack() {
        // 最小缓冲区
        int minBuffSize = AudioTrack.getMinBufferSize(mAudioParam.sampleRate,
                mAudioParam.channels, mAudioParam.bit) * 2;
        // 构建AudioTrack
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                mAudioParam.sampleRate,
                mAudioParam.channels,
                mAudioParam.bit,
                minBuffSize,
                AudioTrack.MODE_STREAM);
        // 设置音量
        mAudioTrack.setVolume(0.99f);
    }

}
