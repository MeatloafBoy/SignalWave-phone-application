package com.example.signalwave

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PhoneFragment : Fragment(R.layout.fragment_phone) {

    private val REQUEST_CALL_PERMISSION = 1
    var selectedPhoneNumber: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                .replace(R.id.fragment_container, MessageFragment())
                .addToBackStack(null)
                .commit()
        }

        // Handle the click on the contact widget
        val fabUser: LinearLayout = view.findViewById(R.id.contacts_section)
        fabUser.setOnClickListener {
            selectedPhoneNumber = "tel:+17207376472" // Set the contact's phone number
            // Update UI to indicate the selected contact
        }

        val fabCall: FloatingActionButton = view.findViewById(R.id.fab_call)
        fabCall.setOnClickListener {
            if (selectedPhoneNumber != null) {
                checkAndRequestPermission()
            } else {
                Toast.makeText(requireContext(), "No contact selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CALL_PHONE),
                REQUEST_CALL_PERMISSION
            )
        } else {
            initiateCall()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CALL_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initiateCall()
            } else {
                Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initiateCall() {
        selectedPhoneNumber?.let { phoneNumber ->
            val dialIntent = Intent(Intent.ACTION_CALL, Uri.parse(phoneNumber))
            startActivity(dialIntent)
        }
    }
}
