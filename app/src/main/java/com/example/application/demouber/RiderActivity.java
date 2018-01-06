package com.example.application.demouber;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    LocationManager locationManager;

    LocationListener locationListener;

    Button callUberButton;

    Boolean requestActive = false;
    Boolean driverActive = true;
    Handler handler = new Handler();

    TextView infoTextView;


    public void checkForUpdate() {
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.whereExists("driverUsername");

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null) {
                    if(objects.size() > 0) {

                        driverActive = true;

                        ParseQuery<ParseUser> query = new ParseUser().getQuery();
                        query.whereEqualTo("username", objects.get(0).getString("driverUsername"));

                        query.findInBackground(new FindCallback<ParseUser>() {
                            @Override
                            public void done(List<ParseUser> objects, ParseException e) {

                                if(e == null && objects.size() > 0)  {
                                    ParseGeoPoint driverLocation = objects.get(0).getParseGeoPoint("location");
                                    if(Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(RiderActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {


                                        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                                        if(lastKnownLocation != null) {

                                            ParseGeoPoint userLocation = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                                            Double distance = driverLocation.distanceInKilometersTo(userLocation);
                                            Log.i("driverLocation", driverLocation.toString());
                                            Log.i("userLocation", userLocation.toString());
                                            Log.i("distance", distance.toString());
                                            if(distance < 0.01) {
                                                infoTextView.setText("Your driver is here!");
                                                ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
                                                query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
                                                query.findInBackground(new FindCallback<ParseObject>() {
                                                    @Override
                                                    public void done(List<ParseObject> objects, ParseException e) {
                                                        if(e == null && objects.size() > 0) {
                                                            for(ParseObject object : objects) {
                                                                object.deleteInBackground();
                                                            }
                                                        }
                                                    }
                                                });
                                                handler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        infoTextView.setText("");
                                                        callUberButton.setVisibility(View.VISIBLE);
                                                        callUberButton.setText(R.string.call_uber);
                                                        requestActive = false;
                                                        driverActive = false;

                                                    }
                                                }, 5000);
                                            } else {
                                                Double distanceOneDP = (double) Math.round(distance * 10)/10;
                                                infoTextView.setText("Your driver is " + distanceOneDP.toString() + " kilometer away!");

                                                LatLng driverLocationLatlng = new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude());
                                                LatLng requestLatitudeLatlng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

                                                ArrayList<Marker> markers = new ArrayList<>();

                                                Log.i("requestLatitudeLatlng", requestLatitudeLatlng.toString());
                                                mMap.clear();

                                                markers.add(mMap.addMarker(new MarkerOptions().position(driverLocationLatlng).title("Driver's Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))));
                                                markers.add(mMap.addMarker(new MarkerOptions().position(requestLatitudeLatlng).title("Your's Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))));

                                                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                                for (Marker marker : markers) {
                                                    builder.include(marker.getPosition());
                                                }
                                                LatLngBounds bounds = builder.build();

                                                int padding = 100; // offset from edges of the map in pixels
                                                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                                                // Add a marker in Sydney and move the camera
                                                mMap.animateCamera(cu);

                                                callUberButton.setVisibility(View.INVISIBLE);

                                                handler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        checkForUpdate();
                                                    }
                                                }, 2000);
                                            }

                                        }

                                    }

                                }
                            }
                        });


                    }

                }
            }
        });
    }

    public void logout(View view) {
        ParseUser.logOut();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public Location getLastKnownLocation() {
        Location lastKnownLocation = null;
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        return lastKnownLocation;

    }

    public void callUber(View view) {
        Log.i("callUber", "callUber");

        if(requestActive) {
            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");

            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e == null) {
                        if(objects.size() > 0) {

                            for(ParseObject object : objects) {
                                object.deleteInBackground();
                            }
                            requestActive = false;
                            callUberButton.setText(R.string.call_uber);
                            checkForUpdate();
                        }
                    }
                }
            });
        } else {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location lastKnownLocation = getLastKnownLocation();

                if(lastKnownLocation != null) {

                    ParseObject request = new ParseObject("Request");
                    request.put("username", ParseUser.getCurrentUser().getUsername());

                    ParseGeoPoint parseGeoPoint = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                    request.put("location", parseGeoPoint);
                    request.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if(e == null) {
                                callUberButton.setText(R.string.cancel_uber);
                                requestActive = true;
                                checkForUpdate();
                            }
                        }
                    });

                } else {
                    Toast.makeText(this, "Could not find location. Please try again later.", Toast.LENGTH_SHORT).show();
                }
            }
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1 ) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Location lastKnownLocation = getLastKnownLocation();

                if(lastKnownLocation != null) {
                    updateMap(lastKnownLocation);
                }
            }
        }
    }

    public void updateMap(Location location) {

        if(driverActive == false) {
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

            mMap.clear();

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12));
            mMap.addMarker(new MarkerOptions().position(userLocation).title("updateMap Your location"));
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        callUberButton = (Button) findViewById(R.id.callUberButton);
        infoTextView = (TextView) findViewById(R.id.infoTextView);

        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");

        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null) {
                    if(objects.size() > 0) {
                        requestActive = true;
                        callUberButton.setText(R.string.cancel_uber);

                        checkForUpdate();
                    }
                }
            }
        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
//                Log.i("onLocationChanged", "----");
                updateMap(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        if(Build.VERSION.SDK_INT < 23) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                Location lastKnownLocation = getLastKnownLocation();

                if(lastKnownLocation != null) {
                    updateMap(lastKnownLocation);
                }
            }
        }


        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }
}
