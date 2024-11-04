package com.ideality.idealityproject

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

interface LoginService {
    val user: Flow<String?>
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(email: String, password: String)
    suspend fun signInAnonymous()
    suspend fun deleteAccount()
    suspend fun signOut()

}

