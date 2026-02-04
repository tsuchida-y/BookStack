# 🔧 アプリ起動エラーのトラブルシューティング

## ❌ 発生しているエラー

### **エラー1: インストール失敗**
```
Installation failed due to: 'Error code: 'UNKNOWN', message='Unknown failure: 
'Exception occurred while executing 'install-create''
Requested internal only, but not enough space
```

### **エラー2: Supabase認証タイムアウト**
```
HttpRequestTimeoutException: Request timeout has expired 
[url=https://zwekxiuxohwglnmkdpno.supabase.co/auth/v1/signup, request_timeout=10000 ms]
```

---

## 🎯 原因

### **原因1: エミュレーター/デバイスのストレージ不足**
- APKのインストールに必要な内部ストレージが不足しています
- CameraX、ML Kitなどのネイティブライブラリが含まれるため、APKサイズが大きくなっています

### **原因2: ネットワーク接続の問題**
- エミュレーターからSupabaseへの接続がタイムアウトしています
- 初回起動時に匿名ログインを試行するため、ネットワークが必要です

---

## ✅ 解決策

### **解決策1: エミュレーターのストレージをクリア（最優先）**

#### **方法A: データを消去（Wipe Data）**
1. Android Studioで **Tools** → **Device Manager** を開く
2. 使用中のエミュレーターの **⋮（三点リーダー）** をクリック
3. **Wipe Data** を選択
4. 確認ダイアログで **OK** をクリック
5. エミュレーターを再起動

#### **方法B: 新しいエミュレーターを作成**
1. Android Studioで **Tools** → **Device Manager** を開く
2. **Create Device** をクリック
3. デバイスを選択（推奨: Pixel 5 以上）
4. システムイメージを選択（推奨: API 34、x86_64）
5. **Advanced Settings** → **Internal Storage** を **2048 MB 以上**に設定
6. **Finish** をクリック

---

### **解決策2: APKサイズの削減（既に実施済み）**

`app/build.gradle.kts` を修正し、エミュレーター用のABIのみを含めるように設定しました：

```kotlin
buildTypes {
    debug {
        ndk {
            // エミュレーター用にx86_64とarm64-v8aのみ含める
            abiFilters += listOf("x86_64", "arm64-v8a")
        }
    }
}
```

**効果:**
- 不要なABI（armeabi-v7a、x86など）を除外し、APKサイズを削減

---

### **解決策3: Gradleキャッシュのクリーンアップ**

ターミナルで以下のコマンドを実行してください：

```bash
cd /Users/tsuchida/Documents/Programming/BookStack
./gradlew clean
./gradlew assembleDebug
```

---

### **解決策4: ネットワーク接続の確認**

#### **エミュレーターのネットワーク設定を確認:**

1. エミュレーターを起動
2. **Settings** → **Network & Internet** → **Internet** を開く
3. Wi-Fiが接続されていることを確認

#### **Supabaseへの接続テスト:**

ターミナルで以下のコマンドを実行し、Supabaseに接続できるか確認：

```bash
curl -I https://zwekxiuxohwglnmkdpno.supabase.co/auth/v1/health
```

**正常な応答例:**
```
HTTP/2 200
```

**接続できない場合:**
- VPN接続を確認
- ファイアウォール設定を確認
- `local.properties` の `SUPABASE_URL` が正しいか確認

---

### **解決策5: タイムアウト時間の延長（一時的な対処）**

`AppModule.kt` で Supabase ClientのHTTPタイムアウトを延長：

```kotlin
// di/AppModule.kt
single<SupabaseClient> {
    createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        // HTTPタイムアウトを延長
        httpEngine {
            requestTimeout = 30000 // 30秒
        }
        
        install(Postgrest) {
            serializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
            })
        }
        install(Auth) {
            sessionManager = SettingsSessionManager()
            alwaysAutoRefresh = true
        }
    }
}
```

---

### **解決策6: オフラインモードで起動（開発用）**

一時的に認証をスキップしてアプリを起動する場合、`MainActivity.kt` を修正：

```kotlin
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookStackTheme {
                // 一時的に認証をスキップ
                // AuthScreen(authViewModel)
                
                // 直接ホーム画面を表示（開発用）
                HomeScreen(userId = "test-user")
            }
        }
    }
}
```

**⚠️ 注意:** これは開発用の一時的な対処です。本番環境では認証を有効にしてください。

---

## 🧪 動作確認手順

### **1. ビルド＆インストール**

```bash
# クリーンビルド
./gradlew clean
./gradlew assembleDebug

# APKサイズの確認
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

**期待される結果:**
- APKサイズ: 約20-40MB（以前より小さくなっているはず）

### **2. エミュレーターで実行**

Android Studioで **Run** → **Run 'app'** を実行

**期待される動作:**
1. アプリが起動する
2. 「ログイン中...」と表示される
3. 数秒後にホーム画面が表示される

### **3. エラーが発生した場合**

Logcatで以下のフィルターを設定し、エラーログを確認：

```
package:com.example.bookstack
```

---

## 📊 エラー別対処法まとめ

| エラーメッセージ | 原因 | 解決策 |
|----------------|------|--------|
| `not enough space` | ストレージ不足 | エミュレーターのWipe Data |
| `Request timeout` | ネットワーク接続 | ネットワーク設定確認、タイムアウト延長 |
| `Installation failed` | APKサイズが大きい | ABI Filtersの設定、クリーンビルド |
| `Unknown failure` | キャッシュ破損 | `./gradlew clean` |

---

## 🔍 デバッグコマンド

### **接続されているデバイスを確認:**
```bash
adb devices
```

### **エミュレーターのストレージ状況を確認:**
```bash
adb shell df -h
```

### **インストール済みアプリを削除:**
```bash
adb uninstall com.example.bookstack
```

### **APKを手動でインストール:**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 📞 サポート情報

### **それでも解決しない場合:**

1. **Android Studioのキャッシュをクリア:**
   - **File** → **Invalidate Caches / Restart** → **Invalidate and Restart**

2. **Gradleのキャッシュをクリア:**
   ```bash
   ./gradlew clean
   rm -rf ~/.gradle/caches
   ./gradlew build
   ```

3. **エミュレーターを完全に削除して再作成:**
   - Device Managerで削除
   - 新規作成時にInternal Storageを3GB以上に設定

---

## ✅ チェックリスト

起動前に以下を確認してください：

- [ ] エミュレーターのストレージを確認（Wipe Data済み）
- [ ] `./gradlew clean` を実行済み
- [ ] `local.properties` に `SUPABASE_URL` と `SUPABASE_KEY` が設定されている
- [ ] エミュレーターがインターネットに接続されている
- [ ] Android Studioのキャッシュをクリア済み（必要に応じて）

---

**最終更新日:** 2026年2月3日
