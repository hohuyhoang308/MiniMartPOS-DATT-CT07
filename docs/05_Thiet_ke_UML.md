# 05. THIẾT KẾ UML

> Sơ đồ vẽ bằng **Mermaid**. Hệ thống gồm **Frontend React (SPA)** gọi **Backend Spring Boot REST API**
> qua HTTP/JSON; backend phân lớp **Controller → Service → Repository → Entity** trên **MySQL**.

## 5.1. Sơ đồ kiến trúc tổng thể (Component)

```mermaid
flowchart LR
    subgraph FE["Frontend - React (SPA)"]
        UI["Pages / Components<br/>POS, Quản trị, Dashboard"]
        AX["api/ (Axios + JWT interceptor)"]
        CTX["Context: Auth, Cart"]
        UI --- CTX
        UI --- AX
    end

    subgraph BE["Backend - Spring Boot REST API"]
        SEC["Security (JWT Filter, CORS)"]
        CTRL["@RestController"]
        SVC["@Service (nghiệp vụ + @Transactional)"]
        REPO["@Repository (Spring Data JPA)"]
        ENT["Entity (ánh xạ bảng)"]
        SEC --> CTRL --> SVC --> REPO --> ENT
    end

    DB[("MySQL 8")]

    AX -- "HTTP/JSON /api/**" --> SEC
    ENT --- DB
```

## 5.2. Class Diagram — Tầng Domain (Entity)

> Phản ánh đúng CSDL ở `04`. Quan hệ kết hợp đúng theo khóa ngoại (1—N).

```mermaid
classDiagram
    class User {
        +Long id
        +String username
        +String passwordHash
        +String fullName
        +Role role
        +UserStatus status
    }
    class Category {
        +Long id
        +String name
        +Status status
    }
    class Unit {
        +Long id
        +String name
    }
    class Product {
        +Long id
        +String barcode
        +String name
        +BigDecimal costPrice
        +BigDecimal salePrice
        +int minStock
        +Status status
    }
    class Supplier {
        +Long id
        +String name
        +String phone
        +String email
    }
    class GoodsReceipt {
        +Long id
        +String code
        +BigDecimal totalAmount
        +LocalDateTime createdAt
    }
    class GoodsReceiptItem {
        +Long id
        +int quantity
        +BigDecimal importPrice
        +LocalDate expiryDate
    }
    class InvoiceItemBatch {
        +Long id
        +int quantity
    }
    class Customer {
        +Long id
        +String fullName
        +String phone
        +int loyaltyPoints
    }
    class Promotion {
        +Long id
        +String code
        +DiscountType discountType
        +BigDecimal discountValue
        +BigDecimal minOrderAmount
        +Integer usageLimit
        +int usedCount
    }
    class WorkShift {
        +Long id
        +BigDecimal openingCash
        +BigDecimal closingCash
        +ShiftStatus status
    }
    class Invoice {
        +Long id
        +String code
        +BigDecimal subtotal
        +BigDecimal discountAmount
        +BigDecimal totalAmount
        +PaymentMethod paymentMethod
        +BigDecimal customerPaid
        +BigDecimal changeAmount
        +int pointsEarned
        +InvoiceStatus status
    }
    class InvoiceItem {
        +Long id
        +int quantity
        +BigDecimal unitPrice
        +BigDecimal subtotal
    }
    class StoreConfig {
        +Byte id
        +String name
        +String taxCode
    }

    Category "1" --> "N" Product
    Unit "1" --> "N" Product
    Supplier "1" --> "N" GoodsReceipt
    User "1" --> "N" GoodsReceipt : created_by
    GoodsReceipt "1" --> "N" GoodsReceiptItem
    Product "1" --> "N" GoodsReceiptItem
    User "1" --> "N" WorkShift : cashier
    WorkShift "1" --> "N" Invoice
    Customer "1" --> "N" Invoice : optional
    Promotion "1" --> "N" Invoice : optional
    Invoice "1" --> "N" InvoiceItem
    Product "1" --> "N" InvoiceItem
    InvoiceItem "1" --> "N" InvoiceItemBatch : phân bổ FIFO
    GoodsReceiptItem "1" --> "N" InvoiceItemBatch : xuất bán
```

> `InvoiceItemBatch` là **bảng nối** giữa dòng bán và lô hàng — bảo đảm truy vết & toàn vẹn tồn kho
> (hủy hóa đơn ⇒ tồn tự hoàn).

## 5.3. Class Diagram — Tầng nghiệp vụ (ví dụ module Bán hàng)

