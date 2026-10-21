package com.example.jap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.data.Guide

/**
 * The guide's floating button: her face in a circle, always in the same corner of the main screens. It opens a
 * sheet to replay [screen]'s tour or ask a common question, which she answers out loud.
 */
@Composable
fun GuideHelperButton(screen: String, modifier: Modifier = Modifier) {
    val guide = LocalGuide.current ?: return
    var open by remember { mutableStateOf(false) }
    Box(
        modifier
            .size(56.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xFFFFE3EC))
            .border(2.dp, Color.White, CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Ask ${Guide.name} for help") { open = true }
            .semantics(mergeDescendants = true) { contentDescription = "Ask ${Guide.name} for help" },
        contentAlignment = Alignment.TopCenter
    ) {
        // Only her face shows inside the circle.
        GuideCharacter("wave", 72.dp, Modifier.wrapContentSize(Alignment.TopCenter, unbounded = true).padding(top = 2.dp))
    }
    if (open) GuideHelpSheet(screen, guide, onDismiss = { open = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuideHelpSheet(screen: String, guide: GuideController, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = scheme.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                GuideCharacter("wave", 64.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Hi, I'm ${Guide.name}!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                    Text("What can I help you with?", color = scheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (guide.hasTour(screen)) {
                HelpRow(Icons.Rounded.Explore, "Show me around this screen") {
                    onDismiss()
                    guide.replay(screen)
                }
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = scheme.outlineVariant)
            }
            Guide.faq.forEach { item ->
                HelpRow(Icons.AutoMirrored.Rounded.HelpOutline, item.question) {
                    onDismiss()
                    guide.say(item.answer, item.pose)
                }
            }
        }
    }
}

@Composable
private fun HelpRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(scheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = scheme.onPrimaryContainer)
        }
        Text(text, fontWeight = FontWeight.Medium, color = scheme.onSurface, modifier = Modifier.weight(1f).padding(horizontal = 14.dp))
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
    }
}

/**
 * Lets the learner choose their guide: a card per guide with their picture and name. Picking one switches the guide
 * everywhere at once, and [onPicked] lets the caller react, for example by having the new guide say hello.
 */
@Composable
fun GuidePicker(progress: com.example.jap.data.Progress, modifier: Modifier = Modifier, onPicked: () -> Unit = {}) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
        Guide.characters.forEach { character ->
            val selected = character.id == Guide.current.id
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) scheme.primaryContainer else scheme.surface)
                    .border(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant, RoundedCornerShape(18.dp))
                    .selectable(selected = selected, role = Role.RadioButton) {
                        progress.updateGuideCharacter(character.id)
                        onPicked()
                    }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GuideCharacter("wave", 64.dp, character = character.id)
                Text(
                    character.name, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    color = if (selected) scheme.onPrimaryContainer else scheme.onSurface, modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
