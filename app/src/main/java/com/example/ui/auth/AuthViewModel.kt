package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.google.firebase.FirebaseApp
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
        }
    }

    fun signUp(
        firstName: String,
        lastName: String,
        email: String,
        pass: String,
        confirmPass: String
    ) {
        if (auth == null) {
            _authState.value = AuthState.Error(
                "Firebase is not configured. Please place google-services.json in the app/ directory."
            )
            return
        }

        val trimmedFirst = firstName.trim()
        val trimmedLast = lastName.trim()
        val trimmedEmail = email.trim()

        if (trimmedFirst.isEmpty()) {
            _authState.value = AuthState.Error("Please enter your first name.")
            return
        }
        if (trimmedLast.isEmpty()) {
            _authState.value = AuthState.Error("Please enter your last name.")
            return
        }
        if (trimmedEmail.isEmpty()) {
            _authState.value = AuthState.Error("Please enter your email address.")
            return
        }
        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!trimmedEmail.matches(emailPattern)) {
            _authState.value = AuthState.Error("Please enter a valid email address.")
            return
        }
        if (pass.isEmpty()) {
            _authState.value = AuthState.Error("Please enter a password.")
            return
        }
        if (pass.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters.")
            return
        }
        if (pass != confirmPass) {
            _authState.value = AuthState.Error("Passwords do not match.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // 1. Create account in Firebase Auth
                val result = auth.createUserWithEmailAndPassword(trimmedEmail, pass).await()
                val user = result.user
                if (user == null) {
                    _authState.value = AuthState.Error("Sign up failed. Please try again.")
                    return@launch
                }

                val currentTime = System.currentTimeMillis()
                val fullName = "$trimmedFirst $trimmedLast".trim()
                val userProfile = UserProfile(
                    uid = user.uid,
                    firstName = trimmedFirst,
                    lastName = trimmedLast,
                    email = trimmedEmail,
                    displayName = fullName,
                    createdAt = currentTime,
                    lastLoginAt = currentTime
                )

                // 2. Persist profile document in Firestore
                try {
                    firestore?.collection("users")
                        ?.document(user.uid)
                        ?.set(userProfile.toMap())
                        ?.await()
                } catch (fe: Exception) {
                    // Firestore sync error (e.g. offline / rules)
                }

                _authState.value = AuthState.Success(user.uid, trimmedEmail, userProfile)
            } catch (e: FirebaseAuthWeakPasswordException) {
                _authState.value = AuthState.Error("Password is too weak. Please use at least 6 characters.")
            } catch (e: FirebaseAuthUserCollisionException) {
                _authState.value = AuthState.Error("An account with this email address already exists.")
            } catch (e: Exception) {
                val raw = e.localizedMessage ?: "Sign up failed."
                if (raw.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true)) {
                    _authState.value = AuthState.Error(
                        "Email/Password sign-in is not enabled in Firebase Console. Go to Authentication > Sign-in method to enable it."
                    )
                } else {
                    _authState.value = AuthState.Error(raw)
                }
            }
        }
    }

    fun signIn(email: String, pass: String) {
        if (auth == null) {
            _authState.value = AuthState.Error(
                "Firebase is not configured. Please place google-services.json in the app/ directory."
            )
            return
        }

        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            _authState.value = AuthState.Error("Please enter your email address.")
            return
        }
        if (pass.isEmpty()) {
            _authState.value = AuthState.Error("Please enter your password.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // 1. Authenticate with Firebase Auth
                val result = auth.signInWithEmailAndPassword(trimmedEmail, pass).await()
                val user = result.user
                if (user == null) {
                    _authState.value = AuthState.Error("Sign in failed.")
                    return@launch
                }

                // 2. Retrieve or update profile from Firestore
                val profile = fetchOrCreateUserProfile(user.uid, trimmedEmail)

                _authState.value = AuthState.Success(user.uid, trimmedEmail, profile)
            } catch (e: FirebaseAuthInvalidUserException) {
                _authState.value = AuthState.Error("No account found with this email.")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _authState.value = AuthState.Error("Incorrect password or email.")
            } catch (e: Exception) {
                val raw = e.localizedMessage ?: "Sign in failed."
                if (raw.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true)) {
                    _authState.value = AuthState.Error(
                        "Email/Password sign-in is not enabled in Firebase Console. Go to Authentication > Sign-in method to enable it."
                    )
                } else {
                    _authState.value = AuthState.Error(raw)
                }
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
                docRef.set(mapOf("lastLoginAt" to currentTime), SetOptions.merge()).await()
                val updatedData = doc.data?.toMutableMap() ?: mutableMapOf()
                updatedData["lastLoginAt"] = currentTime
                UserProfile.fromMap(updatedData)
            } else {
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
            UserProfile(
                uid = uid,
                email = email,
                displayName = email.substringBefore("@"),
                lastLoginAt = currentTime
            )
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
