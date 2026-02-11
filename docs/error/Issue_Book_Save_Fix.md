# 書籍保存エラー修正レポート

## 📋 問題の概要

**症状:**
- バーコードスキャン後、書籍情報は正しく表示される
- しかし「保存」ボタンをタップすると「保存に失敗しました: User not authenticated」というエラーが表示される

**発生日:** 2026年2月11日  
**重要度:** 🔴 高（アプリの基本機能が動作しない）

---

## 🔍 原因分析

### 根本原因

**ファイル名の誤り + 認証タイミングの問題**

1. **ファイル名の重複拡張子**
   - `/app/src/main/java/com/example/bookstack/data/remote/auth/SupabaseAuthDataSource.kt).kt`
   - 拡張子が`.kt).kt`と誤って二重になっていたため、正しく認識されていなかった可能性

2. **認証処理の実行タイミング**
   - `MainActivity`で`lifecycleScope.launch`を使って匿名サインインを実行していた
   - しかし、`setContent`で画面が即座に表示されるため、認証完了前に保存処理が実行されていた
   - `authRepository.getCurrentUserId()`が`null`を返していた

### 関連するログ出力

```
保存に失敗しました: User not authenticated
```

このエラーは`BookDatabaseRepository.insertBook()`の以下の処理で発生：

```kotlin
val userId = authRepository.getCurrentUserId()
    ?: return Result.failure(Exception("User not authenticated"))
```

---

## ✅ 実施した修正内容

### 1. ファイル名の修正

**Before:**
```
/app/src/main/java/com/example/bookstack/data/remote/auth/SupabaseAuthDataSource.kt).kt
```

**After:**
```
/app/src/main/java/com/example/bookstack/data/remote/auth/SupabaseAuthDataSource.kt
```

**操作:**
- 誤ったファイルを削除
- 正しいファイル名で再作成

---

### 2. MainActivityの修正

**変更前の問題点:**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // 問題: lifecycleScopeで非同期実行
    lifecycleScope.launch {
        authViewModel.signInIfNeeded()
    }

    setContent {
        BookStackTheme {
            // 認証完了を待たずに画面表示
            BookScanScreen(...)
        }
    }
}
```

**修正後:**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
        BookStackTheme {
            Surface(...) {
                // ✅ 認証状態を監視
                val sessionStatus by authViewModel.sessionStatus.collectAsState()

                // ✅ Compose内でサインインを実行
                LaunchedEffect(Unit) {
                    authViewModel.signInIfNeeded()
                }

                // ✅ 認証状態に応じて画面を切り替え
                when (sessionStatus) {
                    is SessionStatus.Authenticated -> {
                        // 認証完了後のみスキャン画面を表示
                        BookScanScreen(...)
                    }
                    else -> {
                        // 認証処理中はローディング表示
                        LoadingScreen()
                    }
                }
            }
        }
    }
}
```

**改善点:**
- ✅ `sessionStatus`をComposeで監視し、認証完了を待つ
- ✅ 認証完了前は`LoadingScreen()`を表示
- ✅ 認証完了後のみ`BookScanScreen`を表示

---

### 3. デバッグログの追加

トラブルシューティングのため、以下のファイルに詳細なログを追加：

#### `SupabaseAuthDataSource.kt`
```kotlin
override suspend fun signInAnonymously() {
    Log.d(TAG, "signInAnonymously: Starting anonymous sign-in")
    try {
        auth.signInAnonymously()
        Log.d(TAG, "signInAnonymously: Success")
    } catch (e: Exception) {
        Log.e(TAG, "signInAnonymously: Failed", e)
        throw e
    }
}

override fun getCurrentUserId(): String? {
    val userId = auth.currentUserOrNull()?.id
    Log.d(TAG, "getCurrentUserId: $userId")
    return userId
}
```

#### `BookDatabaseRepository.kt`
```kotlin
suspend fun insertBook(book: Book): Result<Book> {
    val userId = authRepository.getCurrentUserId()
    
    Log.d(TAG, "insertBook: Attempting to get user ID")
    Log.d(TAG, "insertBook: User ID = $userId")
    
    if (userId == null) {
        Log.e(TAG, "insertBook: User not authenticated")
        return Result.failure(Exception("User not authenticated"))
    }
    
    Log.d(TAG, "insertBook: Calling bookDatabaseDataSource.insertBook")
    return bookDatabaseDataSource.insertBook(userId, book)
}
```

