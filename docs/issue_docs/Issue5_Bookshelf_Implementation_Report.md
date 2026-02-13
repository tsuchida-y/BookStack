# Issue #5: 本棚画面（背表紙一覧表示）実装完了レポート

## 📋 実装概要

本レポートは、Issue #5「本棚画面（背表紙一覧表示）の実装」の完了内容をまとめたものです。
BookStackアプリのコア機能である「リアルな背表紙風の本棚表示」を実装しました。

**実装日:** 2026年2月11日  
**担当:** AI Assistant  
**所要時間:** 約1時間

---

## ✅ 実装完了した作業

### 1. Palette APIの依存関係追加

**ファイル:**
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`

**実装内容:**
書影画像からドミナントカラーを抽出するために、Android Palette APIを追加。

```toml
# gradle/libs.versions.toml
palette = "1.0.0"

androidx-palette = { group = "androidx.palette", name = "palette-ktx", version.ref = "palette" }
```

```kotlin
// app/build.gradle.kts
implementation(libs.androidx.palette)
```

---

### 2. BookSpineCard コンポーネントの実装

**ファイル:** `app/src/main/java/com/example/bookstack/ui/components/BookSpineCard.kt`

**実装内容:**
本の背表紙を模したカードコンポーネント。本アプリの最も特徴的なUI要素。

#### 主要機能:

##### a) ドミナントカラー抽出
```kotlin
private suspend fun extractDominantColor(
    context: android.content.Context,
    imageUrl: String
): Color?
```

- Coilで書影画像を読み込み
- Palette APIで主要色（Vibrant → Muted → Dominant の優先順位）を抽出
- 背景色として使用
- 失敗時はデフォルトカラー（本っぽい茶色: `#8B7355`）を使用

##### b) 判型に応じた高さの可変化
```kotlin
private fun getHeightForBookSize(bookSize: BookSize?): Dp {
    return when (bookSize) {
        BookSize.S -> 150.dp      // 文庫、新書
        BookSize.M -> 180.dp      // 四六判、B6判
        BookSize.L -> 210.dp      // A5判、B5判
        BookSize.XL -> 240.dp     // A4判以上
        BookSize.UNKNOWN, null -> 180.dp // デフォルト
    }
}
```

##### c) ページ数に応じた幅（厚み）の可変化
```kotlin
private fun getWidthForPageCount(pageCount: Int?): Dp {
    return when {
        pageCount == null -> 32.dp           // 不明
        pageCount <= 100 -> 24.dp            // 薄い本
        pageCount <= 200 -> 32.dp            // 標準
        pageCount <= 300 -> 40.dp            // やや厚い
        pageCount <= 500 -> 48.dp            // 厚い
        else -> 56.dp                        // 非常に厚い
    }
}
```

##### d) ゆらぎ処理
```kotlin
private fun getHeightVariation(isbn: String): Dp {
    val seed = isbn.hashCode()
    val variation = (seed % 11) - 5 // -5 ~ +5の範囲
    return variation.dp
}
```

- ISBNをシード値として使用
- 各書籍に±5dpのランダムな高さのゆらぎを追加
- 同じ書籍は常に同じゆらぎ値を持つ（再現性あり）

##### e) タイトルの縦書き表示
```kotlin
Text(
    text = book.title,
    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
    color = getContrastingTextColor(dominantColor),
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = Modifier.rotate(-90f) // 90度回転
)
```

##### f) コントラストを考慮したテキスト色
```kotlin
private fun getContrastingTextColor(backgroundColor: Color): Color {
    val luminance = (0.299 * backgroundColor.red +
            0.587 * backgroundColor.green +
            0.114 * backgroundColor.blue)
    
    return if (luminance > 0.5f) Color.Black else Color.White
}
```

---

### 3. BookListViewModel の実装

**ファイル:** `app/src/main/java/com/example/bookstack/ui/booklist/BookListViewModel.kt`

**実装内容:**
本棚画面のUI状態を管理するViewModel。

#### UI状態の定義
```kotlin
sealed interface BookListUiState {
    data object Initial : BookListUiState
    data object Loading : BookListUiState
    data class Success(val books: List<Book>) : BookListUiState
    data object Empty : BookListUiState
    data class Error(val message: String) : BookListUiState
}
```

#### 主要メソッド

##### loadBooks()
```kotlin
fun loadBooks() {
    viewModelScope.launch {
        _uiState.value = BookListUiState.Loading

        bookDatabaseRepository.getAllBooks()
            .onSuccess { books ->
                if (books.isEmpty()) {
                    _uiState.value = BookListUiState.Empty
                } else {
                    _uiState.value = BookListUiState.Success(books)
                }
            }
            .onFailure { exception ->
                _uiState.value = BookListUiState.Error(
                    exception.message ?: "書籍の読み込みに失敗しました"
                )
            }
    }
}
```

