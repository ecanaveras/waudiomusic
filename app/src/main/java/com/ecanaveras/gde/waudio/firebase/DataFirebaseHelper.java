package com.ecanaveras.gde.waudio.firebase;

import android.content.Context;

import com.ecanaveras.gde.waudio.models.WaudioModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by elcap on 08/09/2017.
 */

public class DataFirebaseHelper {

    public static final String REF_WAUDIO_STORE = "waudio-store";
    public static final String REF_WAUDIO_EVENTS = "waudio-events";
    public static final String REF_WAUDIO_TEMPLATES = "waudio-templates";

    private DatabaseReference mDatabase;
    private DatabaseReference mRef;

    public DataFirebaseHelper() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    /**
     * Incremente el valor del child
     *
     * @param ref
     * @param child
     */
    private void incrementValue(String ref, String child, final long newValue) {
        mRef = mDatabase.child(ref).child(child);
        mRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long value = 0;
                if (dataSnapshot.getValue() != null) {
                    value = (long) dataSnapshot.getValue();
                }
                value += (newValue != 0 ? newValue : 1);
                dataSnapshot.getRef().setValue(value);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void incrementValue(String ref, String child) {
        incrementValue(ref, child, 0);
    }

    public void incrementGotoStore() {
        incrementValue(REF_WAUDIO_STORE, "visitas");
    }

    public void incrementWaudioShared() {
        incrementValue(REF_WAUDIO_EVENTS, "compartidos");
    }

    public void incrementWaudioCreated() {
        incrementValue(REF_WAUDIO_EVENTS, "creados");
    }

    public void incrementWaudioDeleted() {
        incrementValue(REF_WAUDIO_EVENTS, "eliminados");
    }

    public void incrementWaudioDeleted(long cantDeletes) {
        incrementValue(REF_WAUDIO_EVENTS, "eliminados", cantDeletes);
    }

    public DatabaseReference getDatabaseReference(String nameRef) {
        return mDatabase.child(nameRef);
    }


}
