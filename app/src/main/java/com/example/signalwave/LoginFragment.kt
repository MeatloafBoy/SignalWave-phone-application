package com.example.signalwave

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException


class LoginFragment : Fragment(R.layout.fragment_login) {
    private lateinit var nameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var roleSpinner: Spinner
    private lateinit var loginButton: Button
    private lateinit var createAccountButton: Button

    // Get a reference to the Firestore database
    private val firestoreDb = Firebase.firestore
    // Get the FirebaseAuth instance
    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize UI components
        nameEditText = view.findViewById(R.id.nameEditText)
        usernameEditText = view.findViewById(R.id.usernameEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        phoneEditText = view.findViewById(R.id.phoneEditText)
        roleSpinner = view.findViewById(R.id.roleSpinner)
        loginButton = view.findViewById(R.id.loginButton)
        createAccountButton = view.findViewById(R.id.createAccountButton)

        auth = FirebaseAuth.getInstance()

        // Set a click listener for the login button
        loginButton.setOnClickListener {
            handleLogin()
        }

        createAccountButton.setOnClickListener {
            handleCreateAccount() // Set click listener for create account button
        }
    }

    private fun handleCreateAccount() {
        val name = nameEditText.text.toString().trim()
        val email = usernameEditText.text.toString().trim() // Use email for Firebase Auth
        val password = passwordEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()
        val role = roleSpinner.selectedItem.toString() // You can store role in Firestore

        // Basic input validation
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || role.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields to create an account", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable the button to prevent multiple clicks
        createAccountButton.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                createAccountButton.isEnabled = true // Re-enable the button

                if (task.isSuccessful) {
                    // User created successfully in Firebase Authentication
                    val user = auth.currentUser
                    Log.d("FirebaseAuth", "createUserWithEmailAndPassword:success")

                    // Now, you can store additional user data like 'role' in Firestore
                    // Use the Firebase Auth User's UID as the document ID in Firestore
                    user?.let {
                        storeUserDataInFirestore(it.uid, name, email, phone, role) // Pass UID, email, and role
                    }

                } else {
                    // Sign in failed
                    Log.w("FirebaseAuth", "createUserWithEmailAndPassword:failure", task.exception)

                    if (task.exception is FirebaseAuthUserCollisionException) {
                        // Handle the case where the email address is already in use
                        Toast.makeText(requireContext(), "Email address is already registered.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            requireContext(), "Authentication failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }

    private fun storeUserDataInFirestore(uid: String, name: String, email: String, phone: String, role: String) {
        val firestoreDb = Firebase.firestore // Assuming you have a Firestore instance

        // Create a map of user data
        val userFirestoreData = hashMapOf(
            "userID" to uid,
            "name" to name,
            "email" to email,
            "role" to role,
            "phone" to phone
        )

        // Store the data in a document with the UID as the document ID
        firestoreDb.collection("users").document(uid)
            .set(userFirestoreData)
            .addOnSuccessListener {
                Log.d("Firestore", "User data stored successfully for UID: $uid")
                Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_SHORT).show()

                // Navigate to MainFragment after both Auth and Firestore are successful
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, MainFragment())
                    .addToBackStack(null)
                    .commit()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error storing user data for UID: $uid", e)
                Toast.makeText(requireContext(), "Failed to store user data.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePhoneNumberInFirestore(uid: String, phoneNumber: String) {
        val userDocumentRef = firestoreDb.collection("users").document(uid)

        // Use the update() method to update specific fields
        userDocumentRef.update("phone", phoneNumber)
            .addOnSuccessListener {
                Log.d("Firestore", "Phone number updated successfully for UID: $uid")
                Toast.makeText(requireContext(), "Phone number updated.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error updating phone number for UID: $uid", e)
                Toast.makeText(requireContext(), "Failed to update phone number.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleLogin() {
        val email = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val desiredRole = roleSpinner.selectedItem.toString()

        // Basic input validation
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable the login button while checking credentials to prevent multiple clicks
        loginButton.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                loginButton.isEnabled = true // Re-enable the login button

                if (task.isSuccessful) {
                    // Firebase Authentication successful
                    Log.d("FirebaseAuth", "signInWithEmailAndPassword:success")
                    val user = auth.currentUser

                    // Now, check the user's role from Firestore
                    user?.let { firebaseUser ->
                        checkUserRoleAndNavigate(firebaseUser.uid, desiredRole)
                    }

                } else {
                    // Firebase Authentication failed
                    Log.w("FirebaseAuth", "signInWithEmailAndPassword:failure", task.exception)

                    // Handle specific authentication errors
                    when (task.exception) {
                        is FirebaseAuthInvalidUserException -> {
                            Toast.makeText(requireContext(), "No account found with this email.", Toast.LENGTH_SHORT).show()
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            Toast.makeText(requireContext(), "Invalid password.", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(
                                requireContext(), "Authentication failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
    }

    private fun checkUserRoleAndNavigate(uid: String, desiredRole: String) {
        firestoreDb.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val storedRole = document.getString("role")

                    Log.d("Login", "Stored role: $storedRole")

                    if (storedRole == desiredRole) {
                        Log.d("Login", "Login successful for user $uid with role $storedRole")
                        Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()

                        val enteredPhoneNumber = phoneEditText.text.toString().trim()
                        val storedPhone = document.getString("phone")
                        Log.d("Login", "Stored phone: $storedPhone")

                        if (enteredPhoneNumber.isNotEmpty() && enteredPhoneNumber != storedPhone) {
                            // Update the phone number in Firestore
                            updatePhoneNumberInFirestore(uid, enteredPhoneNumber)
                        }

                        // Navigate to MainFragment
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, MainFragment())
                            .addToBackStack(null)
                            .commit()
                    } else {
                        Log.w("Login", "Role mismatch for user $uid. Stored role: $storedRole, Desired role: $desiredRole")
                        Toast.makeText(requireContext(), "Role mismatch. Please select the correct role.", Toast.LENGTH_SHORT).show()
                        // Optionally sign out the user from Firebase Auth if role mismatch should prevent access
                        auth.signOut()
                    }
                } else {
                    // This case should ideally not happen if user creation in Auth and Firestore was successful
                    Log.e("Login", "User data not found in Firestore for UID: $uid")
                    Toast.makeText(requireContext(), "User data missing. Please try creating an account again.", Toast.LENGTH_SHORT).show()
                    // Optionally sign out the user from Firebase Auth if data is missing
                    auth.signOut()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Login", "Error fetching user role for UID: $uid", e)
                Toast.makeText(requireContext(), "Error checking user role.", Toast.LENGTH_SHORT).show()
                // Optionally sign out the user on database error
                auth.signOut()
            }
    }
}