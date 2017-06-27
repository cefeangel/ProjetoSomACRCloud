package mapp.com.br.projetosomacrcloud;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


public class ActivityNotificacao extends AppCompatActivity {

    private static final String TAG = "ACRCloud";
    private TextView textTitle, textCliente,textAudioId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_notificacao);

        textTitle = (TextView) findViewById(R.id.text_title);
        textCliente = (TextView) findViewById(R.id.text_cliente);
        textAudioId = (TextView) findViewById(R.id.text_audio_id);


        Log.i(TAG,"ActivityNotificacao -- onCreate ...");

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        nm.cancel(001);

        Bundle b = getIntent().getExtras();
        String title =  b.getString("Title");
        String cliente =  b.getString("Cliente");
        int audioId = b.getInt("AudioId");

        textCliente.setText("Cliente :"+cliente);
        textTitle.setText("Title :"+title);
        textAudioId.setText("Audio Id :"+String.valueOf(audioId));

    }

    @Override
    protected void onResume() {

        super.onResume();

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        nm.cancel(001);

        /*
        Bundle b = getIntent().getExtras();
        String title =  b.getString("Title");
        String cliente =  b.getString("Cliente");

        textCliente.setText(cliente);
        textTitle.setText(title);

        */

       // Log.i(TAG, "ActivityNotificacao -- onResume Clliente " + cliente +" Title " + title);
        Log.i(TAG, "ActivityNotificacao -- onResume Clliente ");

    }

    @Override
    protected void onRestart() {
        super.onRestart();


        Log.i(TAG, "ActivityNotificacao -- onRestart  ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "ActivityNotificacao -- onDestroy  ");
    }

    public void fechar(View view){

        finish();

    }
}
