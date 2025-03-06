package com.example.signalwave

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {


     @SuppressLint("MissingInflatedId", "SetTextI18n")
     override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        val header: Toolbar = findViewById(R.id.header)
        val contactName1: TextView = findViewById(R.id.contact_name)
        val fabMicrophone: FloatingActionButton = findViewById(R.id.fab_microphone)

        // Handle microphone button click
        fabMicrophone.setOnClickListener {
            // Start recording or open voice-related functionality
            startVoiceActivity()
        }

        // Handle clicking on a contact to initiate a call or text
        contactName1.setOnClickListener {
            // Open the Phone page or Text page
            openPhonePage()
        }

    }

    // Function to handle voice functionality (microphone button)
    private fun startVoiceActivity() {
        // Placeholder for handling voice activity
        // For example, open a voice recording activity
    }

    // Function to open the Phone page when a contact is clicked
    private fun openPhonePage() {
        // Open PhonePageActivity when the contact is clicked
    }

}
