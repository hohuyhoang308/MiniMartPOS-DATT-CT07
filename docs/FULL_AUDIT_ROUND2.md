# Full FE + BE Audit (Round 2) — Missing & Unprofessional Functions

> Whole-app deep audit (POS/sales, payments, reporting/cash, inventory/catalog/CRM, and
> cross-cutting engineering quality). Findings cite `file:line`. **[verified]** = re-read
> directly this round. Companion to `ADMIN_SELF_ASSESSMENT.md` (admin P0/P1 already fixed).
>
> Date: 2026-06-14. The codebase is actively changing (EmployeeReport, ProductReport,
> petty-cash/CashMovement, stock write-off were added in parallel) — evaluated current state.

---

## 0. Scores

| Area | Score | Verdict |
|---|---:|---|
| Backend engineering | **7/10** | Integrity-first core (ledger-derived stock, deadlock-safe locks, atomic promo, DB idempotency, strong constraints). Held back by the gaps below. |
| Frontend engineering | **5/10** | Clean & consistent (axios layer, reusable loading/empty states, FE idempotency) but no tests/lint/TS, no error boundary, no a11y/i18n, no pagination. |
| Sales/money correctness | **5.5/10** | One real oversell race + one cash-control authz hole + idempotency-on-double-submit returns 500. |
| Reporting correctness | **6/10** | Cash math is correct; but product/category revenue is gross while headline is net → reports don't reconcile. |
| Professional POS feature completeness | **5/10** | Missing split payment, returns/refunds, thermal printing, X/Z reports, weighed goods, and all chain logistics. |

---

## 1. Verified correctness / security bugs (P0)

1. **Stock oversell race [verified]** — `SaleService.allocateStockFifo` locks `products` rows
   (`findByIdForUpdate`, deadlock-safe) but reads availability from the `v_batch_stock`
   **view**. With InnoDB default **REPEATABLE READ** (no isolation configured anywhere), each
   transaction's consistent-read snapshot is fixed at its first SELECT — which happens *before*
   the lock (idempotency/shift/product reads earlier in `createInvoice`). So two concurrent
   sales of the last unit both see stock available even after serializing on the lock → can
   oversell / negative `on_shelf`. The existence of `StockReconcileJob` + `findNegativeStock()`
   confirms the invariant isn't held by construction. *Fix:* re-read availability with a
   locking/current read after acquiring the lock, or keep a balance column on a locked row, or
   set READ COMMITTED + re-query.

2. **`confirmPaid` has no authorization [verified]** — `PaymentController.confirm`
   (`controller/PaymentController.java:32-33`) has **no `@PreAuthorize`** and no amount check; any
   authenticated user (incl. STAFF) can mark a QR invoice PAID with reference `"XÁC NHẬN TAY"`.
   A cashier can complete a sale that never received money. *Fix:* restrict to ADMIN/MANAGER (or
   require WEB2M-fail first + audit), add a cash-control approval.

3. **Product/category/top revenue is GROSS, headline is NET [verified]** — `total_amount`
   (= subtotal − discount) is used for revenue/dashboard/shift, but `InvoiceItemRepository`
   `productSales`/`categorySales`/`topProducts` use `SUM(ii.subtotal)` (pre-discount,
   `:26,77,84,133,188,194`). Whenever a promo or loyalty redemption applies, ProductReport and
   the dashboard "top products / categories" overstate revenue & profit and **don't reconcile**
   with the revenue report. *Fix:* pro-rate `discount_amount` down to lines, or report product
   revenue from a discounted line value.

4. **Loyalty points lost-update** — `CustomerRepository` has **no row lock**; `SaleService`
   reads `loyaltyPoints`, modifies, writes without locking the customer. Two concurrent sales
   for the same member lost-update the balance and write inconsistent ledger `balance_after`.
   `LoyaltyReconcileJob` only detects drift. *Fix:* lock the customer row (or atomic
   `UPDATE … points = points + ?`).

5. **Idempotency double-submit returns 500** — `createInvoice` does check-then-act on the
   idempotency key; two simultaneous identical submits both miss the lookup, one hits the unique
   constraint, and `InvoiceController.createWithRetry` retries it as a *duplicate-code* case (new
   code, **same idem key**) → fails 3× → 500 instead of returning the original invoice. *Fix:*
   on duplicate-idem-key, re-read and return the existing invoice; only retry on duplicate-code.

6. **Expired goods accepted at receipt** — `GoodsReceiptItemRequest.expiryDate` has no
   `@FutureOrPresent`; `GoodsReceiptService.create` sets it blindly; FE `<input type="date">` has
   no `min`. Already-expired stock can be received (then blocked from sale, but inflates
   stock/value until written off). *Fix:* validate/ warn on past expiry.

---

## 2. Engineering professionalism gaps

### Backend (P1 unless noted)
- **No pagination on any list endpoint** — products / customers / audit / stock-adjust return
  unbounded lists (only invoices capped at 500 by truncation). Contradicts "chain scale". → `Page<>`.
