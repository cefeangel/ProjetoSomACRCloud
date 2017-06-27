package mapp.com.br.projetosomacrcloud;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;

public class ServiceACRCloud extends Service  implements Runnable {

    private static final String TAG = "ACRCloud";
    private static final int MAX_SEC_REC = 7;
    private int segundo ;
    private boolean ativo;
    private MediaRecorder recorder;
    private File recordingFile;

    private int mNotificationId = 001;
    Thread thread;

    @Override
    public IBinder onBind(Intent intent) {

        return  null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        ativo = true;
        segundo = 1;
        Log.i(TAG,"ServiceACRCloud -- onCreate ...");
        new Thread(this).start();

    }
    @Override
    public void onDestroy() {

        ativo = false;
        Log.i(TAG,"ServiceACRCloud -- onDestroy ...");


        super.onDestroy();

    }



    @Override
    public void run() {

      while(ativo) {

          record();
          while (ativo && (segundo <= MAX_SEC_REC)) {

              try {
                  Thread.sleep(1000);
              } catch (InterruptedException e) {
                  e.printStackTrace();
              }

              Log.i(TAG, "ServiceACRCloud -- executando -- segundos " + segundo + "...");


              segundo++;

          }

          recorder.stop();
          Log.i(TAG, "ServiceACRCloud -- recorder.stop()");

          segundo = 1;


          if(ativo) {


              String result = enviarSomACRCloud();


              try {
                  JSONObject j = new JSONObject(result);
                  JSONObject j1 = j.getJSONObject("status");
                  int code = j1.getInt("code");

                  Log.i(TAG, "ServiceACRCloud -- code " + j1.getInt("code"));
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
                      createNotificacao(titulo, msg, ticke,params);

                      Log.i(TAG, "ServiceACRCloud -- Clliente" + Cliente + " Audio_id :" + audio_id + " Title" + title);

                  } else {




                      Log.i(TAG, "ServiceACRCloud -- Error");
                  }


              } catch (JSONException e) {
                  e.printStackTrace();
              }

          }

          ativo = false;
      }
        stopSelf();
    }



    private void record() {

        File somPath = new   File(getFilesDir(), "som");
        somPath.mkdirs();
        recordingFile = new File(somPath, "recording.m4a");

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setAudioEncodingBitRate(16);
        recorder.setAudioSamplingRate(44100);
        recorder.setMaxDuration(7000);
        recorder.setOutputFile(recordingFile.getAbsolutePath());
        Log.i(TAG,"ServiceACRCloud  -- StartRecording File :"+recordingFile.getAbsolutePath());

        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            throw new RuntimeException(
                    "IllegalStateException on MediaRecorder.prepare", e);
        } catch (IOException e) {
            throw new RuntimeException("IOException on MediaRecorder.prepare",e);
        }

        recorder.start();


    }

    private String enviarSomACRCloud(){

        File file = new File(recordingFile.getAbsolutePath());
        byte[] buffer = new byte[1024 * 1024];
        if (!file.exists()) {
            Log.i(TAG," ServiceACRCloud -- Arquivo não existe :");
            return null;
        }
        FileInputStream fin = null;
        int bufferLen = 0;
        try {
            fin = new FileInputStream(file);
            bufferLen = fin.read(buffer, 0, buffer.length);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        Log.i(TAG," ServiceACRCloud -- bufferLen : "+bufferLen);


        byte[] postDatas = new byte[bufferLen];
        System.arraycopy(buffer, 0, postDatas, 0, bufferLen);
        IdentifyProtocolV1 a = new IdentifyProtocolV1();

        // Replace "###...###" below with your project's host, access_key and access_secret.
        // recognize(String host, String accessKey, String secretKey, byte[] queryData, String queryType, int timeout)
        String result = a.recognize("us-west-2.api.acrcloud.com", "83c875588509c603ac5945cb6d6f25f5", "QSotOodQrJEYVMWHziStEjdSXu9IAEkvU1Xh9gZd", postDatas, "audio", 10000);

        Log.i(TAG," ServiceACRCloud -- Result : "+result);

        return result;

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
