# Admin / Chain-Management — Self-Assessment (deep audit)

> A critical, function-level audit of the **ADMIN (chain-wide administrator)** experience:
> is it complete, is the code clean, is it enough for someone running a whole chain, and is
> it modern/convenient vs a professional POS? Findings cite `file:line`. Items marked
> **[verified]** were re-read directly; others come from a thorough module audit.
>
> Date: 2026-06-14 · Companion to `ADMIN_CHAIN_MANAGEMENT_ASSESSMENT.md` (which lists the
> store-scope feature added earlier).

---

## ✅ Resolved in this round (P0 + P1)

All P0 correctness/isolation bugs and the P1 hardening/performance items below were fixed
and both backend (`mvn compile`) and frontend (`npm run build`) pass.

**P0 — correctness / isolation**
- **Cross-store shift close** → `ShiftService.close` now calls `StoreContext.assertSameStore(shift.store.id)`.
- **Product list wrong-store stock** → `ProductService.search` now scopes stock to the active store
  (and sums across stores for whole-chain admin scope); shelf codes are store-scoped.
- **Cross-store customer history leak** → `CustomerService.history` filters invoices by the active
  store via new `InvoiceRepository.findCustomerHistory(...)`.
- **Profit overstated by promotions** → profit is now `net revenue − COGS` (subtracts order-level
  discount). New `InvoiceItemRepository.sumCogs`; report `revenueByPeriod` rewritten to
  `SUM(total_amount) − SUM(cogs)`; the old pre-discount `sumProfit` removed.
- **Timezone unpinned** → new `config/TimeZoneConfig` pins the JVM default to `Asia/Ho_Chi_Minh`
  (JDBC + Jackson were already pinned).

**P1 — hardening + performance**
- **`X-Store-Id` validated** in `StoreContextFilter` (non-existent id ignored → whole-chain, no 500).
- **Integrations recipient delete** now `assertSameStore` on the recipient's store.
- **Manager user-list** now returns only STAFF of their store (`findByStore_IdAndRoleOrderByIdAsc`).
- **Last-admin protection** → `UserService` blocks demoting/locking the last active admin.
- **`GET /store-config` idempotent** → returns a transient default instead of INSERTing on read.
- **Chain-comparison N+1 removed** → `getStoreComparison` now uses 6 constant `GROUP BY store`
  queries (`revenueByStore`/`countByStore`/`cogsByStore`/`lowStockCountByStore`/`outOfStockCountByStore`).
- **Report profit subquery bounded** with a date filter inside the subquery.
- **`shiftReport` N+1 removed** → single `cashSalesByShiftIds` batched query.
- **Frontend cleanup** → `SCOPE_KEY` shared from `api/client.js`; stale selected-store reconciled
  against the loaded store list (resets to whole-chain if the store vanished).

