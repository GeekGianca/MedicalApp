package com.gksoftware.auxiliary_health_app.utils;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import com.gksoftware.auxiliary_health_app.R;
import com.gksoftware.auxiliary_health_app.model.AtentionUser;
import com.gksoftware.auxiliary_health_app.model.Device;

public class Common {
    public static final String KEY_USERS = "docUsers";
    public static final String KEY_USER_PATIENT = "duPatients";
    public static AlertDialog createAlert(Context context, String title, String message, String titlePosBtn, String titleNegBtn, int icon, DialogInterface.OnClickListener listenerPositive, DialogInterface.OnClickListener listenerNegative) {
        return new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setIcon((icon == 0) ? R.drawable.ic_info : icon)
                .setPositiveButton((titlePosBtn == null) ? "Aceptar" : titlePosBtn, listenerPositive)
                .setNegativeButton((titleNegBtn == null) ? "Cancelar" : titleNegBtn, listenerNegative)
                .setCancelable(false)
                .create();
    }
    public static AtentionUser currentSelectedUser;
    public static Device currentBluetoothSelected;
    public static float PROM_RATE_HEART;
}
