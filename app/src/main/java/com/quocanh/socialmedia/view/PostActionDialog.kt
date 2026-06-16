package com.quocanh.socialmedia.view

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.quocanh.socialmedia.firebase.FirebaseManager
import com.quocanh.socialmedia.model.Post
import com.quocanh.socialmedia.utils.CloudinaryManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostActionDialog(
    post: Post? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var content by remember { mutableStateOf(post?.content ?: "") }
    var imageUrl by remember { mutableStateOf(post?.imageUrl ?: "") }
    var isUploading by remember { mutableStateOf(false) }
    
    var currentUserAvatar by remember { mutableStateOf("") }
    var currentUsername by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        FirebaseManager.getCurrentUser()?.uid?.let { uid ->
            FirebaseManager.getUserInfo(uid) { user ->
                user?.let {
                    currentUserAvatar = it.avatar
                    currentUsername = it.username
                }
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploading = true
            CloudinaryManager.uploadImage(it, 
                onSuccess = { url ->
                    imageUrl = url
                    isUploading = false
                },
                onError = {
                    isUploading = false
                }
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Toolbar
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            if (post == null) "Tạo bài viết" else "Chỉnh sửa bài viết",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng")
                        }
                    },
                    actions = {
                        IconButton(onClick = { imagePicker.launch("image/*") }) {
                            Icon(
                                Icons.Default.AddPhotoAlternate, 
                                contentDescription = "Thêm ảnh",
                                tint = Color(0xFF45BD62)
                            )
                        }
                        Button(
                            onClick = { onConfirm(content, imageUrl) },
                            enabled = (content.isNotBlank() || imageUrl.isNotEmpty()) && !isUploading,
                            modifier = Modifier.padding(end = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text(if (post == null) "Đăng" else "Lưu")
                            }
                        }
                    }
                )

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // User Info Header
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentUserAvatar.isNotEmpty()) {
                            AsyncImage(
                                model = currentUserAvatar,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = currentUsername, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                // Editor
                Column(modifier = Modifier.weight(1f)) {
                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { 
                            Text(
                                "Bạn đang nghĩ gì?", 
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    )

                    if (imageUrl.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { imageUrl = "" },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Xóa ảnh", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                }
            }
        }
    }
}
