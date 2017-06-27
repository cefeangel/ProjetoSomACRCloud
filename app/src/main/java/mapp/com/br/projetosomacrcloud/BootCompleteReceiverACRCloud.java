package mapp.com.br.projetosomacrcloud;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class BootCompleteReceiverACRCloud extends BroadcastReceiver {

    private static final String TAG = "ACRCloud";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(TAG,"BootCompleteReceiver -- onReceive ...");

        Intent service = new Intent(context, ServiceACRCloud.class);

       // Intent service = new Intent(context, ServiceACRCloudOffline3.class);

        //context.startService(service);


    }

}
