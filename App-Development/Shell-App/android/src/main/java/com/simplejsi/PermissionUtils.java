package com.simplejsi;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

public class PermissionUtils {

    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static String getPermissionsStatus(Context context) {
        StringBuilder status = new StringBuilder();
        status.append("REQUEST_INSTALL_PACKAGES: ").append(hasPermission(context, android.Manifest.permission.REQUEST_INSTALL_PACKAGES)).append("\n");
        status.append("WRITE_EXTERNAL_STORAGE: ").append(hasPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)).append("\n");
        status.append("READ_EXTERNAL_STORAGE: ").append(hasPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)).append("\n");
        return status.toString();
    }
}
