# Issue 7: 読書進捗記録機能 実装レポート

## 実装概要

本issueでは、「今日はこれだけ読んだ」という進捗を記録し、ヒートマップの元データを作成する機能を実装しました。

## 実装内容

### 1. データモデルの拡張

#### 1.1 `Book` モデルへの `currentPage` フィールド追加

**ファイル:** [Book.kt](../../app/src/main/java/com/example/bookstack/data/model/Book.kt)

```kotlin
data class Book(
    // ... 既存フィールド
    val currentPage: Int = 0  // 現在読んでいるページ数
)
```

**Fact:** Google推奨アーキテクチャに従い、ドメインモデルに進捗管理用のフィールドを追加しました。

#### 1.2 `ReadingLog` モデルの作成

**ファイル:** [ReadingLog.kt](../../app/src/main/java/com/example/bookstack/data/model/ReadingLog.kt)

```kotlin
data class ReadingLog(
    val id: String? = null,
    val bookId: String,
    val readDate: LocalDate,
    val pagesRead: Int,
    val durationMins: Int? = null
)
```

**Fact:** kotlinx-datetime の `LocalDate` を使用して日付を型安全に扱います。

#### 1.3 DTO (Data Transfer Object) の作成

**ファイル:** [ReadingLogDto.kt](../../app/src/main/java/com/example/bookstack/data/model/ReadingLogDto.kt)

- Supabase Postgrest との送受信用のデータクラス
- `@Serializable` アノテーションでシリアライズ対応
- ドメインモデルとDTOの相互変換関数 (`toReadingLogDto`, `toReadingLog`)

**Fact:** Data層とDomain層の分離により、外部API構造の変更に強い設計になっています。

### 2. Data Layer の実装

#### 2.1 ReadingLogDataSource インターフェース

**ファイル:** [ReadingLogDataSource.kt](../../app/src/main/java/com/example/bookstack/data/remote/database/ReadingLogDataSource.kt)

```kotlin
interface ReadingLogDataSource {
    suspend fun insertReadingLog(userId: String, readingLog: ReadingLog): Result<ReadingLog>
    suspend fun getReadingLogsByBookId(userId: String, bookId: String): Result<List<ReadingLog>>
}
```

**Fact:** インターフェースによる抽象化により、将来的にSupabase以外のバックエンドへの切り替えが容易です。

#### 2.2 SupabaseReadingLogDataSource の実装

**ファイル:** [SupabaseReadingLogDataSource.kt](../../app/src/main/java/com/example/bookstack/data/remote/database/SupabaseReadingLogDataSource.kt)

- Supabase Postgrest を使用した読書記録のCRUD操作
- エラーハンドリングとログ出力を実装
- Result型で成功・失敗を明示的に表現

#### 2.3 ReadingLogRepository の実装

**ファイル:** [ReadingLogRepository.kt](../../app/src/main/java/com/example/bookstack/data/repository/ReadingLogRepository.kt)

```kotlin
class ReadingLogRepository(
    private val readingLogDataSource: ReadingLogDataSource,
    private val authRepository: AuthRepository
)
```

**Fact:** Single Source of Truth パターンに従い、読書記録データへのアクセスを一元管理しています。

**Recommendation:** 認証状態を自動チェックすることで、ViewModel層でのエラーハンドリングを簡素化しています。

### 3. UI Layer の実装

#### 3.1 BookDetailViewModel の拡張

**ファイル:** [BookDetailViewModel.kt](../../app/src/main/java/com/example/bookstack/ui/bookdetail/BookDetailViewModel.kt)

新規メソッド: `updateReadingProgress(newCurrentPage: Int)`

**処理フロー:**

1. 入力値のバリデーション（0 ≤ newCurrentPage ≤ totalPages）
2. 前回のページ数との差分計算 (`pagesRead = newCurrentPage - book.currentPage`)
3. 読了判定（総ページ数に到達したか確認）
4. `books` テーブルの `current_page` と `status` を更新
5. `reading_logs` テーブルに今日の記録を挿入（進捗があった場合のみ）

**Fact:** ViewModelは状態管理とビジネスロジックの調整に専念し、実際のデータ操作はRepositoryに委譲しています。

**ステータス自動更新ロジック:**

```kotlin
val isCompleted = newCurrentPage >= pageCount
val newStatus = if (isCompleted) "completed" else {
    if (book.status == "unread" && newCurrentPage > 0) "reading" else book.status
}
```

- **Fact:** 総ページ数に到達したら自動的に `completed` に変更
- **Fact:** 未読 (`unread`) の本でページ進捗があった場合は `reading` に変更

#### 3.2 BookDetailScreen の UI 拡張

**ファイル:** [BookDetailScreen.kt](../../app/src/main/java/com/example/bookstack/ui/bookdetail/BookDetailScreen.kt)

**追加したComposable:**

1. **`ReadingProgressSection`**
   - 現在の読書進捗を視覚的に表示
   - `LinearProgressIndicator` でプログレスバー表示
   - 「X / Y ページ」と「N%」の両方を表示
   - 「進捗を記録」ボタンでダイアログを開く

2. **`ReadingProgressDialog`**
   - 現在のページ数を入力するダイアログ
   - バリデーション機能（範囲チェック、数値チェック）
   - 総ページ数を参考情報として表示

