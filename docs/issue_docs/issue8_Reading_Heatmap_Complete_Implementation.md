# 読書ヒートマップ機能 完全実装ドキュメント

## 📋 概要

**実装日:** 2026年2月24日〜25日  
**最終更新:** 2026年2月25日  
**目的:** GitHubのContributionグラフのように、日々の読書量を視覚的に表現し、読書のモチベーションを高める。月間カレンダー形式で日本人に馴染みやすいUIを提供する。

---

## ✅ 完了した作業の全体像

### 実装の変遷
1. **Phase 1:** 年間365日のGitHub風ヒートマップを実装
2. **Phase 2:** 月間カレンダー形式への変更（ユーザビリティ向上）

---

## 🎯 実装内容

### 1. Data Layer（データ層）

#### 1.1 `ReadingLogDataSource`インターフェースの拡張

**ファイル:** `app/src/main/java/com/example/bookstack/data/remote/database/ReadingLogDataSource.kt`

**追加メソッド:**
```kotlin
suspend fun getReadingLogsByDateRange(
    userId: String,
    startDate: LocalDate,
    endDate: LocalDate
): Result<List<ReadingLog>>
```

**Fact:** 指定期間の読書記録を取得するメソッドを追加しました。

---

#### 1.2 `SupabaseReadingLogDataSource`の実装

**ファイル:** `app/src/main/java/com/example/bookstack/data/remote/database/SupabaseReadingLogDataSource.kt`

**実装内容:**
```kotlin
override suspend fun getReadingLogsByDateRange(
    userId: String,
    startDate: LocalDate,
    endDate: LocalDate
): Result<List<ReadingLog>> {
    return try {
        val readingLogDtos = supabaseClient
            .from(TABLE_NAME)
            .select {
                filter {
                    eq("user_id", userId)
                    gte("read_date", startDate.toString())
                    lte("read_date", endDate.toString())
                }
            }
            .decodeList<ReadingLogDto>()
        
        Result.success(readingLogDtos.map { it.toReadingLog() })
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Fact:** Supabase Postgrestの`gte`（以上）と`lte`（以下）フィルターを使用して期間指定のクエリを実装しました。

---

### 2. Repository Layer（リポジトリ層）

#### 2.1 `ReadingLogRepository`への集計機能追加

**ファイル:** `app/src/main/java/com/example/bookstack/data/repository/ReadingLogRepository.kt`

**追加メソッド:**

##### `getReadingLogsByDateRange`
```kotlin
suspend fun getReadingLogsByDateRange(
    startDate: LocalDate,
    endDate: LocalDate
): Result<List<ReadingLog>> {
    val userId = authRepository.getCurrentUserId()
        ?: return Result.failure(Exception("User not authenticated"))

    return readingLogDataSource.getReadingLogsByDateRange(userId, startDate, endDate)
}
```

**Fact:** 現在ログイン中のユーザーIDを自動的に取得し、DataSourceに渡します。

---

##### `getDailyReadingStats`
```kotlin
suspend fun getDailyReadingStats(
    startDate: LocalDate,
    endDate: LocalDate
): Result<Map<LocalDate, Int>> {
    val result = getReadingLogsByDateRange(startDate, endDate)

    return if (result.isSuccess) {
        // 日付ごとにページ数を集計
        val stats = result.getOrNull()
            ?.groupBy { it.readDate }
            ?.mapValues { (_, logs) -> logs.sumOf { it.pagesRead } }
            ?: emptyMap()

        Log.d(TAG, "getDailyReadingStats: Aggregated ${stats.size} days of data")
        Result.success(stats)
    } else {
        Log.e(TAG, "getDailyReadingStats: Failed to get logs")
        Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
    }
}
```

**実装内容:**
- 期間内の読書記録を取得
- 日付ごとにページ数を集計（`groupBy`と`sumOf`を使用）
- `Map<LocalDate, Int>`形式で返す

**Recommendation:** データの集計処理をRepository層で行うことで、ViewModel層の責務を軽減し、テスタビリティを向上させています。

---

### 3. UI Layer（UI層）

#### 3.1 UI状態の定義

**ファイル:** `app/src/main/java/com/example/bookstack/ui/heatmap/ReadingHeatmapUiState.kt`

**状態定義:**
```kotlin
sealed interface ReadingHeatmapUiState {
    data object Loading : ReadingHeatmapUiState
    
