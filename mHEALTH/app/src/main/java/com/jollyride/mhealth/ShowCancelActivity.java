package com.jollyride.mhealth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

public class ShowCancelActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bottom_sheet_cancel_order);
        showCancelOrderBottomSheet();

    }

    public void showCancelOrderBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_cancel_order, null);
        bottomSheetDialog.setContentView(sheetView);

        MaterialButton btnNo = sheetView.findViewById(R.id.btn_no);
        MaterialButton btnYes = sheetView.findViewById(R.id.btn_yes);

        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
            }
        });

        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelOrder();
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog.show();
    }

    private void cancelOrder() {

    }
}
