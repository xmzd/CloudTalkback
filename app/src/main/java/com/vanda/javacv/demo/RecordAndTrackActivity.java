package com.vanda.javacv.demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.vanda.javacv.demo.audio.AacDecoder;
import com.vanda.javacv.demo.audio.AacEncoder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Date    20/03/2018
 * Author  WestWang
 */

public class RecordAndTrackActivity extends AppCompatActivity {

    private static final String TAG = RecordAndTrackActivity.class.getSimpleName();
    private int sample = 44100;
    private int channelIn = AudioFormat.CHANNEL_IN_MONO;
    private int channelOut = AudioFormat.CHANNEL_OUT_MONO;
    private int format = AudioFormat.ENCODING_PCM_16BIT;

    private int bufferSize = AudioRecord.getMinBufferSize(sample, channelIn, format);

    private AudioRecord recorder;
    private boolean isRecording;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_record_and_track);

        findViewById(R.id.btn_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                record();
            }
        });

        findViewById(R.id.btn_track).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                track();
            }
        });
    }

    private void record() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            doRecord();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 200);
        }
    }

    private void doRecord() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sample, channelIn, format, bufferSize);
        recorder.startRecording();
        isRecording = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
//                File file = new File(path);
//                if (file.exists()) {
//                    file.delete();
//                }
                MulticastSocket ms;
                try {
                    ms = new MulticastSocket();
                    ms.setTimeToLive(32);
                    InetAddress address = InetAddress.getByName("239.0.0.100");

                    byte[] data = new byte[bufferSize];

//                    int minSize = AudioTrack.getMinBufferSize(sample, channelOut, format);
//                    AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, sample, channelOut, format, minSize * 10, AudioTrack.MODE_STREAM);
//                    at.play();
//                FileOutputStream os = null;
                    Log.e(TAG, "start record and track....");
                    AacEncoder aacEncoder = new AacEncoder();
                    try {
//                    os = new FileOutputStream(path);
                        while (isRecording) {
                            int flag = recorder.read(data, 0, bufferSize);
//                            at.write(data, 0, data.length);
                            if (AudioRecord.ERROR_INVALID_OPERATION != flag) {
                                // 转AAC编码
                                byte[] aacData = aacEncoder.encode(data);
                                DatagramPacket dataPacket = new DatagramPacket(aacData, aacData.length, address, 9991);
                                Log.e(TAG, "data length: " + dataPacket.getLength());
                                ms.send(dataPacket);
                            }
//                        os.write(data, 0, bufferSize);
                        }
//                    os.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//                    at.stop();
//                    at.release();

                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }).start();
    }

    private void track() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "start reading audio data....");
                int minSize = AudioTrack.getMinBufferSize(sample, channelOut, format);
                AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, sample, channelOut, format, minSize * 10, AudioTrack.MODE_STREAM);
                try {

                    MulticastSocket ms = new MulticastSocket(9991);
                    InetAddress address = InetAddress.getByName("239.0.0.100");
                    ms.joinGroup(address);

                    AacDecoder aacDecoder = new AacDecoder();

                    byte buff[] = new byte[bufferSize];

                    at.play();
                    while (true) {
                        DatagramPacket dp = new DatagramPacket(buff, bufferSize);
                        Log.e(TAG, "..............................");
                        ms.receive(dp);
                        byte[] dpData = dp.getData();
                        int dpLen = dp.getLength();
                        Log.e(TAG, "read data length...." + dpLen);


                        byte[] out = aacDecoder.decode(dpData);
                        at.write(out, 0, out.length);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                at.stop();
                at.release();
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                doRecord();
            } else {
                Toast.makeText(this, "没得权限啊！！！！", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