- ViewModelの初期化時に自動実行
- `BookDatabaseRepository`から全書籍を取得
- 空の場合は`Empty`状態に遷移
- エラー時は適切なエラーメッセージを設定

##### retry()
```kotlin
fun retry() {
    loadBooks()
}
```

- エラー時のリトライ処理
- 単純に`loadBooks()`を再実行

---

### 4. BookshelfScreen の実装

**ファイル:** `app/src/main/java/com/example/bookstack/ui/booklist/BookshelfScreen.kt`

**実装内容:**
本棚画面のメインComposable。

#### 画面構成

```
┌─────────────────────────────────┐
│  [本棚]                  [+]    │ ← TopAppBar
├─────────────────────────────────┤
│ ┌──┐ ┌───┐ ┌──┐ ┌────┐ ┌──┐   │
│ │本│ │本棚│ │本│ │大きい│ │本│   │ ← LazyVerticalGrid
│ │  │ │    │ │  │ │本    │ │  │   │   (背表紙カード)
│ │  │ │    │ │  │ │      │ │  │   │
│ └──┘ └───┘ └──┘ └────┘ └──┘   │
└─────────────────────────────────┘
```

#### 主要Composable関数

##### BookshelfScreen
- メイン画面
- `Scaffold`でTopAppBarと本体を構成
- UI状態に応じて表示を切り替え

##### LoadingContent
- ローディング中の表示
- `CircularProgressIndicator`と「本棚を読み込んでいます...」メッセージ

##### BookshelfContent
- 書籍一覧のグリッド表示
- `LazyVerticalGrid`を使用
- `GridCells.Adaptive(minSize = 40.dp)`で自動的に列数を調整
- 各書籍は`BookSpineCard`で表示

```kotlin
LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 40.dp),
    contentPadding = PaddingValues(16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxSize()
) {
    items(
        items = books,
        key = { book -> book.id ?: book.isbn }
    ) { book ->
        BookSpineCard(book = book)
    }
}
```

##### EmptyContent
- 書籍が1冊もない場合の表示
- 絵文字と案内メッセージ
- 「本を追加する」ボタン

##### ErrorContent
- エラー発生時の表示
- エラーメッセージ
- 「再試行」ボタン

---

### 5. DIモジュールの更新

**ファイル:** `app/src/main/java/com/example/bookstack/di/AppModule.kt`

**実装内容:**
`BookListViewModel`をKoinのDIコンテナに登録。

```kotlin
// BookList ViewModel (本棚画面用)
viewModel<BookListViewModel> {
    BookListViewModel(
        bookDatabaseRepository = get()
    )
}
```

---

### 6. MainActivityの更新

**ファイル:** `app/src/main/java/com/example/bookstack/MainActivity.kt`

**実装内容:**
認証後に本棚画面を表示し、スキャン画面への遷移を実装。

#### 変更点

##### ViewModelの追加
```kotlin
private val bookListViewModel: BookListViewModel by viewModel()
```

##### 画面遷移の状態管理
```kotlin
var showScanScreen by remember { mutableStateOf(false) }
```

##### 条件分岐による画面表示
```kotlin
when (sessionStatus) {
    is SessionStatus.Authenticated -> {
        if (showScanScreen) {
            BookScanScreen(
                viewModel = bookScanViewModel,
                onNavigateBack = {
                    showScanScreen = false
                    bookListViewModel.loadBooks() // 本棚をリロード
                }
            )
        } else {
            BookshelfScreen(
                viewModel = bookListViewModel,
                onAddBookClick = {
                    showScanScreen = true
                }
            )
        }
    }
    // ...
}
```

**動作フロー:**
1. アプリ起動 → 匿名認証
2. 認証成功 → 本棚画面表示
3. 「+」ボタンタップ → スキャン画面へ遷移
4. スキャン画面で本を追加
5. 戻るボタン → 本棚画面に戻り、自動リロード

---

### 7. テストの実装

**ファイル:** `app/src/test/java/com/example/bookstack/ui/booklist/BookListViewModelTest.kt`

**実装内容:**
`BookListViewModel`の単体テスト。

#### テストケース

##### 1. 書籍データ取得成功時
```kotlin
@Test
fun `初期化時にLoadingとなり、書籍データ取得成功時にSuccessとなる`()
```

- 2冊の本を設定
- ViewModelを初期化
- UI状態が`Success`になり、書籍数が2であることを確認

##### 2. 書籍が0冊の場合
```kotlin
@Test
fun `書籍が0冊の場合、Emptyとなる`()
```

- 空のリストを設定
- UI状態が`Empty`になることを確認

