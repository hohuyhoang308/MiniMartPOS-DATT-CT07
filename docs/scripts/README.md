# Sinh báo cáo Word từ tài liệu Markdown

Thư mục này chứa script Python "extract" tài liệu Markdown thành báo cáo Word (.docx)
chuẩn đồ án: trang bìa, mục lục tự động, tiêu đề nhiều cấp, bảng kẻ khung, hình ảnh có chú thích.

## Cài đặt

```bash
pip install python-docx
```

## Sử dụng

Từ thư mục gốc dự án:

```bash
# Dùng mặc định: đọc docs/BaoCao_QuanLyLuong.md → ghi docs/BaoCao_QuanLyLuong.docx
python docs/scripts/build_docx.py

# Hoặc chỉ định file vào/ra
python docs/scripts/build_docx.py docs/BaoCao_QuanLyLuong.md docs/BaoCao.docx
```

Mở file `.docx` trong Word, chuột phải vào mục lục → **Update Field** để hiển thị số trang.

## Tùy biến

- Sửa thông tin **trang bìa** (trường, khoa, GVHD, sinh viên, năm) ở biến `COVER` đầu file
  `build_docx.py`.
- Đổi **font/cỡ chữ/màu tiêu đề** ở các hằng `BASE_FONT`, `BASE_SIZE`, `HEADING_COLOR`.
- Nội dung báo cáo nằm hoàn toàn trong file Markdown nguồn — sửa Markdown rồi chạy lại script.

## Cú pháp Markdown được hỗ trợ

`#`/`##`/`###`/`####` tiêu đề · `**đậm**` `*nghiêng*` `` `mã` `` · bảng `| a | b |` ·
danh sách `-` và `1.` · trích dẫn `>` · hình `![chú thích](đường_dẫn)`.
</content>
