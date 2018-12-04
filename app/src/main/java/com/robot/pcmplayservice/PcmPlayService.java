package com.robot.pcmplayservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class PcmPlayService extends Service {
    private static final String TAG = "PcmPlayService";
    private int mSampleRate = 48000;                        // 48HZ
    private int mChannel = AudioFormat.CHANNEL_OUT_STEREO;  // double channels
    private int mEncoding = AudioFormat.ENCODING_PCM_16BIT; // 16 bit
    private int mBufferSize = 1024;                         // buffer size in bytes
    private static final int MUTILCAST_PORT = 9456;
    private static final String MUTILCAST_IP_ADDRESS = "239.10.10.10";
    private static final int BUFFER_PACKET_COUNT = 20;

    private MulticastSocket mSocket;
    private InetAddress mAddress;
    private WifiManager.MulticastLock multicastLock;

    private HandlerThread mReceivePcmThread;
    private HandlerThread mPlayPcmThread;
    private Handler mRecivePcmHanler;
    private Handler mPlayPcmHanler;
    private AudioQueue mAudioQueue;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        while (!isWifiConnected()){
            continue;
        }
        try {
            requestMultcastPermission();
            createSocketService();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mBufferSize = AudioTrack.getMinBufferSize(mSampleRate, mChannel, mEncoding);
        mAudioQueue = new AudioQueue(mBufferSize * BUFFER_PACKET_COUNT);
        startReceivePcmThread();
        startPlayPcmThread();
        mRecivePcmHanler.post(new ReceivePcmRunable());
        mPlayPcmHanler.post(new PlayPcmRunable());

        return START_STICKY;
    }

    private void createSocketService() throws IOException {
        Log.i(TAG, "Create pcm play server socket");
        mSocket = new MulticastSocket(MUTILCAST_PORT);
        mAddress = InetAddress.getByName(MUTILCAST_IP_ADDRESS);
        mSocket.joinGroup(mAddress);
    }

    private void requestMultcastPermission(){
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifiManager.createMulticastLock("multicast");
        multicastLock.acquire();
    }

    private boolean isWifiConnected(){
        ConnectivityManager conManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = conManager.getActiveNetworkInfo();
        if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI){
            return true;
        }
        return false;
    }

    private void startReceivePcmThread(){
        mReceivePcmThread = new HandlerThread("StartReceivePcmThread");
        mReceivePcmThread.start();
        mRecivePcmHanler = new Handler(mReceivePcmThread.getLooper());
    }

    private void startPlayPcmThread(){
        mPlayPcmThread = new HandlerThread("StartPlayPcmThread");
        mPlayPcmThread.start();
        mPlayPcmHanler = new Handler(mPlayPcmThread.getLooper());
    }

    private class ReceivePcmRunable implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "receive pcm thread run");
            byte buf[] = new byte[1024];
            DatagramPacket mPacket = new DatagramPacket(buf, buf.length);
//            DatagramPacket mPacket = new DatagramPacket(buf, buf.length, mAddress, MUTILCAST_PORT);
            try {
                while (true){
                    Log.d(TAG, "receive packet...");
                    mSocket.receive(mPacket);
                    Log.d(TAG, "Socket server received packet len: " + mPacket.getLength());
                    mAudioQueue.writeBuffer(buf, mPacket.getLength());
//                mRecivePcmHanler.sendMessage();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class PlayPcmRunable implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "play pcm thread run");
            AudioTrack mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    mSampleRate,
                    mChannel,
                    mEncoding,
                    mBufferSize,
                    AudioTrack.MODE_STREAM);
            byte[] data = new byte[mBufferSize];
            mAudioTrack.play();

            int size = mAudioQueue.readBuffer(data, mBufferSize);
            mAudioTrack.write(data, 0, size);
        }
    }

}
