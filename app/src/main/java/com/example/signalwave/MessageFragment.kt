package com.example.signalwave

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import java.util.*

class MessageFragment : Fragment(R.layout.fragment_message) {
    private lateinit var messageListView: ListView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var messageAdapter: ArrayAdapter<String>
    private lateinit var firestore: FirebaseFirestore

    private val messagesList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_message, container, false)

        messageListView = view.findViewById(R.id.messageListView)
        messageInput = view.findViewById(R.id.messageInput)
        sendButton = view.findViewById(R.id.sendButton)

        firestore = FirebaseFirestore.getInstance()

        messageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, messagesList)
        messageListView.adapter = messageAdapter

        loadMessages()

        sendButton.setOnClickListener {
            sendMessage()
        }

        return view
    }

    private fun loadMessages() {
        // Retrieve messages from Firestore in real-time
        firestore.collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    messagesList.clear()
                    for (document in snapshot.documents) {
                        val message = document.getString("text")
                        if (message != null) {
                            messagesList.add(message)
                        }
                    }
                    messageAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun sendMessage() {
        val messageText = messageInput.text.toString()
        if (messageText.isNotEmpty()) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val message = hashMapOf(
                "text" to messageText,
                "sender" to (currentUser?.email ?: "anonymous"),
                "timestamp" to Date()
            )

            // Add message to Firestore
            firestore.collection("messages").add(message)
            messageInput.text.clear() // Clear input field
        }
    }
}