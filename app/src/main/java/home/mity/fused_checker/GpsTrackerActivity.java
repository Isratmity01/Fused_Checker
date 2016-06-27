package home.mity.fused_checker;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.websmithing.gpstracker.R;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.UUID;

public class GpsTrackerActivity extends ActionBarActivity implements GoogleMap.OnMapClickListener {
    private static final String TAG = "GpsTrackerActivity";

    // use the websmithing defaultUploadWebsite for testing and then check your
    // location with your browser here: https://www.websmithing.com/gpstracker/displaymap.php
    private String defaultUploadWebsite;
    Button btn;
    private static EditText txtUserName;
    private static EditText txtWebsite;
    private static Button trackingButton;
    ArrayList<String> myAList=new ArrayList<String>();
    private boolean currentlyTracking;
    private RadioGroup intervalRadioGroup;
    private int intervalInMinutes = 1;
    private AlarmManager alarmManager;
    private Intent gpsTrackerIntent;
    private PendingIntent pendingIntent;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private GoogleMap googleMap;
    private ArrayList<LatLng> arrayPoints = null;
    PolylineOptions polylineOptions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpstracker);

        btn = (Button) findViewById(R.id.button);
        OnClickListener oclBtnOk = new OnClickListener() {
            @Override
            public void onClick(View v) {
                // change text of the TextView (tvOut)
                btn.setText("Button OK clicked");
                calltoast();
            }
        };

        // assign click listener to the OK button (btnOK)
        btn.setOnClickListener(oclBtnOk);

        trackingButton = (Button) findViewById(R.id.trackingButton);


        SharedPreferences sharedPreferences = this.getSharedPreferences("com.websmithing.gpstracker.prefs", Context.MODE_PRIVATE);
        currentlyTracking = sharedPreferences.getBoolean("currentlyTracking", false);

        boolean firstTimeLoadindApp = sharedPreferences.getBoolean("firstTimeLoadindApp", true);
        if (firstTimeLoadindApp) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("firstTimeLoadindApp", false);
            editor.putString("appID", UUID.randomUUID().toString());
            editor.apply();
        }


        trackingButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                trackLocation(view);
            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        arrayPoints = new ArrayList<LatLng>();
        SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        googleMap = fm.getMap();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        googleMap.setMyLocationEnabled(true);

        googleMap.setOnMapClickListener(this);




    }

    private void saveInterval() {
        if (currentlyTracking) {
            // Toast.makeText(getApplicationContext(), R.string.user_needs_to_restart_tracking, Toast.LENGTH_LONG).show();
        }

        SharedPreferences sharedPreferences = this.getSharedPreferences("com.websmithing.gpstracker.prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();



        editor.apply();
    }

    private void startAlarmManager() {
        Log.d(TAG, "startAlarmManager");

        SharedPreferences sharedPreferences = this.getSharedPreferences("com.websmithing.gpstracker.prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("currentlyProcessingLocation", false);
        editor.putBoolean("stopclicked", false);
        editor.commit();
        Context context = getBaseContext();
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        gpsTrackerIntent = new Intent(context, GpsTrackerAlarmReceiver.class);

        pendingIntent = PendingIntent.getBroadcast(context, 0, gpsTrackerIntent,0);

        intervalInMinutes = sharedPreferences.getInt("intervalInMinutes", 1);

        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                intervalInMinutes * 20000, // 60000 = 1 minute
                pendingIntent);
    }

    private void cancelAlarmManager() {
        Log.d(TAG, "cancelAlarmManager");
        SharedPreferences sharedPreferences = this.getSharedPreferences("com.websmithing.gpstracker.prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putBoolean("stopclicked", true);
        editor.commit();
        Context context = getBaseContext();
        Intent gpsTrackerIntent = new Intent(context, GpsTrackerAlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, gpsTrackerIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarmManager.cancel(pendingIntent);


    }

    // called when trackingButton is tapped
    protected void trackLocation(View v) {
        SharedPreferences sharedPreferences = this.getSharedPreferences("com.websmithing.gpstracker.prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        SharedPreferences sharedPreferences2 = this.getSharedPreferences("com.websmithing.gpstracker.prefs", Context.MODE_PRIVATE);

        if (!saveUserSettings()) {
            return;
        }

        if (!checkIfGooglePlayEnabled()) {
            return;
        }

        if (currentlyTracking) {
            cancelAlarmManager();
            ArrayList<String> myAList=new ArrayList<String>();

            int size=sharedPreferences2.getInt("size",0);

            for(int j=0;j<size;j++)
            {
                myAList.add(sharedPreferences2.getString("val"+j,null));
            }
            Toast.makeText(getApplicationContext(), "positions is "+myAList.toString(), Toast.LENGTH_LONG).show();

            float totalDistanceInMeters = sharedPreferences.getFloat("totalDistanceInMeters", 0f);
                    Log.d("ADebugTag", "Value: " + Float.toString(totalDistanceInMeters));
            Toast.makeText(getApplicationContext(), "vlaue is "+totalDistanceInMeters, Toast.LENGTH_LONG).show();

            currentlyTracking = false;
            editor.putBoolean("currentlyTracking", false);
            editor.putString("sessionID", "");
        } else {
            startAlarmManager();

            currentlyTracking = true;
            editor.putBoolean("currentlyTracking", true);
            editor.putFloat("totalDistanceInMeters", 0f);

            editor.putBoolean("firstTimeGettingPosition", true);
            editor.putString("sessionID", UUID.randomUUID().toString());
            editor.commit();
        }

        editor.apply();

        setTrackingButtonState();
    }

    private boolean saveUserSettings() {
        if (textFieldsAreEmptyOrHaveSpaces()) {
            return false;
        }

        SharedPreferences sharedPreferences = this.getSharedPreferences("com.websmithing.gpstracker.prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();



        editor.apply();

        return true;
    }
private void calltoast()
{
    SharedPreferences sharedPreferences2 = this.getSharedPreferences("com.websmithing.gpstracker.prefs", Context.MODE_PRIVATE);


    int size=sharedPreferences2.getInt("size",0);

    for(int j=0;j<size;j++)
    {
        myAList.add(sharedPreferences2.getString("val"+j,null));
    }
    Toast.makeText(getApplicationContext(), "positions is "+myAList.toString(), Toast.LENGTH_LONG).show();
//onMapClick2();
}
    private boolean textFieldsAreEmptyOrHaveSpaces() {


        return false;
    }

    private boolean hasSpaces(String str) {
        return ((str.split(" ").length > 1) ? true : false);
    }

    private void displayUserSettings() {
        SharedPreferences sharedPreferences = this.getSharedPreferences("com.websmithing.gpstracker.prefs", Context.MODE_PRIVATE);
        intervalInMinutes = sharedPreferences.getInt("intervalInMinutes", 1);




    }

    private boolean checkIfGooglePlayEnabled() {
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            return true;
        } else {
            Log.e(TAG, "unable to connect to google play services.");
            Toast.makeText(getApplicationContext(), "not available", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void setTrackingButtonState() {
        if (currentlyTracking) {
            //trackingButton.setBackgroundResource(Color.);
            trackingButton.setTextColor(Color.BLACK);
            trackingButton.setText("tracking is on");
        } else {
            // trackingButton.setBackgroundResource(R.drawable.red_tracking_button);
            trackingButton.setTextColor(Color.WHITE);
            trackingButton.setText("tracking is off");
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        displayUserSettings();
        setTrackingButtonState();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "GpsTracker Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://home.mity.fused_checker/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "GpsTracker Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://home.mity.fused_checker/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }


    public void onMapClick2() {
        //add marker
        MarkerOptions marker=new MarkerOptions();
        for(int i=0;i<myAList.size();i++)
        {
            String[] latlong =  myAList.get(i).split(",");
            double latitude = Double.parseDouble(latlong[0]);
            double longitude = Double.parseDouble(latlong[1]);
            LatLng location = new LatLng(latitude, longitude);
            marker.position(location);
            arrayPoints.add(location);
        }

        googleMap.addMarker(marker);
        // settin polyline in the map
        polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.RED);
        polylineOptions.width(5);

        polylineOptions.addAll(arrayPoints);
        googleMap.addPolyline(polylineOptions);
    }


    @Override
    public void onMapClick(LatLng latLng) {

    }
}
