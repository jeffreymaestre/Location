package com.jeffrey.uberclon.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jeffrey.uberclon.models.HistoryBooking;

import java.util.HashMap;
import java.util.Map;

public class HistoryBookingProvider {
    private DatabaseReference mDatabase;

    public HistoryBookingProvider() {
        mDatabase = FirebaseDatabase.getInstance().getReference().child("HistoryBooking");
    }

    public Task<Void> create(HistoryBooking historyBooking) {
        return mDatabase.child(historyBooking.getIdHistoryBooking()).setValue(historyBooking);
    }

    public  Task<Void> updateCalificactionClient(String idHistoryBooking, float calificacionClient) {
        Map<String, Object> map = new HashMap<>();
        map.put("calificationClient", calificacionClient);
        return mDatabase.child(idHistoryBooking).updateChildren(map);
    }

    public Task<Void> updateCalificactionDriver(String idHistoryBooking, float calificacionDriver) {
        Map<String, Object> map = new HashMap<>();
        map.put("calificationDriver", calificacionDriver);
        return mDatabase.child(idHistoryBooking).updateChildren(map);
    }

    public DatabaseReference getHistoryBooking(String idHistoryBooking) {
        return mDatabase.child(idHistoryBooking);
    }

}