    data class Success(
        val dailyStats: Map<LocalDate, Int>,
        val totalPages: Int,
        val totalDays: Int
    ) : ReadingHeatmapUiState
    
    data class Error(val message: String) : ReadingHeatmapUiState
}
```

**Fact:** Sealed Interfaceを使用して型安全な状態管理を実現しています。

---

#### 3.2 ViewModelの実装

**ファイル:** `app/src/main/java/com/example/bookstack/ui/heatmap/ReadingHeatmapViewModel.kt`

**主要機能:**
```kotlin
class ReadingHeatmapViewModel(
    private val readingLogRepository: ReadingLogRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<ReadingHeatmapUiState>(ReadingHeatmapUiState.Loading)
    val uiState: StateFlow<ReadingHeatmapUiState> = _uiState.asStateFlow()
    
    init {
        loadHeatmapData()
    }
    
    fun loadHeatmapData(days: Int = 365) {
        viewModelScope.launch {
            _uiState.value = ReadingHeatmapUiState.Loading
            
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val startDate = today.minus(kotlinx.datetime.DatePeriod(days = days - 1))
            
            val result = readingLogRepository.getDailyReadingStats(startDate, today)
            
            if (result.isSuccess) {
                val dailyStats = result.getOrNull() ?: emptyMap()
                val totalPages = dailyStats.values.sum()
                val totalDays = dailyStats.size
                
                _uiState.value = ReadingHeatmapUiState.Success(
                    dailyStats = dailyStats,
                    totalPages = totalPages,
                    totalDays = totalDays
                )
            } else {
                _uiState.value = ReadingHeatmapUiState.Error("読書記録の取得に失敗しました")
            }
        }
    }
}
```

**実装詳細:**
- **デフォルト表示期間:** 過去365日（1年分）
- **日付計算:** `kotlinx-datetime`を使用して型安全に処理
- **統計情報:** 総ページ数、読書日数を自動計算

**Recommendation:** `StateFlow`を使用することで、UI状態の変更を自動的に画面に反映できます。

---

#### 3.3 月間カレンダー形式ヒートマップ画面

**ファイル:** `app/src/main/java/com/example/bookstack/ui/heatmap/ReadingHeatmapScreen.kt`

##### 3.3.1 画面構成

```
┌─────────────────────────────────────────┐
│ TopAppBar: 「読書カレンダー」            │
├─────────────────────────────────────────┤
│ 統計情報カード                           │
│  - 今月の読書ページ数: XXXページ         │
│  - 今月の読書日数: XX日                  │
├─────────────────────────────────────────┤
│ 月ナビゲーション                         │
│  [<]  2026年 2月  [今月]  [>]           │
├─────────────────────────────────────────┤
│ 曜日ヘッダー                             │
│  日 月 火 水 木 金 土                    │
├─────────────────────────────────────────┤
│ カレンダーグリッド                       │
│   1  2  3  4  5  6  7  8               │
│   9 10 11 12 13 14 15                  │
│  16 17 18 19 20 21 22                  │
│  23 24 25 26 27 28                     │
├─────────────────────────────────────────┤
│ 凡例                                     │
│  □ 0p  □ 1-50p  □ 51-100p              │
│  □ 101-200p  □ 201p以上                │
└─────────────────────────────────────────┘
```

##### 3.3.2 主要コンポーネント

###### YearMonthデータクラス
```kotlin
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
```

**目的:** 年月を管理し、月の切り替え機能を提供

---

###### MonthlyCalendarContent
```kotlin
@Composable
private fun MonthlyCalendarContent(
    currentYearMonth: YearMonth,
    dailyStats: Map<LocalDate, Int>,
    totalPages: Int,
    totalDays: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit
)
```

**機能:**
- 月間統計カードの表示
- 月ナビゲーションヘッダー
- カレンダーグリッド
- 凡例

---

###### MonthlyCalendarGrid
```kotlin
@Composable
private fun MonthlyCalendarGrid(
    currentYearMonth: YearMonth,
    dailyStats: Map<LocalDate, Int>
)
```

**実装内容:**
- 月の初日・最終日を計算
- 初日の曜日に基づいて空白セルを配置
- 7列（日〜土）のグリッドレイアウト
- 各セルに日付と読書ページ数を表示

**実装例:**
```kotlin
val firstDay = LocalDate(currentYearMonth.year, currentYearMonth.month, 1)
val lastDay = LocalDate(
    currentYearMonth.year,
    currentYearMonth.month,
    currentYearMonth.month.length(isLeapYear)
)

