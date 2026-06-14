#!/usr/bin/env bash
# =====================================================================
#  SAO LƯU CSDL MiniMart POS (mysqldump) — chạy thủ công hoặc qua cron.
#  Dùng cho cài đặt bằng docker-compose (container tên: minimartpos-mysql).
#
#  Cách dùng:
#     ./scripts/backup-db.sh                 # tạo 1 bản sao lưu .sql.gz vào ./backups
#     RETENTION_DAYS=14 ./scripts/backup-db.sh
#
#  Đặt lịch hằng đêm 1h sáng (crontab -e):
#     0 1 * * *  cd /duong/dan/du-an && ./scripts/backup-db.sh >> backups/backup.log 2>&1
#
#  Khôi phục: xem scripts/restore-db.sh
# =====================================================================
set -euo pipefail

CONTAINER="${DB_CONTAINER:-minimartpos-mysql}"
DB_NAME="${DB_NAME:-pos_convenience_store}"
DB_USER="${DB_USERNAME:-root}"
DB_PASS="${DB_PASSWORD:-root}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="${BACKUP_DIR:-$SCRIPT_DIR/../backups}"
mkdir -p "$OUT_DIR"

STAMP="$(date +%Y%m%d-%H%M%S)"
FILE="$OUT_DIR/pos-$STAMP.sql.gz"

echo "[$(date '+%F %T')] Bắt đầu sao lưu $DB_NAME từ container $CONTAINER ..."
# --single-transaction: ảnh chụp nhất quán không khóa bảng (InnoDB). --routines/--triggers: kèm view/trigger.
docker exec "$CONTAINER" sh -c \
  "exec mysqldump --single-transaction --routines --triggers --default-character-set=utf8mb4 -u'$DB_USER' -p'$DB_PASS' '$DB_NAME'" \
  | gzip > "$FILE"

SIZE="$(du -h "$FILE" | cut -f1)"
echo "[$(date '+%F %T')] Đã tạo $FILE ($SIZE)"

# Dọn bản sao lưu cũ hơn RETENTION_DAYS ngày.
find "$OUT_DIR" -name 'pos-*.sql.gz' -type f -mtime +"$RETENTION_DAYS" -print -delete | \
  sed 's/^/[xoá cũ] /' || true

echo "[$(date '+%F %T')] Hoàn tất. Giữ lại bản trong $RETENTION_DAYS ngày."
