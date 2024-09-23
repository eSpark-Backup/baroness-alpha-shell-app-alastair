package com.simplejsi;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import android.content.ActivityNotFoundException;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.PermissionListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.io.DataOutputStream;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactMethod;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;

@ReactModule(name = SimpleJsiModule.NAME)
public class SimpleJsiModule extends NativeSimpleJsiSpec {
    public static final String NAME = "SimpleJsi";
    private static final int REQUEST_CODE_INSTALL_PACKAGES = 12345;
    private static final String TAG = "MyActivity SimpleJsi";
    private final ReactApplicationContext reactContext;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private Callback permissionCallback;
    private final APManager apManager;

    public SimpleJsiModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        apManager = APManager.getApManager(reactContext);
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

    static {
        System.loadLibrary("react-native-simple-jsi");
    }

    private static native double nativeMultiply(double a, double b);
    private static native double nativeAdd(double a, double b);

    private native void nativeInstall(long jsiPtr);

    // Example method
    // See https://reactnative.dev/docs/native-modules-android
    @Override
    public double multiply(double a, double b) {
        return nativeMultiply(a, b);
    }

    @Override
    public double add(double a, double b) {
        double result = nativeAdd(a, b);
        return result;
    }

    public void turnOnHotspot(final Callback successCallback, final Callback errorCallback) {
        apManager.turnOnHotspot(getReactApplicationContext(), new APManager.OnSuccessListener() {
            @Override
            public void onSuccess(@NonNull String ssid, @NonNull String password) {
                // Log the SSID and password when hotspot is successfully turned on
                Log.d("Hotspot", "SSID: " + ssid);
                Log.d("Hotspot", "Password: " + password);

                // Prepare response to React Native
                WritableMap map = Arguments.createMap();
                map.putString("ssid", ssid);
                map.putString("password", password);
                successCallback.invoke(map);
            }
        }, new APManager.OnFailureListener() {
            @Override
            public void onFailure(int failureCode, Exception e) {
                String errorCode = getErrorCodeFromFailureCode(failureCode);
                errorCallback.invoke(errorCode, e.getMessage());
            }
        });
    }

