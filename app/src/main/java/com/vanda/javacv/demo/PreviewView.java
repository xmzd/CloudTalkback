package com.vanda.javacv.demo;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.vanda.javacv.demo.utils.Logger;
import com.vanda.javacv.demo.utils.YUVUtil;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Date    28/03/2018
 * Author  WestWang
 * 相机预览
 */

public class PreviewView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = PreviewView.class.getSimpleName();
    private LinkedBlockingDeque<byte[]> mDeque = new LinkedBlockingDeque<>();
    private SurfaceHolder mHolder;
    private boolean mIsPreviewOn = false;
    private byte[] mPreviewBuffer;
    private android.hardware.Camera mCamera;
    private int mOrientation;

    public PreviewView(Context context) {
        super(context);
        init();
    }

    public PreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setKeepScreenOn(true);
        CameraHandlerThread thread = new CameraHandlerThread("CameraThread");
        synchronized (thread) {
            thread.openCamera();
        }
        // 相机支持的预览fps
        List<int[]> fps = mCamera.getParameters().getSupportedPreviewFpsRange();
        for (int[] f : fps) {
            if (f.length == 2) {
                Logger.e(TAG, "fps min=" + f[0] + ", max=" + f[1]);
            } else {
                Logger.e(TAG, "fps=" + f[0]);
            }
        }
        // 相机支持的预览分辨率
        List<Camera.Size> list = mCamera.getParameters().getSupportedPreviewSizes();
        for (Camera.Size size : list) {
            if (size != null) {
                Logger.e(TAG, "PreviewSize: " + size.width + " x " + size.height);
            }
        }
        // 相机支持的图片编码
        List<Integer> formats = mCamera.getParameters().getSupportedPreviewFormats();
        for (Integer i : formats) {
            if (i != null) {
                Logger.e(TAG, "PreviewFormat: " + i);
            }
        }
        Logger.e(TAG, "default preview format is " + mCamera.getParameters().getPreviewFormat());
        // 设置相机预览参数
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(320, 240);
        parameters.setPreviewFpsRange(20000, 20000);
        if (formats.contains(ImageFormat.YV12)) {
            parameters.setPreviewFormat(ImageFormat.YV12);
        }
        mCamera.setParameters(parameters);
        Logger.e(TAG, "now preview format is " + mCamera.getParameters().getPreviewFormat());
        // 预览缓冲区
        mPreviewBuffer = new byte[640 * 480 * 3 / 2];
        YUVUtil.preAllocateBuffers(mPreviewBuffer.length);
        mCamera.addCallbackBuffer(mPreviewBuffer);
        mCamera.setPreviewCallbackWithBuffer(this);
        // 处理图像方向
        mOrientation = 90;
        mCamera.setDisplayOrientation(mOrientation);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (camera.getParameters().getPreviewFormat() == ImageFormat.YV12) {
            data = YUVUtil.YV12toNV21(data, getPreviewWidth(), getPreviewHeight());
        }
//        if (mOrientation == 90 || mOrientation == 270) {
//            data = YUVUtil.rotateNV21(data, getPreviewWidth(), getPreviewHeight(), 90);
//        }
        try {
            mDeque.put(data);
        } catch (InterruptedException e) {
            Logger.e(TAG, "error, preview frame put to deque. " + e.getLocalizedMessage());
        }
        camera.addCallbackBuffer(mPreviewBuffer);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // 设置预览为Surface
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHolder.addCallback(null);
    }

    /**
     * 开启预览
     */
    public void startPreview() {
        if (!mIsPreviewOn && mCamera != null) {
            mIsPreviewOn = true;
            mCamera.startPreview();
        }
    }

    /**
     * 关闭预览
     */
    public void stopPreview() {
        if (mIsPreviewOn && mCamera != null) {
            mIsPreviewOn = false;
            mCamera.stopPreview();
        }
    }

    public LinkedBlockingDeque<byte[]> getDeque() {
        return mDeque;
    }

    /**
     * 预览宽度
     */
    public int getPreviewWidth() {
        if (mCamera == null) {
            return 0;
        }
        return mCamera.getParameters().getPreviewSize().width;
    }

    /**
     * 预览高度
     */
    public int getPreviewHeight() {
        if (mCamera == null) {
            return 0;
        }
        return mCamera.getParameters().getPreviewSize().height;
    }

    /**
     * 打开相机
     */
    private void openCameraOriginal(int cameraId) {
        try {
            mCamera = Camera.open(cameraId);
        } catch (Exception e) {
            Logger.e(TAG, "Camera is not available!");
        }
    }

    private class CameraHandlerThread extends HandlerThread {

        private Handler handler;

        CameraHandlerThread(String name) {
            super(name);
            start();
            handler = new Handler(getLooper());
        }

        synchronized void notifyCameraOpened() {
            notify();
        }

        void openCamera() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    openCameraOriginal(1);
                    notifyCameraOpened();
                }
            });
            try {
                wait();
            } catch (InterruptedException e) {
                Logger.d(TAG, "CameraHandlerThread wait was interrupted");
            }
        }
    }
}
