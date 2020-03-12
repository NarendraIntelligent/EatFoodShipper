package com.example.narendra.eatfoodshipper;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.narendra.eatfoodshipper.Common.Common;
import com.example.narendra.eatfoodshipper.Common.DirectionJSONParser;
import com.example.narendra.eatfoodshipper.Model.Requests;
import com.example.narendra.eatfoodshipper.Remote.IGeoCoordinates;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dmax.dialog.SpotsDialog;
import info.hoang8f.widget.FButton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrackingOrder extends FragmentActivity implements OnMapReadyCallback {
    private static final int MY_PERMISSIONS_REQUEST_READ_FINE_LOCATION = 100;

    private GoogleMap mMap;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;
    LocationRequest locationRequest;
    Location mLastLocation;
    Marker mCurrentMarker;
    IGeoCoordinates mService;
    Polyline polyline;
    FButton btn_call, btn_shipped;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_order);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        btn_call = (FButton) findViewById(R.id.btn_call);
        btn_shipped = (FButton) findViewById(R.id.btn_shipped);
        btn_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        btn_shipped.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + Common.currentRequests.getPhone()));
                if (ActivityCompat.checkSelfPermission(TrackingOrder.this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                startActivity(intent);
            }
        });
        btn_shipped.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //we will delete order in table
                //OrderNEEDSHIP
                //ShippingOrder
                //And update status of order to shipped
                shippedOrder();
            }
        });
        mService=Common.getGeoCodeService();
        buildLocationRequest();
        buildLocationCallBack();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.
                ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission
                (this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback, Looper.myLooper());

    }

    private void shippedOrder() {
        FirebaseDatabase.getInstance()
                .getReference(Common.ORDER_NEED_SHIP_TABLE)
                .child(Common.currentShipper.getPhone())
                .child(Common.currentKey)
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Map<String,Object> update_status=new HashMap<>();
                        update_status.put("status","03");

                        FirebaseDatabase.getInstance()
                                .getReference("Requests")
                                .child(Common.currentKey)
                                .updateChildren(update_status)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        //Delete From Shipping order
                                        FirebaseDatabase.getInstance()
                                                .getReference(Common.SHIPPER_INFO_TABLE)
                                                .child(Common.currentKey)
                                                .removeValue()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Toast.makeText(TrackingOrder.this, "Shipped!", Toast.LENGTH_SHORT).show();
                                                          finish();
                                                    }
                                                });


                                    }
                                });

                    }
                });


    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                mLastLocation = locationResult.getLastLocation();
                if(mCurrentMarker!=null)
                    mCurrentMarker.setPosition(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));

                //update location to firebase
                Common.updateShippingInformation(Common.currentKey,mLastLocation);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude())));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(16.0f));
                drawRoute(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()),Common.currentRequests);

            }

            private void drawRoute(final LatLng yourLocation, Requests requests) {
                //clear all polyline
                if(polyline!=null){
                    polyline.remove();
                }
                if(requests.getAddress() !=null && !requests.getAddress().isEmpty())
                {
                    mService.getGeoCode(requests.getAddress()).enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response.body().toString());

                                String lat = ((JSONArray) jsonObject.get("results"))
                                        .getJSONObject(0)
                                        .getJSONObject("geometry")
                                        .getJSONObject("location")
                                        .get("lat").toString();
                                String lng = ((JSONArray) jsonObject.get("results"))
                                        .getJSONObject(0)
                                        .getJSONObject("geometry")
                                        .getJSONObject("location")
                                        .get("lng").toString();
                                LatLng orderLocation = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));

                                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.eatfood);
                                bitmap = Common.scaleBitmap(bitmap, 70, 70);
                                MarkerOptions marker = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                                        .title("Order of" + Common.currentRequests.getPhone())
                                        .position(orderLocation);
                                mMap.addMarker(marker);
                                mService.getDirections(yourLocation.latitude+","+yourLocation.longitude,orderLocation.latitude

                                        +","+orderLocation.longitude)
                                        .enqueue(new Callback<String>() {
                                            @Override
                                            public void onResponse(Call<String> call, Response<String> response) {
                                                new ParseTask().execute(response.body().toString());

                                            }

                                            @Override
                                            public void onFailure(Call<String> call, Throwable t) {

                                            }
                                        });
                            }
                            catch (JSONException e){
                                e.printStackTrace();
                            }}
                        //  draw route

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {

                        }
                    });
                }
                else {
                    if (requests.getLatLng()!=null && !requests.getLatLng().isEmpty())
                    {
                      String[] latlng= requests.getLatLng().split(",");
                      LatLng orderLocation=new LatLng(Double.parseDouble(latlng[0]),Double.parseDouble(latlng[1]));
                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.eatfood);
                        bitmap = Common.scaleBitmap(bitmap, 70, 70);
                        MarkerOptions marker = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                                .title("Order of" + Common.currentRequests.getPhone())
                                .position(orderLocation);
                        mMap.addMarker(marker);
                        mService.getDirections(mLastLocation.getLatitude()+","+mLastLocation.getLongitude(),orderLocation.latitude+","+orderLocation.longitude)
                                .enqueue(new Callback<String>() {
                                    @Override
                                    public void onResponse(Call<String> call, Response<String> response) {
                                        new ParseTask().execute(response.body().toString());
                                    }

                                    @Override
                                    public void onFailure(Call<String> call, Throwable t) {

                                    }
                                }); }
                }
                }
        };
        }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(10f);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        }
    @Override
    protected void onStop() {
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);

        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
//       boolean isSuccess=mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,R.raw.uber_style));
//if(!isSuccess)
//    Log.d("ERROR","MAP STYLE LOAD FAILED");

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                mLastLocation=location;
                LatLng yourLocation = new LatLng(location.getLatitude(), location.getLongitude());
               mCurrentMarker= mMap.addMarker(new MarkerOptions().position(yourLocation).title("Your location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(yourLocation));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(16.0f));

            }
        });
    }

    private class ParseTask extends AsyncTask<String,Integer,List<List<HashMap<String,String>>>> {
      AlertDialog mDialog=new SpotsDialog.Builder().setContext(TrackingOrder.this).build();
        protected void onPreExecute () {
            super.onPreExecute();
            mDialog.show();
            mDialog.setMessage("Please Waiting");
            }

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jobject;
            List<List<HashMap<String,String>>> routes=null;
            try {
                jobject=new JSONObject(strings[0]);
                DirectionJSONParser Parser=new   DirectionJSONParser();
                routes= Parser.parse(jobject);



            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;

        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            mDialog.dismiss();
              ArrayList<LatLng> points = new ArrayList<LatLng>();;
            PolylineOptions lineOptions = new PolylineOptions();;
            MarkerOptions markerOptions = new MarkerOptions();
            LatLng position = null;
//            lineOptions.width(2);
//            lineOptions.color(Color.RED);


            for (int i=0;i<lists.size();i++)
            {


                List<HashMap<String,String>> path=lists.get(i);

                for (int j=0;j<path.size();j++)
                {
                    HashMap<String,String> point=path.get(j);
                    double lat=Double.parseDouble(point.get("lat"));
                    double lng=Double.parseDouble(point.get("lng"));
                   position=new LatLng(lat,lng);
                    points.add(position);
                }


                mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(10));

                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.BLUE);
                lineOptions.geodesic(true);


            }
            mMap.addPolyline(lineOptions);
        }
    }


}
