package com.example.bookstack.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.bookstack.data.model.Book
import com.example.bookstack.data.model.BookSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

/**
 * 本の背表紙を模したカードコンポーネント（改善版）。
 *
 * 特徴:
 * - 書影画像からドミナントカラーを抽出して背景色に使用（グラデーション）
 * - 判型（BookSize）に応じて高さを可変
 * - ページ数に応じて幅（厚み）を可変（拡大版）
 * - タイトルを縦書き風に表示（複数行対応）
 * - 各書籍に微妙なゆらぎを追加
 * - 長押しで拡大プレビュー、タップで詳細画面遷移
 *
 * @param book 表示する書籍情報
 * @param onClick タップ時のコールバック
 * @param modifier Modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookSpineCard(
    book: Book,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // ドミナントカラーを格納するState
    var dominantColor by remember { mutableStateOf(getDefaultColor()) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 長押し時のスケール
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.1f else 1f,
        label = "scale"
    )

    // 判型に応じた高さを計算（ゆらぎを含む）
    val baseHeight = getHeightForBookSize(book.bookSize)
    val heightVariation = getHeightVariation(book.isbn)
    val finalHeight = baseHeight + heightVariation

    // ページ数に応じた幅（厚み）を計算（拡大版）
    val width = getWidthForPageCount(book.pageCount)

    // 書影画像からドミナントカラーを抽出
    LaunchedEffect(book.coverImageUrl) {
        book.coverImageUrl?.let { imageUrl ->
            coroutineScope.launch {
                val color = extractDominantColor(context, imageUrl)
                if (color != null) {
                    dominantColor = color
                }
            }
        }
    }

    // グラデーション効果（強化版）
    val gradient = Brush.verticalGradient(
        colors = listOf(
            dominantColor.copy(alpha = 1.0f),  // 上部は完全不透明
            dominantColor.copy(alpha = 0.95f),
            dominantColor.copy(alpha = 0.85f)  // 下部は少し透明
        )
    )

    Card(
        modifier = modifier
            .width(width)
            .height(finalHeight)
            .scale(scale)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { isPressed = true },
                onLongClickLabel = "プレビュー"
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 12.dp else 6.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // タイトルと著者を縦書き風に表示
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top // 背表紙なので上詰めで配置
            ) {
                // タイトル部分
                book.title.forEach { char ->
                    Text(
                        text = char.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 1.sp // 文字の間隔を調整
                        ),
                        color = getContrastingTextColor(dominantColor),
                        modifier = Modifier.rotate(
                            // 縦書きで回転が必要な記号を判定
                            if (char == 'ー' || char == '～' || char == '—' || char == '-') 90f else 0f
                        )
                    )
                }

                // タイトルと著者の間に少し隙間を空ける
                if (book.author.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // 著者名（少し小さめの縦書き）
                    book.author.take(10).forEach { char -> // 長すぎる場合は制限
                        Text(
                            text = char.toString(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 12.sp
                            ),
                            color = getContrastingTextColor(dominantColor).copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }

    // 長押しリリース時の処理
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

/**
 * 判型（BookSize）に応じた高さを返す。
 *
 * @param bookSize 書籍の判型
 * @return 高さ（Dp）
 */
private fun getHeightForBookSize(bookSize: BookSize?): Dp {
    return when (bookSize) {
        BookSize.S -> 120.dp      // 文庫、新書 (1.2倍)
        BookSize.M -> 144.dp      // 四六判、B6判 (1.2倍)
        BookSize.L -> 168.dp      // A5判、B5判 (1.2倍)
        BookSize.XL -> 192.dp     // A4判以上 (1.2倍)
        BookSize.UNKNOWN, null -> 144.dp // デフォルト (1.2倍)
    }
}

/**
 * ページ数に応じた幅（厚み）を返す（拡大版）。
 *
 * @param pageCount ページ数
 * @return 幅（Dp）
 */
private fun getWidthForPageCount(pageCount: Int?): Dp {
    return when {
        pageCount == null -> 37.5.dp          // 不明 (1.5倍)
        pageCount <= 100 -> 30.dp            // 薄い本 (1.5倍)
        pageCount <= 200 -> 37.5.dp          // 標準 (1.5倍)
        pageCount <= 300 -> 45.dp            // やや厚い (1.5倍)
        pageCount <= 500 -> 52.5.dp          // 厚い (1.5倍)
        else -> 60.dp                        // 非常に厚い (1.5倍)
    }
}

/**
 * ISBNをシード値として、各書籍に一意なゆらぎを生成する。
 * 同じ書籍は常に同じゆらぎ値を持つ。
 *
 * @param isbn ISBN文字列
 * @return ゆらぎの高さ（-5dp ~ +5dp）
 */
private fun getHeightVariation(isbn: String): Dp {
    // ISBNのハッシュコードをシード値として使用
    val seed = isbn.hashCode()
    val variation = (seed % 11) - 5 // -5 ~ +5の範囲
    return (variation.toFloat() * 0.5f).dp
}

/**
 * デフォルトの背表紙色を返す。
 * 書影画像が読み込めない場合や、ドミナントカラー抽出に失敗した場合に使用。
 *
 * @return デフォルトカラー
 */
private fun getDefaultColor(): Color {
    return Color(0xFF8B7355) // 本っぽい茶色
}

/**
 * 背景色に対して適切なコントラストを持つテキスト色を返す。
 *
 * @param backgroundColor 背景色
 * @return テキスト色（白または黒）
 */
private fun getContrastingTextColor(backgroundColor: Color): Color {
    // 輝度を計算（相対輝度の簡易版）
    val luminance = (0.299 * backgroundColor.red +
            0.587 * backgroundColor.green +
            0.114 * backgroundColor.blue)

    // 輝度が0.5以上なら黒、それ以下なら白
    return if (luminance > 0.5f) Color.Black else Color.White
}

/**
 * 書影画像URLからドミナントカラーを抽出する。
 *
 * @param context Context
 * @param imageUrl 書影画像のURL
 * @return ドミナントカラー、失敗時はnull
 */
private suspend fun extractDominantColor(
    context: android.content.Context,
    imageUrl: String
): Color? {
    return withContext(Dispatchers.IO) {
        try {
            // Coilで画像を読み込む
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false) // Palette APIはソフトウェアBitmapが必要
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap

                bitmap?.let {
                    // Palette APIでドミナントカラーを抽出
                    val palette = Palette.from(it).generate()

                    // 優先順位: Vibrant → Muted → DominantColor
                    val swatch = palette.vibrantSwatch
                        ?: palette.mutedSwatch
                        ?: palette.dominantSwatch

                    swatch?.rgb?.let { rgb ->
                        Color(rgb)
                    }
                }
            } else {
                null
            }
        } catch (e: Exception) {
            // エラー時はnullを返す（デフォルトカラーが使用される）
            null
        }
    }
}
