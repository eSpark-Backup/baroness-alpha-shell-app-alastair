package com.simplejsiexample

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

class MainActivity : ReactActivity() {

    companion object {
        private const val REQUEST_PERMISSIONS_CODE = 100
        private const val REQUEST_WRITE_SETTINGS_CODE = 101
        private const val TAG = "MainActivity"
    }

    override fun getMainComponentName(): String = "SimpleJsiExample"

    override fun createReactActivityDelegate(): ReactActivityDelegate =
        DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Checking and requesting permissions")
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_WIFI_STATE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CHANGE_WIFI_STATE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_NETWORK_STATE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.INTERNET)
        }

        if (permissionsNeeded.isNotEmpty()) {
            Log.d(TAG, "checkAndRequestPermissions: Requesting permissions: $permissionsNeeded")
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), REQUEST_PERMISSIONS_CODE)
        } else {
            Log.d(TAG, "checkAndRequestPermissions: All normal permissions already granted")
        }
        checkAndRequestWriteSettingsPermission()
    }

    private fun checkAndRequestWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            Log.d(TAG, "checkAndRequestWriteSettingsPermission: Requesting WRITE_SETTINGS permission")
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, REQUEST_WRITE_SETTINGS_CODE)
        } else {
            Log.d(TAG, "checkAndRequestWriteSettingsPermission: WRITE_SETTINGS permission already granted")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: Permission granted for ${permissions[i]}")
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: Permission denied for ${permissions[i]}")
                    // Handle the permission denial appropriately
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_WRITE_SETTINGS_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(this)) {
                Log.d(TAG, "onActivityResult: WRITE_SETTINGS permission granted")
                Toast.makeText(this, "Write allowed :-)", Toast.LENGTH_LONG).show()
            } else {
                Log.d(TAG, "onActivityResult: WRITE_SETTINGS permission denied")
                Toast.makeText(this, "Write not allowed :-(", Toast.LENGTH_LONG).show()
            }
        }
    }
}
