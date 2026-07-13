package com.example.studyplan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.LinearLayout;

public class DialogHelper {
    public static void styleDialog(AlertDialog dialog, boolean isDanger) {
        if (dialog == null) return;
        Context context = dialog.getContext();
        float density = context.getResources().getDisplayMetrics().density;

        dialog.setOnShowListener(d -> {
            Button btnPositive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            Button btnNegative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);

            if (btnPositive != null) {
                btnPositive.setBackgroundResource(isDanger ? R.drawable.bg_capsule_danger : R.drawable.bg_capsule_selected);
                btnPositive.setTextColor(Color.parseColor(isDanger ? "#E53935" : "#1A73E8"));
                btnPositive.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                btnPositive.setAllCaps(false);

                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) btnPositive.getLayoutParams();
                lp.setMargins((int)(8 * density), (int)(8 * density), (int)(8 * density), (int)(8 * density));
                lp.height = (int)(44 * density);
                btnPositive.setLayoutParams(lp);
            }

            if (btnNegative != null) {
                btnNegative.setBackgroundResource(R.drawable.bg_capsule_unselected);
                btnNegative.setTextColor(Color.parseColor("#5F6368"));
                btnNegative.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                btnNegative.setAllCaps(false);

                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) btnNegative.getLayoutParams();
                lp.setMargins((int)(8 * density), (int)(8 * density), (int)(8 * density), (int)(8 * density));
                lp.height = (int)(44 * density);
                btnNegative.setLayoutParams(lp);
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setWindowAnimations(0);
        }
    }
}
