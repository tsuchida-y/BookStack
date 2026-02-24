# 読書進捗記録機能 - データベースマイグレーション

このドキュメントは、読書進捗記録機能の実装に必要なSupabaseデータベースのマイグレーション手順を説明します。

## 必要なマイグレーション

### 1. `books`テーブルに`current_page`カラムを追加

既存の`books`テーブルに現在読んでいるページ数を記録する`current_page`カラムを追加します。

```sql
-- books テーブルに current_page カラムを追加
ALTER TABLE books
ADD COLUMN IF NOT EXISTS current_page INTEGER DEFAULT 0;

-- current_page のコメントを追加
COMMENT ON COLUMN books.current_page IS '現在読んでいるページ数';
```

### 2. `reading_logs`テーブルの作成

ヒートマップ表示のために、日々の読書進捗を記録する`reading_logs`テーブルを作成します。

```sql
-- reading_logs テーブルの作成
CREATE TABLE IF NOT EXISTS reading_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    book_id UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    read_date DATE NOT NULL,
    pages_read INTEGER NOT NULL,
    duration_mins INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- インデックスの作成
CREATE INDEX IF NOT EXISTS idx_reading_logs_user_id ON reading_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_reading_logs_book_id ON reading_logs(book_id);
CREATE INDEX IF NOT EXISTS idx_reading_logs_read_date ON reading_logs(read_date);

-- カラムにコメントを追加
COMMENT ON TABLE reading_logs IS '読書記録テーブル（ヒートマップ用）';
COMMENT ON COLUMN reading_logs.id IS 'プライマリキー';
COMMENT ON COLUMN reading_logs.user_id IS 'ユーザーID';
COMMENT ON COLUMN reading_logs.book_id IS '書籍ID';
COMMENT ON COLUMN reading_logs.read_date IS '読んだ日付';
COMMENT ON COLUMN reading_logs.pages_read IS 'その日に読んだページ数';
COMMENT ON COLUMN reading_logs.duration_mins IS '読書時間（分）';
COMMENT ON COLUMN reading_logs.created_at IS '作成日時';
```

### 3. RLS（Row Level Security）ポリシーの設定

`reading_logs`テーブルに対して、ユーザーが自分のデータのみアクセスできるようにRLSポリシーを設定します。

```sql
-- RLS を有効化
ALTER TABLE reading_logs ENABLE ROW LEVEL SECURITY;

-- SELECT ポリシー: 自分のデータのみ閲覧可能
CREATE POLICY "Users can view their own reading logs"
ON reading_logs
FOR SELECT
USING (auth.uid() = user_id);

-- INSERT ポリシー: 自分のデータのみ作成可能
CREATE POLICY "Users can insert their own reading logs"
ON reading_logs
FOR INSERT
WITH CHECK (auth.uid() = user_id);

-- UPDATE ポリシー: 自分のデータのみ更新可能
CREATE POLICY "Users can update their own reading logs"
ON reading_logs
FOR UPDATE
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- DELETE ポリシー: 自分のデータのみ削除可能
CREATE POLICY "Users can delete their own reading logs"
ON reading_logs
FOR DELETE
USING (auth.uid() = user_id);
```

## マイグレーション実行手順

### Supabase Dashboard から実行する場合

1. Supabase Dashboard ([https://app.supabase.com/](https://app.supabase.com/)) にログイン
2. 対象のプロジェクトを選択
3. 左サイドバーから「SQL Editor」を選択
4. 上記のSQLを順番に実行
   - まず `ALTER TABLE` でcurrent_pageを追加
   - 次に `CREATE TABLE` でreading_logsを作成
   - 最後に RLS ポリシーを設定

### Supabase CLI から実行する場合

```bash
# マイグレーションファイルを作成
supabase migration new add_reading_progress

# 上記のSQLを生成されたマイグレーションファイルにコピー
# supabase/migrations/<timestamp>_add_reading_progress.sql

# マイグレーションを実行
supabase db push
```

## 確認方法

マイグレーション実行後、以下のSQLで構造を確認できます。

```sql
-- books テーブルの構造確認
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'books'
ORDER BY ordinal_position;

-- reading_logs テーブルの構造確認
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'reading_logs'
ORDER BY ordinal_position;

-- RLS ポリシーの確認
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual
FROM pg_policies
WHERE tablename IN ('books', 'reading_logs');
```

## ロールバック（必要な場合）

マイグレーションを元に戻す必要がある場合は、以下のSQLを実行します。

```sql
-- RLS ポリシーの削除
DROP POLICY IF EXISTS "Users can view their own reading logs" ON reading_logs;
DROP POLICY IF EXISTS "Users can insert their own reading logs" ON reading_logs;
DROP POLICY IF EXISTS "Users can update their own reading logs" ON reading_logs;
DROP POLICY IF EXISTS "Users can delete their own reading logs" ON reading_logs;

-- reading_logs テーブルの削除
DROP TABLE IF EXISTS reading_logs;

-- books テーブルから current_page カラムを削除
ALTER TABLE books DROP COLUMN IF EXISTS current_page;
```

## 注意事項

- **Fact:** このマイグレーションは既存の`books`テーブルにカラムを追加するため、既存データには影響しません（`current_page`はデフォルト値0が設定されます）。
- **Fact:** `reading_logs`テーブルは新規作成なので、既存データへの影響はありません。
- **Recommendation:** 本番環境で実行する前に、開発環境で十分にテストすることを推奨します。
- **Recommendation:** マイグレーション実行前にデータベースのバックアップを取得することを推奨します。
