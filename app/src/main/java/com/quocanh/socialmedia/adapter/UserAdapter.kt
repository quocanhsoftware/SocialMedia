package com.quocanh.socialmedia.adapter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.quocanh.socialmedia.model.User

object UserAdapter {

    @Composable
    fun UserItem(
        user: User,
        onClick: (String) -> Unit
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(user.uid) },
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.avatar.isNotEmpty()) {
                        AsyncImage(
                            model = user.avatar,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person, 
                            contentDescription = null, 
                            modifier = Modifier.size(28.dp), 
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (user.hometown.isNotEmpty()) {
                        Text(
                            text = user.hometown,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    Icons.Default.ChevronRight, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun UserListSheet(
        isOpen: Boolean,
        title: String,
        users: List<User>,
        onDismiss: () -> Unit,
        onUserClick: (String) -> Unit
    ) {
        if (isOpen) {
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    if (users.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Person, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.outlineVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Danh sách này đang trống", 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            items(users, key = { it.uid }) { user ->
                                UserItem(user = user, onClick = {
                                    onDismiss()
                                    onUserClick(it)
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}
