package com.vanda.javacv.demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.vanda.javacv.demo.audio.AudioPlayer;
import com.vanda.javacv.demo.im.AVReceiver;
import com.vanda.javacv.demo.im.AudioEmitter;
import com.vanda.javacv.demo.im.AudioReceiver;
import com.vanda.javacv.demo.im.AudioRecorder;
import com.vanda.javacv.demo.im.IMConstants;
import com.vanda.javacv.demo.im.IMediaReceiver;
import com.vanda.javacv.demo.im.ImageEmitter;
import com.vanda.javacv.demo.im.ImageReceiver;
import com.vanda.javacv.demo.im.socket.IMediaConversation;
import com.vanda.javacv.demo.im.socket.Message;
import com.vanda.javacv.demo.im.socket.SocketClient;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int CODE_CAMERA = 100;
    private static final int CODE_RECORD = 200;

    private ImageView mImageView;
    private FrameLayout mContainer;
    private PreviewView mPreviewView;

    private AVReceiver mAVReceiver;
    private AudioReceiver mAudioReceiver;
    private ImageReceiver mImageReceiver;

    private AudioRecorder mAudioRecorder;
    private ImageEmitter mImageEmitter;
    private AudioEmitter mAudioEmitter;
    private AudioPlayer mAudioPlayer;

    private SocketClient mClient;
    private String mHost = IMConstants.HOST;
    private int mPort = 9998;

    LinearLayout ll;
    Button show;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        mImageView = findViewById(R.id.image);
        mContainer = findViewById(R.id.container);
        ll = findViewById(R.id.ll);
        show = findViewById(R.id.show);
        show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ll.getVisibility() == View.VISIBLE) {
                    ll.setVisibility(View.GONE);
                    show.setText("Show");
                } else {
                    ll.setVisibility(View.VISIBLE);
                    show.setText("Hide");
                }
            }
        });

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTalk();
            }
        });

        mClient = new SocketClient(mHost, mPort);
        mClient.setMediaConversation(new IMediaConversation() {
            @Override
            public void openConversation() {
//                openTalk();
            }

            @Override
            public void closeConversation() {
//                closeTalk();
            }
        });

        findViewById(R.id.connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mClient.connect();
                    }
                }).start();
            }
        });

        findViewById(R.id.signIn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message msg = new Message();
                msg.setMessageId(String.valueOf(System.currentTimeMillis()));
                msg.setMessageType(5);
                msg.setSourcePerson(IMConstants.SOURCE_PERSON);
                msg.setSourceDevice(IMConstants.SOURCE_DEVICE);
                mClient.sendMessage(msg);
            }
        });
        findViewById(R.id.signOut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message msg = new Message();
                msg.setMessageId(String.valueOf(System.currentTimeMillis()));
                msg.setMessageType(6);
                msg.setSourcePerson(IMConstants.SOURCE_PERSON);
                msg.setSourceDevice(IMConstants.SOURCE_DEVICE);
                mClient.sendMessage(msg);
            }
        });
        findViewById(R.id.sendMessage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message msg = new Message();
                msg.setMessageId(String.valueOf(System.currentTimeMillis()));
                msg.setMessageType(1);
                msg.setSourcePerson(IMConstants.SOURCE_PERSON);
                msg.setSourceDevice(IMConstants.SOURCE_DEVICE);
                msg.setTargetRoom("room1");
                msg.setMessageContext("Socket端user1发送消息--" + System.currentTimeMillis());
                mClient.sendMessage(msg);
            }
        });

        findViewById(R.id.voiceCall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                 * "messageId": "消息唯一id",
                 "messageType": 8,
                 "sourcePerson": "视频语音请求的发起者",
                 "sourceDevice": "视频语音请求的发起设备",
                 "targetPerson": "视频语音请求的目标用户",
                 "voiceId": "视频语音回话ID，如果没有传入则会生成一个",
                 */
                Message msg = new Message();
                msg.setMessageId(String.valueOf(System.currentTimeMillis()));
                msg.setMessageType(8);
                msg.setSourcePerson(IMConstants.SOURCE_PERSON);
                msg.setSourceDevice(IMConstants.SOURCE_DEVICE);
                msg.setTargetPerson("user2");
                mClient.sendMessage(msg);
            }
        });
        findViewById(R.id.videoCall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                 * "messageId": "消息唯一id",
                 "messageType": 7,
                 "sourcePerson": "视频语音请求的发起者",
                 "sourceDevice": "视频语音请求的发起设备",
                 "targetPerson": "视频语音请求的目标用户",
                 "voiceId": "视频语音回话ID，如果没有传入则会生成一个",
                 */
                Message msg = new Message();
                msg.setMessageId(String.valueOf(System.currentTimeMillis()));
                msg.setMessageType(7);
                msg.setSourcePerson(IMConstants.SOURCE_PERSON);
                msg.setSourceDevice(IMConstants.SOURCE_DEVICE);
                msg.setTargetPerson("user2");
                mClient.sendMessage(msg);
            }
        });
        findViewById(R.id.ackCall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 音视频请求应答
                mClient.ackMediaCall();
            }
        });
        findViewById(R.id.endCall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 结束音视频通信
                mClient.endMediaCall();
            }
        });
    }

    private void openCamera() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            videoShoot();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CODE_CAMERA);
        }
    }

    private void record() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            doRecord();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, CODE_RECORD);
        }
    }

    private void doRecord() {
        mAudioRecorder = new AudioRecorder();
        mAudioRecorder.startRecording();

        mAudioEmitter = new AudioEmitter(mAudioRecorder.getDeque());
        mAudioEmitter.setSrcName(IMConstants.SOURCE_PERSON);
        mAudioEmitter.setSrcDevice(IMConstants.TARGET_DEVICE);
        mAudioEmitter.setDestName(IMConstants.SOURCE_PERSON);
        mAudioEmitter.setDestDevice(IMConstants.SOURCE_DEVICE);
//        mAudioEmitter.start();
    }

    private void videoShoot() {
        ViewGroup.LayoutParams lp = mContainer.getLayoutParams();
        int width = lp.width;
        lp.height = (int) (width * 4.0f / 3.0f);
        mContainer.setLayoutParams(lp);
        mPreviewView = new PreviewView(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        mContainer.addView(mPreviewView, params);
        // 开始预览
        mPreviewView.startPreview();
        // 发送视频
        mImageEmitter = new ImageEmitter(mPreviewView.getDeque());
        mImageEmitter.setWidth(mPreviewView.getPreviewWidth());
        mImageEmitter.setHeight(mPreviewView.getPreviewHeight());
        mImageEmitter.setSrcName(IMConstants.SOURCE_PERSON);
        mImageEmitter.setSrcDevice(IMConstants.SOURCE_DEVICE);
        mImageEmitter.setDestName(IMConstants.TARGET_PERSON);
        mImageEmitter.setDestDevice(IMConstants.TARGET_DEVICE);
        mImageEmitter.start();
    }

    private void startReceive() {
        // 视频接收
//        mImageReceiver = new ImageReceiver();
//        mImageReceiver.setImageReceiver(new IMediaReceiver() {
//            @Override
//            public void onReceive(byte[] data) {
//                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//                mImageView.setImageBitmap(bitmap);
//            }
//        });
//        mImageReceiver.start();

        mAVReceiver = new AVReceiver();
        mAVReceiver.setImageReceiver(new IMediaReceiver() {
            @Override
            public void onReceive(byte[] data) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                mImageView.setImageBitmap(bitmap);
            }
        });
        // 音频接收
//        mAudioReceiver = new AudioReceiver();
        mAudioPlayer = new AudioPlayer();
//        player.setAudioReceiver(mAudioReceiver);
        mAudioPlayer.setAudioReceiver(mAVReceiver);
        mAudioPlayer.prepare();
//        mAudioReceiver.start();

        mAVReceiver.start();
    }

    /**
     * 开启会话
     */
    private void openTalk() {
        openCamera();
        record();
        startReceive();
    }

    /**
     * 结束会话
     */
    private void closeTalk() {
        mImageEmitter.stop();
        mAudioEmitter.stop();
        mAVReceiver.stop();
        mAudioPlayer.release();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CODE_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    videoShoot();
                } else {
                    Toast.makeText(this, "没得相机权限啊！！！！", Toast.LENGTH_SHORT).show();
                }
                break;
            case CODE_RECORD:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    doRecord();
                } else {
                    Toast.makeText(this, "没得录音权限啊！！！！", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
