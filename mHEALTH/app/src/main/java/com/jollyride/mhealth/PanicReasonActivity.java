package com.jollyride.mhealth;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;

public class PanicReasonActivity extends BaseActivity {

    private MaterialRadioButton radioNoShare, radioCantContact, radioDriverLate,
            radioPriceNotReasonable, radioPickupIncorrect, radioOthers;
    private TextInputEditText editTextMessage;
    private Button buttonSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_panic_reason);

        radioNoShare = findViewById(R.id.radioNoShare);
        radioCantContact = findViewById(R.id.radioCantContact);
        radioDriverLate = findViewById(R.id.radioDriverLate);
        radioPriceNotReasonable = findViewById(R.id.radioPriceNotReasonable);
        radioPickupIncorrect = findViewById(R.id.radioPickupIncorrect);
        radioOthers = findViewById(R.id.radioOthers);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSubmit = findViewById(R.id.buttonSubmit);

        buttonSubmit.setOnClickListener(v -> {
            String selectedReason = "";
            if (radioNoShare.isChecked()) {
                selectedReason = radioNoShare.getText().toString();
            } else if (radioCantContact.isChecked()) {
                selectedReason = radioCantContact.getText().toString();
            } else if (radioDriverLate.isChecked()) {
                selectedReason = radioDriverLate.getText().toString();
            } else if (radioPriceNotReasonable.isChecked()) {
                selectedReason = radioPriceNotReasonable.getText().toString();
            } else if (radioPickupIncorrect.isChecked()) {
                selectedReason = radioPickupIncorrect.getText().toString();
            } else if (radioOthers.isChecked()) {
                selectedReason = radioOthers.getText().toString();
            }

            String message = editTextMessage.getText() != null ? editTextMessage.getText().toString() : "";

            // You can send the panic reason + message back to DriverOnTripActivity using intent extras
            Intent resultIntent = new Intent();
            resultIntent.putExtra("panicReason", selectedReason);
            resultIntent.putExtra("panicMessage", message);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}

