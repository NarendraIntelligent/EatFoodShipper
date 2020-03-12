package com.example.narendra.eatfoodshipper;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.narendra.eatfoodshipper.Common.Common;
import com.example.narendra.eatfoodshipper.Model.Shipper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

import info.hoang8f.widget.FButton;

public class MainActivity extends AppCompatActivity {
    FButton btn_sign_in;
    MaterialEditText edt_phone,edt_password;
    FirebaseDatabase database;
    DatabaseReference shippers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_sign_in=(FButton)findViewById(R.id.btnSignIn);
        edt_password=(MaterialEditText)findViewById(R.id.edtPassword);
        edt_phone=(MaterialEditText)findViewById(R.id.edtPhone);

        //firebase
        database=FirebaseDatabase.getInstance();
        shippers=database.getReference(Common.SHIPPER_TABLE);
        btn_sign_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login(edt_phone.getText().toString(),edt_password.getText().toString());
                
            }
        });
    }

    private void login(String phone, final String password) {
        shippers.child(phone)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists())
                        {
                            Shipper shipper=dataSnapshot.getValue(Shipper.class);
                            if(shipper.getPassword().equals(password)){
                                startActivity(new Intent(MainActivity.this,HomeActivity.class));
                                Toast.makeText(MainActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                Common.currentShipper=shipper;
                               finish();
                            }
                            else {
                                Toast.makeText(MainActivity.this,"Password incorrect!",Toast.LENGTH_SHORT).show();
                            }

                        }
                        else {
                            Toast.makeText(MainActivity.this,"Your shipper's phone not exists",Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

}
