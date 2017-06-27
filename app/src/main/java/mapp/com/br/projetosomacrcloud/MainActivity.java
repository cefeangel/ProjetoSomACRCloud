package mapp.com.br.projetosomacrcloud;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "ACRCloud";
    private Button btnStart,btnStop;
    private TextView label;
    private String path;
    private boolean isBound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        label = (TextView) findViewById(R.id.textView_1);
        this.btnStart = (Button) findViewById(R.id.ButtonStart);
        this.btnStop = (Button) findViewById(R.id.ButtonStop);

        this.btnStart.setOnClickListener(this);
        this.btnStop.setOnClickListener(this);

        this.btnStart.setEnabled(true);
        this.btnStop.setEnabled(false);


        path = getFilesDir().getAbsolutePath().toString()+"/acrcloud/model";

        File file = new File(path);
        if(!file.exists()){
            Log.i(TAG," Não Existe o Diretorio : "+path);
            file.mkdirs();
        }else{
            Log.i(TAG,"Existe o Diretorio : "+path);
        }

        if (!PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext())
                .getBoolean("installed", false)) {
            PreferenceManager.getDefaultSharedPreferences(
                    getApplicationContext())
                    .edit().putBoolean("installed", true).commit();

            copyAssetFolder(getAssets(), "acrcloud",path);
        }





        // Se não possui permissão Gravar Som
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},0);

        }else{
            startApp();
        }

        // Se não possui permissão Internet Som
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.INTERNET},0);

        }

        if(isOnline()) {
            Log.i(TAG,"Tem Internet ");
        }else{
            Log.i(TAG,"Não Tem Internet ");
        }


    }

    private static boolean copyAssetFolder(AssetManager assetManager,
                                           String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            new File(toPath).mkdirs();
            boolean res = true;
            for (String file : files)
                if (file.contains("."))
                    //Copia arquivo
                    res &= copyAsset(assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
                else
                    //Copia Diretorio.
                    res &= copyAssetFolder(assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean copyAsset(AssetManager assetManager,
                                     String fromAssetPath, String toPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public boolean isOnline() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return manager.getActiveNetworkInfo() != null &&
                manager.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    private void startApp(){


        if(PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext())
                .getBoolean("start",false)){

            Log.i(TAG,"getDefaultSharedPreferences : "+true);
            this.btnStart.setEnabled(false);
            this.btnStop.setEnabled(true);
        }else{
            Log.i(TAG,"getDefaultSharedPreferences : "+false);
            this.btnStop.setEnabled(false);
            this.btnStart.setEnabled(true);
        }


    }
    @Override
    public void onClick(View view) {

        Intent it = null;
        switch (view.getId()){

            case R.id.ButtonStart:

                this.btnStop.setEnabled(true);
                this.btnStart.setEnabled(false);
                //it = new Intent(getApplicationContext(),ServiceACRCloudOffline.class);
                it = new Intent(getApplicationContext(),ServiceACRCloud.class);
                //it = new Intent(getApplicationContext(),ServiceACRCloudOffline3.class);
                startService(it);

                PreferenceManager.getDefaultSharedPreferences(
                        getApplicationContext())
                        .edit().putBoolean("start", true).commit();



                break;
            case R.id.ButtonStop:

                this.btnStop.setEnabled(false);
                this.btnStart.setEnabled(true);
                //it = new Intent(getApplicationContext(),ServiceACRCloudOffline.class);
                it = new Intent(getApplicationContext(),ServiceACRCloud.class);
                //it = new Intent(getApplicationContext(),ServiceACRCloudOffline3.class);
                stopService(it);
                PreferenceManager.getDefaultSharedPreferences(
                        getApplicationContext())
                        .edit().putBoolean("start", false).commit();

                break;

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int result : grantResults) {

            if (result == PackageManager.PERMISSION_DENIED) {
                // Alguma permissão foi negada
                Toast.makeText(MainActivity.this, "Para Utilizar o App favor da permissao de gravar", Toast.LENGTH_SHORT).show();
                Log.i(TAG,"Permissao Negada ");
            }else if (result == PackageManager.PERMISSION_GRANTED){
                startApp();
                Log.i(TAG,"Permissao Consedida ");
            }else {
                //Nunca pergunte novamente selecionado, ou política de dispositivo proíbe a aplicação de ter essa permissão.
                Log.i(TAG,"Nunca pergunte novamente selecionado ");

            }
        }

    }


}
