package com.example.jap.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.jap.data.Progress
import com.example.jap.ui.theme.Kon
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.util.Date

private val Card = Color(0xFFFFFFFF)
private val Ink = Color(0xFF16182B)
private val InkSoft = Color(0xFF6B6E85)
private val Hairline = Color(0xFFE7E8EF)
private val Night = Color(0xFF1A2550)
private val Indigo = Color(0xFF34408F)
private val Sunset = Color(0xFFE2563F)
private val Peach = Color(0xFFF59A6B)

private const val NAME_LIMIT = 40

/**
 * A finished course's certificate. The first time any certificate is opened the learner types the
 * name to print; it is confirmed once and cannot be changed afterwards.
 */
@Composable
fun CertificateScreen(progress: Progress, key: String, course: String, onBack: () -> Unit) {
    SubScreen("Certificate", onBack) {
        if (progress.certName.isEmpty()) NameForm(progress) else Certificate(progress, key, course)
    }
}

@Composable
private fun NameForm(progress: Progress) {
    val scheme = MaterialTheme.colorScheme
    var name by rememberSaveable { mutableStateOf(progress.name) }
    var confirm by remember { mutableStateOf(false) }
    val clean = name.trim().replace(Regex("\\s+"), " ")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Seal("合格", 96.dp, Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp))
        Text("Congratulations!", style = MaterialTheme.typography.headlineMedium, color = scheme.onBackground, modifier = Modifier.padding(top = 12.dp))
        Text(
            "You've earned a certificate. Enter your name exactly as it should appear on it.",
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(NAME_LIMIT) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Full name") },
            supportingText = { Text("${clean.length} / $NAME_LIMIT") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            shape = RoundedCornerShape(14.dp)
        )
        Row(
            Modifier
                .padding(top = 12.dp, bottom = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.errorContainer)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = scheme.onErrorContainer)
            Text(
                "The name can be set only once. It will be used on all your certificates and can't be changed later.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onErrorContainer,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        PushButton("Create certificate", onClick = { confirm = true }, enabled = clean.length >= 2)
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Use this name?") },
            text = {
                Column {
                    Text(clean, fontFamily = FontFamily.Serif, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text("You won't be able to change it later.", modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    progress.lockCertName(clean)
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Edit") } }
        )
    }
}

@Composable
private fun Certificate(progress: Progress, key: String, course: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val issued = remember(key) { progress.certificateDate(key) }
    val date = DateFormat.getDateInstance(DateFormat.LONG).format(Date(issued))
    val serial = "MNB-${key.uppercase().take(4)}-${(issued / 1000 % 1_000_000).toString().padStart(6, '0')}"

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
            }
        ) {
            CertificateArt(progress.certName, course, glyphFor(key), date, serial)
        }
        PushButton(
            "Share certificate",
            onClick = {
                scope.launch {
                    val bitmap = layer.toImageBitmap().asAndroidBitmap()
                    share(context, bitmap, course)
                }
            },
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            "Issued to ${progress.certName}. The name on certificates is fixed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

/** The large kana shown behind the title for each certificate. */
private fun glyphFor(key: String) = when {
    key.startsWith("kata") -> "ア"
    key.startsWith("comb") -> "が"
    else -> "あ"
}

private val Spaced = 2.4.sp

/**
 * The certificate itself: a white card with a gradient header carrying a giant kana, then the
 * recipient, the course and a footer of issue date, credential ID and a round award badge.
 */
@Composable
private fun CertificateArt(name: String, course: String, glyph: String, date: String, serial: String) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Card)
            .border(1.dp, Hairline, shape)
    ) {
        // Header.
        Box(
            Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clipToBounds()
                .background(Brush.linearGradient(listOf(Night, Indigo, Sunset), Offset.Zero, Offset(900f, 700f)))
        ) {
            Text(
                glyph,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 260.sp,
                color = Color.White.copy(alpha = 0.10f),
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 30.dp, y = 70.dp)
            )
            Canvas(Modifier.fillMaxSize()) {
                // A thin arc and a sun dot: a quiet nod to the logo.
                drawCircle(Peach, 9.dp.toPx(), Offset(size.width - 44.dp.toPx(), 44.dp.toPx()))
                drawCircle(Color.White.copy(alpha = 0.18f), 120.dp.toPx(), Offset(size.width - 44.dp.toPx(), 44.dp.toPx()), style = Stroke(1.dp.toPx()))
                drawCircle(Color.White.copy(alpha = 0.10f), 170.dp.toPx(), Offset(size.width - 44.dp.toPx(), 44.dp.toPx()), style = Stroke(1.dp.toPx()))
            }
            Column(Modifier.padding(28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandMark(30.dp, shape = RoundedCornerShape(9.dp))
                    Text("MANABI", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, color = Color.White, modifier = Modifier.padding(start = 10.dp))
                }
                Box(Modifier.weight(1f))
                Text("Certificate", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 42.sp)
                Text("of Completion · 修了証", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.82f))
            }
        }

        // Body.
        Column(Modifier.padding(horizontal = 28.dp, vertical = 28.dp)) {
            Label("AWARDED TO")
            Text(
                name,
                fontSize = if (name.length > 20) 28.sp else 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
                lineHeight = 40.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
            Box(
                Modifier
                    .padding(top = 14.dp)
                    .width(56.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(listOf(Indigo, Sunset)))
            )
            Text(
                "for successfully completing",
                fontSize = 15.sp,
                color = InkSoft,
                modifier = Modifier.padding(top = 22.dp)
            )
            Text(course, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 2.dp))
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Japanese writing system")
                Chip("JLPT N5 path")
            }

            Box(Modifier.padding(vertical = 26.dp).fillMaxWidth().height(1.dp).background(Hairline))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column {
                        Label("ISSUED")
                        Text(date, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.padding(top = 3.dp))
                    }
                    Column {
                        Label("CREDENTIAL ID")
                        Text(serial, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.padding(top = 3.dp))
                    }
                }
                Badge()
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = Spaced, color = InkSoft)
}

