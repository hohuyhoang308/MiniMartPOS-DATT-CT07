# Admin / Chain Management — Assessment & Improvement Report

> Scope: the **ADMIN** role (chain-wide administrator) and its ability to manage the
> whole multi-store chain. This document evaluates what existed, what was missing,
> what was implemented in this round, and the roadmap for the remaining gaps.
>
> Date: 2026-06-14 · Audience: developers/maintainers of MiniMartPOS.

---

## 1. Role model (context)

The system uses a **3-tier role model** (`backend/.../entity/enums/Role.java`):

| Role | Store binding | Intended scope |
|------|---------------|----------------|
| `ADMIN` | none (`store_id = null`) | **Chain-wide** administrator — every store |
| `MANAGER` | exactly one store | Single-store management |
| `STAFF` | exactly one store | Point-of-sale operations |

Store isolation is enforced server-side by `StoreContext` / `StoreContextFilter`
(`backend/.../security/`):

- A **store-bound** user (MANAGER/STAFF) is always locked to their own store; the
  `X-Store-Id` header is ignored so they cannot cross store boundaries.
- A **chain admin** (ADMIN) has no bound store, so the effective store is taken from
  the `X-Store-Id` request header. **No header ⇒ `null` ⇒ whole-chain scope** (allowed
  for reads/reports, blocked on per-store writes via `requireStoreId()`).

This means the **backend was already designed for an admin to "drill into" any single
store** — the data layer fully supports per-store filtering. The gap was almost entirely
in the **frontend**, which never used this capability.

---

## 2. What already worked (before this round)

Chain-level management that existed and works:

- **Store CRUD** — `/stores` page + `StoreController` (create / edit / activate-deactivate branches).
- **User & role management** — `/users` page + `UserController` (create users, assign role + store,
  reset password, lock account) across all stores.
- **Chain-wide catalog** — products, categories, units, suppliers, promotions are shared
  across the chain (no `store_id`), managed by admin.
- **Per-store configuration** — `/settings` page lets admin pick a store and configure its
  bank/VietQR, WEB2M, and Telegram integration (passes `storeId` explicitly).
- **Audit log** — `/audit` page, read-only chain-wide action trail.
- **Aggregated dashboard / reports** — `DashboardService` / `ReportService` honor
  `StoreContext`, so with no store selected they already return whole-chain figures.

---

## 3. Gaps identified

### 3.1 Critical — admin had no way to view a single store (FIXED)

`frontend/src/api/client.js` **never sent `X-Store-Id`**, and there was **no store
selector** in the UI (the header only showed a static "Toàn chuỗi" badge). Consequences:

- Admin could only ever see **whole-chain aggregates** on the dashboard/reports.
- Admin could **not drill into one store** to inspect its sales, inventory, or shifts.
- The well-built backend per-store filtering was effectively **dead code** for admins.

### 3.2 High — operational pages were hidden from admin (FIXED)

`navConfig.js` restricted Inventory, ABC/XYZ, Shifts, and Goods-Receipts to `MANAGER`
only, even though the routes in `App.jsx` already allowed `ADMIN`. The admin literally
**had no menu entry** to reach branch inventory or shift oversight.

### 3.3 High — no cross-store comparison (FIXED)

There was no "which store is doing best/worst" view. Admin had to log in as each store's
manager (or eyeball aggregate numbers) to compare branches. No ranking, no side-by-side
KPIs, no per-store stock-alert roll-up.

### 3.4 Medium/Long-term — not addressed in this round (see roadmap §5)

- **Cross-store inventory transfer** — no way to move stock from one branch to another.
- **Per-store pricing / availability** — price and catalog are chain-wide only; a branch
  cannot have its own price or disable a product locally.
- **Granular permissions** — ADMIN is all-or-nothing; no "can manage users but not stores".
- **Regional hierarchy / zones** — flat list of stores; no regional-manager tier.
- **Bulk operations** — config/promotions/prices must be applied one store at a time.
- **Report store-comparison & audit filter-by-store** — reports have no store selector of
  their own (now covered indirectly by the global scope selector) and audit cannot be
  filtered by store.

---

## 4. What was implemented in this round

### 4.1 Admin store-scope selector (the key unlock)

A chain admin can now pick **"Toàn chuỗi"** or **any single branch** from a dropdown in
the top bar. The selection drives `X-Store-Id` on every request, so the **entire app**
(dashboard, reports, inventory, shifts, invoices, …) re-scopes to that store.

New / changed files:

| File | Change |
|------|--------|
| `frontend/src/context/StoreScopeContext.jsx` | **New.** Holds the admin's selected store (`scopeStoreId`, `''` = whole chain), loads the store list, persists to `localStorage['pos_store_scope']`. |
| `frontend/src/api/client.js` | Attaches `X-Store-Id` from `pos_store_scope` on every request. |
| `frontend/src/components/Layout.jsx` | Static badge → **store-scope dropdown** for admin (static branch badge kept for MANAGER/STAFF). Keys `<main>` on the scope so the page **remounts and refetches** when the scope changes. |
| `frontend/src/context/AuthContext.jsx` | Clears `pos_store_scope` on logout. |
| `frontend/src/main.jsx` | Wraps the app in `StoreScopeProvider`. |

Mechanics: changing the scope writes `localStorage` synchronously and bumps the
`<main key=…>`, which remounts the active page; its mount effect re-runs and the axios
interceptor sends the new `X-Store-Id`. Backend `StoreContext` does the rest. No
per-page edits were required.

### 4.2 Cross-store comparison ("So sánh chi nhánh")

New chain-overview page that ranks every branch by monthly revenue and rolls up
stock alerts, with a one-click "drill into this store" that sets the scope and opens
that store's dashboard.

| File | Change |
|------|--------|
| `backend/.../dto/dashboard/StoreKpiResponse.java` | **New** DTO: per-store KPI (revenue today/month, profit month, invoices today, low-stock, out-of-stock). |
| `backend/.../service/DashboardService.java` | **New** `getStoreComparison()` — iterates all stores, reuses existing per-store repository queries, sorts by monthly revenue. |
| `backend/.../controller/DashboardController.java` | **New** `GET /api/dashboard/stores` (`ADMIN` only). |
| `frontend/src/pages/dashboard/ChainOverview.jsx` | **New** page: chain KPI cards + per-store revenue bar chart + comparison table with drill-in. |
| `frontend/src/api/misc.js` | `dashboardApi.storeComparison()`. |

### 4.3 Admin access to operational oversight

| File | Change |
|------|--------|
| `frontend/src/components/navConfig.js` | Admin now sees **So sánh chi nhánh**, **ABC/XYZ**, **Ca làm việc**, **Tồn kho**, **Nhập kho**. |
| `frontend/src/App.jsx` | New `/chain` route; admin home → `/chain`; Inventory & ABC/XYZ wrapped in `StoreScopeRequired`. |
| `frontend/src/components/StoreScopeRequired.jsx` | **New** guard: pages that are inherently per-store (backend `requireStoreId()`) show a friendly "pick a branch" prompt when admin is in whole-chain scope, instead of throwing an API error. |

Note: Shifts and Goods-Receipts **list** views already support whole-chain reads, so they
work for admin in either scope; *creating* a goods receipt still requires a selected store
(by design).

---

## 5. Roadmap for the remaining gaps (not done yet)

Prioritized; each is independent.

1. **Cross-store inventory transfer** *(highest business value)* — a transfer document
   (source store → destination store) that decrements one store's stock and creates an
   incoming batch at the other, with audit + optional in-transit state. Requires a new
   `stock_transfer` entity/flow and UI; reuse the batch model.

2. **Report store-comparison view** — extend `/reports` with a "by store" breakdown table
   (revenue/profit per store for the selected period) and a per-store column in the Excel
   export. The scope selector already covers single-store reports.

3. **Per-store pricing & availability** — optional `store_product` override table
   (`store_id`, `product_id`, `sale_price?`, `active?`); falls back to the chain catalog
   when no override exists. Touches pricing in POS + catalog UI.

4. **Granular admin permissions** — split ADMIN into capability flags (stores, users,
   catalog, config) or add a permission set, so larger chains can delegate safely.

5. **Regional hierarchy** — a `region`/`zone` grouping for stores and a regional-manager
   tier that sees a subset of branches.

6. **Bulk operations** — apply a promotion / price change / Telegram config to multiple
   selected stores at once.

7. **Audit filtering by store/actor** — add `storeId`/`actor` filters to `/audit` for
   faster investigation in a large chain.

---

## 6. How to test the new admin capabilities

1. Log in as `admin` (chain admin).
2. Land on **So sánh chi nhánh** — verify all branches appear, ranked by monthly revenue,
   with stock-alert badges. Click a row → scope switches to that store and the dashboard
   opens scoped to it.
3. Use the top-bar **scope dropdown** to switch between "Toàn chuỗi" and individual
   branches; confirm Dashboard, Reports, Invoices, Shifts re-scope on change.
4. Open **Tồn kho** while on "Toàn chuỗi" → the "pick a branch" prompt shows; select a
   branch → the branch's inventory loads.
5. Log out → confirm the scope resets (no leftover `X-Store-Id`).
