package com.example.chat;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddUserActivity extends AppCompatActivity {

    EditText searchText;
    Button searchButton;
    private String mUsername;
    private String mPhotoUrl;


    private FirebaseRecyclerAdapter<User, UserListActivity.UserViewHolder>
            mFirebaseAdapter;

    private RecyclerView recyclerView;
    private DatabaseReference mFirebaseDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            mUsername = extras.getString("mUsername");
            mPhotoUrl = extras.getString("mPhotoUrl");
        }

        searchText = findViewById(R.id.search_user);
        searchButton = findViewById(R.id.search_button);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseSearch();
            }
        });


        recyclerView = findViewById(R.id.add_user_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();



//
    }

    private void firebaseSearch() {
        Query query = mFirebaseDatabaseReference.child("users").orderByChild("name").startAt(searchText.getText().toString());

        SnapshotParser<User> parser = new SnapshotParser<User>() {
            @Override
            public User parseSnapshot(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                Log.d("logged", user.getName());
                if (user != null) {
                    user.setId(dataSnapshot.getKey());
                }
                return user;
            }
        };

        FirebaseRecyclerOptions<User> options =
                new FirebaseRecyclerOptions.Builder<User>()
                        .setQuery(query, parser)
                        .build();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<User, UserListActivity.UserViewHolder>(options) {
            @Override
            public UserListActivity.UserViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new UserListActivity.UserViewHolder(inflater.inflate(R.layout.item_user, viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(final UserListActivity.UserViewHolder viewHolder,
                                            int position,
                                            final User user) {

                if (user.getName() != null && !user.getName().equals(mUsername)) {
                    viewHolder.usernameView.setText(user.getName());
                    Glide.with(AddUserActivity.this)
                            .load(user.getPhotoUrl())
                            .into(viewHolder.profileAvatarView);
                    viewHolder.usernameView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            AlertDialog alertDialog =  new AlertDialog.Builder(AddUserActivity.this)
                                    .setMessage("Найз нэмэх")
                                    .setNegativeButton("Үгүй", new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface arg0, int arg1) {

                                        }
                                    })
                                    .setPositiveButton("Тийм", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface arg0, int arg1) {

                                            mFirebaseDatabaseReference.child("users").orderByChild("name").equalTo(mUsername)
                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            for (DataSnapshot childSnapshot: snapshot.getChildren()) {
                                                                String key = childSnapshot.getKey();
//                                                                Log.d("Key", key);
                                                                mFirebaseDatabaseReference.child("users/"+key+"/friends").push().child("id").setValue(user.getName());
                                                            }

                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {

                                                        }
                                                    });
                                        }
                                    })
                                    .create();
                                    alertDialog.show();
                            //for negative side button
                            alertDialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(R.color.colorAccent);
//for positive side button
                            alertDialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(R.color.colorPrimary);
//                            Toast.makeText( AddUserActivity.this, user.getName(),Toast.LENGTH_SHORT).show();
//                            Intent intent = new Intent(AddUserActivity.this, Chat.class);
//
//                            List<String> emails = new ArrayList<>();
//                            emails.add(user.getName());
//                            emails.add(mUsername);
//
//                            Collections.sort(emails);
//
//
//                            intent.putExtra("chatRoomId", emails.get(0) + emails.get(1));
//                            intent.putExtra("mPhotoUrl", mPhotoUrl);
//                            intent.putExtra("mUsername", mUsername);
//                            startActivity(intent);
                        }
                    });


                } else {
                    viewHolder.linearLayoutUser.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
                }
            }
        };

        recyclerView.setAdapter(mFirebaseAdapter);
        mFirebaseAdapter.startListening();
    }

}