##### 3. 書籍取得失敗時
```kotlin
@Test
fun `書籍取得に失敗した場合、Errorとなる`()
```

- エラーを返すように設定
- UI状態が`Error`になり、エラーメッセージが設定されることを確認

##### 4. リトライ処理
```kotlin
@Test
fun `retry()を呼ぶと再度loadBooks()が実行される`()
```

- 最初はエラー状態
- `retry()`を呼び出し
- UI状態が`Success`に変わることを確認

#### テスト用モック

##### TestBookDatabaseDataSource
- `BookDatabaseDataSource`の実装
- Supabase接続なしで、メモリ上でデータを管理
- テスト用にエラー状態を切り替え可能

##### TestAuthDataSource
- `AuthDataSource`の実装
- 常に認証済み状態を返す
- テスト用の固定ユーザーID（`test-user-id`）を提供

**テスト結果:** ✅ 全テスト合格

---

## 🏗️ アーキテクチャ

### データフロー

```
┌─────────────────────────────────────────┐
│           UI Layer                      │
│  ┌───────────────────────────────────┐  │
│  │ MainActivity                      │  │
│  │  └─ BookshelfScreen               │  │
│  │       └─ BookSpineCard            │  │
│  └───────────────────────────────────┘  │
│  ┌───────────────────────────────────┐  │
│  │ BookListViewModel                 │  │
│  │  ├── uiState: StateFlow           │  │
│  │  ├── loadBooks()                  │  │
│  │  └── retry()                      │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│       Repository Layer                  │
│  ┌───────────────────────────────────┐  │
│  │ BookDatabaseRepository            │  │
│  │  └── getAllBooks()                │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│       DataSource Layer                  │
│  ┌───────────────────────────────────┐  │
│  │ SupabaseBookDatabaseDataSource    │  │
│  │  └── getAllBooks(userId)          │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│        Supabase Backend                 │
│  ┌───────────────────────────────────┐  │
│  │ books テーブル                     │  │
│  │ (RLS有効、user_idでフィルタ)       │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

---

## 📊 技術的特徴

### 1. パフォーマンス最適化

- **LazyVerticalGrid**: 画面に表示される書籍のみをレンダリング
- **key指定**: 書籍のID/ISBNをkeyに指定し、効率的な再描画
- **非同期画像読み込み**: Coilによる効率的な画像キャッシュ

### 2. リアリティの追加

- **ドミナントカラー**: 各書籍固有の色味を反映
- **可変サイズ**: 判型とページ数に応じた高さと幅
- **ゆらぎ**: ±5dpのランダムな高さの差
- **コントラスト調整**: 背景色に応じて白/黒の文字色を自動選択

### 3. Google推奨アーキテクチャの遵守

- **関心の分離**: UI / ViewModel / Repository / DataSource
- **単一責任の原則**: 各レイヤーが明確な責務を持つ
- **データモデル駆動**: StateFlowによる一方向のデータフロー

---

## 📂 実装ファイル一覧

### 新規作成
```
app/src/main/java/com/example/bookstack/
├── ui/
│   ├── booklist/
│   │   ├── BookshelfScreen.kt           ← NEW (214行)
│   │   └── BookListViewModel.kt         ← NEW (95行)
│   └── components/
│       └── BookSpineCard.kt             ← NEW (236行)
└── test/java/com/example/bookstack/
    └── ui/booklist/
        └── BookListViewModelTest.kt     ← NEW (229行)
