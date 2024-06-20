package com.objectdetectiontfdemo.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class PermissionsUtil {

    // Get camera permission
    public static String[] cameraPermissions() {
        return new String[]{Manifest.permission.CAMERA};
    }

    // Get storage permissions based on Android version
    public static String[] storagePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            return new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE};
        }
    }

    // Check if all permissions in grantResults array are granted
    public static boolean permissionsGranted(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    // Check if all permissions in the Map are granted
    public static boolean permissionsGranted(@Nullable Map<String, Boolean> permissions) {
        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
            if (!entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    // Check if the required permissions are granted (without requesting permissions from the user)
    public static boolean hasPermissions(Context context, String[] requiredPermissions) {
        if (context != null && requiredPermissions != null) {
            for (String permission : requiredPermissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    // Check if the permissions are denied and the "Don't ask again" option is selected
    public static boolean isPermissionDeniedAndDontAskAgain(Activity activity, String[] requiredPermissions) {
        if (activity != null) {
            for (String permission : requiredPermissions) {
                boolean isDontAskAgain = !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
                        && ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_DENIED;
                if (isDontAskAgain) {
                    return isDontAskAgain;
                }
            }
        }
        return false;
    }
}
