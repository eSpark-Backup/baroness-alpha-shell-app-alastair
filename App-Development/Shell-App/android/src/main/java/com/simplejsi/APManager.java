package com.simplejsi;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.DhcpInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class APManager {
    private static final String TAG = "APManager";
    private static APManager apManager;
    private final Utils utils;

    private APManager(Context context) {
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.utils = new Utils();
    }

    public static APManager getApManager(@NonNull Context context) {
        if (apManager == null) {
            apManager = new APManager(context);
        }
        return apManager;
    }

    private String ssid;
    private String password;

    public String getSSID() {
        return ssid;
    }

    public String getPassword() {
        return password;
    }

    public static final int ERROR_GPS_PROVIDER_DISABLED = 0;
    public static final int ERROR_LOCATION_PERMISSION_DENIED = 4;
    public static final int ERROR_DISABLE_HOTSPOT = 1;
    public static final int ERROR_DISABLE_WIFI = 5;
    public static final int ERROR_WRITE_SETTINGS_PERMISSION_REQUIRED = 6;
    public static final int ERROR_UNKNOWN = 3;

    private final WifiManager wifiManager;
    private final LocationManager locationManager;
    private WifiManager.LocalOnlyHotspotReservation reservation;

    public Utils getUtils() {
        return utils;
    }

    private boolean monitoringConnectedDevices = false;

    public void turnOnHotspot(Context context, OnSuccessListener onSuccessListener, OnFailureListener onFailureListener) {
        boolean providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (isDeviceConnectedToWifi()) {
            onFailureListener.onFailure(ERROR_DISABLE_WIFI, null);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (utils.checkLocationPermission(context) && providerEnabled && !isWifiApEnabled()) {
                try {
                    wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
                        public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                            super.onStarted(reservation);
                            APManager.this.reservation = reservation;
                            try {
                                ssid = reservation.getWifiConfiguration().SSID;
                                password = reservation.getWifiConfiguration().preSharedKey;
                                Log.d(TAG, "Hotspot started - SSID: " + ssid + ", Password: " + password);
                                onSuccessListener.onSuccess(ssid, password);

                                // Start monitoring connected devices
                                startMonitoringConnectedDevices();
                            } catch (Exception e) {
                                e.printStackTrace();
                                onFailureListener.onFailure(ERROR_UNKNOWN, e);
                            }
                        }

                        public void onFailed(int reason) {
                            super.onFailed(reason);
                            Log.d(TAG, "Hotspot failed to start - Reason: " + reason);
                            onFailureListener.onFailure(reason == WifiManager.LocalOnlyHotspotCallback.ERROR_TETHERING_DISALLOWED ?
                                    ERROR_DISABLE_HOTSPOT : ERROR_UNKNOWN, null);
                        }
                    }, new Handler(Looper.getMainLooper()));
                } catch (Exception e) {
                    e.printStackTrace();
                    onFailureListener.onFailure(ERROR_UNKNOWN, e);
                }
            } else if (!providerEnabled) {
                Log.d(TAG, "GPS provider not enabled");
                onFailureListener.onFailure(ERROR_GPS_PROVIDER_DISABLED, null);
            } else if (isWifiApEnabled()) {
                Log.d(TAG, "WiFi AP already enabled");
                onFailureListener.onFailure(ERROR_DISABLE_HOTSPOT, null);
            } else {
                Log.d(TAG, "Location permission not granted or hotspot already enabled");
                onFailureListener.onFailure(ERROR_LOCATION_PERMISSION_DENIED, null);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!utils.checkLocationPermission(context)) {
                    Log.d(TAG, "Location permission not granted");
                    onFailureListener.onFailure(ERROR_LOCATION_PERMISSION_DENIED, null);
                    return;
                }
                if (!utils.checkWriteSettingPermission(context)) {
                    Log.d(TAG, "Write settings permission not granted");
                    onFailureListener.onFailure(ERROR_WRITE_SETTINGS_PERMISSION_REQUIRED, null);
                    return;
                }
            }
            try {
                ssid = "AndroidAP_" + new Random().nextInt(10000);
                password = getRandomPassword();
                WifiConfiguration wifiConfiguration = new WifiConfiguration();
                wifiConfiguration.SSID = ssid;
                wifiConfiguration.preSharedKey = password;
                wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifiManager.setWifiEnabled(false);
                setWifiApEnabled(wifiConfiguration, true);
                Log.d(TAG, "Hotspot enabled - SSID: " + ssid + ", Password: " + password);
                onSuccessListener.onSuccess(ssid, password);

                // Start monitoring connected devices
                startMonitoringConnectedDevices();
            } catch (Exception e) {
                e.printStackTrace();
                onFailureListener.onFailure(ERROR_UNKNOWN, e);
            }
        }
    }

    public void disableWifiAp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                reservation.close();
            } else {
                setWifiApEnabled(null, false);
            }
            Log.d(TAG, "Hotspot disabled");
            // Stop monitoring when hotspot is disabled
            stopMonitoringConnectedDevices();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isWifiApEnabled() {
        try {
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            return (boolean) method.invoke(wifiManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isDeviceConnectedToWifi() {
        return wifiManager.getDhcpInfo().ipAddress != 0;
    }

    private void setWifiApEnabled(WifiConfiguration wifiConfiguration, boolean enable) throws Exception {
        Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
        method.invoke(wifiManager, wifiConfiguration, enable);
    }

    public WifiManager getWifiManager() {
        return wifiManager;
    }

    public void startMonitoringConnectedDevices() {
        if (!monitoringConnectedDevices) {
            monitoringConnectedDevices = true;
            Log.d(TAG, "Starting monitoring connected devices");
            // Periodically check connected devices
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    List<String> connectedDevices = getConnectedDeviceNames();
                    if (!connectedDevices.isEmpty()) {
                        Log.d(TAG, "Connected devices: " + connectedDevices);
                    }
                    handler.postDelayed(this, 5000); // Check every 5 seconds
                }
            }, 5000); // Initial delay 5 seconds
        }
    }

    public void stopMonitoringConnectedDevices() {
        monitoringConnectedDevices = false;
        Log.d(TAG, "Stopping monitoring connected devices");
    }

    public List<String> getConnectedDeviceNames() {
        List<String> connectedDeviceNames = new ArrayList<>();
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
    
        // Extract IP addresses of connected devices
        if (dhcpInfo != null) {
            int start = dhcpInfo.ipAddress & dhcpInfo.netmask;
            int end = start | ~dhcpInfo.netmask;
    
            for (int i = start + 1; i <= end - 1; i++) {
                String ipAddress = getIpFromInt(i);
                if (ipAddress != null) {
                    connectedDeviceNames.add(ipAddress);
                }
            }
        }
    
        return connectedDeviceNames;
    }
    
    private String getIpFromInt(int ip) {
        try {
            byte[] ipAddress = BigInteger.valueOf(ip).toByteArray();
            InetAddress address = InetAddress.getByAddress(ipAddress);
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }
    

    public interface OnFailureListener {
        void onFailure(int failureCode, @Nullable Exception e);
    }

    public interface OnSuccessListener {
        void onSuccess(@NonNull String ssid, @NonNull String password);
    }

    private String getRandomPassword() {
        try {
            MessageDigest ms = MessageDigest.getInstance("MD5");
            byte[] bytes = new byte[10];
            new Random().nextBytes(bytes);
            byte[] digest = ms.digest(bytes);
            BigInteger bigInteger = new BigInteger(1, digest);
            return bigInteger.toString(16).substring(0, 10);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "jfs82433#$2";
    }

    public static class Utils {
        public boolean checkLocationPermission(Context context) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        public void askLocationPermission(Activity activity, int requestCode) {
            ActivityCompat.requestPermissions(activity, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, requestCode);
        }

        @RequiresApi(Build.VERSION_CODES.M)
        public void askWriteSettingPermission(@NonNull Activity activity) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        }

        @RequiresApi(Build.VERSION_CODES.M)
        public boolean checkWriteSettingPermission(@NonNull Context context) {
            return Settings.System.canWrite(context);
        }
    }
}