```

### 修正
```
gradle/libs.versions.toml                ← UPDATE (Palette API追加)
app/build.gradle.kts                     ← UPDATE (Palette API追加)
app/src/main/java/com/example/bookstack/
├── MainActivity.kt                      ← UPDATE (画面遷移実装)
└── di/AppModule.kt                      ← UPDATE (ViewModel登録)
```

**合計:**
- 新規ファイル: 4ファイル、774行
- 修正ファイル: 4ファイル

---

## ✅ 完了条件の確認

| 完了条件 | 状態 | 備考 |
|---------|------|------|
| アプリ起動後、認証完了すると本棚画面が表示される | ✅ | MainActivity.ktで実装 |
| Supabaseに登録された書籍がグリッド状に並ぶ | ✅ | LazyVerticalGridで実装 |
| 背表紙のような見た目（縦書きタイトル + 色付き背景） | ✅ | BookSpineCardで実装 |
| 判型（size_type）に応じて高さが異なる | ✅ | S/M/L/XLで150~240dp |
| ページ数に応じて幅（厚み）が異なる | ✅ | 24~56dpで可変 |
| 微妙な高さのゆらぎがある | ✅ | ±5dpのランダムな差 |
| 書籍がない場合は案内メッセージを表示 | ✅ | EmptyContentで実装 |
| ローディング中はプログレスインジケーター表示 | ✅ | LoadingContentで実装 |
| エラー時は適切なメッセージとリトライボタン表示 | ✅ | ErrorContentで実装 |
| 単体テストが作成され、すべて合格 | ✅ | 4テストケース、全合格 |

---

## 🎨 UI設計の工夫

### 背表紙の見た目

実際の本棚に近い見た目を実現するために、以下の工夫を行いました：

1. **色の多様性**: Palette APIで書影から主要色を抽出
2. **サイズのバリエーション**: 判型とページ数で高さと幅を可変
3. **自然なゆらぎ**: 完全に揃わないことでリアリティを追加
4. **視認性の確保**: 背景色に応じてテキスト色を自動調整

### ユーザー体験

- **直感的な操作**: 右上の「+」ボタンで本を追加
- **わかりやすいフィードバック**: 空状態、ローディング、エラーの適切な表示
- **スムーズな遷移**: スキャン画面から戻ると自動でリロード

---

## 🐛 既知の制限事項

1. **Palette API処理**: 画像読み込みと色抽出に時間がかかる場合がある
   - 将来的にはDBに色情報を保存してキャッシュすることを推奨

2. **縦書き表示**: Jetpack Composeは標準で縦書きをサポートしていない
   - 現在はrotation(-90f)で90度回転して対応
   - 長いタイトルは省略される（`maxLines = 1`）

3. **グリッド列数**: デバイスの幅に応じて自動調整
   - 小さい画面では列数が少なくなる可能性がある

---

## 🔄 今後の改善案

### 短期的な改善

1. **色情報のキャッシュ**
   - 一度抽出した色を`books`テーブルの`spine_color`カラムに保存
   - 次回以降はDBから読み込み、Palette API処理を省略

2. **背表紙のタップ処理**
   - 背表紙をタップすると書籍詳細画面に遷移
   - 長押しで選択モード（削除・編集）

3. **ソート・フィルタリング**
   - 著者別、ジャンル別、読書ステータス別の表示
   - 並べ替え（タイトル順、追加日順、読了日順）

### 長期的な改善

1. **アニメーション**
   - 書籍追加時のアニメーション
   - グリッドの並び替えアニメーション

2. **カスタマイズ機能**
   - 背表紙のスタイル変更（シンプル/リッチ）
   - グリッドの列数を手動調整

3. **パフォーマンス最適化**
   - 画像のプリロード
   - より効率的なグリッドレンダリング

---

## 📝 開発者向けメモ

### Palette APIの使用

```kotlin
// 画像読み込み（allowHardware = false が重要）
val request = ImageRequest.Builder(context)
    .data(imageUrl)
    .allowHardware(false) // Palette APIはソフトウェアBitmapが必要
    .build()

// Palette生成
val palette = Palette.from(bitmap).generate()
val swatch = palette.vibrantSwatch ?: palette.mutedSwatch
```

### ゆらぎの再現性

```kotlin
// ISBNのハッシュコードをシード値として使用
val seed = isbn.hashCode()
val variation = (seed % 11) - 5 // -5 ~ +5
```

同じISBNは常に同じハッシュコードを返すため、ゆらぎも常に一定になります。

### テストのポイント

- `StandardTestDispatcher()`を使用してコルーチンの実行を制御
- `advanceUntilIdle()`で非同期処理の完了を待機
- モックは継承ではなく、インターフェース実装で作成

---

## 🎉 まとめ

Issue #5「本棚画面（背表紙一覧表示）の実装」が完了しました。

**実装内容:**
- ✅ Palette APIによるドミナントカラー抽出
- ✅ 判型とページ数に応じた可変サイズ
- ✅ 自然なゆらぎ処理
- ✅ タイトルの縦書き表示
- ✅ LazyVerticalGridによるグリッドレイアウト
- ✅ ローディング、エラー、空状態の適切なハンドリング
- ✅ スキャン画面との画面遷移
- ✅ 単体テストの作成（全テスト合格）

**成果物:**
- 新規ファイル: 4ファイル、774行
- 修正ファイル: 4ファイル
- テストコード: 229行、4テストケース

**ビルド結果:** ✅ BUILD SUCCESSFUL  
**テスト結果:** ✅ 全テスト合格

これにより、BookStackアプリのコア機能である「リアルな背表紙風の本棚表示」が完成しました。
ユーザーは登録した書籍を視覚的に魅力的な形で一覧でき、所有感と達成感を得ることができます。

---

**次のステップ:**
- Issue #6: 書籍詳細画面の実装
- Issue #7: 読書ステータス管理機能
- Issue #8: 本棚のソート・フィルタリング機能
