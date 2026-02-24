package com.example.bookstack.ui.bookdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.bookstack.data.model.Book
import com.example.bookstack.data.model.BookSize

/**
 * 本の詳細画面。
 *
 * 機能:
 * - 書籍の詳細情報表示（大きな書影、タイトル、著者、ページ数）
 * - 読書ステータス変更（未読/読書中/読了）
 * - ページ数の手動修正
 * - 書籍の削除
 *
 * @param viewModel BookDetailViewModel
 * @param bookId 表示する書籍のID
 * @param onNavigateBack 戻るボタン押下時のコールバック
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    viewModel: BookDetailViewModel,
    bookId: String,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // 初回読み込み
    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("本の詳細") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is BookDetailUiState.Loading -> {
                    LoadingContent()
                }
                is BookDetailUiState.Success -> {
                    BookDetailContent(
                        book = state.book,
                        onStatusChange = { newStatus ->
                            viewModel.updateReadingStatus(newStatus)
                        },
                        onPageCountChange = { newPageCount ->
                            viewModel.updatePageCount(newPageCount)
                        },
                        onReadingProgressUpdate = { newCurrentPage ->
                            viewModel.updateReadingProgress(newCurrentPage)
                        },
                        onDeleteBook = {
                            viewModel.deleteBook(state.book.id ?: "") {
                                onNavigateBack()
                            }
                        }
                    )
                }
                is BookDetailUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadBook(bookId) }
                    )
                }
            }
        }
    }
}

/**
 * ローディング表示。
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * 書籍詳細情報の表示。
 */
@Composable
private fun BookDetailContent(
    book: Book,
    onStatusChange: (ReadingStatus) -> Unit,
    onPageCountChange: (Int) -> Unit,
    onReadingProgressUpdate: (Int) -> Unit,
    onDeleteBook: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPageCountDialog by remember { mutableStateOf(false) }
    var showReadingProgressDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 書影画像
        AsyncImage(
            model = book.coverImageUrl,
            contentDescription = "書籍の表紙",
            modifier = Modifier
                .size(width = 200.dp, height = 280.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray),
            contentScale = ContentScale.Crop
        )

        // タイトル
        Text(
            text = book.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // 著者
        Text(
            text = book.author,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 書籍情報カード
        BookInfoCard(
            isbn = book.isbn,
            pageCount = book.pageCount,
            bookSize = book.bookSize,
            onEditPageCount = { showPageCountDialog = true }
        )

        // ステータス変更セクション
        StatusChangeSection(
            currentStatus = ReadingStatus.fromValue(book.status),
            onStatusChange = onStatusChange
        )

        // 読書進捗記録セクション
        ReadingProgressSection(
            currentPage = book.currentPage,
            totalPages = book.pageCount ?: 0,
            onUpdateProgress = { showReadingProgressDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 削除ボタン
        OutlinedButton(
            onClick = { showDeleteDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "削除",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("この本を削除")
        }
    }

    // 削除確認ダイアログ
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            bookTitle = book.title,
            onConfirm = {
                showDeleteDialog = false
                onDeleteBook()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // ページ数編集ダイアログ
    if (showPageCountDialog) {
        PageCountEditDialog(
            currentPageCount = book.pageCount ?: 0,
            onConfirm = { newPageCount ->
                showPageCountDialog = false
                onPageCountChange(newPageCount)
            },
            onDismiss = { showPageCountDialog = false }
        )
    }

    // 読書進捗入力ダイアログ
    if (showReadingProgressDialog) {
        ReadingProgressDialog(
            currentPage = book.currentPage,
            totalPages = book.pageCount ?: 0,
            onConfirm = { newCurrentPage ->
                showReadingProgressDialog = false
                onReadingProgressUpdate(newCurrentPage)
            },
            onDismiss = { showReadingProgressDialog = false }
        )
    }
}

/**
 * 書籍情報カード。
 */
@Composable
private fun BookInfoCard(
    isbn: String,
    pageCount: Int?,
    bookSize: BookSize?,
    onEditPageCount: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoRow(label = "ISBN", value = isbn)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoRow(
                    label = "ページ数",
                    value = pageCount?.toString() ?: "不明",
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEditPageCount) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "編集",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            InfoRow(
                label = "判型",
                value = bookSize?.name ?: "不明"
            )
        }
    }
}

/**
 * 情報行コンポーネント。
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * ステータス変更セクション。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusChangeSection(
    currentStatus: ReadingStatus,
    onStatusChange: (ReadingStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "読書ステータス",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = currentStatus.displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ReadingStatus.entries.forEach { status ->
                    DropdownMenuItem(
                        text = { Text(status.displayName) },
                        onClick = {
                            onStatusChange(status)
                            expanded = false
                        },
                        leadingIcon = {
                            if (status == currentStatus) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * エラー表示。
 */
@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Text("再試行")
            }
        }
    }
}

/**
 * 削除確認ダイアログ。
 */
@Composable
private fun DeleteConfirmationDialog(
    bookTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("本を削除しますか？") },
        text = {
            Text("「$bookTitle」を削除します。この操作は取り消せません。")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("削除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

/**
 * ページ数編集ダイアログ。
 */
@Composable
private fun PageCountEditDialog(
    currentPageCount: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var pageCountText by remember { mutableStateOf(currentPageCount.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ページ数を修正") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("正しいページ数を入力してください。")
                OutlinedTextField(
                    value = pageCountText,
                    onValueChange = {
                        pageCountText = it
                        error = null
                    },
                    label = { Text("ページ数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newPageCount = pageCountText.toIntOrNull()
                    if (newPageCount == null || newPageCount <= 0) {
                        error = "正しいページ数を入力してください"
                    } else {
                        onConfirm(newPageCount)
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

/**
 * 読書進捗セクション。
 */
@Composable
private fun ReadingProgressSection(
    currentPage: Int,
    totalPages: Int,
    onUpdateProgress: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "読書進捗",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onUpdateProgress) {
                    Text("進捗を記録")
                }
            }

            // 進捗バー
            if (totalPages > 0) {
                val progress = (currentPage.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f)
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$currentPage / $totalPages ページ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "ページ数が設定されていません",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 読書進捗入力ダイアログ。
 */
@Composable
private fun ReadingProgressDialog(
    currentPage: Int,
    totalPages: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var newPageText by remember { mutableStateOf(currentPage.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("読書進捗を記録") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("現在読んでいるページ数を入力してください。")
                OutlinedTextField(
                    value = newPageText,
                    onValueChange = {
                        newPageText = it
                        error = null
                    },
                    label = { Text("現在のページ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true
                )
                if (totalPages > 0) {
                    Text(
                        text = "総ページ数: $totalPages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newPage = newPageText.toIntOrNull()
                    if (newPage == null) {
                        error = "正しいページ数を入力してください"
                    } else if (totalPages > 0 && (newPage < 0 || newPage > totalPages)) {
                        error = "0〜${totalPages}の範囲で入力してください"
                    } else if (newPage < 0) {
                        error = "0以上の値を入力してください"
                    } else {
                        onConfirm(newPage)
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
