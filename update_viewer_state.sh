sed -i '/var readingMode by remember { mutableStateOf(ReadingMode.STANDARD) }/i \    val pagerState = rememberPagerState(pageCount = { previewState.pageCount })\n    val lazyListState = rememberLazyListState()' app/src/main/java/com/example/ui/components/PdfViewerDialog.kt

sed -i '/val pagerState = rememberPagerState/d' app/src/main/java/com/example/ui/components/PdfViewerDialog.kt
sed -i '/val lazyListState = rememberLazyListState/d' app/src/main/java/com/example/ui/components/PdfViewerDialog.kt