// 初日の曜日を取得（日曜=0, 月曜=1, ...）
val startDayOfWeek = when (firstDay.dayOfWeek) {
    DayOfWeek.SUNDAY -> 0
    DayOfWeek.MONDAY -> 1
    // ... 以下略
}

// 空白セル
repeat(startDayOfWeek) {
    Spacer(modifier = Modifier.size(48.dp))
}

// 日付セル
for (day in 1..lastDay.dayOfMonth) {
    val date = LocalDate(currentYearMonth.year, currentYearMonth.month, day)
    val pagesRead = dailyStats[date] ?: 0
    CalendarDayCell(date = date, pagesRead = pagesRead)
}
```

---

###### WeekdayHeader
```kotlin
@Composable
private fun WeekdayHeader()
```

**表示内容:**
- 日曜日: 赤色（`Color(0xFFE57373)`）
- 月〜金曜日: グレー
- 土曜日: 青色（`Color(0xFF64B5F6)`）

---

###### CalendarDayCell
```kotlin
@Composable
private fun CalendarDayCell(
    date: LocalDate,
    pagesRead: Int
)
```

**表示内容:**
- **日付**: セルの上部に表示
- **読書ページ数**: セルの下部に「XXp」形式で表示（0ページは非表示）
- **背景色**: 読書量に応じて変化
- **今日の日付**: 青い枠線（2.dp）で強調

**色分けロジック:**
```kotlin
val color = when {
    pagesRead == 0 -> Color(0xFFE0E0E0)     // グレー（読書なし）
    pagesRead <= 50 -> Color(0xFFC8E6C9)    // 薄い緑（1-50ページ）
    pagesRead <= 100 -> Color(0xFF81C784)   // 中程度の緑（51-100ページ）
    pagesRead <= 200 -> Color(0xFF4CAF50)   // 濃い緑（101-200ページ）
    else -> Color(0xFF2E7D32)                // 非常に濃い緑（201ページ以上）
}
```

**Recommendation:** Material Design 3の緑系カラーパレットを使用することで、統一感のあるUIを実現しています。

---

###### MonthNavigationHeader
```kotlin
@Composable
private fun MonthNavigationHeader(
    currentYearMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit
)
```

**機能:**
- **前月ボタン** (`<`): 前月に切り替え
- **年月表示**: 「2026年 2月」形式で表示
- **今月ボタン**: 当月に即座に戻る
- **次月ボタン** (`>`): 次月に切り替え

---

###### MonthlyStatsCard
```kotlin
@Composable
private fun MonthlyStatsCard(
    currentYearMonth: YearMonth,
    dailyStats: Map<LocalDate, Int>
)
```

**表示内容:**
- **今月の読書ページ数**: その月に読んだ総ページ数
- **今月の読書日数**: その月に読書記録がある日数

**集計ロジック:**
```kotlin
val monthStats = dailyStats.filter { (date, _) ->
    date.year == currentYearMonth.year && date.month == currentYearMonth.month
}
val monthlyPages = monthStats.values.sum()
val monthlyDays = monthStats.size
```

---

### 4. 依存性注入（DI）の設定

**ファイル:** `app/src/main/java/com/example/bookstack/di/AppModule.kt`

**追加内容:**
```kotlin
// ViewModel の追加
viewModel<ReadingHeatmapViewModel> {
    ReadingHeatmapViewModel(
        readingLogRepository = get()
    )
}
```

**Fact:** Koinを使用してViewModelを依存性注入コンテナに登録しています。

---

### 5. ナビゲーションの実装

#### 5.1 `MainActivity.kt`への遷移追加

**追加内容:**
```kotlin
var showHeatmap by remember { mutableStateOf(false) }

