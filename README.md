# BookStack

「あなたの読書生活を、美しく可視化する」蔵書管理・積読消化アプリ。
物理的な本棚のようなビジュアルと、読書習慣の可視化してモチベーションを維持します。

## 📚 ドキュメント

- [設計書 (DOCUMENT.md)](docs/DOCUMENT.md)
- [ER図 (Entity Relationship Diagram)](docs/diagrams/erd.md)

### 実装完了レポート
- [Issue #1 & #2: プロジェクト初期設定とSupabase接続](docs/issue_docs/Issue1_2_Implementation_Report.md)
- [Issue #3: 書籍API取得実装](docs/issue_docs/Issue3_Implementation_Report.md)
- [Issue #4: バーコードスキャン実装](docs/issue_docs/issue4_Barcode_Scan_Implementation_Report.md)
- [DI リファクタリング完了報告](docs/issue_docs/DI_Refactoring_Summary.md)

## 🛠 技術スタック

* **言語:** Kotlin
* **UI:** Jetpack Compose (Material3)
* **Backend:** Supabase
    * Auth (認証)
    * PostgreSQL (データベース)
    * pgvector (AIベクトル検索)
* **API通信:** Ktor Client
* **画像読み込み:** Coil

## 🚀 セットアップ手順

このプロジェクトを手元で動かすための手順です。

### 1. リポジトリのクローン
```
git clone https://github.com/tsuchida-y/bookstack.git
```

### 2. 環境変数の設定
このアプリはSupabaseに接続するためにAPIキーが必要です。
プロジェクトのルートディレクトリに `local.properties` ファイルを作成（既にある場合は開く）し、以下の情報を追記してください。

**local.properties**
```properties
# Supabaseの Project Settings > API から取得
SUPABASE_URL="https://your-project-id.supabase.co
SUPABASE_KEY="your-anon-public-key"
```
※ `SUPABASE_KEY` には `anon` (public) キーを使用してください。`service_role` キーは絶対に入れないでください。

### 3. Supabaseバックエンドの構築
Supabaseのダッシュボードでプロジェクトを作成し、**SQL Editor** で以下のクエリを実行してテーブルを作成してください。

<details>
<summary><strong>クリックしてSQLを展開</strong></summary>

```sql
-- 1. ベクトル検索拡張機能
create extension if not exists vector;

-- 2. ユーザープロフィール
create table public.profiles (
  id uuid references auth.users not null primary key,
  display_name text,
  avatar_url text,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 3. 書籍テーブル
create table public.books (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references public.profiles(id) not null,
  isbn text not null,
  title text not null,
  authors jsonb, 
  cover_url text,
  spine_color text,
  size_type text, 
  page_count integer,
  status text check (status in ('unread', 'reading', 'completed')),
  current_page integer default 0,
  embedding vector(1536),
  added_at timestamp with time zone default timezone('utc'::text, now()) not null,
  completed_at timestamp with time zone
);

-- 4. 読書ログ
create table public.reading_logs (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references public.profiles(id) not null,
  book_id uuid references public.books(id) on delete cascade not null,
  read_date date not null default current_date,
  pages_read integer not null,
  duration_mins integer
);

-- 5. セキュリティ設定 (RLS)
alter table profiles enable row level security;
alter table books enable row level security;
alter table reading_logs enable row level security;

-- 6. アクセスポリシー
create policy "Public profiles are viewable by everyone." on profiles for select using (true);
create policy "Users can insert their own profile." on profiles for insert with check (auth.uid() = id);
create policy "Users can update own profile." on profiles for update using (auth.uid() = id);

create policy "Users can see own books." on books for select using (auth.uid() = user_id);
create policy "Users can insert own books." on books for insert with check (auth.uid() = user_id);
create policy "Users can update own books." on books for update using (auth.uid() = user_id);
create policy "Users can delete own books." on books for delete using (auth.uid() = user_id);

create policy "Users can manage own logs." on reading_logs for all using (auth.uid() = user_id);
```
</details>

### 4. Storageの設定
Supabaseの **Storage** メニューで `covers` という名前の新しいバケットを作成し、**Public Bucket** をONに設定してください。

### 5. アプリのビルドと実行
Android Studioでプロジェクトを開き、Build > Rebuild Project を実行してからアプリを起動してください。