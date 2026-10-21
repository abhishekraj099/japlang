package com.example.jap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.jap.data.Config

/** A legal document shown in the app: the privacy policy, terms of use or licenses. */
enum class LegalDoc(val title: String, val file: String) {
    PRIVACY("Privacy policy", "legal/privacy.md"),
    TERMS("Terms of use", "legal/terms.md"),
    LICENSES("Licenses", "legal/licenses.md")
}

/**
 * Shows one of the documents in assets/legal: headings, paragraphs and bullet lists, with **bold** runs. The
 * effective date and contact address come from config.json, so the same files can also be hosted on the web.
 */
@Composable
fun LegalScreen(doc: LegalDoc, onBack: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val lines = remember(doc) {
        val text = runCatching { context.assets.open(doc.file).bufferedReader().use { it.readText() } }.getOrDefault("")
        text.replace("{date}", Config.legalDate)
            .replace("{contact}", Config.contactEmail.ifBlank { "the contact address in the app's store listing" })
            .lines()
    }
    SubScreen(doc.title, onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            lines.forEach { line ->
                when {
                    line.startsWith("# ") -> Unit // The screen title already says it.
                    line.startsWith("## ") -> Text(
                        line.removePrefix("## "), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = scheme.onBackground, modifier = Modifier.padding(top = 20.dp, bottom = 6.dp)
                    )
                    line.startsWith("- ") -> Row(Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 6.dp)) {
                        Text("•  ", color = scheme.onSurfaceVariant)
                        Text(bold(line.removePrefix("- ")), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    }
                    line.isBlank() -> Unit
                    else -> Text(
                        bold(line), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

/** Turns **bold** runs into bold text. */
private fun bold(text: String): AnnotatedString = buildAnnotatedString {
    text.split("**").forEachIndexed { i, part -> if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(part) } else append(part) }
}