@Composable
private fun Chip(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Indigo,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Indigo.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** A round award badge: gradient ring, notched rosette edge and 合格 at the centre. */
@Composable
private fun Badge() {
    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            val outer = size.width / 2
            // Rosette edge.
            repeat(24) { i ->
                val a = Math.toRadians(i * 15.0)
                drawCircle(
                    Sunset.copy(alpha = 0.16f), outer * 0.12f,
                    Offset(c.x + (kotlin.math.cos(a) * outer * 0.86f).toFloat(), c.y + (kotlin.math.sin(a) * outer * 0.86f).toFloat())
                )
            }
            drawCircle(Brush.linearGradient(listOf(Indigo, Sunset), Offset(0f, 0f), Offset(size.width, size.height)), outer * 0.80f, c)
            drawCircle(Card, outer * 0.66f, c)
            drawCircle(Brush.linearGradient(listOf(Indigo, Sunset), Offset(0f, 0f), Offset(size.width, size.height)), outer * 0.66f, c, style = Stroke(1.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("合格", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Night, lineHeight = 24.sp)
            Text("PASSED", fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Sunset)
        }
    }
}

private fun share(context: Context, bitmap: Bitmap, course: String) {
    val dir = File(context.cacheDir, "certificates").apply { mkdirs() }
    val file = File(dir, "certificate.png")
    // Lay the card on a soft background with a margin, so its rounded corners don't show as black in chat apps.
    val margin = bitmap.width / 14
    val framed = Bitmap.createBitmap(bitmap.width + margin * 2, bitmap.height + margin * 2, Bitmap.Config.ARGB_8888)
    android.graphics.Canvas(framed).apply {
        drawColor(0xFFF1EEF6.toInt())
        // The captured layer is a hardware bitmap, which a software canvas can't draw.
        drawBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, false), margin.toFloat(), margin.toFloat(), null)
    }
    file.outputStream().use { framed.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, "I completed $course on Manabi!")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(Intent.createChooser(send, null)) }
}
