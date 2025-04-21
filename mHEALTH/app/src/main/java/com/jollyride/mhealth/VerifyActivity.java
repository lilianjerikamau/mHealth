package com.jollyride.mhealth;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.jollyride.mhealth.R;
import com.google.android.material.button.MaterialButton;

public class VerifyActivity extends BaseActivity {

    FirebaseAuth mAuth = FirebaseAuth.getInstance();

    EditText[] codeDigits = new EditText[4];
    private int currentIndex = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);

        this.getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        Intent intent = new Intent(VerifyActivity.this, SignInActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        finish();
                    }
                });

        codeDigits[0] = findViewById(R.id.codeDigit1);
        codeDigits[1] = findViewById(R.id.codeDigit2);
        codeDigits[2] = findViewById(R.id.codeDigit3);
        codeDigits[3] = findViewById(R.id.codeDigit4);

        TextView enable_loca = findViewById(R.id.enable_loca);
        try{
            String phoneNo = getIntent().getStringExtra("phoneNo");
            enable_loca.setText("A code has been sent to "+phoneNo+" via SMS");
        }catch (Exception e){}
        MaterialButton callButton = findViewById(R.id.callButton);

        callButton.setVisibility(View.GONE);

        for (EditText digit : codeDigits) {
            digit.setShowSoftInputOnFocus(false); // disables soft keyboard
            digit.setCursorVisible(false);        // hides blinking cursor
            digit.setFocusable(false);            // disables manual typing
            digit.setFocusableInTouchMode(false);
        }


        // Now set up the keypad after views are ready
        setupNumberPad();


        for (int i = 0; i < codeDigits.length; i++) {
            final int index = i;
            codeDigits[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < codeDigits.length - 1) {
                        codeDigits[index + 1].requestFocus();
                    }
                }
            });

            codeDigits[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_DEL &&
                        codeDigits[index].getText().toString().isEmpty() &&
                        index > 0) {
                    codeDigits[index - 1].requestFocus();
                }
                return false;
            });

            codeDigits[i].setOnFocusChangeListener((v, hasFocus) -> {
                View underline = ((ViewGroup) v.getParent()).getChildAt(1);
                underline.setSelected(hasFocus || !((EditText) v).getText().toString().isEmpty());
            });
        }

        //Error State
        //You can also toggle underline color to red (error) like this:
        /*for (EditText digit : codeDigits) {
            View underline = ((ViewGroup) digit.getParent()).getChildAt(1);
            underline.setBackgroundResource(R.color.underline_error); // switch to red
        }*/


    }

    private void setupNumberPad() {
        int[] ids = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3,
                R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7,
                R.id.btn8, R.id.btn9
        };

        for (int id : ids) {
            Button btn = findViewById(id);
            btn.setOnClickListener(v -> {
                if (currentIndex < codeDigits.length) {
                    codeDigits[currentIndex].setText(btn.getText().toString());
                    currentIndex++;
                }
            });
        }

        ImageButton deleteBtn = findViewById(R.id.btnDelete);
        deleteBtn.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                codeDigits[currentIndex].setText("");
            }
        });
    }


    private void triggerShakeAnimation() {
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);

        for (EditText digit : codeDigits) {
            digit.startAnimation(shake);

            // Also switch underline to red if needed
            View underline = ((ViewGroup) digit.getParent()).getChildAt(1);
            underline.setBackgroundResource(R.color.underline_error);
        }
        // 🔥 Add vibration here
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(150); // milliseconds
            }
        }
    }

    /*public void onDoneClicked(View view) {
        StringBuilder code = new StringBuilder();
        boolean hasEmpty = false;

        for (EditText digit : codeDigits) {
            String val = digit.getText().toString();
            if (val.isEmpty()) {
                hasEmpty = true;
                break;
            }
            code.append(val);
        }

        if (hasEmpty || !code.toString().equals("1234")) { // Example check
            triggerShakeAnimation();
            // 🔄 Reset underline after 2 seconds
            new Handler().postDelayed(() -> {
                for (EditText digit : codeDigits) {
                    View underline = ((ViewGroup) digit.getParent()).getChildAt(1);
                    underline.setBackgroundResource(R.drawable.line_underline_selector);
                }
            }, 2000);
        } else {
            // Proceed with success
        }
    }*/

    public void onDoneClicked(View view) {
        StringBuilder code = new StringBuilder();
        for (EditText digit : codeDigits) {
            code.append(digit.getText().toString());
        }
        Log.e("Verify","Code: "+code);
        if (code.length() < 4 || !code.toString().equals("1234") ) {
            triggerShakeAnimation(); // Optional: Shake/vibrate on error
            // Delay reset to allow shake to be visible
            new Handler().postDelayed(() -> resetCodeInput(), 400);
            return;
        }

        // Proceed to verify
        Intent intent = new Intent(VerifyActivity.this, BookingDemoActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private void resetCodeInput() {
        for (EditText digit : codeDigits) {
            digit.setText("");
        }
        currentIndex = 0;
        codeDigits[0].requestFocus(); // move to the first digit
    }


    public void verifyCodeManually(String verificationIdGlobal, String otp) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationIdGlobal, otp);
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Save to Firestore here too if not already done
            }
        });
    }





}