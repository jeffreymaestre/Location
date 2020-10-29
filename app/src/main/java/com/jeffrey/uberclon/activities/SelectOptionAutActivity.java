 package com.jeffrey.uberclon.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.jeffrey.uberclon.R;
import com.jeffrey.uberclon.activities.client.RegisterActivity;
import com.jeffrey.uberclon.activities.driver.RegisterDriverActivity;
import com.jeffrey.uberclon.includes.MyToolbar;


 public class SelectOptionAutActivity extends AppCompatActivity {


     Button mButtonGoToLogin;
     Button mButtonGoToRegister;
     SharedPreferences mPref;


     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_option_aut);

        MyToolbar.show(this, "Seleccionar opcion", true);
        mPref = getApplicationContext().getSharedPreferences("typeUser", MODE_PRIVATE);


        mButtonGoToLogin = findViewById(R.id.btnGoToLogin);
        mButtonGoToRegister = findViewById(R.id.btnGoToRegister);
        mButtonGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToLogin();
            }
        });
        mButtonGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToRegister();
            }
        });
    }

    public void goToLogin(){

        Intent intent= new Intent(SelectOptionAutActivity.this, LoginActivity.class);
        startActivity(intent);

    }

     public void goToRegister(){

         String typeUser = mPref.getString("user", "");
         if (typeUser.equals("client")){

             Intent intent= new Intent(SelectOptionAutActivity.this, RegisterActivity.class);
             startActivity(intent);
         }
            else{
             Intent intent= new Intent(SelectOptionAutActivity.this, RegisterDriverActivity.class);
             startActivity(intent);
         }

     }
}