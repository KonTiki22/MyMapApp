package com.example.mymapapp;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, CustomResultReceiver.AppReceiver {

    private CustomResultReceiver resultReceiver;
    private View infoWindowContainer;


    double lat, lon;
    Marker userLoc;
    MarkerOptions user;


    boolean camMoveToUser = true;
    boolean markerChoosed = false;

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;

    private GoogleMap mMap;
    ArrayList<Marker> markList = new ArrayList<>();
    Marker saveMarker;

    LatLng myPlace;

    TextView tasks;
    TextView title;
    Button button;



    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationService();
    }





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        myPlace = new LatLng(52.27537, 104.2774);

        Drawable vector = getResources().getDrawable(R.drawable.ic_baseline_emoji_people_24);
        Bitmap bitmap = getBitmap((VectorDrawable) vector, R.color.black);
        user = new MarkerOptions().position(myPlace).title("Вы здесь").icon(BitmapDescriptorFactory.fromBitmap(bitmap));


        title = findViewById(R.id.quest_title);
        button = findViewById(R.id.button);

        //startLocationService();
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        }
        else {
            startLocationService();
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        tasks = findViewById(R.id.tasks);
        tasks.setText("Выберите квест на карте");


        infoWindowContainer = findViewById(R.id.container_popup);
        infoWindowContainer.setVisibility(View.INVISIBLE);

    }







    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        markList.add(0, (mMap.addMarker(new MarkerOptions().position(new LatLng(52.260361, 104.262611)).title("Лёгкая прогулка"))));
        markList.add(0, (mMap.addMarker(new MarkerOptions().position(new LatLng(52.272194, 104.260806)).title("Заброшенный бункер"))));
        markList.add(0, (mMap.addMarker(new MarkerOptions().position(new LatLng(52.269677, 104.283267)).title("Остров \"Юность\""))));
        // Add a marker in Sydney and move the camera
        //userLoc = mMap.addMarker(user);

        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPlace, 16f));
        //mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        mMap.setOnMapClickListener(latLng -> {
            infoWindowContainer.setVisibility(View.INVISIBLE);
            if(!markerChoosed) {
                saveMarker = null;
            }
        });
        mMap.setOnMarkerClickListener(m -> {
            if(!m.getTitle().equals("Вы здесь")){
                title.setText(m.getTitle());
                infoWindowContainer.setVisibility(View.VISIBLE);
                saveMarker = m;
            }
            return false;
        });

    }



    private boolean isLocationServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context. ACTIVITY_SERVICE);
        if (activityManager != null) {
            for (ActivityManager. RunningServiceInfo service : activityManager. getRunningServices(Integer.MAX_VALUE)) {
                if (LocationService.class.getName().equals(service.service.getClassName())) {
                    if (service.foreground) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }



    private void startLocationService() {
        if(!isLocationServiceRunning()) {
            Intent intent = new Intent(MapsActivity.this, LocationService.class);
            intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
            resultReceiver = new CustomResultReceiver(new Handler(), this);
            intent.putExtra("receiver", resultReceiver);
            startService(intent);
            Toast. makeText( this, "Location service started", Toast.LENGTH_SHORT). show();
        }
    }




    private void stopLocationService() {
        if (isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent. setAction(Constants.ACTION_STOP_LOCATION_SERVICE);
            startService(intent);
            Toast. makeText( this, "Location service stopped", Toast.LENGTH_SHORT).show();
        }
    }



    private void registerService() {
        Intent intent = new Intent(getApplicationContext(), LocationService.class);

        /*
         * Step 2: We pass the ResultReceiver via the intent to the intent service
         * */
        resultReceiver = new CustomResultReceiver(new Handler(), this);
        intent.putExtra("receiver", resultReceiver);
        startService(intent);
    }



    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        lat = resultData.getDouble("lat");
        lon = resultData.getDouble("lon");
        if(lat != 0) {
            if(userLoc != null) {
                userLoc.remove();
            }
            myPlace = new LatLng(lat, lon);
            user.position(myPlace);
            userLoc = mMap.addMarker(user);
            if(camMoveToUser) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPlace, 13f));
                camMoveToUser = !camMoveToUser;
            }

            button.setOnClickListener(v -> {
                if(button.getText().equals(""))
                if(saveMarker != null && !markerChoosed) {
                    Drawable vectorBook = getResources().getDrawable(R.drawable.ic_baseline_menu_book_24);
                    Bitmap bitmapBook = getBitmap((VectorDrawable) vectorBook, R.color.black);
                    saveMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmapBook));
                    markerChoosed = true;
                    tasks.setText("Идите к маркеру квеста");

                }

            });
            if(markerChoosed) {
                LatLng mcoords = saveMarker.getPosition();
                Location locationOne = new Location("Quest");
                Location locationTwo = new Location("User");
                locationOne.setLatitude(mcoords.latitude);
                locationOne.setLongitude(mcoords.longitude);
                locationTwo.setLatitude(myPlace.latitude);
                locationTwo.setLongitude(myPlace.longitude);
                float distance = locationOne.distanceTo(locationTwo);
                if(distance < 7) {
                    Drawable vectorBook = getResources().getDrawable(R.drawable.ic_baseline_menu_book_24);
                    Bitmap bitmapBook = getBitmap((VectorDrawable) vectorBook, R.color.gold);
                    saveMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmapBook));
                    button.setText("Читать");
                    tasks.setText("Прочтите новеллу");
                }
            }

        }
        //textView.setText(lat + "   " + lon);
    }

    Bitmap getBitmap(VectorDrawable vectorDrawable, int color) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth()*2, vectorDrawable.getIntrinsicHeight()*2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setColorFilter(new PorterDuffColorFilter(getResources().getColor(color), PorterDuff.Mode.MULTIPLY));
        vectorDrawable.setBounds(0,0,canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(resultReceiver != null) {
        }
    }
}





