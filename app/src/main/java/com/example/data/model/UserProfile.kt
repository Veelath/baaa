package com.example.data.model

data class UserProfile(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val displayName: String = "",
    val createdAt: Long = 0L,
    val lastLoginAt: Long = 0L
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "displayName" to if (displayName.isNotBlank()) displayName else "$firstName $lastName".trim(),
            "createdAt" to createdAt,
            "lastLoginAt" to lastLoginAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): UserProfile {
            val fName = map["firstName"] as? String ?: ""
            val lName = map["lastName"] as? String ?: ""
            val dName = map["displayName"] as? String ?: "$fName $lName".trim()
            return UserProfile(
                uid = map["uid"] as? String ?: "",
                firstName = fName,
                lastName = lName,
                email = map["email"] as? String ?: "",
                displayName = dName,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
                lastLoginAt = (map["lastLoginAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
