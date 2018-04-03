package com.vanda.javacv.demo.im.socket;

import com.google.gson.Gson;
import com.vanda.javacv.demo.im.IMConstants;
import com.vanda.javacv.demo.utils.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Date    28/02/2018
 * Author  WestWang
 * IM客户端，TCP/IP长连接
 */

public class SocketClient {

    private static final String TAG = SocketClient.class.getSimpleName();
    private final String CHARSET = "utf-8";
    private String mHost;
    private int mPort;
    private Socket mSocket = null;
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

    /**
     * 创建Socket，建立连接
     */
    public void connect() {
        try {
            log("start connecting...");
            mSocket = new Socket(mHost, mPort);
            // 读取服务端发送过来的消息
            handleReader(mSocket.getInputStream());
            // 发送消息到服务端
            mWriter = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream(), CHARSET));
            log("connect success.");
        } catch (IOException e) {
            log("Connect failed! " + e.getLocalizedMessage());
        }
    }

    /**
     * 接受处理服务端发送过来的消息
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
                        // 解析message
                        Message msg = new Gson().fromJson(message, Message.class);

                        if (msg != null) {
                            if (msg.getAckType() != 0) {
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
                                sendAck(message);
                            }
                        }


                        dealMessage(msg);

                    }
                } catch (IOException e) {
                    log("error, handle Socket InputStream. " + e.getLocalizedMessage());
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
        switch (msg.getMessageType()) {
            // 服务端消息接收任务
            case 1:
                break;
            // 服务端消息接收后确认任务
            case 2:
                break;
            // 服务端消息发送任务
            case 3:
                break;
            // 服务端消息发送后确认任务
            case 4:
                break;
            // 用户登录任务
            case 5:
                break;
            // 用户登出任务
            case 6:
                break;
            // 服务端消息接收：视频语音呼叫任务
            case 7:
                break;
            // 服务端消息接收：语音呼叫任务
            case 8:
                break;
            // 服务端消息接收：视频/语音应答任务
            case 9:
                break;
            // 服务端消息接收：视频/语音呼叫取消任务
            case 10:
                break;
            // 服务端消息转发：视频语音呼叫任务
            case 11:
                break;
            // 服务端消息转发：语音呼叫任务
            case 12:
                break;
            // 服务端消息转发：视频/语音应答任务
            case 13:
                break;
            // 服务端消息转发：视频/语音呼叫取消任务
            case 14:
                break;
        }
        voiceId = msg.getVoiceId();
        targetPerson = msg.getSourcePerson();
        int type = msg.getMessageType();
        if (type == 11 || type == 12) {
            // 音视频请求，回执
            mediaMsg = new Message();
            mediaMsg.setMessageId(String.valueOf(System.currentTimeMillis()));
            mediaMsg.setMessageType(9);
            mediaMsg.setSourcePerson(IMConstants.SOURCE_PERSON);
            mediaMsg.setSourceDevice(IMConstants.SOURCE_DEVICE);
            mediaMsg.setTargetPerson(msg.getSourcePerson());
            mediaMsg.setTargetDevice(msg.getSourceDevice());
            mediaMsg.setVoiceId(msg.getVoiceId());
        } else if (type == 13) {
            // 开启音视频会话
            if (mConversation != null) {
                mConversation.openConversation();
            }
        } else if (type == 10 || type == 14) {
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
        msg.setMessageId(String.valueOf(System.currentTimeMillis()));
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
            mAckMsg.setSourcePerson(msg.getTargetPerson());
            mAckMsg.setSourceDevice(msg.getTargetDevice());
            doSend(mAckMsg);
        }
    }

    public void sendAck() {
        doSend(mAckMsg);
    }

    private void doSend(Message msg) {
        if (msg == null) {
            return;
        }
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
