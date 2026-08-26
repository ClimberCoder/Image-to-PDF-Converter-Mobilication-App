sed -i '/Document Info/d' app/src/main/java/com/example/ui/components/PdfViewerDialog.kt
sed -i '/onClick = { showMenu = false; showInfoDialog = true }/d' app/src/main/java/com/example/ui/components/PdfViewerDialog.kt
sed -i '0,/DropdownMenuItem(/s//DropdownMenuItem(\n                                    text = { Text("Document Info") },\n                                    onClick = { showMenu = false; showInfoDialog = true }\n                                )\n                                DropdownMenuItem(/' app/src/main/java/com/example/ui/components/PdfViewerDialog.kt
