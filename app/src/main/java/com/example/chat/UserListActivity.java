package com.example.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.service.autofill.Dataset;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.example.chat.Chat.ANONYMOUS;

public class UserListActivity extends AppCompatActivity {


    public static class UserViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView usernameView;
        CircleImageView profileAvatarView;
        LinearLayout linearLayoutUser;
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            linearLayoutUser = itemView.findViewById(R.id.linear_layout_user);
            usernameView = itemView.findViewById(R.id.user_name);
            profileAvatarView = itemView.findViewById(R.id.profileAvatarView);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int pos = getAdapterPosition();

        }
    }


    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private GoogleSignInClient mSignInClient;

    private FirebaseRecyclerAdapter<User, UserViewHolder>
            mFirebaseAdapter;

    private RecyclerView usersRecyclerView;

    private DatabaseReference mFirebaseDatabaseReference;
    private String mUsername;
    private String mPhotoUrl;
    private List<String> friends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        mUsername = ANONYMOUS;

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mUsername = mFirebaseUser.getDisplayName();

            if (mFirebaseUser.getPhotoUrl() != null) {
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            }
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mSignInClient = GoogleSignIn.getClient(this, gso);

        usersRecyclerView = findViewById(R.id.users_recycler_view);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        friendsLoader();
    }

    private void friendsLoader() {
        DatabaseReference usersRef = mFirebaseDatabaseReference.child("users");
        SnapshotParser<User> parser = new SnapshotParser<User>() {
            @Override
            public User parseSnapshot(DataSnapshot dataSnapshot) {

                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    user.setId(dataSnapshot.getKey());
                }
                return user;
            }
        };

        FirebaseRecyclerOptions<User> options =
                new FirebaseRecyclerOptions.Builder<User>()
                        .setQuery(usersRef, parser)
                        .build();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<User, UserViewHolder>(options) {
            @Override
            public UserViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new UserViewHolder(inflater.inflate(R.layout.item_user, viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(final UserViewHolder viewHolder,
                                            int position,
                                            final User user) {

                if (friends.contains(user.getName()) && user.getName() != null && !user.getName().equals(mUsername)) {
                    viewHolder.usernameView.setText(user.getName());
                    Glide.with(UserListActivity.this)
                            .load(user.getPhotoUrl())
                            .into(viewHolder.profileAvatarView);
                    viewHolder.usernameView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            Toast.makeText( UserListActivity.this, user.getName(),Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(UserListActivity.this, Chat.class);

                            List<String> emails = new ArrayList<>();
                            emails.add(user.getName());
                            emails.add(mFirebaseUser.getDisplayName());

                            Collections.sort(emails);


                            intent.putExtra("chatRoomId", emails.get(0) + emails.get(1));
                            intent.putExtra("mPhotoUrl", mPhotoUrl);
                            intent.putExtra("mUsername", mUsername);
                            startActivity(intent);
                        }
                    });


                } else {
                    viewHolder.linearLayoutUser.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
                }
            }
        };

        usersRecyclerView.setAdapter(mFirebaseAdapter);
    }

    private void friendsChecker() {
        DatabaseReference friendsRef = mFirebaseDatabaseReference.child("users/"+mUsername+"/friends");

        friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                Toast.makeText(UserListActivity.this, "Hi", Toast.LENGTH_SHORT).show();
                friends = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Friends friend = ds.getValue(Friends.class);
                    friends.add(friend.getId());
                    Log.d("Friend", friend.getId());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                mFirebaseAuth.signOut();
                mSignInClient.signOut();

                mUsername = ANONYMOUS;
                startActivity(new Intent(this, SignInActivity.class));
                finish();
                return true;
            case R.id.add_user_menu:
                Intent intent = new Intent(this, AddUserActivity.class);
                intent.putExtra("mPhotoUrl", mPhotoUrl);
                intent.putExtra("mUsername", mUsername);
                startActivity(intent);
//                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        friendsChecker();
        mFirebaseAdapter.startListening();

    }

    @Override
    public void onPause() {
        mFirebaseAdapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        friendsChecker();
        friendsLoader();
        mFirebaseAdapter.startListening();

    }

}