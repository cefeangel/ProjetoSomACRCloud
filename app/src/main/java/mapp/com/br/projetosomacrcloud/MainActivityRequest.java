package mapp.com.br.projetosomacrcloud;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivityRequest extends AppCompatActivity  implements View.OnClickListener{

    private static final String TAG = "ACRCloud";
    private Button btnStart,btnStop;
    private TextView label;
    private String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        label = (TextView) findViewById(R.id.textView_1);
        this.btnStart = (Button) findViewById(R.id.ButtonStart);
        this.btnStop = (Button) findViewById(R.id.ButtonStop);

        this.btnStart.setOnClickListener(this);
        this.btnStop.setOnClickListener(this);

        this.btnStop.setEnabled(false);
        this.btnStart.setEnabled(false);

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
        };
    }

    public boolean isOnline() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return manager.getActiveNetworkInfo() != null &&
                manager.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    private void startApp(){
        this.btnStop.setEnabled(false);
        this.btnStart.setEnabled(true);
    }

    @Override
    public void onClick(View view) {


        Intent it = null;
        switch (view.getId()){

            case R.id.ButtonStart:

                this.btnStop.setEnabled(true);
                this.btnStart.setEnabled(false);
                it = new Intent(getApplicationContext(),ServiceACRCloud.class);
                startService(it);
                break;

            case R.id.ButtonStop:

                this.btnStop.setEnabled(false);
                this.btnStart.setEnabled(true);
                it = new Intent(getApplicationContext(),ServiceACRCloud.class);
                stopService(it);


                break;

        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int result : grantResults) {

            if (result == PackageManager.PERMISSION_DENIED) {
                // Alguma permissão foi negada
                Toast.makeText(MainActivityRequest.this, "Para Utilizar o App favor da permissao de gravar", Toast.LENGTH_SHORT).show();
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
