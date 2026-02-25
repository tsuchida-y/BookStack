package com.example.bookstack.ui.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

/**
 * 読書ヒートマップ画面。
 *
 * 機能:
 * - 月間カレンダー形式で読書記録を可視化
 * - 日付ごとの読書量を色の濃淡で表現
 * - 月の切り替え機能
 * - 統計情報の表示（総ページ数、読書日数）
 *
 * @param viewModel ReadingHeatmapViewModel
 * @param onNavigateBack 戻るボタン押下時のコールバック
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingHeatmapScreen(
    viewModel: ReadingHeatmapViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    var currentYearMonth by remember { mutableStateOf(YearMonth(today.year, today.month)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("読書カレンダー") },
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
                is ReadingHeatmapUiState.Loading -> {
                    LoadingContent()
                }
                is ReadingHeatmapUiState.Success -> {
                    MonthlyCalendarContent(
                        currentYearMonth = currentYearMonth,
                        dailyStats = state.dailyStats,
                        totalPages = state.totalPages,
                        totalDays = state.totalDays,
                        onPreviousMonth = {
                            currentYearMonth = currentYearMonth.previousMonth()
                        },
                        onNextMonth = {
                            currentYearMonth = currentYearMonth.nextMonth()
                        },
                        onTodayClick = {
                            currentYearMonth = YearMonth(today.year, today.month)
                        }
                    )
                }
                is ReadingHeatmapUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadHeatmapData() }
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "読書記録を読み込んでいます...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * 月間カレンダー形式のコンテンツ。
 */
@Composable
private fun MonthlyCalendarContent(
    currentYearMonth: YearMonth,
    dailyStats: Map<LocalDate, Int>,
    totalPages: Int,
    totalDays: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 統計情報カード
        MonthlyStatisticsCard(
            currentYearMonth = currentYearMonth,
            dailyStats = dailyStats
        )

        // 月の切り替えヘッダー
        MonthNavigationHeader(
            currentYearMonth = currentYearMonth,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onTodayClick = onTodayClick
        )

        // カレンダーグリッド
        MonthlyCalendarGrid(
            currentYearMonth = currentYearMonth,
            dailyStats = dailyStats
        )

        // 凡例
        HeatmapLegend()
    }
}

/**
 * 月間統計情報カード。
 */
@Composable
private fun MonthlyStatisticsCard(
    currentYearMonth: YearMonth,
    dailyStats: Map<LocalDate, Int>
) {
    // 当月のデータのみを集計
    val monthlyData = dailyStats.filter { (date, _) ->
        date.year == currentYearMonth.year && date.month == currentYearMonth.month
    }

    val monthlyPages = monthlyData.values.sum()
    val monthlyDays = monthlyData.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "今月の読書ページ数",
                value = "$monthlyPages ページ"
            )
            HorizontalDivider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp)
            )
            StatItem(
                label = "今月の読書日数",
                value = "$monthlyDays 日"
            )
        }
    }
}

/**
 * 月のナビゲーションヘッダー。
 */
@Composable
private fun MonthNavigationHeader(
    currentYearMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 前月ボタン
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "前月"
            )
        }

        // 年月表示
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${currentYearMonth.year}年 ${currentYearMonth.month.value}月",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            // 今月へ戻るボタン
            TextButton(onClick = onTodayClick) {
                Text("今月", style = MaterialTheme.typography.bodySmall)
            }
        }

        // 次月ボタン
        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "次月"
            )
        }
    }
}

/**
 * 統計アイテム。
 */
@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

/**
 * 月間カレンダーグリッド。
 * 日本の標準的なカレンダー形式（日曜始まり）で表示。
 */
