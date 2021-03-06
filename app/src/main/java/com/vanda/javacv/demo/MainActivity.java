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

import com.vanda.javacv.demo.im.talkback.AudioPlayer;
import com.vanda.javacv.demo.im.talkback.AudioRecorder;
import com.vanda.javacv.demo.im.IMConstants;
import com.vanda.javacv.demo.im.talkback.ITalkbackReceiver;
import com.vanda.javacv.demo.im.talkback.ITalkbackConversation;
import com.vanda.javacv.demo.im.socket.Message;
import com.vanda.javacv.demo.im.socket.SocketClient;
import com.vanda.javacv.demo.im.talkback.TalkbackTransfer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int CODE_CAMERA = 100;
    private static final int CODE_RECORD = 200;

    private ImageView mImageView;
    private FrameLayout mContainer;
    private PreviewView mPreviewView;

    private AudioRecorder mAudioRecorder;
    private AudioPlayer mAudioPlayer;
    private TalkbackTransfer mTransfer;

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
        findViewById(R.id.closeAudio).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioPlayer.release();
            }
        });

        // 开启
        findViewById(R.id.open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTalk();
            }
        });
        // 关闭
        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeTalk();
            }
        });

        mClient = new SocketClient(mHost, mPort);
        mClient.setMediaConversation(new ITalkbackConversation() {
            @Override
            public void openConversation() {
                openTalk();
            }

            @Override
            public void closeConversation() {
                closeTalk();
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

        mTransfer.setAudioDataSource(mAudioRecorder.getDeque());
        mTransfer.startEmitAudio();
    }

    private void videoShoot() {
        if (mPreviewView == null || mPreviewView.getParent() == null) {
            // 调整预览界面
            ViewGroup.LayoutParams lp = mContainer.getLayoutParams();
            int width = lp.width;
            lp.height = (int) (width * 4.0f / 3.0f);
            mContainer.setLayoutParams(lp);

            mPreviewView = new PreviewView(this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            mContainer.addView(mPreviewView, params);
        }
        // 开始预览
        mPreviewView.startPreview();
        // 发送视频
        mTransfer.setImageSize(mPreviewView.getPreviewWidth(), mPreviewView.getPreviewHeight());
        mTransfer.setImageDataSource(mPreviewView.getDeque());
        mTransfer.startEmitImage();
    }

    private void startReceive() {
        mAudioPlayer = new AudioPlayer();
        mAudioPlayer.play();
        // 视频接收
        mTransfer.setImageReceiver(new ITalkbackReceiver() {
            @Override
            public void onReceive(byte[] data) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                mImageView.setImageBitmap(bitmap);
            }
        });
        // 音频接收
        mTransfer.setAudioReceiver(mAudioPlayer);
        mTransfer.startReceive();
    }

    /**
     * 开启会话
     */
    private void openTalk() {
        mTransfer = new TalkbackTransfer();
        mTransfer.setDataEntity(IMConstants.SOURCE_PERSON,
                IMConstants.SOURCE_DEVICE,
                IMConstants.TARGET_PERSON,
                IMConstants.TARGET_DEVICE);
        openCamera();
        record();
        startReceive();
    }

    /**
     * 结束会话
     */
    private void closeTalk() {
        mPreviewView.stopPreview();
        mAudioRecorder.stopAndRelease();
        mTransfer.stopAndRelease();
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
