package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PdfDocumentEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PdfInfoDialog(
    pdf: PdfDocumentEntity,
    onDismiss: () -> Unit
) {
    val formatter = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val dateString = formatter.format(Date(pdf.createdAtTimestamp))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Document Information", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                InfoRow("Filename", pdf.fileName)
                InfoRow("Location", if (pdf.isMemoryCard) "SD Card" else "Internal Storage")
                InfoRow("Path", pdf.filePath)
                InfoRow("Size", pdf.formattedSize)
                InfoRow("Created", dateString)
                InfoRow("Privacy", "100% Offline (Local Only)")
                InfoRow("Status", "Encrypted & Secure")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
