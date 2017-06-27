package mapp.com.br.projetosomacrcloud;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.acrcloud.rec.sdk.ACRCloudClient;
import com.acrcloud.rec.sdk.ACRCloudConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import android.support.v4.content.IntentCompat;

public class ServiceACRCloudOffline extends Service implements Runnable {

    private static final String TAG = "ACRCloud";
    private static final int MAX_SEC_REC = 7;
    private static final int RECORDER_BPP = 16;
    private int segundo;
    private boolean ativo;
    private final IBinder binder = new LocalBinder();

    private String path;
    private File recordingFile;
    private File recordingFile2;
    private AudioRecord audioRecord;

    boolean isRecording = false;
    int frequency = 8000;
    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORDER_SAMPLERATE = 44100;
    //private static final int RECORDER_SAMPLERATE = 8000;

    private ACRCloudClient mClient;
    private ACRCloudConfig mConfig;

    private int mNotificationId = 001;
    private int bufferSize = 0;
    Thread thread;
    private PowerManager.WakeLock wl;

    public class LocalBinder extends Binder {

        ServiceACRCloudOffline getService() {

            return ServiceACRCloudOffline.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLockServiceACRCloud");
        if ((wl != null) &&           // we have a WakeLock
                (wl.isHeld() == false)) {  // but we don't hold it
            wl.acquire();
        }


        PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext())
                .edit().putBoolean("start", true).commit();

        super.onCreate();


        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        path = getFilesDir().getAbsolutePath().toString()+"/acrcloud/model";
        ativo = true;
        segundo = 1;
        Log.i(TAG,"ServiceACRCloudOffline -- onCreate ...");
        new Thread(this).start();

    }

    @Override
    public void onDestroy() {

        ativo = false;
        segundo = 7;
        Log.i(TAG,"ServiceACRCloudOffline -- onDestroy ...");

        if(wl.isHeld()){
            wl.release();
        }

        super.onDestroy();
        /*
        Intent broadcastIntent = new Intent("RESTART_SERVICE_ARCCLOUD");
        sendBroadcast(broadcastIntent);
        */
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

                Log.i(TAG, "ServiceACRCloudOffline -- executando -- segundos " + segundo + "...");


                segundo++;

            }

            segundo = 1;

            isRecording = false;

            copyWaveFile(recordingFile.getAbsolutePath(),recordingFile2.getAbsolutePath());
            deleteTempFile();

            if(ativo) {

                String result = enviarSomACRCloud();


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
        }

    }


    private void record() {

        //Criar um Thread somente para gravar o som.
        Thread t1 = new Thread(new Runnable() {

            @Override
            public void run() {


                File somPath = new   File(getFilesDir(), "som");
                somPath.mkdirs();
                recordingFile = new File(somPath, "recording.RIFF");

                isRecording = true;

                try {

                    Log.i(TAG, "ServiceACRCloudOffline -- Diretorio AudioRecord "+recordingFile.getAbsolutePath());

                    /*
                    DataOutputStream dos = new DataOutputStream(
                            new BufferedOutputStream(new FileOutputStream(
                                    recordingFile)));

                    int bufferSize = AudioRecord.getMinBufferSize(frequency,
                            channelConfiguration, audioEncoding);
                   */
                    audioRecord = new AudioRecord(
                            MediaRecorder.AudioSource.MIC, frequency,
                            channelConfiguration, audioEncoding, bufferSize);

                    //short[] buffer = new short[bufferSize];
                    //byte [] buffer = new byte[bufferSize];
                    audioRecord.startRecording();

                    writeAudioDataToFile();
                     /*
                    while(isRecording) {


                        int bufferReadResult = audioRecord.read(buffer, 0, buffer.length);
                        dos.write(buffer,0,buffer.length);


                    }
                    */
                    audioRecord.stop();
                    //dos.close();

                    Log.i(TAG, "ServiceACRCloudOffline --  audioRecord.stop()");
                    Log.i(TAG, "ServiceACRCloudOffline --  dos.close()");

                }catch (Throwable t) {

                    Log.e(TAG, "ServiceACRCloudOffline -- Recording Failed");
                }

            }
        });
        t1.start();


    }

    private void writeAudioDataToFile(){

        Log.i(TAG,"writeAudioDataToFile -- ");
        byte data[] = new byte[bufferSize];

        File somPath = new   File(getFilesDir(), "som");
        somPath.mkdirs();

        if(!somPath.exists()){
            somPath.mkdirs();;
        }
        recordingFile = new File(somPath, "record_temp.wav");

        recordingFile2 = new File(somPath, "recording.wav");

        String filename = recordingFile.getAbsolutePath();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int read = 0;

        if(null != os){
            while(isRecording){
                read = audioRecord.read(data, 0, bufferSize);

                if(AudioRecord.ERROR_INVALID_OPERATION != read){
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteTempFile() {
        File file = new File(recordingFile.getAbsolutePath());
        Log.i(TAG,"deleteTempFile() -- "+recordingFile.getAbsolutePath());
        file.delete();
    }

    private void copyWaveFile(String inFilename,String outFilename){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 1;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            //AppLog.logString("File size: " + totalDataLen);
            Log.i(TAG,"ile size: -- "+totalDataLen);
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while(in.read(data) != -1){
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    private String enviarSomACRCloud(){

        String result = "";

        File file = new File(recordingFile2.getAbsolutePath());
        byte[] buffer = new byte[1024 * 1024];
        if (!file.exists()) {
            Log.i(TAG," ServiceACRCloudOffline -- Arquivo não existe :");
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


        Log.i(TAG," ServiceACRCloudOffline -- bufferLen : "+bufferLen);


        byte[] postDatas = new byte[bufferLen];
        System.arraycopy(buffer, 0, postDatas, 0, bufferLen);

        //Configuracao API ACRCloud.
        this.mConfig = new ACRCloudConfig();

        Log.i(TAG,"ServiceACRCloudOffline -- Diretorio existe "+path);
        this.mConfig.context = this;
        this.mConfig.host = path;
        this.mConfig.dbPath = path; // offline db path, you can change it with other path which this app can access.
        this.mConfig.accessKey = "0a9b4a6a7cdce4af68c0371d90cdb87c";
        this.mConfig.accessSecret = "WnQ8kd2DM050GCUagzx1ymMzbV8JOEarRrMH1kBs";
        this.mConfig.reqMode = ACRCloudConfig.ACRCloudRecMode.REC_MODE_LOCAL;


        this.mClient = new ACRCloudClient();


        if (this.mClient.initWithConfig(this.mConfig)) {
            //Passa byte de audio gravado para api do ACRCloud para analise e retorno um string que Json
            result =  this.mClient.recognize(postDatas,bufferLen);
        }

        Log.i(TAG,"Result  "+result);

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


    public boolean isServicoAtivo(){

        return this.ativo;
    }

    public int getSegundosServico(){

        return this.segundo;
    }

    /*
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent,flags,startId);
        Log.i(TAG, "ServiceACRCloudOffline -- onStartCommand ");
        return START_STICKY;
    }
    */

}
