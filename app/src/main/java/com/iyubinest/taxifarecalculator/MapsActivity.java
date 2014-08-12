package com.iyubinest.taxifarecalculator;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements GoogleMap.OnMapClickListener{

    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private List<MarkerOptions> mMarkerOptionsList = new ArrayList<MarkerOptions>();
    private AsyncHttpClient mAsyncHttpClient = new AsyncHttpClient();

    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initViews();
    }

    private void initViews() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            mMap.setOnMapClickListener(this);
            mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        }
        findCurrentLocation();
    }

    private void findCurrentLocation() {
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mLocationManager.removeUpdates(mLocationListener);
                mProgressBar.setVisibility(View.GONE);
                centerMapUsingLocation(location);
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
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 50, mLocationListener);
    }

    private void centerMapUsingLocation(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng,15);
        mMap.animateCamera(cameraUpdate);
    }

    private void getRouteInfoUsingJsonArray(JSONArray jsonSteps) {
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.add(mMarkerOptionsList.get(0).getPosition());
        for(int i = 0; i<jsonSteps.length();i++){
            try{
                JSONObject endLocation = jsonSteps.getJSONObject(i).getJSONObject("end_location");
                LatLng latLng = new LatLng(endLocation.getDouble("lat"),endLocation.getDouble("lng"));
                polylineOptions.add(latLng);
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        polylineOptions.add(mMarkerOptionsList.get(1).getPosition());
        polylineOptions.color(getResources().getColor(R.color.green_app));
        polylineOptions.width(10);
        mMap.addPolyline(polylineOptions);
    }

    private void calculateFare(int distance) {
        int units = distance/100 +25;
        if(units<50) units=50;
        String message = String.valueOf(units*75) + " $";
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();
    }

    private String getRouteUrl(LatLng start, LatLng end){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("https://maps.googleapis.com/maps/api/directions/json?origin=");
        stringBuilder.append(start.latitude);
        stringBuilder.append(",");
        stringBuilder.append(start.longitude);
        stringBuilder.append("&destination=");
        stringBuilder.append(end.latitude);
        stringBuilder.append(",");
        stringBuilder.append(end.longitude);
        stringBuilder.append("&sensor=false");
        return stringBuilder.toString();
    }

    public void restartMap(View view){
        mMap.clear();
        mMarkerOptionsList.clear();
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if(mMarkerOptionsList.size()<2){
            MarkerOptions marker = new MarkerOptions().position(latLng);
            mMap.addMarker(marker);
            mMarkerOptionsList.add(marker);
        }
        if(mMarkerOptionsList.size()==2){
            mProgressBar.setVisibility(View.VISIBLE);
            mAsyncHttpClient.get(getRouteUrl(mMarkerOptionsList.get(0).getPosition(),mMarkerOptionsList.get(1).getPosition()),new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    super.onSuccess(statusCode, headers, response);
                    mProgressBar.setVisibility(View.GONE);
                    try{
                        JSONObject legsObject = response.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0);
                        int distance = legsObject.getJSONObject("distance").getInt("value");
                        JSONArray stepsArray = legsObject.getJSONArray("steps");
                        calculateFare(distance);
                        getRouteInfoUsingJsonArray(stepsArray);
                    }catch (JSONException e){
                        e.printStackTrace();
                    }

                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);
                    restartMap(null);
                }
            });
        }
    }
}
