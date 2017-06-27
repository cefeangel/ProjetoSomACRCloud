package mapp.com.br.projetosomacrcloud;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RestartServiceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "ACRCloud";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(TAG,"RestartServiceBroadcastReceiver -- onReceive ...");

        //context.startService(new Intent(context, ServiceACRCloud.class));

        //context.startService(new Intent(context, ServiceACRCloudOffline3.class));
    }
}