```mermaid
classDiagram
    class SaleController {
        <<RestController>>
        +createInvoice(InvoiceRequest) InvoiceResponse
        +openShift(ShiftRequest) ShiftResponse
        +closeShift(Long id) ShiftResponse
    }
    class SaleService {
        <<Service>>
        +createInvoice(InvoiceRequest) Invoice
        -deductStockFIFO(productId, qty)
        -validateStock(items)
    }
    class InventoryService {
        <<Service>>
        +currentStock(productId) int
        +lowStockProducts() List
        +expiringBatches() List
    }
    class PromotionService {
        <<Service>>
        +validateAndApply(code, subtotal) BigDecimal
    }
    class InvoiceRepository { <<Repository>> }
    class GoodsReceiptItemRepository { <<Repository>> }
    class ProductRepository { <<Repository>> }

    SaleController --> SaleService
    SaleService --> InvoiceRepository
    SaleService --> GoodsReceiptItemRepository
    SaleService --> InventoryService
    SaleService --> PromotionService
    InventoryService --> GoodsReceiptItemRepository
    InventoryService --> ProductRepository
```

## 5.4. Sequence Diagram — UC01 Đăng nhập (JWT)

```mermaid
sequenceDiagram
    actor U as Người dùng
    participant FE as React (Login page)
    participant API as AuthController
    participant SVC as AuthService
    participant DB as MySQL

    U->>FE: Nhập username + password
    FE->>API: POST /api/auth/login {username, password}
    API->>SVC: authenticate(username, password)
    SVC->>DB: tìm user theo username
    DB-->>SVC: user (password_hash, role, status)
    SVC->>SVC: BCrypt.matches(password, hash)?
    alt Hợp lệ & ACTIVE
        SVC-->>API: sinh JWT (chứa role)
        API-->>FE: 200 {token, role, fullName}
        FE->>FE: Lưu token, điều hướng theo vai trò
    else Sai / bị khóa
        SVC-->>API: lỗi xác thực
        API-->>FE: 401 Unauthorized
        FE-->>U: Báo "Sai tài khoản hoặc bị khóa"
    end
```

## 5.5. Sequence Diagram — UC09 + UC10 Bán hàng & Thanh toán (trọng tâm)

```mermaid
sequenceDiagram
    actor C as Thu ngân
    participant FE as React (POS page)
    participant API as SaleController
    participant SVC as SaleService
    participant INV as InventoryService
    participant PRO as PromotionService
    participant DB as MySQL

    Note over C,FE: Đã mở ca (UC08)
    C->>FE: Quét mã vạch sản phẩm
    FE->>API: GET /api/products/barcode/{code}
    API->>INV: currentStock(productId)
    INV->>DB: v_product_stock (tồn từ các lô)
    DB-->>INV: tồn kho
    INV-->>API: sản phẩm + tồn
    API-->>FE: thông tin sản phẩm
    FE->>FE: Thêm vào giỏ (CartContext), tính tổng tạm

    C->>FE: (tùy chọn) nhập mã giảm giá + gắn khách
    C->>FE: Bấm Thanh toán (tiền mặt / QR)
    FE->>API: POST /api/invoices {items, customerId?, promoCode?, payment}

    API->>SVC: createInvoice(request)
    activate SVC
    Note over SVC,DB: @Transactional
    SVC->>INV: validateStock(items)
    INV->>DB: kiểm tra tồn từng SP
    alt Đủ tồn
        SVC->>PRO: validateAndApply(promoCode, subtotal)
        PRO-->>SVC: discountAmount
        SVC->>DB: INSERT invoices + invoice_items
        SVC->>DB: chọn lô FIFO theo HSD, ghi invoice_item_batches
        SVC->>DB: cộng loyalty_points (nếu có khách)
        SVC->>DB: used_count++ (nếu có KM)
        SVC-->>API: Invoice (đã lưu)
        API-->>FE: 201 {invoice, changeAmount}
        FE-->>C: Hiện hóa đơn + cho in/xuất PDF (UC12)
    else Một SP vừa hết tồn
        SVC->>DB: ROLLBACK
        SVC-->>API: lỗi tồn kho
        API-->>FE: 409 Conflict
        FE-->>C: Báo lỗi, không hoàn tất
    end
    deactivate SVC
```

## 5.6. Sequence Diagram — UC07 Lập phiếu nhập kho

