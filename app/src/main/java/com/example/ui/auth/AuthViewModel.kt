package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(
        val uid: String,
        val email: String,
        val profile: UserProfile? = null
    ) : AuthState()
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

    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Exception) {
        null
    }

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val user = auth?.currentUser
        if (user != null) {
            viewModelScope.launch {
                val profile = fetchUserProfile(user.uid, user.email ?: "")
                _authState.value = AuthState.Success(user.uid, user.email ?: "", profile)
            }
        } else if (auth == null) {
            _authState.value = AuthState.Error(
                "Firebase is not configured. Please add your google-services.json to the app/ directory."
            )
        }
    }

    private fun validateInputs(email: String, pass: String): String? {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            return "Shepherd ID (Email) cannot be empty."
        }
        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!trimmedEmail.matches(emailPattern)) {
            return "Please enter a valid email address."
        }
        if (pass.isEmpty()) {
            return "Gate Key (Password) cannot be empty."
        }
        if (pass.length < 6) {
            return "Password must be at least 6 characters long."
        }
        return null
    }

    fun signUp(email: String, pass: String) {
        if (auth == null) {
            _authState.value = AuthState.Error("Firebase is not configured. Please add your google-services.json.")
            return
        }

        val validationError = validateInputs(email, pass)
        if (validationError != null) {
            _authState.value = AuthState.Error(validationError)
            return
        }

        val trimmedEmail = email.trim()

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // 1. Create user in Firebase Auth
                val result = auth.createUserWithEmailAndPassword(trimmedEmail, pass).await()
                val user = result.user
                if (user == null) {
                    _authState.value = AuthState.Error("Sign up failed: User creation returned null.")
                    return@launch
                }

                val currentTime = System.currentTimeMillis()
                val userProfile = UserProfile(
                    uid = user.uid,
                    email = trimmedEmail,
                    displayName = trimmedEmail.substringBefore("@"),
                    createdAt = currentTime,
                    lastLoginAt = currentTime
                )

                // 2. Persist user document in Firestore 'users' collection
                try {
                    firestore?.collection("users")
                        ?.document(user.uid)
                        ?.set(userProfile.toMap())
                        ?.await()
                } catch (fe: Exception) {
                    // Log or handle Firestore write error gracefully (e.g. offline/rules)
                }

                _authState.value = AuthState.Success(user.uid, trimmedEmail, userProfile)
            } catch (e: FirebaseAuthWeakPasswordException) {
                _authState.value = AuthState.Error("The password is too weak. Please use at least 6 characters.")
            } catch (e: FirebaseAuthUserCollisionException) {
                _authState.value = AuthState.Error("An account with this email already exists.")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Sign up failed.")
            }
        }
    }

    fun signIn(email: String, pass: String) {
        if (auth == null) {
            _authState.value = AuthState.Error("Firebase is not configured. Please add your google-services.json.")
            return
        }

        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || pass.isEmpty()) {
            _authState.value = AuthState.Error("Email and password cannot be empty.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // 1. Authenticate with Firebase Auth
                val result = auth.signInWithEmailAndPassword(trimmedEmail, pass).await()
                val user = result.user
                if (user == null) {
                    _authState.value = AuthState.Error("Sign in failed: User is null.")
                    return@launch
                }

                // 2. Validate/Fetch/Update user profile from Firestore
                val profile = fetchOrCreateUserProfile(user.uid, trimmedEmail)

                _authState.value = AuthState.Success(user.uid, trimmedEmail, profile)
            } catch (e: FirebaseAuthInvalidUserException) {
                _authState.value = AuthState.Error("No account found with this email.")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _authState.value = AuthState.Error("Incorrect password or malformed email.")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Sign in failed.")
            }
        }
    }

    private suspend fun fetchUserProfile(uid: String, email: String): UserProfile {
        return try {
            val doc = firestore?.collection("users")?.document(uid)?.get()?.await()
            if (doc != null && doc.exists()) {
                UserProfile.fromMap(doc.data ?: emptyMap())
            } else {
                UserProfile(uid = uid, email = email, displayName = email.substringBefore("@"))
            }
        } catch (e: Exception) {
            UserProfile(uid = uid, email = email, displayName = email.substringBefore("@"))
        }
    }

    private suspend fun fetchOrCreateUserProfile(uid: String, email: String): UserProfile {
        val currentTime = System.currentTimeMillis()
        return try {
            val docRef = firestore?.collection("users")?.document(uid)
            val doc = docRef?.get()?.await()

            if (doc != null && doc.exists()) {
                // Update lastLoginAt in Firestore
                docRef.set(mapOf("lastLoginAt" to currentTime), SetOptions.merge()).await()
                val updatedData = doc.data?.toMutableMap() ?: mutableMapOf()
                updatedData["lastLoginAt"] = currentTime
                UserProfile.fromMap(updatedData)
            } else {
                // Create user profile if it doesn't exist yet in Firestore
                val newProfile = UserProfile(
                    uid = uid,
                    email = email,
                    displayName = email.substringBefore("@"),
                    createdAt = currentTime,
                    lastLoginAt = currentTime
                )
                docRef?.set(newProfile.toMap())?.await()
                newProfile
            }
        } catch (e: Exception) {
            UserProfile(uid = uid, email = email, displayName = email.substringBefore("@"), lastLoginAt = currentTime)
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
