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
