package com.example.signalwave

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.activityViewModels
import android.view.LayoutInflater
import com.example.signalwave.model.User
import androidx.core.net.toUri

class PhoneFragment : Fragment(R.layout.fragment_phone) {

    private val REQUESTCALLPERMISSION = 1
    var selectedPhoneNumber: String? = null

    // Get the shared ViewModel
    private val sharedUsersViewModel: SharedUsersViewModel by activityViewModels()

    private lateinit var contactsContainerPhone: LinearLayout

    private var selectedContactView: View? = null

    private val currentUserId: String?
        get() = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contactsContainerPhone = view.findViewById(R.id.contacts_container_phone)

        val fabSignal: FloatingActionButton = view.findViewById(R.id.fab_signal)
        fabSignal.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MainFragment())
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

        // Observe the users from the shared ViewModel
        sharedUsersViewModel.users.observe(viewLifecycleOwner) { users ->
            populateContacts(users)
        }

        val fabCall: FloatingActionButton = view.findViewById(R.id.fab_call)
        fabCall.setOnClickListener {
            Log.d("PhoneFragment", "Call button clicked. selectedPhoneNumber: $selectedPhoneNumber")
            if (selectedPhoneNumber != null) {
                checkAndRequestPermission()
            } else {
                Toast.makeText(requireContext(), "No contact selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateContacts(users: List<User>) {
        contactsContainerPhone.removeAllViews() // Clear existing views before adding

        selectedContactView = null // Reset selected view when repopulating

        val inflater = LayoutInflater.from(requireContext()) // Get a LayoutInflater

        val usersToDisplay = users.filter { user -> user.userID != currentUserId }

        for (user in usersToDisplay) {
            Log.d("PhoneFragment", "Processing user: ${user.name}, Phone: ${user.phone}")
            // Inflate the contact item layout
            val contactView = inflater.inflate(R.layout.contact_item, contactsContainerPhone, false)

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

                selectedContactView = it
                it.setBackgroundResource(R.drawable.contact_background_selected)

                selectedPhoneNumber = user.phone // Set the selected phone number from the User object
                Log.d("PhoneFragment", "Contact selected. Setting selectedPhoneNumber to: ${user.phone}")
            }

            // Add the inflated contact view to the container LinearLayout
            contactsContainerPhone.addView(contactView)
        }
    }

    private fun checkAndRequestPermission() {
        Log.d("PhoneFragment", "checkAndRequestPermission() called")
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("PhoneFragment", "CALL_PHONE permission not granted. Requesting permission.")
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CALL_PHONE),
                REQUESTCALLPERMISSION
            )
        } else {
            Log.d("PhoneFragment", "CALL_PHONE permission already granted. Initiating call.")
            initiateCall(selectedPhoneNumber.toString())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("PhoneFragment", "onRequestPermissionsResult() called with requestCode: $requestCode")

        if (requestCode == REQUEST_CALL_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("PhoneFragment", "CALL_PHONE permission granted in onRequestPermissionsResult. Initiating call.")
                initiateCall(selectedPhoneNumber.toString())
            } else {
                Log.d("PhoneFragment", "CALL_PHONE permission denied in onRequestPermissionsResult.")
                Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initiateCall(phoneNumber: String) {
        // Check for CALL_PHONE permission first (for Android M and above)
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // Request the permission if it's not granted
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PERMISSION)
        } else {
            // Permission is already granted, proceed with the call
            try {
                val dialIntent = Intent(Intent.ACTION_CALL, "tel:$phoneNumber".toUri())
                startActivity(dialIntent)
            } catch (e: SecurityException) {
                // Handle the case where permission was somehow still denied or an issue occurred
                Toast.makeText(requireContext(), "Permission denied to make a call", Toast.LENGTH_SHORT).show()
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "No app found to handle call intent", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CALL_PERMISSION = 101
    }
}
