package com.ideality.idealityproject

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ideality.idealityproject.ui.theme.AppTheme

@Composable
fun LoginScreen(vm: LoginViewModel, openAndPopUp: (String, String) -> Unit) {
    val context = LocalContext.current
    val showEmailError by vm.showEmailError
    val showPasswordError by vm.showPasswordError
    val user by vm.user.collectAsState(null)
    var passwordHidden by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(vm.isSignedIn) {
        if (vm.isSignedIn) {
            vm.user.collect { userId ->
                if (userId != null) {
                    Toast.makeText(context, "User logged in", Toast.LENGTH_SHORT).show()
                    openAndPopUp(MAIN, LOGIN_SCREEN)
                    // Optionally reset isSignedIn if needed
                }
            }
        }
    }

    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.8f).fillMaxHeight(0.6f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                OutlinedTextField(
                    label = { Text("Email") },
                    value = vm.email,
                    onValueChange = {
                        vm.email = filterMaxLength(it,256)
                        vm.showEmailError.value = false
                    },
                    isError = showEmailError,
                    supportingText = @Composable {
                        if (showEmailError) {
                            Text("Invalid email address")
                        } else {
                            Text("")
                        }
                    },

                )
                OutlinedTextField(
                    label = { Text("Password") },
                    value = vm.password,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = showPasswordError,
                    visualTransformation = PasswordVisualTransformation(),
                    onValueChange = {
                        vm.password = filterMaxLength(it,50)
                        vm.showPasswordError.value = false
                    },
                    trailingIcon = @Composable {
                        IconButton(onClick = { passwordHidden = !passwordHidden }) {
                            val visibilityIcon =
                                if (passwordHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            // Provide localized description for accessibility services
                            val description = if (passwordHidden) "Show password" else "Hide password"
                            Icon(imageVector = visibilityIcon, contentDescription = description)
                        }
                    },
                    supportingText = @Composable {
                        if (showPasswordError) {
                            Text("Password should be longer than 8 characters", color = Color.Red)
                        } else {
                            Text("")
                        }
                    }
                )

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(0.8f).height(40.dp),
                    onClick = {

                        if (vm.email.text.isBlank()) {
                            vm.showEmailError.value = true
                        }
                        if (vm.password.text.isEmpty()) {
                            vm.showPasswordError.value = true
                        }
                        if (showEmailError || showPasswordError) return@OutlinedButton

                        vm.signIn()
//                        Log.d("LoginScreen", "Username: ${user.orEmpty()}")
//                        if (user != null) {
//                            Toast.makeText(context, "User logged in", Toast.LENGTH_SHORT).show()
//                            openAndPopUp(MAIN, LOGIN_SCREEN)
//                        }
                    }
                ) {
                    Text("Sign In")
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(0.8f).height(40.dp),
                    onClick = {
                        if (vm.email.text.isBlank()) {
                            vm.showEmailError.value = true
                        }
                        if (vm.password.text.isEmpty()) {
                            vm.showPasswordError.value = true
                        }
                        if (showEmailError || showPasswordError) return@OutlinedButton
                        vm.signUp()
//                        Log.d("LoginScreen", "Username: ${user.orEmpty()}")
//                        if (user != null) {
//                            Toast.makeText(context, "User logged in", Toast.LENGTH_SHORT).show()
//                            openAndPopUp(MAIN, LOGIN_SCREEN)
//                        }
                    }
                ) {
                    Text("Sign Up")
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(0.8f).height(40.dp),
                    onClick = {
                        vm.showPasswordError.value = false
                        vm.showEmailError.value = false

                        vm.signUpAnonymously()
//                        Log.d("LoginScreen", "Username: ${user.orEmpty()}")
//                        if (user != null) {
//                            Toast.makeText(context, "User logged in", Toast.LENGTH_SHORT).show()
//                            openAndPopUp(MAIN, LOGIN_SCREEN)
//                        }
                    }
                ) {
                    Text("Continue as guest")
                }
        }
        }
    }

}

fun filterMaxLength(field: TextFieldValue,len: Int): TextFieldValue {
    val text = field.text
    val selection = field.selection
    val overflow = text.length - len
    return if (overflow > 0) {
        val head = selection.end - len
        val tail = selection.end
        if (head >= 0) {
            field.copy(text = text.substring(0, head) + text.substring(tail, text.length), selection = TextRange(head))
        } else {
            field.copy(text = text.take(len), selection = TextRange(len))
        }
    } else {
        field
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginPreview() = LoginScreen(vm = LoginViewModel(LoginServiceImpl()), {_,_ ->})