**Deferred:** short-TTL caching on dashboard/comparison (#14, perf-only) and the P2 feature
builds below (inter-store transfer, per-store pricing, stocktake, bulk import, audit
filters/export, reports by-store) — these are new modules, not fixes.

---

## 0. Executive verdict

| Dimension | Score | One-line |
|---|---:|---|
| **Feature completeness for a chain admin** | **6.5 / 10** | Core management + per-store drill-down + comparison exist; chain logistics (transfers, per-store pricing, central purchasing, aggregated stock) are missing. |
| **Code cleanliness** | **7 / 10** | Consistent layering, good audit logging, deliberate concurrency control. Hurt by duplication, doc-rot, a few "GET that writes", N+1s. |
| **Correctness / data trust** | **5.5 / 10** | Solid stock/FEFO core, but profit ignores promo discount, timezone is unpinned, and the product list shows the wrong store's stock. |
| **Security / multi-tenant isolation** | **6.5 / 10** | Strong central design (bound users can't spoof store), but isolation is opt-in per-service and **2 holes are confirmed**. |
| **Modern UX vs professional POS** | **5.5 / 10** | Clean, charts, empty states, good micro-touches; but no pagination/search/bulk/export where a chain needs them. |

**Overall: a strong, well-architected student/SME-grade chain POS admin — not yet a
production-grade professional chain suite.** The foundation is genuinely good; the gaps are
concentrated in (a) a handful of correctness/isolation bugs that must be fixed, and (b)
chain-scale logistics & analytics features that don't exist yet.

---

## 1. Per-module scorecard

| Module | Clean | Complete | Headline issue |
|---|:--:|:--:|---|
| Store mgmt | 4/5 | 3/5 | No delete/archive; deactivating a store silently locks out its users with no warning; code-uniqueness case-insensitive in app but not in DB |
| User mgmt | 3/5 | 3/5 | **[verified]** manager list leaks non-STAFF peers; no last-admin/self-demote protection; no search/filter/pagination |
| Settings / StoreConfig | 4/5 | 3/5 | `GET` lazily **writes** inside a write-transaction; no bank-info (BIN/account) validation |
| Integrations (WEB2M/Telegram) | 3/5 | 3/5 | Recipient DELETE has no store guard; outbound HTTP inside `@Transactional sync()`; weak WEB2M matching |
| Audit | 4/5 | 2/5 | No filter by store/date/actor, no export, 200-row hard cap; returns entity not DTO |
| Dashboard | 3/5 | 3/5 | ~14 serial queries; timezone unpinned; blank page on error |
| Chain comparison (new) | 4/5 | 3/5 | **N+1**: 6 queries × N stores; today/month only, no date range, no export |
| Reports | 3/5 | 3/5 | Profit excludes order-level discount; `shiftReport` N+1; no per-store breakdown/export |
| Catalog (Product/…) | 4/5 | 3/5 | **[verified]** product list shows an arbitrary store's stock; chain-wide price/cost only |
| Inventory/Shelf/Receipt | 4/5 | 3/5 | No stocktake / write-off; expired goods accepted at receipt; per-store only (no chain rollup) |

---

## 2. Confirmed critical issues (fix before "production")

### P0 — Correctness / security (data integrity & isolation)

1. **Cross-store shift close [verified]** — `ShiftService.close()` (`service/ShiftService.java:103-131`)
   checks role + "is it mine" but **never `assertSameStore`**. A manager of Store A can
   `POST /api/shifts/{id}/close` on a Store B shift (sequential id), writing its closing
   cash & cash-difference audit. *Fix:* `StoreContext.assertSameStore(shift.getStore().getId())`.

2. **Product list shows the wrong store's stock [verified]** — `ProductService.search()`
   (`service/ProductService.java:130-131`) uses `stockRepository.findAll()` and merges
   `(a,b)->a`, ignoring `StoreContext`. List vs detail (`stockView`, scoped) disagree; the
   admin/manager sees a random store's stock in the catalog. *Fix:* scope to
   `StoreContext.currentStoreId()` or return an explicit cross-store total.

3. **Cross-store customer history leak [verified]** — `CustomerService.history()`
   (`service/CustomerService.java:99-113`) calls `findByCustomerIdAndStatus…` with **no store
   filter**, so any staff at any store can read a customer's full chain-wide purchase
   history. (Customers are chain-shared by design; the *invoice* history should still be
   store-scoped for non-admins.)

4. **Profit overstated by promotions** — revenue = `subtotal − discount_amount`, but profit
   = `Σ(unit_price − import_price)·qty` uses pre-discount line prices
   (`repository/InvoiceItemRepository.java:40-48` + `InvoiceRepository.revenueByPeriod`).
   Order-level promo discount is never subtracted from profit → every promo inflates
   reported profit. The chain admin's P&L is wrong whenever promotions run.

5. **Timezone unpinned** — `LocalDate.now()` (`DashboardService.java:55,117`, ReportService)
   uses JVM tz while MySQL `DATE_FORMAT`/`HOUR` uses DB session tz. If they differ, "today",
   hourly and daily buckets disagree (midnight invoices land in the wrong day). *Fix:* pin
   `Asia/Ho_Chi_Minh` on JVM + JDBC `serverTimezone` + `spring.jackson.time-zone`.

### P1 — Isolation hardening & robustness

6. **`X-Store-Id` not validated** (`security/StoreContextFilter.java:30`) — admin can send a
   non-existent/inactive store id; reads return empty, some writes throw a messy 500 instead
   of a clean 400. Validate against an ACTIVE store at the edge.
7. **Integrations recipient DELETE unscoped** (`IntegrationsController` → recipient service
   `delete(id)`) — by-PK delete, no store check. Harmless while admin-only, dangerous if the
   controller is ever opened to managers (the codebase is trending that way).
8. **Manager user-list scope ≠ write scope [verified]** — `UserService.findAll()` returns all
   users of the store including other MANAGERs; only writes are STAFF-gated. Filter the list to STAFF.
9. **No last-admin protection** — an admin can demote/lock the only other admin or demote
   themselves, bricking chain administration (`UserService.update`).
10. **`GET /store-config` writes** — `getEntity()` is `@Transactional` and INSERTs a config
    row on a read (`StoreConfigService`). Make lazy-create explicit, keep GET idempotent.

### P1 — Performance (won't scale to a real chain)

11. **Chain comparison N+1** — `getStoreComparison()` runs 6 queries **per store**
    (`DashboardService.java:128-139`), incl. `findLowStock(id).size()` materialising the full
    list just to count. ~300 queries at 50 stores. *Fix:* 3-4 `GROUP BY store_id` queries.
12. **Report profit subquery scans full history** — the profit `LEFT JOIN` in
    `revenueByPeriod` has **no date filter inside the subquery**, so it aggregates the entire
    `invoice_item_batches` history on every report.
13. **`shiftReport` N+1** — one `sumCashSalesByShift` per shift (`ReportService.java:72`).
14. **No caching** anywhere on read-heavy dashboard/comparison; short TTL would help a lot.

---

## 3. Completeness gaps for a chain manager (features that don't exist)

Highest business value first:

1. **Inter-store stock transfer** — no way to move stock A→B (only warehouse↔shelf within one store).
2. **Aggregated cross-store inventory view** — can't see "product X: store A 5 / B 0 / C 12" in one screen (only chain-wide counts exist).
3. **Per-store price / cost / availability** — price, cost, tax, min-stock are chain-global
   columns on `Product`; a store can't have its own price or disable a SKU. Worse, goods
   receipt overwrites the **global** `costPrice` per store (last-writer-wins).
4. **Central purchasing** — goods receipt & reorder suggestions are per single store; no
   consolidated PO across stores, no supplier-on-product.
5. **Stocktake / cycle-count / write-off** — stock only moves via receipt or sale; no way to
   adjust for shrinkage/damage, and **expired goods are accepted at receipt** and never
   discardable, permanently inflating stock & corrupting reorder math.
6. **Reports by store & over time** — comparison is today/month only; reports have no
   per-store dimension and the Excel export is single, store-agnostic.
7. **Audit forensics** — no filter by store/date/actor, no export, capped at 200 rows.
8. **Store delete/archive, opening hours, store detail (health) view.**
9. **Granular admin permissions, regional/zone hierarchy, bulk operations.**
10. **Customer/loyalty analytics** (RFM, new-vs-returning, redemption) — data exists, not surfaced.

---

## 4. Modern / convenient vs a professional POS

**Where it already feels modern:** clean dashboard with area/pie/bar charts, KPI + trend
pills, quick-range chips, FEFO shelving preview, reorder→receipt prefill, contextual
info-banners, skeleton loaders (Users), consistent empty states, the new store-scope
switcher + chain leaderboard with drill-in. These are genuinely good touches.

**Where it falls short of professional:**
- **No pagination/server paging** on Products, Users, Audit — fine at demo size, unusable at
  real volumes (a minimart chain is 5k–20k SKUs; Products renders the entire array).
- **No bulk product import (CSV/Excel)** — onboarding a store's catalog is manual, one by one.
- **Image is URL-only** — no upload/storage/thumbnail; no barcode/shelf-label printing.
- **Audit/Reports have no export** (audit) / no per-store export (reports).
- **Charts are display-only** — no click-to-filter, drill-down, sortable ranking columns,
  or series toggling.
- **Error UX** — dashboard renders a blank page on load error with no retry.
- **No date-range picker on the dashboard**; comparison can't pick a period.
- **No scheduled/emailed reports, no PDF P&L.**

---

## 5. Prioritized action list

**P0 (correctness/isolation — do first):** #1 shift-close store guard · #2 product-list
store scope · #3 customer-history store scope · #4 profit minus discount · #5 pin timezone.

**P1 (hardening + scale):** #6 validate `X-Store-Id` · #7 scope recipient delete · #8 manager
user-list STAFF-only · #9 last-admin protection · #10 idempotent GET store-config · #11
GROUP-BY chain comparison · #12 bounded report profit subquery · #13 shiftReport single query · #14 caching.

**P2 (features for a real chain):** inter-store transfer · aggregated inventory view ·
per-store price/cost · stocktake + expiry write-off · reports by-store & date-range + export ·
audit filters/export/pagination · bulk product import + pagination.

**Cross-cutting cleanup:** consolidate the 4 duplicated `resolve(storeId)` helpers; remove
`CHAIN_ADMIN`/`CASHIER` doc-rot (now 3-tier); share `SCOPE_KEY` between `client.js` and
`StoreScopeContext`; reconcile a stale selected store against the loaded store list; return
DTOs from the audit endpoint; add a shared frontend CRUD hook + chart components to kill copy-paste.
