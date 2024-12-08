package com.ideality.idealityproject

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginService: LoginService
): ViewModel() {
    private val _email = mutableStateOf(TextFieldValue(""))
    private val _password = mutableStateOf(TextFieldValue(""))
    var email: TextFieldValue
        get() = _email.value
        set(value) {
            _email.value = value
        }
    var password: TextFieldValue
        get() = _password.value
        set(value) {
            _password.value = value
        }

    var isSignedIn by mutableStateOf(false)

    val user = loginService.user

    val showEmailError = mutableStateOf(false)
    val showPasswordError = mutableStateOf(false)

    fun launchCatching(block: suspend () -> Unit) {
        viewModelScope.launch(CoroutineExceptionHandler { context, throwable ->
            Log.d("LoginViewModel", "Got error (${throwable.message.orEmpty()})")
        }) {
            block()
        }
    }

    fun signIn() {
        launchCatching {
            loginService.signIn(email.text, password.text)
            isSignedIn = true
        }
    }

    fun signUp() {
        launchCatching {
            loginService.signUp(email.text, password.text)
            isSignedIn = true
        }
    }

    fun signUpAnonymously() {
        launchCatching {
            loginService.signInAnonymous()
            isSignedIn = true
        }
    }

    fun signOut() {
        launchCatching {
            loginService.signOut()
            isSignedIn = false
        }
    }
}