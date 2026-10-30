package com.wordpilot.assistant.keyboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmojiPanel(
    onEmojiClicked: (String) -> Unit,
    onBackspace: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        "😀" to listOf("😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🥲", "🥹", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🥸", "🤩", "🥳", "😏", "😒", "😞", "😔", "😟", "😕"),
        "👍" to listOf("👍", "👎", "👊", "✊", "🤛", "🤜", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✌️", "🤞", "🤟", "🤘", "👌", "🤌", "🤏", "👈", "👉", "👆", "👇", "☝️", "✋", "🤚", "🖐", "🖖", "👋", "🤙", "💪", "🦾", "🖕", "✍️", "🤳", "💅"),
        "❤️" to listOf("❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❤️‍🔥", "❤️‍🩹", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "💌", "💋", "💯", "💢", "💥", "💫", "💨", "🕳", "💬", "👁️‍🗨️", "🗨️", "🗯️", "💭", "💤"),
        "🎉" to listOf("🎉", "🎊", "🎂", "🎁", "🎈", "🎆", "🎇", "🧨", "✨", "🎃", "🎄", "🎋", "🎍", "🎏", "🎐", "🎑", "🧧", "🎀", "🎗️", "🎟️", "🎫", "🎖️", "🏆", "🏅", "🥇", "🥈", "🥉", "⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🏉"),
        "🔥" to listOf("🔥", "⚡", "🌟", "⭐", "🌈", "☀️", "🌤️", "⛅", "🌥️", "☁️", "🌦️", "🌧️", "⛈️", "🌩️", "🌨️", "❄️", "☃️", "⛄", "🌬️", "💨", "🌪️", "🌫️", "🌊", "💧", "💦", "🫧")
    )

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val currentEmojis = categories[selectedCategoryIndex].second

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(265.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Top Category Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for ((index, category) in categories.withIndex()) {
                val isSelected = selectedCategoryIndex == index
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF1976D2).copy(alpha = 0.2f) else Color.Transparent,
                    modifier = Modifier.clickable { selectedCategoryIndex = index }
                ) {
                    Text(
                        text = category.first,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 2. Emoji Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(currentEmojis) { emoji ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onEmojiClicked(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 22.sp)
                }
            }
        }

        // 3. Bottom Row: Back to ABC, Space, Backspace
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .width(70.dp)
                    .height(38.dp)
                    .clickable { onClose() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("ABC", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .padding(horizontal = 8.dp)
                    .clickable { onEmojiClicked(" ") }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("space", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .width(60.dp)
                    .height(38.dp)
                    .clickable { onBackspace() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
