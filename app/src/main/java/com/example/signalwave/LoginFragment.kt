package com.example.signalwave

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment

class LoginFragment : Fragment(R.layout.fragment_login) {
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var roleSpinner: Spinner
    private lateinit var loginButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize UI components
        usernameEditText = view.findViewById(R.id.usernameEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        roleSpinner = view.findViewById(R.id.roleSpinner)
        loginButton = view.findViewById(R.id.loginButton)

        // Set a click listener for the login button
        loginButton.setOnClickListener {
            handleLogin()
        }
    }

    private fun handleLogin() {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val role = roleSpinner.selectedItem.toString()

        // Basic input validation
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Check credentials (this is just a basic example, usually you'd query a database or use Firebase)
        if (isValidCredentials(username, password, role)) {
            Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()

            // Navigate to MainFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MainFragment())
                .addToBackStack(null)
                .commit()
        } else {
            Toast.makeText(requireContext(), "Invalid credentials", Toast.LENGTH_SHORT).show()
        }
    }

    // Dummy function to validate credentials
    private fun isValidCredentials(username: String, password: String, role: String): Boolean {
        // In a real application, you'd query the database or authenticate with a server
        return when {
            username == "admin" && password == "12345" && role == "Admin" -> true
            username == "employee" && password == "67890" && role == "Employee" -> true
            username == "manager" && password == "11223" && role == "Manager" -> true
            else -> false
        }
    }
}