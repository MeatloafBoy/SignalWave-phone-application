package com.example.signalwave

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.google.firebase.auth.FirebaseAuth
import kotlin.getValue
import com.example.signalwave.model.User

class MainFragment : Fragment(R.layout.fragment_main) {
    private val db = Firebase.firestore


    private lateinit var auth: FirebaseAuth

    private val sharedUsersViewModel: SharedUsersViewModel by activityViewModels()


    private lateinit var contactsContainerMain: LinearLayout

    private var selectedContactView: View? = null

    private var selectedUserForCall: User? = null

    private val REQUEST_AUDIO_PERMISSION = 103

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        AgoraManager.initialize(requireContext())

        contactsContainerMain = view.findViewById(R.id.contacts_container_main)

        sharedUsersViewModel.users.observe(viewLifecycleOwner) { users ->
            populateContacts(users)
        }

        val currentUserUid = auth.currentUser?.uid

        if (currentUserUid != null) {
            db.collection("users")
                .whereNotEqualTo("userID", currentUserUid) // Filter out the current user
                .get()
                .addOnSuccessListener { result ->
                    // Extract documents into a list of User objects
                    val userList = result.toObjects(User::class.java)

                    // Set the complete list in your ViewModel
                    sharedUsersViewModel.setUsers(userList)

                    // Log the total number of users fetched once
                    Log.d("Firestore Debug", "Fetched ${userList.size} users from Firestore (excluding current user).")
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore Error", "Error getting users", exception)
                }
        } else {
            Log.w("MainFragment", "No current user logged in. Cannot fetch contacts.")
            // Handle the case where there is no logged-in user, e.g., navigate to login
        }


        val fabPhone: FloatingActionButton = view.findViewById(R.id.fab_phone)
        fabPhone.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PhoneFragment())
                .addToBackStack(null)
                .commit()
        }

        val fabMessage: FloatingActionButton = view.findViewById(R.id.fab_message)
        fabMessage.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ContactsFragment())
                .addToBackStack(null)
                .commit()
        }

        val micButton: FloatingActionButton = view.findViewById(R.id.fab_mic)
        micButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Start streaming audio only if a user is selected
                    if (selectedUserForCall != null) {
                        requestAudioPermissionAndStartStreaming()
                    } else {
                        Toast.makeText(requireContext(), "Please select a contact first.", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Stop streaming audio
                    stopStreamingAudio()
                    v.performClick() // For accessibility - this also triggers the OnClickListener
                    true
                }
                else -> false
            }
        }

        micButton.setOnClickListener {
            Log.d("FAB", "Mic clicked (accessibility fallback)")

            // You might want to provide feedback here if no contact is selected
            if (selectedUserForCall == null) {
                Toast.makeText(
                    requireContext(),
                    "Hold to talk after selecting a contact.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun requestAudioPermissionAndStartStreaming() {
        Log.d("MainFragment", "requestAudioPermissionAndStartStreaming called")
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_AUDIO_PERMISSION
            )
        } else {
            startStreamingAudio()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_AUDIO_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Agora", "RECORD_AUDIO permission granted. Starting audio stream.")
                    startStreamingAudio()
                } else {
                    Log.w("Agora", "RECORD_AUDIO permission denied.")
                    Toast.makeText(requireContext(), "Audio recording permission denied. Cannot stream audio.", Toast.LENGTH_SHORT).show()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun populateContacts(users: List<User>) {
        contactsContainerMain.removeAllViews() // Clear existing views before adding

        selectedContactView = null // Reset selected view when repopulating

        val inflater = LayoutInflater.from(requireContext()) // Get a LayoutInflater

        for (user in users) {
            // Inflate the contact item layout
            val contactView = inflater.inflate(R.layout.contact_item, contactsContainerMain, false)

            // Assign a tag to the view
            contactView.tag = user.userID

            // Set user data to the views in the inflated layout
            val contactName = contactView.findViewById<TextView>(R.id.contact_name)
            val contactPosition = contactView.findViewById<TextView>(R.id.contact_position)

            contactName.text = user.name
            contactPosition.text = user.role
            contactView.setBackgroundResource(R.drawable.contact_background_unselected)

            // Add a click listener to the contact view
            contactView.setOnClickListener {
                selectedContactView?.setBackgroundResource(R.drawable.contact_background_unselected)

                val userId = it.tag as? String
                selectedUserForCall = sharedUsersViewModel.users.value?.find { user -> user.userID == userId }

                selectedContactView = it
                it.setBackgroundResource(R.drawable.contact_background_selected)
            }

            // Add the inflated contact view to the container LinearLayout
            contactsContainerMain.addView(contactView)
        }
    }


    private fun joinChannel() {
        // Check if the RTC engine is initialized before joining
        if (AgoraManager.rtcEngine == null) {
            Log.e("Agora", "Agora RTC Engine not initialized when trying to join channel")
            // Optionally, handle this error, perhaps inform the user or try to re-initialize
            return
        }

        val options = ChannelMediaOptions().apply {
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            publishMicrophoneTrack = true
        }

        AgoraManager.rtcEngine?.joinChannel(
            AgoraManager.Token,
            AgoraManager.ChannelName,
            0,
            options
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopStreamingAudio() // Ensure channel is left
    }


    private fun startStreamingAudio() {
        Log.d("AudioStream", "Starting audio stream...")

        if (AgoraManager.rtcEngine == null) {
            Log.e("Agora", "RTC Engine is null. Aborting join.")
            Toast.makeText(requireContext(), "Audio system not ready. Try again shortly.", Toast.LENGTH_SHORT).show()
            return
        }

        joinChannel()
    }

    private fun stopStreamingAudio() {
        Log.d("AudioStream", "Stopping audio stream...")
        AgoraManager.rtcEngine?.leaveChannel()
    }


}
