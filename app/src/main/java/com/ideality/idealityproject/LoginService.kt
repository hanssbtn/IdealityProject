package com.ideality.idealityproject

import kotlinx.coroutines.flow.Flow

interface LoginService {
    val user: Flow<String?>
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(email: String, password: String)
    suspend fun signInAnonymous()
    suspend fun deleteAccount()
    suspend fun signOut()

}