- **No refresh token / revocation (P0-ish)** — 24h access JWT only; a leaked token is valid 24h.
  → short access + httpOnly refresh + rotation/jti revocation.
- **Login events not audited** — `AuthService.login` never logs success/failure/lockout though
  `AuditService` exists. → record with username + IP.
- **No security headers / CSP** — `SecurityConfig` never calls `.headers(...)`. → add CSP, HSTS,
  frame-options, referrer-policy.
- **Account lockout is in-memory only** — `ConcurrentHashMap`, lost on restart, not multi-node.
- **No `@Version` optimistic locking** anywhere (loyalty is the visible victim, #1.4).
- **No DB migration tool** — boot-time `schema.sql` re-run + hand-coded conditional ALTERs; no
  Flyway/Liquibase, no versioning/rollback.
- **No Actuator / health / metrics / structured logging / request-IDs.**
- **Audit endpoint leaks the entity** — `AuditController.recent()` returns raw `AuditLog`. → DTO.
- **Weak password policy** — `@Size(min=6)` only, no complexity.
- **`@Scheduled` jobs have no ShedLock** — safe only because they're idempotent; risky if scaled out.

### Frontend (P1)
- **No global ErrorBoundary** — any render throw blanks the whole SPA.
- **Almost no tests; no CI** — 5 BE unit tests, 0 FE tests, no `.github/workflows`; Playwright
  installed but unused. → GitHub Actions `mvn test` + Vitest/Playwright smoke.
- **No linter/formatter, no TS/prop-types** — plain JS, no contracts.
- **No route code-splitting** — all 24 routes statically imported (`App.jsx`).
- **Accessibility weak** — core POS add-to-cart is `<div onClick>` (`Pos.jsx:276`), icon-only
  buttons without `aria-label`, toasts not `aria-live`.
- **No i18n** — strings hardcoded Vietnamese.
- **JWT in localStorage** on a money app (XSS-exposed); demo passwords pre-filled on Login.
- **No list pagination/virtualization** — every list renders the full array; the GoodsReceipts
  SKU `<select>` renders *all* SKUs per line (unusable at 5k–20k SKUs).
- **Heavy CRUD/report duplication** — no `useCrud` hook; EmployeeReport/ProductReport duplicate
  ~70% of each other; chart palettes/configs copy-pasted across Dashboard/ChainOverview/Reports.
- **`ShiftService.listAll()` N+1** — `toResponse` runs ~4 queries/shift × 200 shifts.

---

## 3. Missing professional-POS features (P2 — new modules)

**Sales/checkout:** split / partial payment, multiple tenders per bill, **returns/refunds**
(removed earlier — only full cancel exists), park/hold bill server-side (only client localStorage
today), manual line discount / price override, customer-facing display, **thermal/ESC-POS
printing + cash-drawer kick** (only 80mm PDF today), offline mode, **weighed goods / decimal
quantity** (qty is Integer), POS hotkeys.

**Reporting/cash:** date-range pickers on Dashboard & ChainOverview (both fixed to today/month),
store comparison over an arbitrary range, **X/Z reports** (tender + tax breakdown per shift),
**VAT/GTGT report** (`taxAmount` is stored but never reported), **inventory valuation report**
(stock × import cost), CSV/PDF export on the new reports (only revenue → Excel today), drill-down
from report rows to invoices, profit/category trend, cash-movement audit listing.

**Inventory/chain:** inter-store stock transfer, per-store price/availability (price/cost/tax/
min-stock are chain-global columns; receipt overwrites the **global** cost last-writer-wins),
central purchasing / PO with approval, aggregated "where is this SKU across stores" view,
barcode/shelf-label printing, **stocktake / cycle-count** (the new write-off is shrinkage, not a
counted reconciliation), supplier-on-product (reorder can't say which supplier), bulk product
import (CSV), product image upload (URL-only today), per-store promotion usage limits (global today).

**CRM:** customer segments / RFM / tags / campaigns, tiers, gift cards / store credit (loyalty
points are the only stored value), phone-format validation.

---

## 4. Prioritized action list

**P0 (correctness / cash control):** #1.1 oversell race · #1.2 `confirmPaid` authz · #1.3
gross-vs-net product revenue · #1.4 loyalty lost-update · #1.5 idempotency-500 · #1.6 expired-receipt guard.

**P1 (professionalism):** pagination · refresh-token + login audit + security headers ·
`ShiftService.listAll` N+1 · FE ErrorBoundary · CI + a starter test suite · audit DTO ·
migration tool (Flyway).

**P2 (feature modules):** returns/refunds · split payment · thermal printing · X/Z + VAT +
valuation reports + export · inter-store transfer · per-store pricing · stocktake · bulk import ·
pagination/virtualization at SKU scale.

> Note: the underlying architecture (ledger-derived stock, deadlock-safe locking, atomic promo
> consumption, DB-level idempotency, strong constraints, hierarchical authz + per-service store
> isolation) is genuinely solid. The P0 items are fixable defects, not architectural rewrites.
