package com.jollyride.mhealth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class SignInActivity extends BaseActivity {

    MaterialButton signInButtom;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        this.getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        finish();
                    }
                });

        TextInputEditText userNameEditText = findViewById(R.id.userNameEditText);
        TextInputEditText passwordEditText = findViewById(R.id.passwordEditText);
        progressBar = findViewById(R.id.progressBar);
        signInButtom = findViewById(R.id.signInButtom);
        TextView signInDriverButtom = findViewById(R.id.signInDriverButtom);
        TextView goToSignUp = findViewById(R.id.goToSignUp);

        signInButtom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = userNameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                signInButtom.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                signInUser(userName, password, "rider");
            }
        });
        signInDriverButtom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = userNameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                signInUser(userName, password, "driver");
            }
        });

        goToSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            }
        });
    }

    public void signInUser(String identifier, String password, String userType) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (Patterns.PHONE.matcher(identifier).matches()) {
            // 📱 Identifier is phone number – start OTP login
            startPhoneLogin(identifier);
        } else if (Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
            // 📧 Identifier is email – login directly
            mAuth.signInWithEmailAndPassword(identifier, password)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(this, "Logged in via email", Toast.LENGTH_SHORT).show();

                        // 🔥 Update userType in Firestore
                        String userId = mAuth.getCurrentUser().getUid();
                        db.collection("users").document(userId)
                                .update("userType", userType)
                                .addOnSuccessListener(unused -> Log.d("Firestore", "User type set to: " + userType))
                                .addOnFailureListener(e -> Log.e("Firestore", "Failed to set user type: " + e.getMessage()));

                        // Proceed to next activity
                        if (userType.equals("rider")) {
                            Intent intent = new Intent(SignInActivity.this, RiderHomeActivity.class);
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(SignInActivity.this, DriverHomeActivity.class);
                            startActivity(intent);
                        }
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        signInButtom.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // 🤔 Assume it's a username – lookup in Firestore
            db.collection("users")
                    .whereEqualTo("userName", identifier)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            String email = queryDocumentSnapshots.getDocuments().get(0).getString("email");
                            String phone = queryDocumentSnapshots.getDocuments().get(0).getString("phoneNo");

                            if (!TextUtils.isEmpty(email)) {
                                // 🔐 Sign in with email and password
                                mAuth.signInWithEmailAndPassword(email, password)
                                        .addOnSuccessListener(authResult -> {
                                            Log.d("Sign In", "Logged in with username via email");

                                            // 🔥 Update userType in Firestore
                                            String userId = mAuth.getCurrentUser().getUid();
                                            db.collection("users").document(userId)
                                                    .update("userType", userType)
                                                    .addOnSuccessListener(unused -> Log.d("Firestore", "User type set to: " + userType))
                                                    .addOnFailureListener(e -> Log.e("Firestore", "Failed to set user type: " + e.getMessage()));

                                            if (userType.equals("rider")) {
                                                Intent intent = new Intent(SignInActivity.this, RiderHomeActivity.class);
                                                startActivity(intent);
                                            } else {
                                                Intent intent = new Intent(SignInActivity.this, DriverHomeActivity.class);
                                                startActivity(intent);
                                            }
                                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            signInButtom.setVisibility(View.VISIBLE);
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            Log.e("Sign In", "Login failed: " + e.getMessage());
                                        });
                            } else if (!TextUtils.isEmpty(phone)) {
                                // 🔄 Fallback to phone login
                                startPhoneLogin(phone);
                            } else {
                                signInButtom.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "No valid login method found for username", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            signInButtom.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        signInButtom.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void startPhoneLogin(String phoneNumber) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        FirebaseAuth.getInstance().signInWithCredential(credential)
                                .addOnSuccessListener(authResult -> {
                                    Toast.makeText(SignInActivity.this, "Phone login successful!", Toast.LENGTH_SHORT).show();
                                });
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        signInButtom.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        Log.e("Sign In", "Verification failed: " + e.getMessage());
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        Toast.makeText(SignInActivity.this, "OTP sent to your phone.", Toast.LENGTH_SHORT).show();
                    }
                }).build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }
}