    public void requestInstallPackagesPermission(Callback callback) {
        permissionCallback = callback;
        // Check if permissions are granted
        int readStoragePermission = ContextCompat.checkSelfPermission(reactContext,
                android.Manifest.permission.READ_EXTERNAL_STORAGE);
        int writeStoragePermission = ContextCompat.checkSelfPermission(reactContext,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int installPackagesPermission = ContextCompat.checkSelfPermission(reactContext,
                android.Manifest.permission.REQUEST_INSTALL_PACKAGES);

        Log.i("Permission", "READ_EXTERNAL_STORAGE Permission: "
                + (readStoragePermission == PackageManager.PERMISSION_GRANTED));
        Log.i("Permission", "WRITE_EXTERNAL_STORAGE Permission: "
                + (writeStoragePermission == PackageManager.PERMISSION_GRANTED));
        Log.i("Permission", "REQUEST_INSTALL_PACKAGES Permission: "
                + (installPackagesPermission == PackageManager.PERMISSION_GRANTED));

        if (readStoragePermission != PackageManager.PERMISSION_GRANTED
                || writeStoragePermission != PackageManager.PERMISSION_GRANTED
                || installPackagesPermission != PackageManager.PERMISSION_GRANTED) {

            boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(getCurrentActivity(),
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(getCurrentActivity(),
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(getCurrentActivity(),
                            android.Manifest.permission.REQUEST_INSTALL_PACKAGES);

            if (showRationale) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Log.d("Permission", "Explanation should be shown");
                permissionCallback.invoke("Explanation should be shown");
            } else {
                // Request permissions
                ActivityCompat.requestPermissions(getCurrentActivity(),
                        new String[] { android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                android.Manifest.permission.REQUEST_INSTALL_PACKAGES },
                        PERMISSION_REQUEST_CODE);
                Log.d("Permission", "Requesting permissions");
                permissionCallback.invoke("Requested");
            }
        } else {
            // Permissions are already granted
            Log.d("Permission", "Permissions already granted");
            permissionCallback.invoke("Already Granted");
        }
    }

    public String installAPKCMD(String apkPath) {
        StringBuilder resultBuilder = new StringBuilder();
        StringBuilder errorLogBuilder = new StringBuilder();

        // Check if the file exists
        File file = new File(apkPath);
        if (!file.exists()) {
            resultBuilder.append("APK file does not exist at path: ").append(apkPath);
            return resultBuilder.toString();
        }

        // Construct the command to install the APK using 'pm' command
        String installCommand = "cat "+ file.getAbsolutePath() + " | pm install -S " + file.length();

        try {
            // Execute the command as superuser
            Process process = Runtime.getRuntime().exec("su");
            OutputStream out = process.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(out);

            // Grant necessary permissions to the APK file
            dataOutputStream.writeBytes("chmod 777 " + file.getPath() + "\n");

            // Perform the silent installation
            dataOutputStream.writeBytes(installCommand + "\n");

            dataOutputStream.flush();
            dataOutputStream.close();
            out.close();

            // Capture and process the error stream
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorLogBuilder.append(line).append("\n");
            }

            int result = process.waitFor();
            if (result == 0) {
                resultBuilder.append("APK installed successfully from path: ").append(apkPath);
            } else {
                resultBuilder.append("Failed to install APK from path: ").append(apkPath)
                        .append(". Error code: ").append(result).append("\nError log:\n")
                        .append(errorLogBuilder.toString());
            }
        } catch (IOException | InterruptedException e) {
            resultBuilder.append("Exception occurred while installing APK: ").append(e.getMessage());
        }

        return resultBuilder.toString();
    }

    public void installAPKIntent(String filePath) {
        File apkFile = new File(filePath);

        // Check if the file exists
        if (!apkFile.exists()) {
            // Handle the file not found scenario
            Log.e("Install", "File not found: " + filePath);
            // Optionally, you can show a message to the user or perform some other action
            Toast.makeText(reactContext, "File not found: " + filePath, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setAction(Intent.ACTION_VIEW);
        Uri apkUri;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String authority = reactContext.getPackageName() + ".provider";
            apkUri = FileProvider.getUriForFile(reactContext, authority, apkFile);
        } else {
            apkUri = Uri.fromFile(apkFile);
        }

        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        try {
            reactContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "Error opening file: " + e.getMessage(), e);
            Toast.makeText(reactContext, "Error opening file: " + e.getMessage(), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void listFilesInPath(String path) {
        File rootDir = new File(path);
        File[] files = rootDir.listFiles();

        if (files != null) {
            for (File file : files) {
                Log.e(TAG, "File: " + file.getName());
            }
        } else {
            Log.e(TAG, "No files found in the root directory.");
        }
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public void install() {
        try {
            nativeInstall(reactContext.getJavaScriptContextHolder().get());
            // return true;
        }
        catch (Exception exception) {
            // return false;
        }
    }

    private String getErrorCodeFromFailureCode(int failureCode) {
        switch (failureCode) {
            case APManager.ERROR_GPS_PROVIDER_DISABLED:
                return "GPS_PROVIDER_DISABLED";
            case APManager.ERROR_LOCATION_PERMISSION_DENIED:
                return "LOCATION_PERMISSION_DENIED";
            case APManager.ERROR_DISABLE_HOTSPOT:
                return "DISABLE_HOTSPOT";
            case APManager.ERROR_DISABLE_WIFI:
                return "DISABLE_WIFI";
            case APManager.ERROR_WRITE_SETTINGS_PERMISSION_REQUIRED:
                return "WRITE_SETTINGS_PERMISSION_REQUIRED";
            default:
                return "UNKNOWN_ERROR";
        }
    }

    private static final int FTP_PORT = 2221; // Example port, choose one that is not in use

    private FtpServer ftpServer;

    public void startFTPServer() {
        Log.d(TAG, "Starting FTP Server");
        FtpServerFactory serverFactory = new FtpServerFactory();

        ListenerFactory factory = new ListenerFactory();
        factory.setPort(FTP_PORT);
        serverFactory.addListener("default", factory.createListener());

        // Set user credentials (example, replace with your actual logic)
        BaseUser user = new BaseUser();
        user.setName("anonymous");
        user.setPassword(""); // Anonymous user, no password
        user.setHomeDirectory("/sdcard/Download"); // Example home directory

        // Grant write permission to the user's home directory
        user.setAuthorities(Collections.singletonList(new WritePermission()));

        try {
            serverFactory.getUserManager().save(user);
        } catch (FtpException e) {
            Log.e(TAG, "Error saving FTP user: " + e.getMessage());
            // errorCallback.invoke("Error saving FTP user: " + e.getMessage());
            return; // Exit the method if the user cannot be saved
        }

        Ftplet myFtplet = new DefaultFtplet() {
            @Override
            public FtpletResult onUploadStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
                String fileName = request.getArgument();
                String directory = session.getFileSystemView().getWorkingDirectory().getAbsolutePath();
                FtpFile file = session.getFileSystemView().getFile(directory + "/" + fileName);

                long fileSize = file.getSize(); // Assuming the file size is available at start
                Log.d(TAG, "File Size: " + fileSize);
                Log.d(TAG, "Directory: " + directory);
                Log.d(TAG, "Upload started: " + fileName);
                session.setAttribute("fileName", fileName);
                session.setAttribute("uploadedSize", 0L);
                return FtpletResult.DEFAULT;
            }

            @Override
            public FtpletResult onAppendEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
                Log.d(TAG, "On Append End");
                updateProgress(session, request);
                return FtpletResult.DEFAULT;
            }

            @Override
            public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
                updateProgress(session, request);
                String fileName = request.getArgument();
                String directory = session.getFileSystemView().getWorkingDirectory().getAbsolutePath();
                FtpFile file = session.getFileSystemView().getFile(directory + "/" + fileName);

                long fileSize = file.getSize(); // Assuming the file size is available at start
                Log.d(TAG, "File Size: " + fileSize);
                Log.d(TAG, "Upload completed: " + fileName);

                String apkFilePath = "/storage/emulated/0/Download/" + fileName;
                Log.d(TAG, "APK File PATH " + apkFilePath);

                // Show a loading circle (ProgressDialog)
                // final ProgressDialog progressDialog = new ProgressDialog(reactContext);
                // progressDialog.setMessage("Installing...");
                // progressDialog.setCancelable(false);
                // progressDialog.show();
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(reactContext, "File Received: Installing", Toast.LENGTH_LONG).show();
                    }
                });
                String installStatus = installAPKCMD(apkFilePath);
                Log.d(TAG, "APK installStatus " + installStatus);
                // progressDialog.dismiss();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(reactContext, "App installed and updated", Toast.LENGTH_LONG).show();
                    }
                });

                // String packageName = getPackageNameFromApk(apkFilePath);
                // Log.d(TAG, "Package Name" + packageName);
                // launchApp(packageName);

                return FtpletResult.DEFAULT;
            }

            private void updateProgress(FtpSession session, FtpRequest request) throws FtpException, IOException {
                String fileName = (String) session.getAttribute("fileName");
                long uploadedSize = session.getFileSystemView().getFile(fileName).getSize();
                session.setAttribute("uploadedSize", uploadedSize);

                Long fileSize = (Long) session.getAttribute("fileSize");
                if (fileSize == null) {
                    fileSize = uploadedSize;  // Use the uploaded size as a proxy for file size
                    session.setAttribute("fileSize", fileSize);
                }

                int progress = (int) ((uploadedSize * 100) / fileSize);
                Log.d(TAG, "Upload progress: " + fileName + " (" + progress + "%)");
            }
        };

        Map<String, Ftplet> ftplets = new HashMap<>();
        ftplets.put("myFtplet", myFtplet);
        serverFactory.setFtplets(ftplets);

        ftpServer = serverFactory.createServer();

        try {
            ftpServer.start();
            Log.d(TAG, "FTP Server started on port " + FTP_PORT);
            // successCallback.invoke("FTP Server started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting FTP Server: " + e.getMessage());
            // errorCallback.invoke("Error starting FTP Server: " + e.getMessage());
        }
    }

    public void launchApp(String packageName) {
        PackageManager pm = reactContext.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            reactContext.startActivity(launchIntent);
        }
        else {
            // Handle the case where the package was not found
            Toast.makeText(reactContext, "App not found", Toast.LENGTH_SHORT).show();
            Log.e("LaunchApp", "Package not found: " + packageName);
        }
    }

    public String getPackageNameFromApk(String apkFilePath) {
        PackageManager pm = reactContext.getPackageManager();
        PackageInfo packageInfo = pm.getPackageArchiveInfo(apkFilePath, 0);
        if (packageInfo != null) {
            return packageInfo.packageName;
        } else {
            return null;
        }
    }
}
