package com.example.jap.ui

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

private val Sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val Field = RoundedCornerShape(14.dp)

/**
 * Sign-in form. For now it only checks that the email looks like one and that a password
 * was typed; the password goes nowhere. [onSignIn] is where real authentication plugs in.
 */
@Composable
fun AuthScreen(onSignIn: (email: String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var tried by rememberSaveable { mutableStateOf(false) }

    val emailOk = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val passwordOk = password.isNotEmpty()

    fun submit() {
        tried = true
        if (emailOk && passwordOk) onSignIn(email)
    }

    Column(Modifier.fillMaxSize().background(scheme.background).imePadding().verticalScroll(rememberScrollState())) {
        BrandHeader(Modifier.height(340.dp))
        // The sheet slides up over the foot of the header.
        Column(
            Modifier
                .offset(y = (-28).dp)
                .fillMaxWidth()
                .clip(Sheet)
                .background(scheme.background)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Text("Welcome", style = MaterialTheme.typography.headlineMedium, color = scheme.onBackground)
            Text(
                "Sign in to keep your streak and progress.",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Email") },
                isError = tried && !emailOk,
                supportingText = { if (tried && !emailOk) Text("Enter a valid email address.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                shape = Field
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                singleLine = true,
                label = { Text("Password") },
                isError = tried && !passwordOk,
                supportingText = { if (tried && !passwordOk) Text("Enter a password.") },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                shape = Field
            )
            Spacer(Modifier.height(12.dp))
            PushButton("Continue", ::submit)
            Text(
                "New here? Continue with your email and we'll set you up.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
