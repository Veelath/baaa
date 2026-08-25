package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val uid: String, val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val user = auth?.currentUser
        if (user != null) {
            _authState.value = AuthState.Success(user.uid, user.email ?: "")
        } else if (auth == null) {
            _authState.value = AuthState.Error("Firebase is not configured. Please add your google-services.json in the Settings.")
        }
    }

    fun signUp(email: String, pass: String) {
        if (auth == null) {
            _authState.value = AuthState.Error("Firebase is not configured. Please add your google-services.json.")
            return
        }
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty.")
            return
        }
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val user = result.user
                if (user != null) {
                    _authState.value = AuthState.Success(user.uid, user.email ?: "")
                } else {
                    _authState.value = AuthState.Error("Sign up failed.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun signIn(email: String, pass: String) {
        if (auth == null) {
            _authState.value = AuthState.Error("Firebase is not configured. Please add your google-services.json.")
            return
        }
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                val user = result.user
                if (user != null) {
                    _authState.value = AuthState.Success(user.uid, user.email ?: "")
                } else {
                    _authState.value = AuthState.Error("Sign in failed.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            }
        }
    }

    fun signOut() {
        auth?.signOut()
        _authState.value = AuthState.Idle
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
