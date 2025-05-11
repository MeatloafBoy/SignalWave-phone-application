package com.example.signalwave

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.signalwave.model.User
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ContactsFragment : Fragment(R.layout.fragment_contacts), ContactAdapter.OnContactClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    private val contactsList = mutableListOf<User>() // Assuming you have a User data class
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_contacts, container, false)

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

        recyclerView = view.findViewById(R.id.contactsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        contactAdapter = ContactAdapter(contactsList, this) // Pass the listener
        recyclerView.adapter = contactAdapter

        fetchContacts() // Fetch users from Firestore

        return view
    }

    private fun fetchContacts() {
        val currentUserUid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                contactsList.clear()
                for (document in result) {
                    val user = document.toObject(User::class.java)

                    val userWithId = user.copy(userID = document.id)

                    // Exclude the current user from the contact list
                    if (userWithId.userID != currentUserUid) {
                        contactsList.add(user)
                    } else {
                        Log.d("ContactsFragment", "Excluding current user: ${user.name}")
                    }
                }
                contactAdapter.notifyDataSetChanged()
                Log.d("ContactsFragment", "Fetched ${contactsList.size} contacts.")
            }
            .addOnFailureListener { exception ->
                Log.e("ContactsFragment", "Error fetching contacts", exception)
                Toast.makeText(requireContext(), "Failed to load contacts.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onContactClick(contact: User) {
        val currentUserUid = auth.currentUser?.uid ?: return
        val otherUserUid = contact.userID ?: return

        if (currentUserUid == null) {
            Log.e("ContactsFragment", "Current user UID is null. User not logged in?")
            Toast.makeText(requireContext(), "Please log in to message.", Toast.LENGTH_SHORT).show()
            return // Exit if current user is null
        }

        if (otherUserUid == null) {
            Log.e("ContactsFragment", "Selected contact UID is null for contact: ${contact.name}")
            Toast.makeText(requireContext(), "Cannot message this contact (missing ID).", Toast.LENGTH_SHORT).show()
            return // Exit if selected contact UID is null
        }

        // Determine/Create conversation ID
        val conversationId = getOrCreateConversationId(currentUserUid, otherUserUid)
        Log.d("ContactsFragment", "Generated conversation ID: $conversationId")

        // Navigate to MessageFragment
        val messageFragment = MessageFragment.newInstance(conversationId)
        Log.d("ContactsFragment", "Created MessageFragment instance.")

        try {
            // Use the FragmentManager to perform the fragment transaction
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, messageFragment)
                .addToBackStack(null)
                .commit()
            Log.d("ContactsFragment", "Fragment transaction committed successfully.")

        } catch (e: IllegalStateException) {
            Log.e("ContactsFragment", "IllegalStateException during fragment transaction", e)
            // This might happen if the activity is being destroyed.
            // You might need to check isResumed() before committing the transaction
        } catch (e: Exception) {
            Log.e("ContactsFragment", "Unexpected error during fragment transaction", e)
        }
    }

    private fun getOrCreateConversationId(uid1: String, uid2: String): String {
         val sortedUids = listOf(uid1, uid2).sorted()
        return "${sortedUids[0]}_${sortedUids[1]}"
    }
}