@Composable
private fun MonthlyCalendarGrid(
    currentYearMonth: YearMonth,
    dailyStats: Map<LocalDate, Int>
) {
    // 月の初日
    val firstDayOfMonth = LocalDate(currentYearMonth.year, currentYearMonth.month, 1)
    // 月の最終日
    val lastDayOfMonth = LocalDate(
        currentYearMonth.year,
        currentYearMonth.month,
        currentYearMonth.month.length(isLeapYear(currentYearMonth.year))
    )

    // 月の初日の曜日（日曜=0, 月曜=1, ..., 土曜=6）
    // DayOfWeek.MONDAY=1, TUESDAY=2, ..., SUNDAY=7なので調整
    val firstDayOfWeek = when (firstDayOfMonth.dayOfWeek) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }

    // カレンダーグリッドに表示する全ての日付を生成
    val calendarDays = buildList {
        // 前月の余白部分
        repeat(firstDayOfWeek) {
            add(null) // 空のセル
        }

        // 当月の日付
        var currentDate = firstDayOfMonth
        while (currentDate <= lastDayOfMonth) {
            add(currentDate)
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 曜日ヘッダー
            WeekdayHeader()

            Spacer(modifier = Modifier.height(8.dp))

            // カレンダーグリッド
            LazyVerticalGrid(
                columns = GridCells.Fixed(7), // 7列（日〜土）
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(300.dp) // 高さを固定
            ) {
                items(calendarDays) { date ->
                    if (date != null) {
                        val pagesRead = dailyStats[date] ?: 0
                        CalendarDayCell(
                            date = date,
                            pagesRead = pagesRead
                        )
                    } else {
                        // 空のセル
                        Box(modifier = Modifier.size(40.dp))
                    }
                }
            }
        }
    }
}

/**
 * 曜日ヘッダー。
 */
@Composable
private fun WeekdayHeader() {
    val weekdays = listOf("日", "月", "火", "水", "木", "金", "土")
    val weekdayColors = listOf(
        Color(0xFFE57373), // 日曜: 赤
        Color.Gray,        // 月曜
        Color.Gray,        // 火曜
        Color.Gray,        // 水曜
        Color.Gray,        // 木曜
        Color.Gray,        // 金曜
        Color(0xFF64B5F6)  // 土曜: 青
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekdays.forEachIndexed { index, day ->
            Text(
                text = day,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = weekdayColors[index],
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * カレンダーの日付セル。
 */
@Composable
private fun CalendarDayCell(
    date: LocalDate,
    pagesRead: Int
) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val isToday = date == today

    val color = when {
        pagesRead == 0 -> Color(0xFFE0E0E0) // グレー
        pagesRead <= 50 -> Color(0xFFC8E6C9) // 薄い緑
        pagesRead <= 100 -> Color(0xFF81C784) // 中程度の緑
        pagesRead <= 200 -> Color(0xFF4CAF50) // 濃い緑
        else -> Color(0xFF2E7D32) // 非常に濃い緑
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = color,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isToday) 2.dp else 0.5.dp,
                color = if (isToday) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (pagesRead > 0) Color.White else Color.Gray
            )
            if (pagesRead > 0) {
                Text(
                    text = "${pagesRead}p",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * ヒートマップの凡例。
 */
@Composable
private fun HeatmapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "少",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(end = 8.dp)
        )

        // 凡例のセル
        listOf(
            Color(0xFFE0E0E0), // 0ページ
            Color(0xFFC8E6C9), // 1-50ページ
            Color(0xFF81C784), // 51-100ページ
            Color(0xFF4CAF50), // 101-200ページ
            Color(0xFF2E7D32)  // 201ページ以上
        ).forEach { color ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(16.dp)
                    .background(color = color, shape = RoundedCornerShape(2.dp))
                    .border(width = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f), shape = RoundedCornerShape(2.dp))
            )
        }

        Text(
            text = "多",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp)
        )
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
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text("再試行")
            }
        }
    }
}

/**
 * 年月を表すデータクラス。
 */
data class YearMonth(
    val year: Int,
    val month: Month
) {
    fun previousMonth(): YearMonth {
        return if (month == Month.JANUARY) {
            YearMonth(year - 1, Month.DECEMBER)
        } else {
            YearMonth(year, Month(month.value - 1))
        }
    }

    fun nextMonth(): YearMonth {
        return if (month == Month.DECEMBER) {
            YearMonth(year + 1, Month.JANUARY)
        } else {
            YearMonth(year, Month(month.value + 1))
        }
    }
}

/**
 * 閏年判定。
 */
private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

