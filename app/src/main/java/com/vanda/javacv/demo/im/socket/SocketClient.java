package com.vanda.javacv.demo.im.socket;

import com.google.gson.Gson;
import com.vanda.javacv.demo.im.IMConstants;
import com.vanda.javacv.demo.utils.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Date    28/02/2018
 * Author  WestWang
 */

public class SocketClient {

    private static final String TAG = SocketClient.class.getSimpleName();
    private final String CHARSET = "utf-8";
    private String mHost;
    private int mPort;
    private Socket mSocket = null;
    private BufferedReader mReader = null;
    private BufferedWriter mWriter = null;
    private boolean mMsgAck = false;
    private int mRepeatTimes = 0;
    private Timer mTimer = null;
    private TimerTask mTimerTask = null;
    private Message mAckMsg = null;
    private IMediaConversation mConversation;

    public SocketClient(String host, int port) {
        mHost = host;
        mPort = port;
    }

    public void connect() {
        try {
            log("start connecting...");
            mSocket = new Socket(mHost, mPort);
            // 读取服务端发送过来的消息
            InputStream is = mSocket.getInputStream();
            handleReader(is);
            mReader = new BufferedReader(new InputStreamReader(is));
            // 发送消息到服务端
            mWriter = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream(), CHARSET));
            log("connect success.");
        } catch (IOException e) {
            log("Connect failed! " + e.getLocalizedMessage());
        }
    }

    /**
     * 接受消息
     */
    private void handleReader(final InputStream is) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int maxLen = 2048;
                byte[] bytes = new byte[maxLen];
                int len;
                try {
                    while ((len = (is.read(bytes, 0, maxLen))) != -1) {
                        String message = new String(bytes, 0, len);
                        log("New Message: " + message);
                        Message msg = new Gson().fromJson(message, Message.class);

                        dealMessage(msg);

                        if (msg != null && msg.getAckType() != 0) {
                            // 收到ack消息，取消重发机制
                            mMsgAck = true;
                            if (mTimer != null) {
                                mTimer.cancel();
                                mTimer = null;
                            }
                            if (mTimerTask != null) {
                                mTimerTask.cancel();
                                mTimerTask = null;
                            }
                        } else {
//                            sendAck(message);
                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace(System.out);
                }
            }
        }).start();
    }

    Message mediaMsg;
    String voiceId;
    String targetPerson;

    private void dealMessage(Message msg) {
        if (msg == null) {
            return;
        }
        voiceId = msg.getVoiceId();
        targetPerson = msg.getSourcePerson();
        int type = msg.getMessageType();
        if (type == 7 || type == 8) {
            // 音视频请求，回执
            mediaMsg = new Message();
            mediaMsg.setMessageId("9999");
            mediaMsg.setMessageType(9);
            mediaMsg.setSourcePerson(IMConstants.SOURCE_PERSON);
            mediaMsg.setSourceDevice(IMConstants.SOURCE_DEVICE);
            mediaMsg.setTargetPerson(msg.getSourcePerson());
            mediaMsg.setTargetDevice(msg.getSourceDevice());
            mediaMsg.setVoiceId(msg.getVoiceId());
        } else if (type == 9) {
            // 开启音视频会话
            if (mConversation != null) {
                mConversation.openConversation();
            }
        } else if (type == 10) {
            // 终止音视频会话
            if (mConversation != null) {
                mConversation.closeConversation();
            }
        }
    }

    public void ackMediaCall() {
        doSend(mediaMsg);
        if (mConversation != null) {
            mConversation.openConversation();
        }
    }

    /**
     * "messageId": "消息唯一id",
     * "messageType": 10 ,
     * "sourcePerson":"发起终止信号的用户",
     * "sourceDevice":"发起终止信号的设备",
     * "targetPerson": "接受终止信号的用户",
     * "repeatTimes": 0 ,
     * "voiceId":"视频语音回话ID，从服务器上发来的消息肯定携带"
     */
    public void endMediaCall() {
        Message msg = new Message();
        msg.setMessageId("101010");
        msg.setMessageType(10);
        msg.setSourcePerson(IMConstants.SOURCE_PERSON);
        msg.setSourceDevice(IMConstants.SOURCE_DEVICE);
        msg.setTargetPerson(targetPerson);
        msg.setRepeatTimes(0);
        msg.setVoiceId(voiceId);
        sendMessage(msg);
    }

    public void sendMessage(Message msg) {
        mMsgAck = false;
        mRepeatTimes = 0;
        doSend(msg);
        startTimer(msg);
    }

    private void sendAck(String source) {
        Gson gson = new Gson();
        Message msg = gson.fromJson(source, Message.class);
        if (msg != null && msg.getAckType() == 0) {
            mAckMsg = new Message();
            mAckMsg.setAckType(1);
            mAckMsg.setMessageType(4);
            mAckMsg.setMessageId(msg.getMessageId());
            mAckMsg.setOriginalMessageId(msg.getOriginalMessageId());
            mAckMsg.setSourcePerson(msg.getTargetPerson());
            mAckMsg.setSourceDevice(msg.getTargetDevice());
            doSend(mAckMsg);
        }
    }

    public void sendAck() {
        doSend(mAckMsg);
    }

    private void doSend(Message msg) {
        msg.setSendTime(System.currentTimeMillis());
        final String content = new Gson().toJson(msg);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mWriter == null) {
                    log("Error, please call connect method first.");
                    return;
                }
                try {
                    mWriter.write(content);
                    mWriter.flush();
                    log("Send message: " + content);
                } catch (IOException e) {
                    log("Error, send message from client. " + e.getLocalizedMessage());
                }
            }
        }).start();
    }

    private void startTimer(final Message msg) {
        mTimer = new Timer(true);
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (!mMsgAck) {
                    log("Resend");
                    mRepeatTimes++;
                    msg.setRepeatTimes(mRepeatTimes);
                    doSend(msg);
                } else {
                    mTimer.cancel();
                    mTimerTask.cancel();
                }
            }
        };
        mTimer.schedule(mTimerTask, 60 * 1000, 60 * 1000);
    }

    public void disConnect() {
        try {
            if (mReader != null) {
                mReader.close();
            }
            if (mWriter != null) {
                mWriter.close();
            }
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setMediaConversation(IMediaConversation conversation) {
        mConversation = conversation;
    }

    private boolean isEmpty(String s) {
        return s == null || "".equals(s);
    }

    private void log(String msg) {
        Logger.e(TAG, msg);
    }
}
