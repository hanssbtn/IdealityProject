package com.ideality.idealityproject

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LoginServiceImpl @Inject constructor(): LoginService {
    override val user: Flow<String?>
        get() = callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                this.trySend(auth.currentUser?.uid)
            }
            Firebase.auth.addAuthStateListener(listener)
            awaitClose { Firebase.auth.removeAuthStateListener(listener)}
        }

    override suspend fun signIn(email: String, password: String) {
        Firebase.auth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun signUp(email: String, password: String) {
        Firebase.auth.createUserWithEmailAndPassword(email, password).await()
    }

    override suspend fun signInAnonymous() {
        Firebase.auth.signInAnonymously().await()
    }

    override suspend fun signOut() {
        Firebase.auth.signOut()
    }

    override suspend fun deleteAccount() {
        Firebase.auth.currentUser!!.delete().await()
    }
}