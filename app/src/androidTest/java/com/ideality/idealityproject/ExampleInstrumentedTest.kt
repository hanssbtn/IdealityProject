package com.ideality.idealityproject

import android.util.Log
import androidx.compose.ui.layout.FirstBaseline
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuthEmailException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.runners.Parameterized
import kotlin.math.log

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(Parameterized::class)
class ExampleInstrumentedTest {

    @Parameterized.Parameter(0)
    lateinit var email: String

    @Parameterized.Parameter(1)
    lateinit var password: String

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("test@example.com", "password123"),
                arrayOf("invalid@example.com", "wrongpassword")
            )
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + CoroutineExceptionHandler { _, err ->
        Log.e("LoginTest", "Failed to log in.", err)
    })

    @Test
    fun storageBucketTest() {
        assert(com.google.firebase.FirebaseApp.getInstance().options.storageBucket != null)
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.ideality.idealityproject", appContext.packageName)
    }

    @Test
    fun signUpTest() {
        Log.d("signUp", "Logging out")
        logoutTest()
        try {
            var result: Boolean
            runBlocking {
                result = signUp(email, password)
                Log.d("signUp", "Finished Test, result: ${result}")
            }
            assert(Firebase.auth.currentUser != null)
        } catch (e: FirebaseAuthUserCollisionException) {
            assert(Firebase.auth.currentUser == null)
        }
    }

    @Test
    fun signInTest() {
        Log.d("signIn", "Logging out")
        logoutTest()
        var result: Boolean
        runBlocking {
            result = signIn(email, password)
            Log.d("signIn", "Finished Test, result: ${result}")
        }
        assert(Firebase.auth.currentUser != null)
    }

    @Test
    fun anonymousLoginTest() {
        Log.d("anonymousLogin", "Logging out")
        logoutTest()
        runBlocking {
            Firebase.auth.signInAnonymously().await()
        }
        Log.d("anonymousLogin", "Finished Test, result: ${Firebase.auth.currentUser != null}")
        assert(Firebase.auth.currentUser != null)
    }

    @Test
    fun logoutTest() {
        if (Firebase.auth.currentUser == null) return
        // Perform logout
        Firebase.auth.signOut()
        // Check that the user is logged out
        assert(Firebase.auth.currentUser == null)
    }

    private suspend fun signIn(email: String, password: String): Boolean {
        Firebase.auth.signInWithEmailAndPassword(email, password).await()
        return Firebase.auth.currentUser != null
    }

    private suspend fun signUp(email: String, password: String): Boolean {
        Firebase.auth.createUserWithEmailAndPassword(email, password).await()
        return Firebase.auth.currentUser != null
    }
}