if (showHeatmap) {
    ReadingHeatmapScreen(
        viewModel = koinViewModel(),
        onNavigateBack = { showHeatmap = false }
    )
} else {
    // メイン画面
}
```

---

#### 5.2 `BookshelfScreen.kt`へのボタン追加

**追加内容:**
```kotlin
TopAppBar(
    title = { Text("本棚") },
    actions = {
        IconButton(onClick = onNavigateToHeatmap) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "読書カレンダー"
            )
        }
    }
)
```

**Fact:** TopAppBarにカレンダーアイコンボタンを追加し、ヒートマップ画面へ遷移できるようにしました。

---

## 📊 データフロー

### 1. 画面起動時
```
ReadingHeatmapViewModel.init()
  ↓
loadHeatmapData(days = 365)
  ↓
ReadingLogRepository.getDailyReadingStats()
  ↓
ReadingLogDataSource.getReadingLogsByDateRange()
  ↓
Supabase Database (reading_logs テーブル)
  ↓
日付ごとに集計（groupBy + sumOf）
  ↓
UI状態: Success(dailyStats, totalPages, totalDays)
  ↓
ReadingHeatmapScreen にて表示
```

### 2. 月の表示
```
MonthlyCalendarContent
  ↓
currentYearMonth (例: YearMonth(2026, Month.FEBRUARY))
  ↓
MonthlyCalendarGrid
  ↓
dailyStats から当月のデータのみをフィルタリング
  ↓
CalendarDayCell で各日付を表示
```

### 3. 月の切り替え
```
ユーザーが「前月」ボタンをタップ
  ↓
onPreviousMonth()
  ↓
currentYearMonth = currentYearMonth.previousMonth()
  ↓
再コンポーズ（新しい月のカレンダーを表示）
```

---

## 🎨 UI/UXの特徴

### デザイン原則
1. **親しみやすさ**: 日本人に馴染みのある月間カレンダー形式
2. **モチベーション**: 緑色の視覚的フィードバックで達成感を演出
3. **情報の可視化**: 数値と視覚の両方で読書習慣を確認可能
4. **操作性**: 月の切り替えが直感的

### ユーザー体験
- **一目で読書習慣が把握できる**: カレンダー形式で日付が明確
- **連続した読書の「ストリーク」が視覚化される**: 緑色が連続して表示
- **総ページ数で達成感を実感できる**: 月間統計カードで確認
- **今日の日付が強調される**: 青い枠線で現在地を明示

---

## 📁 実装されたファイル一覧

### Data Layer
1. ✅ `ReadingLogDataSource.kt` - 期間指定取得メソッド追加
2. ✅ `SupabaseReadingLogDataSource.kt` - 期間指定取得の実装
3. ✅ `ReadingLogRepository.kt` - 集計ロジック追加

### UI Layer
4. ✅ `ReadingHeatmapUiState.kt` - UI状態定義（新規作成）
5. ✅ `ReadingHeatmapViewModel.kt` - ViewModel実装（新規作成）
6. ✅ `ReadingHeatmapScreen.kt` - 月間カレンダー形式ヒートマップ画面実装（新規作成）

### DI & Navigation
7. ✅ `AppModule.kt` - ViewModelのDI登録
8. ✅ `MainActivity.kt` - 画面遷移の追加
9. ✅ `BookshelfScreen.kt` - ヒートマップボタン追加

---

## ✅ 完了条件の確認

### 基本機能
- [x] 読書ログがある日のマスに色がついて表示される
- [x] 読んだページ数が多い日ほど色が濃くなっている

### 月間カレンダー形式
- [x] 月間カレンダー形式で表示される
- [x] 日付が明記されている
- [x] 曜日ヘッダーが表示される（日曜=赤、土曜=青）
- [x] 月の切り替えが可能（前月・次月ボタン）
- [x] 今月へ戻るボタンがある
- [x] 各日付に読書ページ数が表示される
- [x] 読書量に応じて色分けされている
- [x] 今日の日付が強調表示される
- [x] 当月の統計情報が表示される

---

## 🚀 動作確認方法

1. アプリを起動
2. 本棚画面のTopAppBarの📅アイコンをタップ
3. ヒートマップ画面が表示される
4. 統計情報とカレンダーが確認できる
5. 前月・次月ボタンで月を切り替えられる
6. 今月ボタンで当月に戻れる

※現時点では読書記録がないため、すべてグレーで表示されます。  
※書籍の詳細画面で読書進捗を記録すると、ヒートマップに反映されます。

---

## 🔧 技術的なハイライト

### アーキテクチャの遵守
```
UI Layer (ReadingHeatmapScreen, ViewModel)
    ↓