#### `SupabaseBookDatabaseDataSource.kt`
```kotlin
override suspend fun insertBook(userId: String, book: Book): Result<Book> {
    return try {
        Log.d(TAG, "insertBook: Starting insert for userId=$userId, isbn=${book.isbn}")
        
        val bookDto = book.toBookDto(userId)
        Log.d(TAG, "insertBook: BookDto created: $bookDto")

        Log.d(TAG, "insertBook: Calling Supabase insert")
        val insertedDto = supabaseClient
            .from(TABLE_NAME)
            .insert(bookDto) { select() }
            .decodeSingle<BookDto>()

        Log.d(TAG, "insertBook: Success - inserted book with id=${insertedDto.id}")
        Result.success(insertedDto.toBook())
    } catch (e: Exception) {
        Log.e(TAG, "insertBook: Failed", e)
        e.printStackTrace()
        Result.failure(e)
    }
}
```

---

## 🧪 テスト方法

### 1. 正常系テスト

#### **手順:**
1. アプリを起動
2. ローディング画面が表示される（認証処理中）
3. 自動的にスキャン画面に遷移
4. バーコードをスキャン
5. 書籍情報が表示される
6. 「保存」ボタンをタップ

#### **期待される結果:**
- ✅ 「保存しました」または保存成功のメッセージが表示される
- ✅ Supabaseの`books`テーブルにデータが保存される

---

### 2. ログ確認方法

#### **Android StudioのLogcatで以下のタグをフィルタリング:**
```
MainActivity
SupabaseAuthDataSource
BookDatabaseRepository
SupabaseBookDatabase
```

#### **正常な場合のログの流れ:**
```
MainActivity: onCreate: Starting app
MainActivity: LaunchedEffect: Calling signInIfNeeded
SupabaseAuthDataSource: signInAnonymously: Starting anonymous sign-in
SupabaseAuthDataSource: signInAnonymously: Success
MainActivity: SessionStatus: Authenticated

// 保存ボタンタップ時
BookDatabaseRepository: insertBook: Attempting to get user ID
BookDatabaseRepository: insertBook: User ID = 12345678-abcd-...
BookDatabaseRepository: insertBook: Calling bookDatabaseDataSource.insertBook
SupabaseBookDatabase: insertBook: Starting insert for userId=12345678-abcd-..., isbn=9784...
SupabaseBookDatabase: insertBook: BookDto created: BookDto(...)
SupabaseBookDatabase: insertBook: Calling Supabase insert
SupabaseBookDatabase: insertBook: Success - inserted book with id=abcd1234-...
```

---

### 3. Supabaseダッシュボードでの確認

