package com.robot.pcmplayservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("PcmPlayService", "start pcm play service");
        Intent playService = new Intent(context, PcmPlayService.class);
        playService.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(playService);
    }
}
