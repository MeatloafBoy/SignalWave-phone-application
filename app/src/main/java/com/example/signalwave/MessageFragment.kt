package com.example.signalwave

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app. Fragment
import com.google.android.material. floatingactionbutton. FloatingActionButton
import com.google.firebase. firestore. FirebaseFirestore
import com.google.firebase. auth. FirebaseAuth
import com.google.firebase. firestore. Query
import java.util.Date
import com.example.signalwave. model. Message


class MessageFragment : Fragment(R.layout.fragment_message) {

    companion object {
        private const val ARG_CONVERSATION_ID = "conversation_id"

        fun newInstance(conversationId: String): MessageFragment {
            val fragment = MessageFragment()
            val args = Bundle()
            args.putString(ARG_CONVERSATION_ID, conversationId)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var messageListView: ListView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button

    private lateinit var messageAdapter: MessageAdapter
    private lateinit var firestore: FirebaseFirestore

    private val messagesList = mutableListOf<Message>()
    private val senderNames = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize adapter
        messageAdapter = MessageAdapter(requireContext(), 0, messagesList, senderNames)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_message, container, false)

        // Initialize UI elements
        messageListView = view.findViewById(R.id.messageListView)
        messageInput = view.findViewById(R.id.messageInput)
        sendButton = view.findViewById(R.id.sendButton)

        // Set adapter
        messageListView.adapter = messageAdapter

        val fabSignal: FloatingActionButton = view.findViewById(R.id.fab_signal)
        fabSignal.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MainFragment())
                .addToBackStack(null)
                .commit()
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

        sendButton.setOnClickListener {
            sendMessage()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve conversationId from arguments and load messages
        val conversationId = arguments?.getString(ARG_CONVERSATION_ID)
        if (conversationId != null) {
            loadMessages(conversationId) // Pass the ID to loadMessages
        } else {
            Toast.makeText(requireContext(), "Error: Conversation ID not provided.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMessages(conversationId: String) {
        // Retrieve messages from Firestore in real-time
        firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    messagesList.clear()
                    val uniqueSenderIds = mutableSetOf<String>()

                    for (document in snapshot.documents) {
                        val message = document.toObject(Message::class.java)
                        if (message != null) {
                            messagesList.add(message)
                            uniqueSenderIds.add(message.senderId ?: "")
                        }
                    }

                    fetchSenderNames(uniqueSenderIds)

                    messageListView.smoothScrollToPosition(messagesList.size - 1)
                } else {
                    messagesList.clear()
                    messageAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun fetchSenderNames(senderIds: Set<String>) {
        val idsToFetch = senderIds.filter { !senderNames.containsKey(it) && it.isNotEmpty() }

        if (idsToFetch.isEmpty()) {
            // No new sender IDs to fetch
            messageAdapter.notifyDataSetChanged()
            return
        }

        // Fetch user data for the new sender IDs
        firestore.collection("users")
            .whereIn("userID", idsToFetch.toList())
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val userId = document.id
                    val userName = document.getString("name")
                    if (userName != null) {
                        senderNames[userId] = userName
                    }
                }
                // Now that we have the names, update the adapter
                messageAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                // Handle error fetching user names
                Log.e("MessageFragment", "Error fetching sender names", e)

                messageAdapter.notifyDataSetChanged()
            }
    }

    private fun sendMessage() {
        val conversationId = arguments?.getString(ARG_CONVERSATION_ID)
        if (conversationId == null) {
            Toast.makeText(requireContext(), "Cannot send message: Conversation ID is missing.", Toast.LENGTH_SHORT).show()
            return
        }

        val messageText = messageInput.text.toString()
        if (messageText.isNotEmpty()) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val message = hashMapOf(
                "text" to messageText,
                "senderId" to (currentUser?.uid ?: "anonymous"),
                "timestamp" to Date(),
                "read" to false
            )

            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener {
                    messageInput.text.clear()
                    firestore.collection("conversations")
                        .document(conversationId)
                        .update("lastMessageTimestamp", Date())
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
