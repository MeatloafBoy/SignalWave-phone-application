package com.example.signalwave.model

data class User(
        val userID: String = "",
        val name: String = "",
        val email: String = "",
        val role: String = "",
        val phone: String? = null
    )