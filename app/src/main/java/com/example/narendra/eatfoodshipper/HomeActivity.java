package com.example.narendra.eatfoodshipper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.narendra.eatfoodshipper.Common.Common;
import com.example.narendra.eatfoodshipper.Model.Requests;
import com.example.narendra.eatfoodshipper.Model.Token;
import com.example.narendra.eatfoodshipper.ViewHolder.OrderViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class HomeActivity extends AppCompatActivity {
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;
    LocationRequest locationRequest;
    Location mLastLocation;
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    FirebaseDatabase database;
    DatabaseReference shipperOrders;
    FirebaseRecyclerAdapter<Requests,OrderViewHolder> adapter;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        //check permission
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                   Manifest.permission.ACCESS_FINE_LOCATION,
                       Manifest.permission.ACCESS_COARSE_LOCATION


                }, Common.REQUEST_CODE);

            } else {
                buildLocationRequest();
                buildLocationCallBack();
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            }
        else {
            buildLocationRequest();
            buildLocationCallBack();
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }
        database=FirebaseDatabase.getInstance();
shipperOrders=database.getReference(Common.ORDER_NEED_SHIP_TABLE);
recyclerView=(RecyclerView)findViewById(R.id.recycler_orders);
recyclerView.setHasFixedSize(true);
layoutManager=new LinearLayoutManager(this);
recyclerView.setLayoutManager(layoutManager);
updateTokenShipper(FirebaseInstanceId.getInstance().getToken());
loadAllOrderNeedShip(Common.currentShipper.getPhone());
    }

    private void loadAllOrderNeedShip(String phone) {
        DatabaseReference orderInchildofShipper=shipperOrders.child(phone);
        FirebaseRecyclerOptions<Requests> listorders=new FirebaseRecyclerOptions.Builder<Requests>()
                .setQuery(orderInchildofShipper,Requests.class)
                .build();
        adapter=new FirebaseRecyclerAdapter<Requests, OrderViewHolder>(listorders) {
            @Override
            protected void onBindViewHolder(@NonNull OrderViewHolder viewHolder, final int position, @NonNull final Requests model) {
                viewHolder.txtOrderId.setText(adapter.getRef(position).getKey());
                viewHolder.txtOrderStatus.setText(Common.convertCodeToStatus(model.getStatus()));
                viewHolder.txtOrderAddress.setText(model.getAddress());
                viewHolder.txtOrderPhone.setText(model.getPhone());
                viewHolder.txtOrderDate.setText(Common.getDate(Long.parseLong(adapter.getRef(position).getKey())));
                viewHolder.btnshipping.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                      Common.createShippingOrder(adapter.getRef(position).getKey(),
                              Common.currentShipper.getPhone(),
                              mLastLocation);
                      Common.currentRequests =model;
                      Common.currentKey=adapter.getRef(position).getKey();
                      startActivity(new Intent(HomeActivity.this,MapsActivity.class));

                    }
                });
            }

            @NonNull
            @Override
            public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemview= LayoutInflater.from(parent.getContext()).inflate(R.layout.order_view_layout,parent,false);
                return new OrderViewHolder(itemview);

            }
        };
        adapter.startListening();
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);
    }

    private void updateTokenShipper(String token) {
        DatabaseReference tokens=database.getReference("Tokens");
        Token data=new Token(token,false);
        tokens.child(Common.currentShipper.getPhone()).setValue(data);
    }
    protected void onResume(){
        super.onResume();
        loadAllOrderNeedShip(Common.currentShipper.getPhone());
    }

    @Override
    protected void onStop() {
        if(adapter!=null)
            adapter.stopListening();
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Common.REQUEST_CODE: {
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        buildLocationRequest();
                        buildLocationCallBack();
                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                            return;
                        }
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

                    }
                    else {
                        Toast.makeText(this,"You should assign permission !",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void buildLocationCallBack() {
        locationCallback=new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                mLastLocation=locationResult.getLastLocation();

            }
        };

    }

    private void buildLocationRequest() {
        locationRequest=new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(10f);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);

    }
}
