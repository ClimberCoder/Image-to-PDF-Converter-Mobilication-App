sed -i '/Document Info/d' app/src/main/java/com/example/ui/components/PdfViewerDialog.kt
sed -i '/onClick = { showMenu = false; showInfoDialog = true }/d' app/src/main/java/com/example/ui/components/PdfViewerDialog.kt
sed -i '/DropdownMenuItem(/i\
                                DropdownMenuItem(\
                                    text = { Text("Document Info") },\
                                    onClick = { showMenu = false; showInfoDialog = true }\
                                )' app/src/main/java/com/example/ui/components/PdfViewerDialog.kt
