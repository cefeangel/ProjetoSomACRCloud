package mapp.com.br.projetosomacrcloud;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.acrcloud.rec.sdk.ACRCloudClient;
import com.acrcloud.rec.sdk.ACRCloudConfig;
import com.acrcloud.rec.sdk.IACRCloudListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Random;

public class ServiceACRCloudOffline3 extends Service implements Runnable,IACRCloudListener {


    private static final String TAG = "ACRCloud";
    private boolean ativo;
    private ACRCloudClient mClient;
    private ACRCloudConfig mConfig;
    private boolean initState = false;
    private String path;
    private int mNotificationId = 001;


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {

        path = getFilesDir().getAbsolutePath().toString()+"/acrcloud/model";
        ativo = true;
        Log.i(TAG,"ServiceACRCloudOffline -- onCreate ...");
        new Thread(this).start();

    }

    @Override
    public void run() {

        Log.i(TAG, "ServiceACRCloudOffline -- Thread 1 ");


        record();

        while (ativo) {



            try {
                Log.i(TAG, "ServiceACRCloudOffline -- Thread.sleep (Antes) -- ");
                Thread.sleep(9000);
                Log.i(TAG, "ServiceACRCloudOffline -- Thread.sleep (Depois) -- ");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ativo = false;
            this.mClient.stopRecordToRecognize();
            this.mClient.cancel();


        }

        stopSelf();
    }
    private void record() {
        //Criar um Thread somente para gravar o som.
        Thread t1 = new Thread(new Runnable() {

            @Override
            public void run() {

                Log.i(TAG, "ServiceACRCloudOffline -- Thread 2 ");

                //Configuracao API ACRCloud.
                mConfig = new ACRCloudConfig();
                mConfig.acrcloudListener = new IACRCloudListener(){
                    @Override
                    public void onResult(String result) {

                        Log.i(TAG, "ServiceACRCloudOffline -- onResult() -- ");

                        try {

                            JSONObject j = new JSONObject(result);
                            JSONObject j1 = j.getJSONObject("status");
                            int code = j1.getInt("code");

                            Log.i(TAG, "ServiceACRCloudOffline -- code " + j1.getInt("code"));
                            //sucesso
                            if (code == 0) {
                                JSONObject metadata = j.getJSONObject("metadata");
                                JSONArray custom_files = metadata.getJSONArray("custom_files");

                                String Cliente = custom_files.getJSONObject(0).getString("Cliente");
                                int audio_id = custom_files.getJSONObject(0).getInt("audio_id");
                                String title = custom_files.getJSONObject(0).getString("title");

                                Intent params = new Intent(getApplicationContext(),ActivityNotificacao.class);
                                params.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP  |Intent.FLAG_ACTIVITY_SINGLE_TOP);


                                Bundle b = new Bundle();
                                b.putInt("AudioId",audio_id);
                                b.putString("Cliente",Cliente);
                                b.putString("Title",title);

                                params.putExtras(b);

                                String msg = "Cliente :" + Cliente + " -- Audio_Id :" + audio_id + " -- Title" + title;

                                String ticke = "ProjetoSomACRCloud ";
                                String titulo = "Sucess ProjetoSomACRCloud";

                                createNotificacao(titulo,msg,ticke,params);

                                Log.i(TAG, "ServiceACRCloudOffline -- Clliente" + Cliente + " Audio_id :" + audio_id + " Title" + title);

                            } else {


                        /*
                        String msg = j1.getString("msg");

                        String txt = "Msg :"+msg+" Code :"+code;
                        String ticke = " ACRCloud ";
                        String titulo = "Teste ACRCloud";

                        //createNotificacao(titulo, txt, ticke);

                        */

                                Log.i(TAG, "ServiceACRCloud -- Error");
                            }


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                    @Override
                    public void onVolumeChanged(double v) {

                    }
                };

                Log.i(TAG,"ServiceACRCloudOffline -- Diretorio existe "+path);
                mConfig.context = getApplicationContext();
                mConfig.host = path;
                mConfig.dbPath = path; // offline db path, you can change it with other path which this app can access.
                mConfig.accessKey = "0a9b4a6a7cdce4af68c0371d90cdb87c";
                mConfig.accessSecret = "WnQ8kd2DM050GCUagzx1ymMzbV8JOEarRrMH1kBs";
                mConfig.reqMode = ACRCloudConfig.ACRCloudRecMode.REC_MODE_LOCAL;


                mClient = new ACRCloudClient();

                if (initState) {
                    mClient.startPreRecord(3000);
                }
                mClient.startRecognize();


            }
        });
        t1.start();
    }

    @Override
    public void onResult(String result) {
        Log.i(TAG, "ServiceACRCloudOffline -- onResult() -- ");

        try {

            JSONObject j = new JSONObject(result);
            JSONObject j1 = j.getJSONObject("status");
            int code = j1.getInt("code");

            Log.i(TAG, "ServiceACRCloudOffline -- code " + j1.getInt("code"));
            //sucesso
            if (code == 0) {
                JSONObject metadata = j.getJSONObject("metadata");
                JSONArray custom_files = metadata.getJSONArray("custom_files");

                String Cliente = custom_files.getJSONObject(0).getString("Cliente");
                int audio_id = custom_files.getJSONObject(0).getInt("audio_id");
                String title = custom_files.getJSONObject(0).getString("title");

                Intent params = new Intent(this,ActivityNotificacao.class);
                params.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP  |Intent.FLAG_ACTIVITY_SINGLE_TOP);


                Bundle b = new Bundle();
                b.putInt("AudioId",audio_id);
                b.putString("Cliente",Cliente);
                b.putString("Title",title);

                params.putExtras(b);

                String msg = "Cliente :" + Cliente + " -- Audio_Id :" + audio_id + " -- Title" + title;

                String ticke = "ProjetoSomACRCloud ";
                String titulo = "Sucess ProjetoSomACRCloud";

                createNotificacao(titulo,msg,ticke,params);

                Log.i(TAG, "ServiceACRCloudOffline -- Clliente" + Cliente + " Audio_id :" + audio_id + " Title" + title);

            } else {


                        /*
                        String msg = j1.getString("msg");

                        String txt = "Msg :"+msg+" Code :"+code;
                        String ticke = " ACRCloud ";
                        String titulo = "Teste ACRCloud";

                        //createNotificacao(titulo, txt, ticke);

                        */

                Log.i(TAG, "ServiceACRCloud -- Error");
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onVolumeChanged(double v) {

    }

    @Override
    public void onDestroy() {

        ativo = false;

        Log.i(TAG,"ServiceACRCloudOffline -- onDestroy ...");
        if (this.mClient != null) {
            this.mClient.release();
            this.initState = false;
            this.mClient = null;
        }

        super.onDestroy();
        Intent broadcastIntent = new Intent("RESTART_SERVICE_ARCCLOUD");
        sendBroadcast(broadcastIntent);

    }

    private void createNotificacao(CharSequence title, CharSequence message, CharSequence ticher,Intent params){

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(),new Random().nextInt(),params,PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());

        builder.setContentIntent(pi);
        builder.setSmallIcon(R.drawable.ic_stat_sond);
        builder.setTicker(ticher);
        builder.setContentTitle(title);
        builder.setContentText(message);

        builder.setVibrate(new long[]{ 100, 250, 100, 500 });

        nm.notify(mNotificationId, builder.build());

        try{

            //add um som na notificação.
            Uri som = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone toque = RingtoneManager.getRingtone(getApplicationContext(),som);
            toque.play();

        }catch (Exception e){}
    }
}
