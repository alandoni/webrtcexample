package com.adqmobile.webrtcapp

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionHandler(
    private val permissionType: PermissionType,
    private val context: Activity,
    private val listener: PermissionHandlerListener) {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1
    }

    fun checkPermission() {
        if (ContextCompat.checkSelfPermission(context, permissionType.value) != PackageManager.PERMISSION_GRANTED) {
            requestPermission()
        } else {
            listener.onPermissionGranted(permissionType)
        }
    }

    private fun requestPermission(dialogShown: Boolean = false) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(context, permissionType.value) && !dialogShown) {
            showPermissionDialog()
        } else {
            ActivityCompat.requestPermissions(context, arrayOf(permissionType.value), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    private fun showPermissionDialog() {
        val resource = when(permissionType) {
            PermissionType.CAMERA ->
                "Camera"
            PermissionType.AUDIO ->
                "Microphone"
            PermissionType.SIP ->
                "SIP connection"
        }

        AlertDialog.Builder(context)
            .setTitle("Resource needed")
            .setMessage("This app needs the following resource to work: $resource")
            .setPositiveButton("Grant") { dialog, _ ->
                dialog.dismiss()
                requestPermission(true)
            }
            .setNegativeButton("Deny") { dialog, _ ->
                dialog.dismiss()
                onPermissionDenied()
            }
            .show()
    }

    private fun onPermissionDenied() {
        Toast.makeText(context, "Camera Permission Denied", Toast.LENGTH_LONG).show()
        listener.onPermissionDenied(permissionType)
    }

    interface PermissionHandlerListener {
        fun onPermissionGranted(permissionType: PermissionType)
        fun onPermissionDenied(permissionType: PermissionType)
    }

    enum class PermissionType(val value: String) {
        CAMERA(Manifest.permission.CAMERA),
        AUDIO(Manifest.permission.RECORD_AUDIO),
        SIP(Manifest.permission.USE_SIP)
    }
}
