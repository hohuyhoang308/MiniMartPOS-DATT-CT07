# Chạy hệ thống POS bằng Docker

Chạy **toàn bộ** (MySQL + Backend + Frontend) chỉ với một lệnh — không cần cài JDK, Maven, Bun hay MySQL trên máy.

## Yêu cầu
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (đã bật, có Docker Compose v2).

## Chạy
```bash
docker compose up -d --build
```
Lần đầu sẽ mất vài phút (tải thư viện Maven + Bun, dựng ảnh). Sau đó:

- Ứng dụng:  **http://localhost:8088**
- API trực tiếp (tuỳ chọn): http://localhost:8080
- MySQL (tuỳ chọn, từ máy host): `localhost:3307`, user `root`

**Tài khoản demo** (mật khẩu `123456`): `admin` · `manager` · `cashier`.

Backend tự tạo schema + view và seed dữ liệu demo (sản phẩm, hoá đơn 30 ngày) ngay lần khởi động đầu.

## Lệnh hữu ích
```bash
docker compose logs -f backend     # xem log backend (theo dõi quá trình seed/khởi động)
docker compose ps                  # trạng thái 3 dịch vụ
docker compose down                # dừng (giữ nguyên dữ liệu)
docker compose down -v             # dừng + XOÁ dữ liệu MySQL (reset sạch)
docker compose up -d --build       # build lại sau khi sửa code
```

## Cấu hình khi deploy thật
Tạo file `.env` (xem `.env.example`) để đổi giá trị nhạy cảm — file `.env` đã được `.gitignore`:
```env
DB_PASSWORD=<mat-khau-manh>
JWT_SECRET=<chuoi-ngau-nhien-toi-thieu-32-ky-tu>
```

## Vận hành production (ops)

### Health check
Backend phơi `GET /actuator/health` (công khai, không lộ chi tiết). Docker tự đánh dấu container
UNHEALTHY nếu app/DB lỗi; `frontend` chỉ khởi động khi `backend` đã **healthy** (`condition: service_healthy`).
```bash
curl http://localhost:8080/actuator/health      # {"status":"UP"}
docker compose ps                               # cột STATUS hiển thị (healthy)
```

### Sao lưu & khôi phục CSDL
```bash
./scripts/backup-db.sh                           # tạo backups/pos-<thời gian>.sql.gz (giữ 7 ngày)
RETENTION_DAYS=30 ./scripts/backup-db.sh         # đổi số ngày giữ
./scripts/restore-db.sh backups/pos-XXXX.sql.gz  # khôi phục (hỏi xác nhận trước khi ghi đè)
```
Đặt lịch hằng đêm 1h sáng (`crontab -e`):
```cron
0 1 * * *  cd /duong/dan/du-an && ./scripts/backup-db.sh >> backups/backup.log 2>&1
```

### Bật HTTPS (TLS)
1. Lấy chứng chỉ: production dùng Let's Encrypt (`certbot`); test cục bộ:
   `./scripts/gen-self-signed-cert.sh ten-mien.cua-ban` → tạo `certs/fullchain.pem` + `certs/privkey.pem`.
2. Trong `docker-compose.yml`, ở service `frontend`: mount cert + dùng `nginx-tls.conf` + mở cổng 443:
   ```yaml
   frontend:
     ports: ["443:443", "80:80"]
     volumes:
       - ./certs:/etc/nginx/certs:ro
       - ./frontend/nginx-tls.conf:/etc/nginx/conf.d/default.conf:ro
   ```
3. Đặt `CORS_ORIGINS=https://ten-mien.cua-ban` cho service `backend`.

### Bật chế độ production (QUAN TRỌNG)
Mặc định hệ thống chạy profile DEV (seed tài khoản demo `admin/123456` + dữ liệu giả). Khi deploy thật,
đặt `SPRING_PROFILES_ACTIVE=prod` cho service `backend` để **tắt seed demo** và **bắt buộc** `JWT_SECRET` thật.
> Lưu ý: ở `prod` seeder demo tắt → cần tạo tài khoản admin đầu tiên (xem mục bootstrap admin trong README/đề xuất).

## Kiến trúc khi chạy Docker
```
trình duyệt ──> frontend (nginx :80, cổng host 8088)
                   │  static React build (SPA)
                   └─ proxy /api/* ──> backend (Spring Boot :8080)
                                          └──> mysql :3306 (volume: mysql_data)
```
Frontend gọi `/api` **cùng origin** nên không vướng CORS; nginx chuyển tiếp sang backend trong mạng nội bộ của Compose.

## Cổng đã chọn (tránh đụng dịch vụ local)
| Dịch vụ  | Trong container | Trên máy host |
|----------|-----------------|----------------|
| frontend | 80              | **8088**       |
| backend  | 8080            | 8080           |
| mysql    | 3306            | **3307** (tránh MySQL/ServBay 3306) |

## Lưu ý
- Test backend chạy riêng (không nằm trong ảnh để build nhanh):
  `cd backend && mvn test`.
- Dữ liệu MySQL nằm ở Docker volume `mysql_data`, không mất khi `down` (chỉ mất khi `down -v`).
