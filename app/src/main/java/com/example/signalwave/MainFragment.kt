package com.example.signalwave

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainFragment : Fragment(R.layout.fragment_main) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                .replace(R.id.fragment_container, MessageFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
