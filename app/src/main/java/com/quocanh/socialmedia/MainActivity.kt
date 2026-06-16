package com.quocanh.socialmedia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.quocanh.socialmedia.firebase.FirebaseManager
import com.quocanh.socialmedia.ui.theme.SocialMediaTheme
import com.quocanh.socialmedia.utils.CloudinaryManager
import com.quocanh.socialmedia.view.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Cloudinary
        CloudinaryManager.init(this)

        setContent {
            SocialMediaTheme {
                var currentScreen by remember { 
                    mutableStateOf(if (FirebaseManager.getCurrentUser() != null) "check_info" else "login") 
                }
                var targetUserId by remember { mutableStateOf<String?>(null) }

                // Kiểm tra xem người dùng đã cập nhật thông tin bổ sung chưa
                if (currentScreen == "check_info") {
                    val currentUser = FirebaseManager.getCurrentUser()
                    if (currentUser != null) {
                        FirebaseManager.getUserInfo(currentUser.uid) { user ->
                            // Nếu đã có ngày sinh (birthday) nghĩa là đã hoàn tất hồ sơ
                            if (user != null && user.birthday.isNotEmpty()) {
                                currentScreen = "home"
                            } else {
                                // Nếu chưa có thông tin bổ sung, yêu cầu nhập
                                currentScreen = "user_info"
                            }
                        }
                    } else {
                        currentScreen = "login"
                    }
                }

                when (currentScreen) {
                    "login" -> LoginScreen(
                        onNavigateToRegister = { currentScreen = "register" },
                        onLoginSuccess = { currentScreen = "check_info" }
                    )
                    "register" -> RegisterScreen(
                        onSuccess = { currentScreen = "login" },
                        onNavigateToLogin = { currentScreen = "login" }
                    )
                    "user_info" -> UserInfoScreen(
                        onComplete = { currentScreen = "home" }
                    )
                    "home" -> HomeScreen(
                        onLogout = {
                            FirebaseManager.logout()
                            currentScreen = "login"
                        },
                        onNavigateToProfile = { userId ->
                            targetUserId = userId
                            currentScreen = "profile"
                        }
                    )
                    "profile" -> {
                        targetUserId?.let { userId ->
                            ProfileScreen(
                                userId = userId,
                                onBack = { currentScreen = "home" },
                                onNavigateToProfile = { newUserId ->
                                    targetUserId = newUserId
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
