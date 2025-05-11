package com.example.signalwave

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentTransaction
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val permissionReqId = 22

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d("FirestoreDebug", "Firebase Initialized Successfully")

        setContentView(R.layout.activity_main)

        loadFragment(LoginFragment())

        if (!checkPermissions()) {
            requestPermissions()
        }else {
            initializeAgoraSDK()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()

        transaction.setCustomAnimations(
            R.anim.enter_anim,
            R.anim.exit_anim
        )

        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun checkPermissions(): Boolean {
        for (permission in getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, getRequiredPermissions(), permissionReqId)

    }

    private fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionReqId) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("Permissions", "All required permissions granted")
                initializeAgoraSDK()// Initialize Agora AFTER permissions are granted

            } else {
                Log.d("Permissions", "Some required permissions not granted")
                Toast.makeText(this, "Some permissions were denied. Calling features may not work.", Toast.LENGTH_LONG).show()            }
        }
    }

    private fun initializeAgoraSDK() {
        // Initialize Agora ONLY here or in onCreate if permissions are already granted
        Log.d("Agora", "Initializing Agora SDK")
        AgoraManager.initialize(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        AgoraManager.destroy()
    }
}
