package com.example.signalwave

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import com.example.signalwave.model.User

class SharedUsersViewModel : ViewModel() {

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    private val db = Firebase.firestore

    init {
        fetchUsers()
    }

    private fun fetchUsers() {
        viewModelScope.launch {
            db.collection("users")
                .get()
                .addOnSuccessListener { result ->
                    val userList = result.mapNotNull { document ->
                        document.toObject(User::class.java)
                    }
                    _users.value = userList
                    Log.d("SharedUsersViewModel", "Users fetched: ${userList.size}")
                }
                .addOnFailureListener { exception ->
                    Log.e("SharedUsersViewModel", "Error getting users", exception)
                }
        }
    }

    fun setUsers(userList: List<User>) {
        _users.value = userList
    }
}