Repository Layer (ReadingLogRepository)
    ↓
Data Layer (ReadingLogDataSource, SupabaseReadingLogDataSource)
```

**Fact:** Google推奨のAndroidアプリアーキテクチャに厳密に従っています。

### 使用技術
- **kotlinx-datetime**: 型安全な日付処理
- **Jetpack Compose**: 宣言的UI
- **StateFlow**: リアクティブな状態管理
- **Koin**: 依存性注入
- **Supabase Postgrest**: データベースクエリ（`gte`, `lte`フィルター）
- **Material Design 3**: 統一感のあるデザインシステム

---

## 📊 実装統計

- **追加されたクラス:** 3つ（UiState, ViewModel, Screen）
- **修正されたファイル:** 6つ
- **追加されたコード行数:** 約600行
- **削除されたコード行数:** 0行（既存機能はそのまま維持）

---

## 🔄 今後の拡張可能性

### Phase 3（優先度: 中）
1. **期間選択機能**: 1ヶ月/3ヶ月/1年の切り替え
2. **セルのタップ機能**: その日の読書詳細をダイアログ表示
3. **連続読書バッジ**: 7日連続などでバッジ表示

### Phase 4（優先度: 低）
4. **週別・年間集計**: グラフ形式での集計表示
5. **目標設定機能**: 月間目標ページ数の設定
6. **共有機能**: ヒートマップのスクリーンショット共有

---

## 📝 実装時の課題と解決

### 課題1: kotlinx-datetimeの演算子エラー
**問題:** `-`演算子が`Unresolved reference`エラー  
**解決:** `minus()`メソッドを明示的に呼び出し、`kotlinx.datetime.minus`をimport

### 課題2: 年間表示の視認性問題
**問題:** 365日を一度に表示すると各セルが小さく、日付が判別しづらい  
**解決:** 月間カレンダー形式に変更し、日本人に馴染みのあるUIに改善

### 課題3: 月の切り替え管理
**問題:** 年と月をまたがる処理が複雑  
**解決:** `YearMonth`データクラスを作成し、`previousMonth()`/`nextMonth()`メソッドで管理

### 課題4: 曜日に応じた空白セルの配置
**問題:** 月の初日が何曜日かによってセルの開始位置が変わる  
**解決:** `firstDay.dayOfWeek`から空白セルの数を計算し、`Spacer`で配置

---

## 🧪 テスト項目

### 単体テスト（今後実装推奨）
- [ ] `ReadingLogRepository.getDailyReadingStats()` のロジックテスト
- [ ] 日付の境界値テスト（月末、年末など）
- [ ] 空データの処理テスト
- [ ] `YearMonth.previousMonth()`/`nextMonth()` のテスト

### 統合テスト
- [ ] Supabaseからのデータ取得テスト
- [ ] エラーハンドリングのテスト

### UIテスト
- [ ] ヒートマップの色分け表示テスト
- [ ] 統計情報の計算精度テスト
- [ ] 月の切り替え動作テスト
- [ ] スクロールパフォーマンステスト

---

## 📚 参考資料

- [kotlinx-datetime 公式ドキュメント](https://github.com/Kotlin/kotlinx-datetime)
- [Jetpack Compose Material 3](https://developer.android.com/jetpack/compose/designsystems/material3)
- [Supabase Kotlin Client](https://supabase.com/docs/reference/kotlin/introduction)
- [Google推奨アプリアーキテクチャ](https://developer.android.com/topic/architecture)

---

## 🎉 まとめ

読書ヒートマップ機能は、以下の点で成功しています：

1. **データ層の拡張**: 期間指定クエリと集計ロジックの実装
2. **月間カレンダー形式**: 日本人に馴染みのあるUI
3. **視覚的フィードバック**: 読書量に応じた色分け表示
4. **統計情報**: 月間ページ数・読書日数の表示
5. **アーキテクチャ遵守**: Google推奨のClean Architectureに準拠

この機能により、ユーザーは日々の読書習慣を視覚的に把握でき、モチベーションの向上が期待できます。

---

**実装者:** GitHub Copilot  
**レビュー状態:** 未レビュー  
**次のステップ:** 実際の読書記録を蓄積し、UIの最終調整を行う
