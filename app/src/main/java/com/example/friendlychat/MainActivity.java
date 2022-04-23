package com.example.friendlychat;

/** import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
 */
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final String FRIENDLY_MSG_LENGTH_KEY= "friendly_msg_length";
    public static final int RC_SIGN_IN = 1;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    Uri mImageUri;
String downloadUrl;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessageDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
   // private FirebaseStorage mFirebaseStorage;
   StorageReference ref,Strg;
   private FirebaseRemoteConfig mFirebaseRemoteConfig;


    private String mUsername;

    private static final int RC_PHOTO_PICKER =  2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth =FirebaseAuth.getInstance();
       // mFirebaseStorage = FirebaseStorage.getInstance();


        mMessageDatabaseReference =mFirebaseDatabase.getReference().child("messages");
      // mChatPhotoStorageReference = mFirebaseStorage.getReferenceFromUrl("gs://friendlychat-e98c4.appspot.com");
      // mChatPhotoStorageReference = mFirebaseStorage.getReference("chat_photos");
        Strg= FirebaseStorage.getInstance().getReference();

        mFirebaseRemoteConfig= FirebaseRemoteConfig.getInstance();


        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"),RC_PHOTO_PICKER);
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               
                FriendlyMessage friendlyMessage= new FriendlyMessage(mMessageEditText.getText().toString(),mUsername,null);
                mMessageDatabaseReference.push().setValue(friendlyMessage);
                // Clear input box
                mMessageEditText.setText("");
            }
        });

    mAuthStateListener =new FirebaseAuth.AuthStateListener() {
        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
          FirebaseUser user = firebaseAuth.getCurrentUser();
          if(user !=null){
              //user is signed in

              //Toast.makeText(MainActivity.this, "You're now signed in",Toast.LENGTH_SHORT).show();
              onSignedInIntialize(user.getDisplayName());

          }else{
              onSignedOutCleanup();
              //user is signed out and that's where firebase UI come in picture
              startActivityForResult(
                      AuthUI.getInstance()
                              .createSignInIntentBuilder()
                              .setAvailableProviders(Arrays.asList(
                                      new AuthUI.IdpConfig.EmailBuilder().build(),

                                      new AuthUI.IdpConfig.GoogleBuilder().build()))


                              .build(),
                      RC_SIGN_IN);
          }
        }
    };

       /** FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

        Map<String , Object> defaultConfigMap =new HashMap<>();
        defaultConfigMap.put(FRIENDLY_MSG_LENGTH_KEY, DEFAULT_MSG_LENGTH_LIMIT);
         mFirebaseRemoteConfig.setDefaultsAsync(defaultConfigMap);
         fetchConfig();  */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
            return super.onOptionsItemSelected(item);
        }


    }

    @Override
    protected void onPause(){
        super.onPause();
        if(mAuthStateListener !=null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
        mMessageAdapter.clear();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode,data);
        if (requestCode== RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "signed in", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "sign in Canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        if (resultCode == RESULT_OK) {

            // compare the resultCode with the
            // SELECT_PICTURE constant
            if (requestCode == RC_PHOTO_PICKER) {
                // Get the url of the image from data
                mImageUri = data.getData();
                ref = FirebaseStorage.getInstance().getReference("chat_photos");
                ref.putFile(mImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        Strg.child("chat_photos").getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                downloadUrl= task.getResult().toString();
                                FriendlyMessage friendlyMessage= new FriendlyMessage(null,mUsername,downloadUrl);
                                mMessageDatabaseReference.push().setValue(friendlyMessage);

                            }
                        });


                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });

            }
        }

        if(mImageUri != null){


                    

        }else{
            Toast.makeText(this, "no file selected", Toast.LENGTH_SHORT).show();
        }




        }



    @Override
    protected void onResume(){
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
    private void onSignedInIntialize(String username){
    mUsername= username;
        attachDatabaseReadListener();
    }


    private void onSignedOutCleanup(){
   mUsername= ANONYMOUS;
    mMessageAdapter.clear();

    }
    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    FriendlyMessage friendlyMessage = snapshot.getValue(FriendlyMessage.class);
                    mMessageAdapter.add(friendlyMessage);
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            mMessageDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    private  void detachDatabaseReadListener(){
        if(mChildEventListener !=null) {
            mMessageDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener =null;
        }
    }



  /** public void fetchConfig() {
       long cacheExpiration = 3600; // 1 hour in seconds
       // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
       // server. This should not be used in release builds.

       mFirebaseRemoteConfig.fetch(cacheExpiration)
               .addOnSuccessListener(new OnSuccessListener<Void>() {
                   @Override
                   public void onSuccess(Void aVoid) {
                       // Make the fetched config available
                       // via FirebaseRemoteConfig get<type> calls, e.g., getLong, getString.
                       mFirebaseRemoteConfig.fetchAndActivate();

                       // Update the EditText length limit with
                       // the newly retrieved values from Remote Config.
                       applyRetrievedLengthLimit();
                   }
               })
               .addOnFailureListener(new OnFailureListener() {
                   @Override
                   public void onFailure(@NonNull Exception e) {
                       // An error occurred when fetching the config.
                       Log.w(TAG, "Error fetching config", e);

                       // Update the EditText length limit with
                       // the newly retrieved values from Remote Config.
                       applyRetrievedLengthLimit();
                   }
               });
   }

    /**
     * Apply retrieved length limit to edit text field. This result may be fresh from the server or it may be from
     * cached values.
     */
   /** private void applyRetrievedLengthLimit() {
        Long friendly_msg_length = mFirebaseRemoteConfig.getLong(FRIENDLY_MSG_LENGTH_KEY);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(friendly_msg_length.intValue())});
        Log.d(TAG, FRIENDLY_MSG_LENGTH_KEY + " = " + friendly_msg_length);
    } */

}
