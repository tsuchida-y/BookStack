package com.example.bookstack.ui.scan

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.bookstack.data.model.Book
import com.example.bookstack.util.BarcodeAnalyzer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors

/**
 * バーコードスキャン画面。
 *
 * 機能:
 * 1. カメラ権限のリクエスト
 * 2. CameraXによるカメラプレビュー表示
 * 3. ML Kitによるバーコード検出
 * 4. 書籍情報の確認ダイアログ表示
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BookScanScreen(
    viewModel: BookScanViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // カメラ権限がない場合は権限リクエスト画面を表示
    if (!cameraPermissionState.status.isGranted) {
        CameraPermissionScreen(
            permissionState = cameraPermissionState,
            onNavigateBack = onNavigateBack
        )
        return
    }

    // UI状態に応じた画面を表示
    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is BookScanUiState.Idle,
            is BookScanUiState.Scanning -> {
                // カメラプレビュー表示
                CameraPreviewScreen(
                    onBarcodeDetected = { isbn ->
                        viewModel.searchBookByIsbn(isbn)
                    }
                )
            }
            is BookScanUiState.Loading -> {
                // ローディング表示
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is BookScanUiState.BookFound -> {
                // 書籍情報確認ダイアログ
                BookConfirmationDialog(
                    book = state.book,
                    onConfirm = {
                        viewModel.saveBook(state.book)
                    },
                    onDismiss = {
                        viewModel.resetToScanning()
                    }
                )
            }
            is BookScanUiState.Error -> {
                // エラーダイアログ
                ErrorDialog(
                    message = state.message,
                    onDismiss = {
                        viewModel.resetToScanning()
                    }
                )
            }
            is BookScanUiState.Saved -> {
                // 保存完了ダイアログ
                SavedDialog(
                    onDismiss = {
                        viewModel.resetToScanning()
                    },
                    onNavigateBack = onNavigateBack
                )
            }
        }

        // 戻るボタン（常に表示）
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "戻る",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * カメラ権限リクエスト画面
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CameraPermissionScreen(
    permissionState: PermissionState,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "カメラ権限が必要です",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "バーコードをスキャンするには、カメラへのアクセスを許可してください。",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { permissionState.launchPermissionRequest() }) {
            Text("権限を許可")
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateBack) {
            Text("キャンセル")
        }
    }
}

/**
 * カメラプレビュー画面
 */
@Composable
private fun CameraPreviewScreen(
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isProcessing by remember { mutableStateOf(false) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // プレビュー設定
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // 画像解析設定（バーコードスキャン）
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(
                            Executors.newSingleThreadExecutor(),
                            BarcodeAnalyzer { isbn ->
                                if (!isProcessing) {
                                    isProcessing = true
                                    onBarcodeDetected(isbn)
                                }
                            }
                        )
                    }

                // カメラ選択（背面カメラ）
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // 既存のカメラをアンバインド
                    cameraProvider.unbindAll()

                    // カメラをバインド
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    println("Camera binding failed: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )

    // スキャンガイド（中央に枠線）
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(300.dp, 200.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        ) {}
        Text(
            text = "バーコードを枠内に合わせてください",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

/**
 * 書籍情報確認ダイアログ
 */
@Composable
private fun BookConfirmationDialog(
    book: Book,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("書籍を登録しますか？") },
        text = {
            Column {
                Text("タイトル: ${book.title}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("著者: ${book.author}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("ISBN: ${book.isbn ?: "不明"}")
                book.pageCount?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ページ数: $it ページ")
                }
                book.bookSize?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("判型: ${it.name}")
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("登録")
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
 * エラーダイアログ
 */
@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("エラー") },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

/**
 * 保存完了ダイアログ
 */
@Composable
private fun SavedDialog(
    onDismiss: () -> Unit,
    onNavigateBack: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("登録完了") },
        text = { Text("書籍を本棚に追加しました！") },
        confirmButton = {
            Button(onClick = onNavigateBack) {
                Text("本棚に戻る")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("続けてスキャン")
            }
        }
    )
}
