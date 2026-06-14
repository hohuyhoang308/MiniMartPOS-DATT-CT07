#!/usr/bin/env bash
# =====================================================================
#  Tạo chứng chỉ TLS TỰ KÝ để TEST HTTPS cục bộ (KHÔNG dùng cho production thật —
#  trình duyệt sẽ cảnh báo. Production: dùng Let's Encrypt / chứng chỉ của nhà cung cấp).
#
#  Cách dùng:  ./scripts/gen-self-signed-cert.sh [domain]
#  Kết quả:    certs/fullchain.pem , certs/privkey.pem  (mount vào /etc/nginx/certs)
# =====================================================================
set -euo pipefail
DOMAIN="${1:-localhost}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT="${SCRIPT_DIR}/../certs"
mkdir -p "$OUT"

openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
  -keyout "$OUT/privkey.pem" \
  -out "$OUT/fullchain.pem" \
  -subj "/C=VN/O=MiniMart POS/CN=$DOMAIN" \
  -addext "subjectAltName=DNS:$DOMAIN"

echo "Đã tạo chứng chỉ tự ký cho '$DOMAIN' tại $OUT/ (hiệu lực 365 ngày)."
echo "Production: thay bằng chứng chỉ thật (Let's Encrypt: certbot) rồi mount cùng đường dẫn."
