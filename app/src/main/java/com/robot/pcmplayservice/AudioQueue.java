package com.robot.pcmplayservice;

/**
 * Create on: 2018-11-23
 *    Author: minhua huang
 */

public class AudioQueue {
    private volatile int mFront;
    private volatile int mRear;
    private int mQueueSize; // Queue capacity in bytes
    private byte[] mBuf;    // Data buffer
    private volatile int mBufSize;   // Data size

    public AudioQueue(int queueSize){
        mQueueSize = queueSize;
        assert (queueSize > 0);
        mBuf = new byte[mQueueSize];
        mFront = 0;
        mRear = 0;
        mBufSize = 0;
    }

    public synchronized void writeBuffer(byte[] res, int len){
        int size = mBufSize + len;
        if(size > mQueueSize){
            return;
        }
        for (int i = 0; i < len; i++){
            mBuf[mRear] = res[i];
            mRear = (mRear + 1) % mQueueSize;
            mBufSize++;
        }
    }

    public synchronized int readBuffer(byte[] dst, int len){
        int size = len;
        if(len > mBufSize){
            size = mBufSize;
        }
        for (int i = 0; i < size; i++) {
            dst[i] = mBuf[mFront];
            mFront = (mFront + 1) % mQueueSize;
            mBufSize--;
        }
        return size;
    }

    public boolean empty(){
        return (mBufSize == 0);
    }

    public boolean full(){
        return (mBufSize == mQueueSize);
    }

}
