package com.jeffrey.uberclon.providers;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class InfoProvider {
    DatabaseReference mDatabase;

    public InfoProvider() {
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Info");
    }

    public DatabaseReference getInfo(){
        return mDatabase;
    }
}
