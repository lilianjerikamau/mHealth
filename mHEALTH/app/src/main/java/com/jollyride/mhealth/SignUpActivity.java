package com.jollyride.mhealth;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.jollyride.mhealth.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

public class SignUpActivity extends BaseActivity {

    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String verificationIdGlobal; // store OTP verification ID
    private View bottomLayout;
    private int originalBottomMargin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        this.getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        finish();
                    }
                });

        bottomLayout = findViewById(R.id.bottomLayout);
        TextInputEditText fullNameEditText = findViewById(R.id.fullNameEditText);
        TextInputEditText userNameEditText = findViewById(R.id.userNameEditText);
        TextInputEditText phoneNoEditText = findViewById(R.id.phoneNoEditText);
        TextInputEditText emailEditText = findViewById(R.id.emailEditText);
        AutoCompleteTextView genderDropdown = findViewById(R.id.genderDropdown);
        TextInputEditText bloodGroupEditText = findViewById(R.id.bloodGroupEditText);
        TextInputEditText dobEditText = findViewById(R.id.dobEditText);
        TextInputEditText addressEditText = findViewById(R.id.addressEditText);
        TextInputEditText passwordEditText = findViewById(R.id.passwordEditText);
        MaterialButton registerButton = findViewById(R.id.registerButton);
        TextView goToSignIn  = findViewById(R.id.goToSignIn);

        // Save the original bottom margin for later use
        originalBottomMargin = getOriginalBottomMargin();
        // Add Focus Change Listener on TextInputEditText
        addressEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // When EditText gets focus, increase the bottom margin
                increaseBottomMargin();
            } else {
                // When EditText loses focus, reset the bottom margin
                resetBottomMargin();
            }
        });
        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // When EditText gets focus, increase the bottom margin
                increaseBottomMargin();
            } else {
                // When EditText loses focus, reset the bottom margin
                resetBottomMargin();
            }
        });


        String[] genders = new String[]{"Male", "Female"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                genders
        );
        genderDropdown.setAdapter(adapter);

        dobEditText.setOnClickListener(view -> {
            final Calendar calendar = Calendar.getInstance();

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    SignUpActivity.this, // or use `getContext()` in a fragment
                    (view1, selectedYear, selectedMonth, selectedDay) -> {
                        String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        dobEditText.setText(date);
                    },
                    year, month, day
            );

            datePickerDialog.show();
        });


        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fullName = fullNameEditText.getText().toString().trim();
                String userName = userNameEditText.getText().toString().trim();
                String phoneNo = phoneNoEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();
                String gender = genderDropdown.getText().toString().trim();
                String bloodGroup = bloodGroupEditText.getText().toString().trim();
                String dob = dobEditText.getText().toString().trim();
                String address = addressEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                signUpUser(fullName,userName,phoneNo,email,gender,bloodGroup,dob,address,password);
                Intent intent = new Intent(SignUpActivity.this, VerifyActivity.class);
                intent.putExtra("phoneNo",phoneNo.toString());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            }
        });


        goToSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            }
        });

    }

    // Method to get the original bottom margin
    private int getOriginalBottomMargin() {
        ViewGroup.LayoutParams layoutParams = bottomLayout.getLayoutParams();
        if (layoutParams instanceof LinearLayout.LayoutParams) {
            return ((LinearLayout.LayoutParams) layoutParams).bottomMargin;
        } else if (layoutParams instanceof FrameLayout.LayoutParams) {
            return ((FrameLayout.LayoutParams) layoutParams).bottomMargin;
        }
        return 0; // Default to 0 if margin is not available
    }

    // Method to increase the bottom margin
    private void increaseBottomMargin() {
        ViewGroup.LayoutParams layoutParams = bottomLayout.getLayoutParams();
        if (layoutParams instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) layoutParams).bottomMargin = 500;  // Increase margin as needed
        } else if (layoutParams instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) layoutParams).bottomMargin = 500;  // Increase margin as needed
        }
        bottomLayout.setLayoutParams(layoutParams);
    }

    // Method to reset the bottom margin
    private void resetBottomMargin() {
        ViewGroup.LayoutParams layoutParams = bottomLayout.getLayoutParams();
        if (layoutParams instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) layoutParams).bottomMargin = originalBottomMargin;
        } else if (layoutParams instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) layoutParams).bottomMargin = originalBottomMargin;
        }
        bottomLayout.setLayoutParams(layoutParams);
    }
    public void signUpUser(
            String fullName, String userName, String phoneNo, String email, String gender,
            String bloodGroup, String dob, String address, String password) {

        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("fullName", fullName);
        userDetails.put("userName", userName);
        userDetails.put("phoneNo", phoneNo);
        userDetails.put("email", email);
        userDetails.put("gender", gender);
        userDetails.put("bloodGroup", bloodGroup);
        userDetails.put("dob", dob);
        userDetails.put("address", address);

        if (!TextUtils.isEmpty(email)) {
            // Email/Password Signup
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            saveUserToFirestore(userDetails);
                        } else {
                            Log.e("Sign Up", "Signup Failed: " + task.getException().getMessage());
                        }
                    });
        }
        if (!TextUtils.isEmpty(phoneNo)) {
            // Phone Auth
            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                    .setPhoneNumber(phoneNo)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(PhoneAuthCredential credential) {
                            mAuth.signInWithCredential(credential)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            saveUserToFirestore(userDetails);
                                        }
                                    });
                        }

                        @Override
                        public void onVerificationFailed(FirebaseException e) {
                            Log.e("Sign Up","Verification Failed: " + e.getMessage());
                        }

                        @Override
                        public void onCodeSent(String verificationId,
                                               PhoneAuthProvider.ForceResendingToken token) {
                            verificationIdGlobal = verificationId;
                            saveUserToFirestore(userDetails);
                            // Show UI to enter the OTP (optional)
                            Toast.makeText(SignUpActivity.this, "OTP Sent", Toast.LENGTH_SHORT).show();
                        }
                    }).build();

            PhoneAuthProvider.verifyPhoneNumber(options);
        }

        if(TextUtils.isEmpty(email) && TextUtils.isEmpty(phoneNo)){
            Toast.makeText(this, "Please enter either email or phone number", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserToFirestore(Map<String, Object> userDetails) {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).set(userDetails)
                .addOnSuccessListener(aVoid -> {
                    //Toast.makeText(this, "User registered and saved!", Toast.LENGTH_SHORT).show();
                    Log.d("Sign Up","User registered and saved!");
                })
                .addOnFailureListener(e -> {
                    //Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                    Log.e("Sign Up","Failed to save user data");
                });
    }

    private void saveLoginDetails(String username, String phone, String password) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    "secure_login_prefs",
                    masterKeyAlias,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("username", username);
            editor.putString("phone", phone);
            editor.putString("password", password);
            editor.apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}