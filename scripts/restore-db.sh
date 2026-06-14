#!/usr/bin/env bash
# =====================================================================
#  KHÔI PHỤC CSDL MiniMart POS từ một bản sao lưu .sql.gz (tạo bởi backup-db.sh).
#
#  Cách dùng:
#     ./scripts/restore-db.sh backups/pos-20260614-010000.sql.gz
#
#  CẢNH BÁO: ghi đè dữ liệu hiện tại của database. Hãy chắc chắn trước khi chạy ở production.
# =====================================================================
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Cách dùng: $0 <duong-dan-file.sql.gz>" >&2
  exit 1
fi
FILE="$1"
[ -f "$FILE" ] || { echo "Không tìm thấy file: $FILE" >&2; exit 1; }

CONTAINER="${DB_CONTAINER:-minimartpos-mysql}"
DB_NAME="${DB_NAME:-pos_convenience_store}"
DB_USER="${DB_USERNAME:-root}"
DB_PASS="${DB_PASSWORD:-root}"

read -r -p "Khôi phục '$FILE' vào '$DB_NAME' (ghi đè dữ liệu hiện tại)? [yes/N] " ans
[ "$ans" = "yes" ] || { echo "Đã hủy."; exit 0; }

echo "[$(date '+%F %T')] Đang khôi phục ..."
gunzip -c "$FILE" | docker exec -i "$CONTAINER" sh -c \
  "exec mysql --default-character-set=utf8mb4 -u'$DB_USER' -p'$DB_PASS' '$DB_NAME'"
echo "[$(date '+%F %T')] Khôi phục xong từ $FILE"
