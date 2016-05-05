package com.example.sitesas.clima;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(savedInstanceState == null){
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ForecastFragment()).commit();
        }
    }

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String EXTRA_MESSAGE = "message";
    private static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_EXPIRATION_TIME = "onServerExpirationTimeMs";
    private static final String PROPERTY_USER = "user";
    String SENDER_ID = "253757430574"; // Número del proyecto en Google Console
    public static final long EXPIRATION_TIME_MS = 1000 * 3600 * 24 * 7;
    static final String TAG = "GCMDemo";
    private String regid;
    private GoogleCloudMessaging gcm;

    private EditText txtUsuario;
    private Button btnRegistrar;

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if(id == R.id.action_settings){
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if(id == R.id.action_map_main){
            openPreferredLocationInMap();
            return true;
        }

        if(id == R.id.notification){
            notification();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void notification(){
        context = getApplicationContext();
        TareaRegistroGCM tarea = new TareaRegistroGCM();
        tarea.execute();
    }

    private void openPreferredLocationInMap(){
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        String location = sharedPrefs.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));

        Uri geoLocation = Uri.parse("geo:0,0?").buildUpon()
                .appendQueryParameter("q", location)
                .build();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if(intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }else{
            Log.d(LOG_TAG, "Couldn't call " + location + ", no receiving apps installed!");
        }
    }

    public class TareaRegistroGCM extends AsyncTask<String,Integer,String> {
        @Override
        protected String doInBackground(String... params) {

            String msg = "";
            try{
                if (gcm == null){
                    gcm = GoogleCloudMessaging.getInstance(context);
                }
                regid = gcm.register(SENDER_ID);
                Log.d(TAG, "Registrado en GCM: registration_id=" + regid);

            }catch (IOException ex){
                Log.d(TAG, "Error registro en GCM:" + ex.getMessage());
            }
            return msg;
        }

        private void setRegistrationId(Context context, String user, String regId){
            SharedPreferences prefs = getSharedPreferences(
                    MainActivity.class.getSimpleName(),
                    Context.MODE_PRIVATE);
            int appVersion = getAppVersion(context);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PROPERTY_USER, user);
            editor.putString(PROPERTY_REG_ID, regId);
            editor.putInt(PROPERTY_APP_VERSION, appVersion);
            editor.putLong(PROPERTY_EXPIRATION_TIME,
                    System.currentTimeMillis() + EXPIRATION_TIME_MS);
            editor.commit();
        }
    }

    private static int getAppVersion(Context context){
        try { PackageInfo packageInfo = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        }catch (PackageManager.NameNotFoundException e){
            throw new RuntimeException("Error al obtener versión: " + e);
        }
    }
}
