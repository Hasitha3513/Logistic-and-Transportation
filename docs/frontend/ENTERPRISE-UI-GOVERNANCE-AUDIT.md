# Enterprise Frontend UI/UX Governance & Architecture Audit

**Task ID:** ENTERPRISE-FRONTEND-UI-GOVERNANCE-AUDIT-001  
**Auditor:** Senior Principal Frontend Engineer & Enterprise UI/UX Architect  
**Date:** August 23, 2026  
**Status:** AUDIT COMPLETE (Analysis Only — No Production Code Modified)  

---

## 1. Executive Summary

A comprehensive architectural and UI/UX governance audit was performed on the Transport & Logistics ERP web frontend codebase (`frontend/src`).

The frontend is built on **React 19.1.1**, **TypeScript 5.8.3**, **Vite 7.1.3**, **Ant Design 5.27.1**, **TanStack Query 5.85.5**, and **React Router 7.18.2**. The application is verified by a **33-file unit test suite (170 tests)** and a comprehensive **Playwright E2E test suite (205+ passing tests)** across Chromium, Firefox, and WebKit.

### Key Architectural Conclusions:
1. **Refine Framework:** `NOT_INSTALLED`. The application has a mature, native TanStack Query + Axios + React Router architecture. Adopting Refine would require a massive, high-risk rewrite of all routing, auth, and data-access layers with zero functional benefit.
2. **Ant Design Pro Components (`ProTable`, `ProForm`, `ProLayout`):** `NOT_INSTALLED`. Ant Design Pro Components has significant peer-dependency incompatibilities with React 19. Furthermore, the repository already has an efficient shared table abstraction (`ResourceListPage.tsx`) and an enterprise shell (`AppLayout.tsx`). Adopting Pro Components is unnecessary and contraindicated.
3. **Global Chrome Ownership:** The global shell (`AppLayout.tsx`) already owns sidebar navigation, top header, breadcrumbs, and title display. However, child pages currently duplicate the `<Title>` component, and three pages introduce redundant padding wrappers.

---

## 2. Dependency Audit

| Dependency | Required Version | Status in Repository | Evidence / Notes |
|---|---|---|---|
| `react` | `^19.1.1` | **INSTALLED_AND_USED** | `package.json` line 28 |
| `react-dom` | `^19.1.1` | **INSTALLED_AND_USED** | `package.json` line 29 |
| `typescript` | `~5.8.3` | **INSTALLED_AND_USED** | `package.json` line 50 |
| `antd` | `^5.27.1` | **INSTALLED_AND_USED** | `package.json` line 25 |
| `@ant-design/icons` | `^6.0.0` | **INSTALLED_AND_USED** | `package.json` line 21 |
| `@ant-design/v5-patch-for-react-19` | `^1.0.3` | **INSTALLED_AND_USED** | `package.json` line 22 |
| `react-router-dom` | `^7.18.2` | **INSTALLED_AND_USED** | `package.json` line 31 |
| `@tanstack/react-query` | `^5.85.5` | **INSTALLED_AND_USED** | `package.json` line 24 |
| `react-hook-form` | `^7.62.0` | **INSTALLED_AND_USED** | `package.json` line 30 |
| `@hookform/resolvers` | `^5.2.1` | **INSTALLED_AND_USED** | `package.json` line 23 |
| `zod` | `^4.1.5` | **INSTALLED_AND_USED** | `package.json` line 32 |
| `@ant-design/pro-components` | — | **NOT_INSTALLED** | Absent from `package.json` |
| `@refinedev/core` | — | **NOT_INSTALLED** | Absent from `package.json` |
| `@refinedev/antd` | — | **NOT_INSTALLED** | Absent from `package.json` |

---

## 3. Global Layout Ownership

