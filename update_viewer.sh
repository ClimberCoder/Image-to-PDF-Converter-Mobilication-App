sed -i 's/var showMenu by remember { mutableStateOf(false) }/var showMenu by remember { mutableStateOf(false) }; var showInfoDialog by remember { mutableStateOf(false) }/' app/src/main/java/com/example/ui/components/PdfViewerDialog.kt

sed -i '/DropdownMenuItem(/i\
                                DropdownMenuItem(\
                                    text = { Text("Document Info") },\
                                    onClick = { showMenu = false; showInfoDialog = true }\
                                )' app/src/main/java/com/example/ui/components/PdfViewerDialog.kt

sed -i '/} \/\/ Top Controls/i\
                if (showInfoDialog) {\
                    com.example.ui.components.PdfInfoDialog(\
                        pdf = pdf,\
                        onDismiss = { showInfoDialog = false }\
                    )\
                }' app/src/main/java/com/example/ui/components/PdfViewerDialog.kt