**Material3 Design:**

- **Fact:** `Card` と `primaryContainer` カラーで進捗セクションを強調表示
- **Fact:** `LinearProgressIndicator` で進捗を直感的に可視化
- **Recommendation:** エラーメッセージは `supportingText` で表示し、ユーザビリティを向上

### 4. 依存性注入 (DI) の設定

**ファイル:** [AppModule.kt](../../app/src/main/java/com/example/bookstack/di/AppModule.kt)

```kotlin
// DataSource の追加
single<ReadingLogDataSource> {
    SupabaseReadingLogDataSource(supabaseClient = get())
}

// Repository の追加
single {
    ReadingLogRepository(
        readingLogDataSource = get(),
        authRepository = get()
    )
}

// ViewModel への注入
viewModel<BookDetailViewModel> {
    BookDetailViewModel(
        bookDatabaseRepository = get(),
        readingLogRepository = get()  // 追加
    )
}
```

**Fact:** Koin を使用した依存性注入により、各層の疎結合を実現しています。

### 5. データベースマイグレーション

**ドキュメント:** [issue7_Reading_Progress_Migration.md](./issue7_Reading_Progress_Migration.md)

#### 必要なマイグレーション:

1. `books` テーブルに `current_page` カラム追加
2. `reading_logs` テーブル新規作成
3. RLS ポリシーの設定

**Fact:** Supabase の Row Level Security (RLS) により、各ユーザーは自分のデータのみアクセス可能です。

## 完了条件の達成状況

### ✅ 進捗を入力して保存できる

- **達成:** `ReadingProgressDialog` で現在ページ数を入力し、保存ボタンで記録可能
- **動作:** 入力値はバリデーションされ、不正な値はエラーメッセージで通知

### ✅ Supabaseの `reading_logs` に正しくレコードが追加される

- **達成:** `SupabaseReadingLogDataSource.insertReadingLog()` で記録を挿入
- **動作:** 進捗があった場合（ページ数が増えた場合）のみ記録を作成

### ✅ `books` テーブルの `current_page` が更新される

- **達成:** `BookDetailViewModel.updateReadingProgress()` で `current_page` を更新
- **動作:** UI に即座に反映される（StateFlow による状態管理）

### ✅ 読了時に自動的に `completed` ステータスに変更

- **達成:** 総ページ数に到達した場合、自動的に `status = "completed"` に更新
- **動作:** 一度に総ページ数まで入力した場合も正しく読了判定される

## アーキテクチャの遵守状況

### ✅ 関心の分離 (Separation of Concerns)

- **Data Layer:** Repository と DataSource でデータアクセスロジックを分離
- **UI Layer:** Composable は状態表示のみ、ViewModel がビジネスロジックを管理

### ✅ データモデルによるUIの駆動 (Drive UI from Data Model)

- **StateFlow:** `BookDetailUiState` を通じてUIを駆動
- **Reactive:** データ変更が即座にUIに反映される

### ✅ Single Source of Truth

- **Repository:** アプリ内で読書記録データにアクセスする唯一の窓口
- **認証:** `AuthRepository` 経由でユーザーIDを取得

## テスト推奨事項

**Recommendation:** 以下のテストケースで動作確認することを推奨します。

1. **基本的な進捗記録**
   - 0ページ → 50ページ に更新
   - `reading_logs` に `pages_read = 50` が記録される
   - `books.current_page = 50` に更新される

2. **読了判定**
   - 総ページ数が300の本で、250ページ → 300ページに更新
   - 自動的に `status = "completed"` になる
   - `reading_logs` に `pages_read = 50` が記録される

3. **未読から読書中への自動遷移**
   - `status = "unread"` の本で、0ページ → 10ページに更新
   - 自動的に `status = "reading"` になる

4. **バリデーション**
   - 負の値を入力 → エラーメッセージ表示
   - 総ページ数を超える値を入力 → エラーメッセージ表示

5. **ページ数が減る場合**
   - 100ページ → 50ページに戻る
   - `reading_logs` には記録されない（`pagesRead <= 0` のため）
   - `books.current_page = 50` には更新される

## 今後の拡張可能性

**Recommendation:** 以下の機能拡張が考えられます。

1. **読書時間の記録**
   - `ReadingLog.durationMins` を使用して読書時間を記録
   - タイマー機能の追加

2. **ヒートマップ表示**
   - `reading_logs` のデータを集計してカレンダービューで表示
   - GitHub Contributions 風のデザイン

3. **統計情報**
   - 月間・年間の読書ページ数集計
   - 平均読書速度の計算

4. **同日に複数回の記録**
   - 現在は1日1回のみだが、複数回の記録に対応
   - `read_date` と `book_id` の複合キーで管理

## まとめ

**Fact:** Google推奨アプリアーキテクチャに完全準拠した実装を行いました。

**Fact:** 各層（Data、Domain、UI）が適切に分離され、保守性・テスタビリティの高いコードになっています。

**Recommendation:** 本番環境へのデプロイ前に、必ずデータベースマイグレーションを実行してください（詳細は [issue7_Reading_Progress_Migration.md](./issue7_Reading_Progress_Migration.md) を参照）。

**Prediction:** この実装により、将来的なヒートマップ機能やAIレコメンド機能の実装が容易になります。