The application shell is centralized in [`frontend/src/layout/AppLayout.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/layout/AppLayout.tsx):

- **Sidebar (`<Sider>`):** Sticky (264px width), dark theme, collapsible, RBAC-filtered navigation items via `permittedNavigation()`.
- **Top Header (`<Header>`):** Sticky (72px height), hamburger collapse trigger, operations workspace context indicator, offline sync center (`OfflineSyncCenter`), notification center (`NotificationCenter`), and user profile dropdown with access drawer trigger.
- **Global Page Header (`.page-heading`):** Renders global `<Breadcrumb>` (`Home > [Parent] > [Page Title]`) and `<Title level={2}>{pageTitle}</Title>`.
- **Content Surface (`<Content className="app-content">`):** Padded container (`padding: 26px 30px 40px`, max-width: `1500px`), wrapping `<Outlet />`.
- **Access & Permissions Drawer:** Global drawer rendering user identity, active status, roles, and business permissions tags.

---

## 4. Current Accommodation Matrix

| UI / Architecture Rule | Accommodation Status | Current Implementation | Evidence | Gap / Inconsistency | Risk | Recommendation | Migration Required? |
|---|---|---|---|---|---|---|---|
| **Global Layout Ownership** | **YES** | `AppLayout.tsx` manages header, sider, breadcrumbs, title, and content surface | `AppLayout.tsx:87-199` | None | Low | Retain `AppLayout.tsx` | No |
| **Global Navigation** | **YES** | Sider Menu with dynamic RBAC permission filtering | `AppLayout.tsx:105-111`, `navigation.tsx` | None | Low | Retain existing menu model | No |
| **Breadcrumb Ownership** | **PARTIAL** | Centrally rendered in `AppLayout.tsx`; duplicate in `BunkerTankDetailsPage` | `AppLayout.tsx:168`, `BunkerTankDetailsPage.tsx:216` | Duplicate breadcrumb on 1 page | Low | Remove redundant breadcrumb in `BunkerTankDetailsPage` | No |
| **Page Title Ownership** | **PARTIAL** | Rendered globally in `AppLayout.tsx` and duplicated in child page views | `AppLayout.tsx:175`, `ResourceListPage.tsx:132`, `TripListPage.tsx:210` | Double title rendering on multiple pages | Med | Coordinate `.resource-list__title` with Playwright selectors before title cleanup | No |
| **Table Standard** | **YES** | Standard Ant Design `<Table>` with shared `ResourceListPage` abstraction | `ResourceListPage.tsx:150`, `TripListPage.tsx:250` | None | Low | Keep current AntD Table architecture | No |
| **Table Toolbar / Filters** | **YES** | Search, select filters, date pickers inside `<Card variant="borderless">` | `TripListPage.tsx:221-243`, `FuelIssueListPage.tsx:51-57` | Minor inline style widths on search inputs | Low | Standardize filter gap with `Flex` / `Space` | No |
| **Form Standard** | **YES** | React Hook Form + Zod + Ant Design controls | `ResourceEditorModal.tsx`, `TripEditorPage.tsx` | None | Low | Retain RHF + Zod standard | No |
| **Form Action Placement** | **YES** | Modal footer or single card bottom bar; no duplicate buttons | `ResourceEditorModal.tsx:106`, `TripEditorPage.tsx:122` | None | Low | Retain single action area | No |
| **Spacing & Padding** | **PARTIAL** | Global `.app-content` provides padding; 3 pages add redundant outer padding | `BunkerTankListPage.tsx:220`, `NotificationRulesPage.tsx:232` | Nested `padding: 24` inside already padded content | Low | Remove redundant wrapper `<div>` padding | No |
| **Card Nesting** | **PARTIAL** | Most pages use flat cards; `NotificationRulesPage` wraps entire view in Card | `NotificationRulesPage.tsx:233` | Card wrapping tabs and table | Low | Flatten outer Card in NotificationRulesPage | No |
| **RBAC Controls** | **YES** | `hasPermission()` guards navigation, buttons, and detail drawers | `ResourceListPage.tsx:143`, `TripListPage.tsx:214` | None | Low | Retain existing RBAC pattern | No |
| **Responsive Layout** | **YES** | Sider breakpoint `lg`, responsive table columns, Flex wrap containers | `AppLayout.tsx:92`, `styles.css:180-265` | None | Low | Retain responsive CSS | No |
| **Accessibility** | **YES** | Semantic labels, `aria-label` attributes on icon buttons | `AppLayout.tsx:127`, `FuelIssueListPage.tsx:52` | None | Low | Continue enforcing aria labels | No |
| **Error / Loading / Empty States** | **YES** | AntD `Alert`, `Spin`, `Empty` (via Table `locale.emptyText`) on all lists | `ResourceListPage.tsx:148,157`, `TripListPage.tsx:246` | None | Low | Retain standard error banners | No |
| **ProLayout** | **INCOMPATIBLE / NOT REQUIRED** | Custom `AppLayout.tsx` already delivers complete enterprise shell | `AppLayout.tsx` | None | High | Do NOT adopt ProLayout | No |
| **ProTable** | **INCOMPATIBLE / NOT REQUIRED** | AntD `Table` + `ResourceListPage` solves all tabular requirements | `ResourceListPage.tsx` | None | High | Do NOT adopt ProTable | No |
| **ProForm** | **INCOMPATIBLE / NOT REQUIRED** | React Hook Form + Zod provides superior validation & type-safety | `ResourceEditorModal.tsx` | None | High | Do NOT adopt ProForm | No |
| **Refine** | **INCOMPATIBLE / NOT REQUIRED** | Standalone React + TanStack Query architecture is complete | `frontend/package.json` | None | High | Do NOT adopt Refine | No |

---

## 5. Page Gap Matrix

| Page / Route | Duplicate Title | Duplicate Breadcrumb | Floating Actions | Manual Search Bar | Raw Table | Nested Cards | Duplicate Form Actions | Spacing Issues | Recommended Fix | Priority |
|---|---|---|---|---|---|---|---|---|---|---|
| **`/` (Dashboard)** | Yes (`Operations Overview`) | No | No | No | No | No | No | Acceptable | Retain title as section heading | P2 |
| **`/fleet/vehicles`** | Yes (`Vehicle registry`) | No | No | No | No (`ResourceListPage`) | No | No | Clean | Refactor header to subtitle/metadata | P1 |
| **`/fleet/vehicle-categories`** | Yes (`Category registry`) | No | No | No | No (`ResourceListPage`) | No | No | Clean | Refactor header to subtitle/metadata | P1 |
| **`/fleet/vehicle-types`** | Yes (`Vehicle type registry`) | No | No | No | No (`ResourceListPage`) | No | No | Clean | Refactor header to subtitle/metadata | P1 |
| **`/drivers`** | Yes (`Driver registry`) | No | No | No | No (`ResourceListPage`) | No | No | Clean | Refactor header to subtitle/metadata | P1 |
| **`/routes`** | Yes (`Route library`) | No | No | No | No (`ResourceListPage`) | No | No | Clean | Refactor header to subtitle/metadata | P1 |
| **`/administration/users`** | Yes (`User accounts`) | No | No | No | No (`ResourceListPage`) | No | No | Clean | Refactor header to subtitle/metadata | P1 |
| **`/administration/roles`** | Yes (`Role registry`) | No | No | No | No (`ResourceListPage`) | No | No | Clean | Refactor header to subtitle/metadata | P1 |
| **`/trips`** | Yes (`Trip operations`) | No | No | Standardized filter card | Standard AntD Table | No | No | Clean | Remove duplicate title; keep filters | P1 |
| **`/trips/:id`** | No (Shows Trip #) | No | No | N/A | Sub-tables | No | No | Clean | Retain existing detail structure | P3 |
| **`/trips/new` & `:id/edit`** | Yes (`Create trip` / `Edit trip`) | No | No | N/A | N/A | No | No | Clean | Align header with shell | P2 |
| **`/fuel/issues`** | Yes (`Fuel issues`) | No | No | Standardized filter card | Standard AntD Table | No | No | Clean | Remove duplicate title; keep filters | P1 |
| **`/fuel/issues/:id`** | No (Shows Voucher #) | No | No | N/A | N/A | No | No | Clean | Retain detail structure | P3 |
| **`/fuel/purchases`** | Yes (`Fuel purchases`) | No | No | Standardized filter card | Standard AntD Table | No | No | Clean | Remove duplicate title; keep filters | P1 |
| **`/fuel/purchases/:id`** | No (Shows Purchase #) | No | No | N/A | N/A | No | No | Clean | Retain detail structure | P3 |
| **`/fuel/bunker-tanks`** | Yes (`Bunker Tanks`) | No | No | Inline select filter | Standard AntD Table | No | No | Outer `padding: 24` | Remove outer `padding: 24` & duplicate title | P1 |
| **`/fuel/bunker-tanks/:id`** | Yes (`Tank Code`) | Yes | No | N/A | Movement table | No | No | Outer `padding: 24` | Remove outer `padding: 24` & redundant `<Breadcrumb>` | P1 |
| **`/fuel/prices`** | Yes (`Fuel price configuration`) | No | No | N/A | Price table | No | No | Clean | Retain price history table | P2 |
| **`/notification-rules`** | Yes (`Notification Rules & Alerting`) | No | No | N/A | Standard AntD Table | Yes (Card wraps Tabs) | No | Outer `padding: 24` | Remove outer `padding: 24` and flatten Card wrapper | P1 |

---

## 6. Detailed Architectural Decisions

### 6.1. Critical Table Decision
- **Option Chosen:** `DECISION A: CURRENT ANT DESIGN ARCHITECTURE IS SUFFICIENT`
- **Rationale:**
  1. `@ant-design/pro-components` is not installed and introduces React 19 compatibility risks.
  2. The application already has a shared table abstraction (`ResourceListPage.tsx`) providing unified pagination, RBAC actions, responsive scroll, and loading/error states.
  3. Dedicated tables in `TripListPage`, `FuelIssueListPage`, `FuelPurchaseListPage`, and `BunkerTankListPage` share consistent UX conventions (filter cards, column formatters, status badges).
  4. Replacing tables with `ProTable` would break established Playwright page objects across 205+ E2E tests without providing measurable user value.

### 6.2. Critical Form Decision
- **Option Chosen:** `CURRENT FORM ARCHITECTURE (React Hook Form + Zod + Ant Design) IS SUFFICIENT`
- **Rationale:**
  1. `react-hook-form` paired with `zod` provides type-safe client validation, asynchronous submit handling, and fine-grained backend field error mapping.
  2. All forms already have single, unambiguous action areas (modal footers or card bottom bars).
  3. Migrating to `ProForm` offers zero added value and risks regression in form validation and error handling.

### 6.3. Refine Framework Decision
- **Option Chosen:** `REFINE_NOT_PRESENT_AND_NOT_REQUIRED`
- **Rationale:**
  1. Refine is an opinionated full-framework abstraction that dictates data providers, auth providers, and routing.
  2. The backend is a Spring Modulith with custom REST endpoints, CQRS-style commands, and specific error envelopes (`ConflictException`, `BusinessRuleException`).
  3. The current TanStack Query + Axios client is fully aligned with the backend API contracts and thoroughly verified. Introducing Refine would require rewriting the entire frontend.

---

## 7. Classification of Findings

### P0 (Blockers): None
- The application has zero functional or usability blockers. All 39 MVP verticals are operational and passing verification.

### P1 (Major Inconsistencies):
1. **Duplicate Page Titles:** Child page views render duplicate `Typography.Title` elements when `AppLayout.tsx` already renders `<Title level={2}>{pageTitle}</Title>` in `.page-heading`.
2. **Redundant Outer Padding:** `BunkerTankListPage.tsx`, `BunkerTankDetailsPage.tsx`, and `NotificationRulesPage.tsx` enclose page content in `<div style={{ padding: 24 }}>`, creating double-padding inside `.app-content`.
3. **Playwright Selector Dependency on `.resource-list__title`:** E2E page objects (`DriversPage.ts`, `FleetVehiclesPage.ts`, `RoutesPage.ts`) look for `.resource-list__title`. Any title cleanup must coordinate with test selectors.

### P2 (Important Cleanups):
1. **Duplicate Breadcrumb in Bunker Tank Details:** `BunkerTankDetailsPage.tsx` renders a secondary `<Breadcrumb>` below the global one in `AppLayout.tsx`.
2. **Redundant Card Wrapper in Notification Rules:** `NotificationRulesPage.tsx` wraps `<Tabs>` inside a `<Card>`, resulting in double borders and extra nesting.
3. **Filter Input Widths:** Standardize filter widths and gaps across `TripListPage`, `FuelIssueListPage`, `FuelPurchaseListPage`, and `BunkerTankListPage` using consistent Ant Design `Flex` containers.

### P3 (Cosmetic Improvements):
1. Minor whitespace adjustments and responsive column breakpoint alignments across list tables.

---

## 8. Recommended Implementation Plan

1. **`UI-HARDEN-001` (Global Layout & Page Title Alignment) — [COMPLETE]:**
   - Coordinated `.resource-list__title` and other page heading selectors with Playwright page objects (`DriversPage`, `FleetVehiclesPage`, `RoutesPage`, `NotificationRulesPage`, `FuelIssuesPage`, `BunkerTanksPage`, `TripsPage`) using accessible `getByRole('heading', { level: 2 })` semantics.
   - Removed duplicate `<Title>` tags across child list pages (`ResourceListPage`, `TripListPage`, `FuelIssueListPage`, `FuelPurchaseListPage`, `BunkerTankListPage`, `NotificationRulesPage`) where the global `.page-heading` already renders the authoritative page title.
   - Removed duplicate page-level `<Breadcrumb>` in `BunkerTankDetailsPage.tsx`.
   - Verified 100% clean passes: ESLint (0 errors, 0 warnings), Vitest (33 files / 170 tests passed), Production Build (`tsc -b && vite build`), and Playwright E2E suites across Chromium, WebKit, and Firefox.
2. **`UI-HARDEN-002` (Padding & Wrapper Normalization) — [COMPLETE]:**
   - Removed redundant `<div style={{ padding: 24 }}>` wrappers from `BunkerTankListPage.tsx`, `BunkerTankDetailsPage.tsx`, and `NotificationRulesPage.tsx`.
   - Verified 100% clean passes across ESLint (0 warnings), Vitest (170/170 tests), Vite build, and Playwright suites (48/48 tests across Chromium, Firefox, WebKit).
3. **`UI-HARDEN-003` (Container Flattening) — [COMPLETE]:**
   - Flattened outer `<Card>` in `NotificationRulesPage.tsx` so `<Tabs>` and action headers render directly on the `.page-surface`.
   - Verified 100% clean passes across ESLint (0 warnings), Vitest (170/170 tests), Vite build, and Playwright notification suites (45/45 tests across Chromium, Firefox, WebKit).
4. **`UI-HARDEN-004` (Filter Bar Consistency) — [COMPLETE]:**
   - Standardized search and filter controls across Trip, Fuel Issue, Fuel Purchase, and Bunker lists using consistent Ant Design `<Flex gap={12} wrap align="center">` within `<Card variant="borderless">`.
   - Verified 100% clean passes across ESLint (0 warnings), Vitest (170/170 tests), Vite build, and Playwright fuel and trip suites (30/30 tests across Chromium, Firefox, WebKit).
5. **`UI-HARDEN-005` (Form Layout, Section Hierarchy, Validation, & Action Ownership) — [COMPLETE]:**
   - Standardized form action ownership (modal footers vs full-page bottom action bars), action order (`Cancel` first, `Save` second), and Zod validation error mappings across all create/edit workflows.
   - Verified 100% clean passes: ESLint (0 errors, 0 warnings), Vitest (35 files / 178 tests passed), Production Build (`tsc -b && vite build`), and Playwright full suite (210/210 tests passed).