```mermaid
sequenceDiagram
    actor M as Quản lý
    participant FE as React (Nhập kho)
    participant API as GoodsReceiptController
    participant SVC as GoodsReceiptService
    participant DB as MySQL

    M->>FE: Chọn NCC, thêm dòng (SP, SL, giá nhập, HSD)
    FE->>API: POST /api/goods-receipts {supplierId, items[]}
    API->>SVC: create(request)
    activate SVC
    Note over SVC,DB: @Transactional
    SVC->>DB: INSERT goods_receipts
    loop từng dòng
        SVC->>DB: INSERT goods_receipt_items (tạo lô mới)
    end
    SVC->>DB: cập nhật cost_price (tùy chọn)
    SVC-->>API: phiếu nhập đã lưu
    deactivate SVC
    API-->>FE: 201 Created
    FE-->>M: Báo thành công, tồn kho tăng
```

## 5.6b. Sequence Diagram — Thanh toán QR + đối soát WEB2M + Telegram (UC21/UC22/UC23)

> **VietQR** chỉ hiển thị mã QR; **WEB2M** poll giao dịch ngân hàng để xác nhận; **Telegram** báo kết quả.

```mermaid
sequenceDiagram
    actor C as Thu ngân
    participant FE as React (POS)
    participant API as PaymentController
    participant SVC as PaymentService
    participant DB as MySQL
    participant JOB as Web2mSyncJob (định kỳ)
    participant W2M as WEB2M API
    participant TELE as Telegram Bot

    Note over C,FE: Hóa đơn đã lưu, chọn thanh toán QR
    C->>FE: Chọn "Thanh toán QR"
    FE->>API: GET /api/payments/qr?invoiceId=...
    API->>SVC: tạo payment_transaction (PENDING, transfer_content)
    SVC->>DB: INSERT payment_transactions
    SVC-->>API: dựng URL ảnh VietQR (từ store_config + amount + content)
    API-->>FE: {qrUrl, transferContent}
    FE-->>C: Hiển thị mã QR cho khách quét

    loop Frontend poll trạng thái
        FE->>API: GET /api/payments/{invoiceId}/status
        API-->>FE: PENDING / PAID
    end

    par Job đối soát tự động chạy nền
        JOB->>W2M: GET web2m_api_url (lịch sử giao dịch)
        W2M-->>JOB: danh sách giao dịch ngân hàng
        JOB->>DB: tìm payment PENDING khớp (số tiền + nội dung CK)
        alt Khớp giao dịch
            JOB->>DB: UPDATE status=PAID, bank_reference, paid_at
            JOB->>TELE: gửi thông báo (nếu notify_payment) tới telegram_recipients
        end
    end

    FE->>API: GET /api/payments/{invoiceId}/status
    API-->>FE: PAID
    FE-->>C: Báo "Đã nhận thanh toán", cho in hóa đơn
```

## 5.7. Activity Diagram — Luồng bán hàng tại quầy

```mermaid
flowchart TD
    A([Bắt đầu ca - mở ca]) --> B[Quét/nhập mã vạch]
    B --> C{Tìm thấy sản phẩm?}
    C -- Không --> B2[Báo: không tồn tại] --> B
    C -- Có --> D{Còn tồn kho?}
    D -- Không --> D2[Cảnh báo hết hàng] --> B
    D -- Có --> E[Thêm vào giỏ, tính tổng tạm]
    E --> F{Quét tiếp?}
    F -- Có --> B
    F -- Không --> G{Gắn khách / mã giảm giá?}
    G -- Có --> H[Áp khách + KM, tính lại tổng]
    G -- Không --> I
    H --> I[Chọn hình thức thanh toán]
    I --> J{Tiền mặt hay QR?}
    J -- Tiền mặt --> K[Nhập tiền khách đưa]
    K --> L{Đủ tiền?}
    L -- Không --> K
    L -- Có --> M[Tính tiền thừa]
    J -- QR --> N[Sinh mã QR, xác nhận đã nhận]
    M --> O[[Transaction: lưu HĐ, trừ tồn FIFO,<br/>tích điểm, tăng lượt KM]]
    N --> O
    O --> P{Thành công?}
    P -- Không --> P2[Rollback, báo lỗi] --> I
    P -- Có --> Q[In / xuất hóa đơn PDF]
    Q --> R([Kết thúc giao dịch])
```

## 5.8. Activity Diagram — Cảnh báo tồn kho & HSD (UC17)

```mermaid
flowchart TD
    A([Mở màn hình Kho]) --> B[Lấy v_product_stock]
    B --> C{current_stock ≤ min_stock?}
    C -- Có --> D[Đánh dấu ĐỎ: tồn thấp]
    C -- Không --> E[Hiển thị bình thường]
    D --> F
    E --> F[Lấy v_expiring_batches]
    F --> G{Có lô HSD ≤ 30 ngày?}
    G -- Có --> H[Cảnh báo VÀNG: cận/quá HSD]
    G -- Không --> I([Kết thúc])
    H --> I
```
