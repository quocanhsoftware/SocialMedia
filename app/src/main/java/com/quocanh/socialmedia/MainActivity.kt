package com.quocanh.socialmedia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.quocanh.socialmedia.firebase.FirebaseManager
import com.quocanh.socialmedia.ui.theme.SocialMediaTheme
import com.quocanh.socialmedia.view.HomeScreen
import com.quocanh.socialmedia.view.LoginScreen
import com.quocanh.socialmedia.view.RegisterScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SocialMediaTheme {
                // Tự động vào Home nếu đã đăng nhập
                var currentScreen by remember { 
                    mutableStateOf(if (FirebaseManager.getCurrentUser() != null) "home" else "login") 
                }

                when (currentScreen) {
                    "login" -> LoginScreen(
                        onNavigateToRegister = { currentScreen = "register" },
                        onLoginSuccess = { currentScreen = "home" }
                    )
                    "register" -> RegisterScreen(
                        onSuccess = { currentScreen = "login" }
                    )
                    "home" -> HomeScreen(
                        onLogout = {
                            FirebaseManager.logout()
                            currentScreen = "login"
                        }
                    )
                }
            }
        }
    }
}
