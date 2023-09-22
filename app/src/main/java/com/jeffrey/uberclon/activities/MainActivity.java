 package com.jeffrey.uberclon.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.hbb20.CountryCodePicker;
import com.jeffrey.uberclon.R;
import com.jeffrey.uberclon.providers.AuthProvider;


 public class MainActivity extends AppCompatActivity {

     Button mButtonGoToLogin;
     CountryCodePicker mCountryCode;
     EditText mEditTextPhone;

     AuthProvider mAuthProvider;

     @Override
     protected void onCreate(Bundle savedInstanceState) {
         setTheme(R.style.AppTheme);
         super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_main);

         mAuthProvider = new AuthProvider();

         mCountryCode = findViewById(R.id.ccp);
         mEditTextPhone = findViewById(R.id.editTextPhone);

         mButtonGoToLogin    = findViewById(R.id.btnGoToLogin);
         mButtonGoToLogin.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 goToLogin();
             }
         });

     }

     public void goToLogin() {
         String code = mCountryCode.getSelectedCountryCodeWithPlus();
         String phone = mEditTextPhone.getText().toString();

         if (!phone.equals("")) {
             Intent intent = new Intent(MainActivity.this, PhoneAuthActivity.class);
             intent.putExtra("phone", code + phone);
             startActivity(intent);
         }
         else {
             Toast.makeText(this, "Debes ingresar el telefono", Toast.LENGTH_SHORT).show();
         }

     }

     @Override
     protected void onStart() {
         super.onStart();
         if (mAuthProvider.existSession()) {
             Intent intent = new Intent(MainActivity.this, MapClientActivity.class);
             startActivity(intent);
         }
     }
 }