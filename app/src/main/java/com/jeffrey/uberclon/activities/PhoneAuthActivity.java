package com.jeffrey.uberclon.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.jeffrey.uberclon.R;
import com.jeffrey.uberclon.models.Client;
import com.jeffrey.uberclon.providers.AuthProvider;
import com.jeffrey.uberclon.providers.ClientProvider;

public class PhoneAuthActivity extends AppCompatActivity {

    Button mButtonCodeVerification;
    EditText mEditTextCodeVerification;

    String mExtraPhone;

    String mVerificationId;
    AuthProvider mAuthProvider;
    ClientProvider mClientProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_auth);

        mAuthProvider = new AuthProvider();
        mClientProvider = new ClientProvider();

        mButtonCodeVerification = findViewById(R.id.btnCodeVerification);
        mEditTextCodeVerification = findViewById(R.id.editTextCodeVerification);

        mExtraPhone = getIntent().getStringExtra("phone");
        Toast.makeText(this, "Telefono: " + mExtraPhone, Toast.LENGTH_LONG).show();

        mAuthProvider.sendCodeVerification(mExtraPhone, mCallbacks);

        mButtonCodeVerification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String code = mEditTextCodeVerification.getText().toString();
                if (!code.equals("") && code.length() >= 6) {
                    signIn(code);
                }
                else {
                    Toast.makeText(PhoneAuthActivity.this, "Debes ingresar el codigo", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
            // LA AUTENTICACION SE REALIZA EXITOSAMENTE
            // EL USUARIO HAYA INSERTADO CORRECTAMENTE EL CODIGO DE VERIFICACION
            // NUESTRO DISPOSITIVO MOVIL HAYA DETECTADO AUTOMATICAMENTE EL CODIGO

            String code = phoneAuthCredential.getSmsCode();

            if (code != null) {
                mEditTextCodeVerification.setText(code);
                signIn(code);
            }

        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            // CUANDO EL ENVIO DEL SMS FALLA
            Toast.makeText(PhoneAuthActivity.this, "Se produjo un error " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            // CODIGO DE VERIFICACION SE ENVIA TRAVES DE MENSAJE DE TEXT SMS
            super.onCodeSent(verificationId, forceResendingToken);
            Toast.makeText(PhoneAuthActivity.this, "El codigo se envio", Toast.LENGTH_LONG).show();
            mVerificationId = verificationId;
        }
    };

    private void signIn(String code) {
        mAuthProvider.signInPhone(mVerificationId, code).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // EL USUARIO YA INICIO SESION

                    mClientProvider.getClient(mAuthProvider.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Intent intent = new Intent(PhoneAuthActivity.this, MapClientActivity.class);
                                startActivity(intent);
                            }
                            else {
                                createInfo();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


                }
                else {
                    Toast.makeText(PhoneAuthActivity.this, "No se pudo iniciar sesion", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void createInfo() {
        Client client = new Client();
        client.setId(mAuthProvider.getId());
        client.setPhone(mExtraPhone);

        mClientProvider.create(client).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> taskCreate) {
                if (taskCreate.isSuccessful()) {
                    Intent intent = new Intent(PhoneAuthActivity.this, RegisterActivity.class);
                    startActivity(intent);
                }
                else {
                    Toast.makeText(PhoneAuthActivity.this, "No se pudo crear la informacion", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}