#### **手順:**
1. [Supabaseダッシュボード](https://app.supabase.com/)にアクセス
2. プロジェクトを選択
3. 左メニューから「Table Editor」を選択
4. `books`テーブルを開く

#### **確認項目:**
- ✅ 新しい行が追加されている
- ✅ `user_id`が匿名ユーザーのUUIDになっている
- ✅ `isbn`, `title`, `authors`などが正しく保存されている
- ✅ `added_at`タイムスタンプが記録されている

---

## 🔧 修正ファイル一覧

| ファイルパス | 変更内容 |
|------------|---------|
| `/app/src/main/java/com/example/bookstack/data/remote/auth/SupabaseAuthDataSource.kt` | ファイル名修正 + ログ追加 |
| `/app/src/main/java/com/example/bookstack/MainActivity.kt` | 認証状態の監視 + LoadingScreen追加 |
| `/app/src/main/java/com/example/bookstack/data/repository/BookDatabaseRepository.kt` | デバッグログ追加 |
| `/app/src/main/java/com/example/bookstack/data/remote/database/SupabaseBookDatabaseDataSource.kt` | デバッグログ追加 |

---

## 📊 アーキテクチャの改善点

### Before: 認証タイミングの問題

```
MainActivity.onCreate()
  ├── lifecycleScope.launch { signInIfNeeded() } ← 非同期実行（完了を待たない）
  └── setContent { BookScanScreen(...) }         ← すぐに表示
```

**問題:**
- 認証処理が完了する前に画面が表示される
- `getCurrentUserId()`が`null`を返す可能性がある

---

### After: 認証完了を待つ設計

```
MainActivity.onCreate()
  └── setContent {
        LaunchedEffect { signInIfNeeded() }       ← Compose内で実行
        when (sessionStatus) {                     ← 認証状態を監視
          Authenticated → BookScanScreen(...)      ← 認証後のみ表示
          NotAuthenticated → LoadingScreen()       ← 認証中はローディング
        }
      }
```

**改善点:**
- ✅ 認証状態を`StateFlow`で監視
- ✅ 認証完了まで機能画面を表示しない
- ✅ ユーザーにローディング状態を通知

---

## 🚨 追加で確認すべき事項

### 1. Supabase RLSポリシーの確認

`books`テーブルのRLSポリシーが正しく設定されているか確認してください。

#### **必要なポリシー:**

```sql
-- INSERT権限（新規登録）
create policy "Users can insert own books." 
  on books for insert 
  with check (auth.uid() = user_id);

-- SELECT権限（取得）
create policy "Users can see own books." 
  on books for select 
  using (auth.uid() = user_id);

-- UPDATE権限（更新）
create policy "Users can update own books." 
  on books for update 
  using (auth.uid() = user_id);

-- DELETE権限（削除）
create policy "Users can delete own books." 
  on books for delete 
  using (auth.uid() = user_id);
```

#### **確認方法:**
1. Supabaseダッシュボード → Table Editor → `books`
2. 「Policies」タブを開く
3. 上記4つのポリシーが有効になっていることを確認

---

### 2. 匿名ユーザー認証の有効化

Supabase側で匿名認証が有効になっているか確認してください。

#### **確認方法:**
1. Supabaseダッシュボード → Authentication → Settings
2. 「Auth Providers」セクション
3. 「Enable Anonymous sign-ins」がONになっていることを確認

---

### 3. ネットワーク接続のテスト

初回起動時にSupabaseへの接続がタイムアウトしていないか確認してください。

#### **確認ポイント:**
- ✅ エミュレータ/実機でネットワーク接続が有効
- ✅ `BuildConfig.SUPABASE_URL`と`SUPABASE_KEY`が正しい
- ✅ Firewall/VPNでSupabaseがブロックされていない

---

## 📝 今後の改善提案

### 1. エラーメッセージの日本語化

現在のエラーメッセージ:
```
保存に失敗しました: User not authenticated
```

改善案:
```kotlin
sealed class SaveBookError(message: String) : Exception(message) {
    object NotAuthenticated : SaveBookError("ログインが必要です。アプリを再起動してください。")
    object NetworkError : SaveBookError("ネットワーク接続に失敗しました。")
    object SupabaseError : SaveBookError("データベースへの保存に失敗しました。")
}
```

---

### 2. リトライ機能の実装

認証失敗時やネットワークエラー時に自動リトライする仕組みを追加。

```kotlin
suspend fun insertBookWithRetry(book: Book, maxRetries: Int = 3): Result<Book> {
    repeat(maxRetries) { attempt ->
        val result = insertBook(book)
        if (result.isSuccess) return result
        
        Log.w(TAG, "Retry $attempt/$maxRetries")
        delay(1000 * (attempt + 1)) // 指数バックオフ
    }
    return Result.failure(Exception("保存に失敗しました（最大試行回数超過）"))
}
```

---

### 3. オフライン対応

Room Databaseをローカルキャッシュとして使用し、オンライン時にSupabaseと同期。

---

## ✅ 完了条件の達成状況

| 条件 | 状態 | 備考 |
|------|------|------|
| ファイル名の修正 | ✅ 完了 | `SupabaseAuthDataSource.kt`を正しく再作成 |
| 認証タイミングの修正 | ✅ 完了 | `MainActivity`で認証完了を待つように変更 |
| デバッグログの追加 | ✅ 完了 | 主要なファイルにログを追加 |
| ビルド成功 | ✅ 完了 | `./gradlew assembleDebug` 成功 |
| 実機テスト | ⚠️ 未実施 | ユーザーによる動作確認が必要 |

---

## 🎯 ユーザーへの依頼事項

### 1. アプリの実行とテスト

以下の手順でアプリをテストしてください：

1. **アプリを起動**
   - Android Studioの「Run」ボタンをクリック
   - またはコマンド: `./gradlew installDebug`

2. **動作確認**
   - ローディング画面が表示されることを確認
   - 自動的にスキャン画面に遷移することを確認
   - バーコードをスキャンして書籍情報を取得
   - 「保存」ボタンをタップ

3. **結果の報告**
   - ✅ 保存が成功した → 問題解決！
   - ❌ まだエラーが出る → Logcatのログを共有してください

---

### 2. Logcatの確認

エラーが発生した場合、以下のログを共有してください：

```
Android Studio → Logcat → フィルタに以下を入力:
tag:MainActivity|SupabaseAuthDataSource|BookDatabaseRepository|SupabaseBookDatabase
```

---

### 3. Supabaseダッシュボードの確認

保存成功後、Supabaseの`books`テーブルにデータが保存されているか確認してください。

---

## 📚 参考資料

- [Supabase Auth - Anonymous Sign In](https://supabase.com/docs/guides/auth/auth-anonymous)
- [Jetpack Compose - Side Effects](https://developer.android.com/jetpack/compose/side-effects)
- [Kotlin Flow - StateFlow](https://kotlinlang.org/docs/flow.html#stateflow-and-sharedflow)

---

**修正日:** 2026年2月11日  
**修正者:** GitHub Copilot (AI Assistant)
