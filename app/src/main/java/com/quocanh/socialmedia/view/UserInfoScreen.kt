package com.quocanh.socialmedia.view

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.quocanh.socialmedia.firebase.FirebaseManager
import com.quocanh.socialmedia.model.User
import com.quocanh.socialmedia.utils.CloudinaryManager
import com.quocanh.socialmedia.utils.ToastUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    var username by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Nam") }
    var birthday by remember { mutableStateOf("") }
    var hometown by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseManager.getCurrentUser()
        currentUser?.let { user ->
            FirebaseManager.getUserInfo(user.uid) { fetchedUser ->
                fetchedUser?.let {
                    username = it.username
                    gender = it.gender.ifEmpty { "Nam" }
                    birthday = it.birthday
                    hometown = it.hometown
                    avatarUrl = it.avatar
                }
            }
        }
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            birthday = "$dayOfMonth/${month + 1}/$year"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.surface
        )
    )

    Box(modifier = Modifier.fillMaxSize().background(gradientBrush)) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Hoàn thiện hồ sơ", fontWeight = FontWeight.ExtraBold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Avatar Picker with Camera Icon
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (avatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Ảnh đại diện", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        if (username.isNotEmpty()) {
                            Text(
                                text = "Chào $username!",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Vui lòng cung cấp thêm một vài thông tin để mọi người biết về bạn rõ hơn.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )
                        }

                        // Gender Selection
                        Text(
                            text = "Giới tính",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            GenderOption("Nam", gender == "Nam") { gender = "Nam" }
                            GenderOption("Nữ", gender == "Nữ") { gender = "Nữ" }
                            GenderOption("Khác", gender == "Khác") { gender = "Khác" }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Birthday field
                        OutlinedTextField(
                            value = birthday,
                            onValueChange = { },
                            label = { Text("Ngày sinh") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { datePickerDialog.show() },
                            readOnly = true,
                            enabled = false,
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = hometown,
                            onValueChange = { hometown = it },
                            label = { Text("Quê quán") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = {
                        isLoading = true
                        
                        val onSaveUserInfo: (String) -> Unit = { finalAvatarUrl ->
                            val user = User(
                                uid = FirebaseManager.auth.currentUser?.uid ?: "",
                                email = FirebaseManager.auth.currentUser?.email ?: "",
                                username = username,
                                avatar = finalAvatarUrl,
                                gender = gender,
                                birthday = birthday,
                                hometown = hometown
                            )
                            FirebaseManager.updateUserInfo(user) { success, _ ->
                                isLoading = false
                                if (success) {
                                    onComplete()
                                } else {
                                    ToastUtils.showToast(context, "Cập nhật thất bại")
                                }
                            }
                        }

                        if (selectedImageUri != null) {
                            CloudinaryManager.uploadImage(
                                uri = selectedImageUri!!,
                                onSuccess = { url ->
                                    onSaveUserInfo(url)
                                },
                                onError = { error ->
                                    isLoading = false
                                    ToastUtils.showToast(context, "Lỗi tải ảnh: $error")
                                }
                            )
                        } else {
                            onSaveUserInfo(avatarUrl)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("HOÀN TẤT", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun GenderOption(label: String, isSelected: Boolean, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier
            .clickable { onSelect() }
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
