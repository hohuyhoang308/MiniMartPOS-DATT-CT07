# 03. SƠ ĐỒ USE CASE

> Sơ đồ vẽ bằng **Mermaid** — xem trực tiếp trên VS Code (cài extension "Markdown Preview Mermaid Support")
> hoặc trên GitHub. Có thể export PNG để dán vào báo cáo.
> Quan hệ kế thừa actor: **Manager kế thừa Cashier**, **Admin kế thừa Manager** (ai quyền cao có luôn quyền thấp).

## 3.1. Sơ đồ Use Case tổng quát

```mermaid
flowchart LR
    Cashier(("👤 Thu ngân"))
    Manager(("👤 Quản lý"))
    Admin(("👤 Chủ cửa hàng"))

    Admin -.kế thừa.-> Manager
    Manager -.kế thừa.-> Cashier

    subgraph HT["Hệ thống POS Cửa hàng tiện lợi"]
        UC01["UC01 Đăng nhập/Đăng xuất"]
        UC08["UC08 Mở/Đóng ca"]
        UC09["UC09 Bán hàng tại quầy"]
        UC10["UC10 Thanh toán & lập hóa đơn"]
        UC11["UC11 Áp dụng khuyến mãi"]
        UC12["UC12 In/Xuất hóa đơn PDF"]
        UC21["UC21 Thanh toán QR"]
        UC15["UC15 Quản lý KH thân thiết"]

        UC03["UC03 Quản lý danh mục"]
        UC04["UC04 Quản lý đơn vị tính"]
        UC05["UC05 Quản lý sản phẩm"]
        UC06["UC06 Quản lý nhà cung cấp"]
        UC07["UC07 Lập phiếu nhập kho"]
        UC13["UC13 Quản lý hóa đơn"]
        UC14["UC14 Hủy hóa đơn"]
        UC16["UC16 Quản lý khuyến mãi"]
        UC17["UC17 Tồn kho & cảnh báo"]
        UC18["UC18 Dashboard"]
        UC19["UC19 Báo cáo & xuất file"]

        UC02["UC02 Quản lý tài khoản & phân quyền"]
        UC20["UC20 Cấu hình cửa hàng"]
    end

    Cashier --- UC01
    Cashier --- UC08
    Cashier --- UC09
    Cashier --- UC10
    Cashier --- UC11
    Cashier --- UC12
    Cashier --- UC21
    Cashier --- UC15

    Manager --- UC03
    Manager --- UC04
    Manager --- UC05
    Manager --- UC06
    Manager --- UC07
    Manager --- UC13
    Manager --- UC14
    Manager --- UC16
    Manager --- UC17
    Manager --- UC18
    Manager --- UC19

    Admin --- UC02
    Admin --- UC20
```

## 3.2. Use Case con — Nghiệp vụ bán hàng (chi tiết quan hệ include/extend)

```mermaid
flowchart LR
    Cashier(("👤 Thu ngân"))

    UC09["UC09 Bán hàng tại quầy"]
    UC10["UC10 Thanh toán & lập hóa đơn"]
    UC11["UC11 Áp dụng khuyến mãi"]
    UC12["UC12 In/Xuất hóa đơn"]
    UC15["UC15 Gắn khách thân thiết"]
    UC21["UC21 Thanh toán QR"]
    CHK["Kiểm tra tồn kho"]
    PAY["Tính tiền & tiền thừa"]

    Cashier --- UC09
    Cashier --- UC10

    UC09 -. include .-> CHK
    UC09 -. extend .-> UC15
    UC10 -. include .-> PAY
    UC10 -. include .-> UC12
    UC10 -. extend .-> UC11
    UC10 -. extend .-> UC21
```

> **include**: luôn xảy ra (bán hàng luôn kiểm tra tồn; thanh toán luôn tính tiền & lập hóa đơn).
> **extend**: tùy chọn (gắn khách thân thiết, áp khuyến mãi, thanh toán QR).

## 3.3. Use Case con — Quản trị & kho

```mermaid
flowchart LR
    Manager(("👤 Quản lý"))
    UC05["UC05 Quản lý sản phẩm"]
    UC07["UC07 Lập phiếu nhập kho"]
    UC17["UC17 Tồn kho & cảnh báo"]
    INC["Cộng tồn kho"]
    LOW["Cảnh báo tồn thấp / cận HSD"]

    Manager --- UC05
    Manager --- UC07
    Manager --- UC17

    UC07 -. include .-> INC
    UC17 -. include .-> LOW
    UC05 -. liên quan .-> UC17
```
