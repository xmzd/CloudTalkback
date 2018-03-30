package com.vanda.javacv.demo.im.socket;

/**
 * Date    28/02/2018
 * Author  WestWang
 */

public class Message {

    private int messageType;
    private String messageId;
    private String sourcePerson;
    private String sourceDevice;
    private String targetRoom;
    private String messageContext;
    private String targetDevice;
    private String targetPerson;

    private int ackType;
    private String errorMessage;
    private long latestExeTime;
    private int repeatTimes;
    private long sendTime;
    private String originalMessageId;
    private String voiceId;

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSourcePerson() {
        return sourcePerson;
    }

    public void setSourcePerson(String sourcePerson) {
        this.sourcePerson = sourcePerson;
    }

    public String getSourceDevice() {
        return sourceDevice;
    }

    public void setSourceDevice(String sourceDevice) {
        this.sourceDevice = sourceDevice;
    }

    public String getTargetRoom() {
        return targetRoom;
    }

    public void setTargetRoom(String targetRoom) {
        this.targetRoom = targetRoom;
    }

    public String getMessageContext() {
        return messageContext;
    }

    public void setMessageContext(String messageContext) {
        this.messageContext = messageContext;
    }

    public int getAckType() {
        return ackType;
    }

    public void setAckType(int ackType) {
        this.ackType = ackType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getLatestExeTime() {
        return latestExeTime;
    }

    public void setLatestExeTime(long latestExeTime) {
        this.latestExeTime = latestExeTime;
    }

    public int getRepeatTimes() {
        return repeatTimes;
    }

    public void setRepeatTimes(int repeatTimes) {
        this.repeatTimes = repeatTimes;
    }

    public long getSendTime() {
        return sendTime;
    }

    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }

    public String getTargetDevice() {
        return targetDevice;
    }

    public void setTargetDevice(String targetDevice) {
        this.targetDevice = targetDevice;
    }

    public String getTargetPerson() {
        return targetPerson;
    }

    public void setTargetPerson(String targetPerson) {
        this.targetPerson = targetPerson;
    }

    public String getOriginalMessageId() {
        return originalMessageId;
    }

    public void setOriginalMessageId(String originalMessageId) {
        this.originalMessageId = originalMessageId;
    }

    public String getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }
}
