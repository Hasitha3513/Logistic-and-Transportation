# UI Hardening Task Implementation Report: UI-HARDEN-001

**Task ID:** UI-HARDEN-001  
**Title:** Global Layout, Page Title, and Breadcrumb Ownership Alignment  
**Status:** COMPLETE  
**Date:** August 23, 2026  
**Author:** Senior Principal Frontend Engineer & Enterprise UI/UX Architect  

---

## 1. Problem Summary

Prior to this hardening, the enterprise shell ([`AppLayout.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/layout/AppLayout.tsx)) rendered the global page breadcrumb (`Home > [Parent] > [Page Title]`) and the authoritative page heading (`<Title level={2}>{pageTitle}</Title>`).

However, multiple child list page views simultaneously rendered their own `<Typography.Title level={3}>` elements:
- [`ResourceListPage.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/pages/ResourceListPage.tsx) (Vehicles, Vehicle Categories, Vehicle Types, Drivers, Routes, Users, Roles)
- [`TripListPage.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/trips/TripListPage.tsx)
- [`FuelIssueListPage.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/fuel/FuelIssueListPage.tsx)
- [`FuelPurchaseListPage.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/fuel/FuelPurchaseListPage.tsx)
- [`BunkerTankListPage.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/fuel/BunkerTankListPage.tsx)
- [`NotificationRulesPage.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/notifications/NotificationRulesPage.tsx)

In addition:
- [`BunkerTankDetailsPage.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/fuel/BunkerTankDetailsPage.tsx) rendered an in-page duplicate `<Breadcrumb>` beneath the global breadcrumb.
- Playwright page objects (`DriversPage`, `FleetVehiclesPage`, `RoutesPage`, `NotificationRulesPage`, `FuelIssuesPage`, `BunkerTanksPage`, `TripsPage`) had selector couplings to `.resource-list__title` or child page headings.

---

## 2. Ownership Rules Established

1. **Single Global Page Title Owner:**
   `AppLayout.tsx` is the sole authoritative owner of top-level page titles (`<Title level={2}>{pageTitle}</Title>`), dynamically derived from the route registry in `navigation.tsx`.
2. **Single Global Breadcrumb Owner:**
   `AppLayout.tsx` is the sole authoritative owner of hierarchical breadcrumb navigation (`Home > [Parent Module] > [Page Title]`).
3. **Child Page Content Subtitles & Descriptions:**
   Child views retain contextual subtitles, operational descriptions, and RBAC tags as secondary typography (`<Typography.Text type="secondary">` or `<Paragraph type="secondary">`) without duplicate `<Title>` tags.
4. **Record Detail & Section Headings:**
   Record-specific headings (e.g. `tank.tankCode` in `BunkerTankDetailsPage`, `trip.tripNumber` in `TripDetailsPage`) and internal section headings (e.g. `Operations Overview` on the Dashboard) are retained as legitimate content-level hierarchy.

---

## 3. Changes Made

### 3.1. Frontend Page Components
1. [`ResourceListPage.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/pages/ResourceListPage.tsx):
   - Removed duplicate `<Typography.Title level={3} className="resource-list__title">{title}</Typography.Title>`.
   - Preserved description text and full/read-only RBAC access badges in `<Space direction="vertical" size={4}>`.
2. [`TripListPage.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/trips/TripListPage.tsx):
   - Removed duplicate `<Title level={3} className="trip-list__title">Trip operations</Title>`.
   - Preserved operational description `<Text type="secondary">`.
3. [`FuelIssueListPage.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/fuel/FuelIssueListPage.tsx):
   - Removed duplicate `<Typography.Title level={3}>Fuel issues</Typography.Title>`.
   - Preserved operational dispensing description.
4. [`FuelPurchaseListPage.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/fuel/FuelPurchaseListPage.tsx):
   - Removed duplicate `<Typography.Title level={3}>Fuel purchases</Typography.Title>`.
   - Preserved invoice/reconciliation tracking description.
5. [`BunkerTankListPage.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/fuel/BunkerTankListPage.tsx):
   - Removed duplicate `<Title level={3}>Bunker Tanks</Title>`.
   - Preserved bulk depot storage description.
6. [`BunkerTankDetailsPage.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/fuel/BunkerTankDetailsPage.tsx):
   - Removed duplicate page-level `<Breadcrumb>` component.
   - Retained record detail heading `<Title level={3}>{tank.tankCode}</Title>`.
7. [`NotificationRulesPage.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/notifications/NotificationRulesPage.tsx):
   - Removed duplicate `<Title level={3}>Notification Rules & Alerting</Title>`.
   - Preserved event triggers and routing configuration paragraph.
8. [`ResourceEditorModal.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/pages/ResourceEditorModal.tsx):
   - Migrated dependent dropdown watch logic from `form.watch` to `useWatch` hook to ensure 100% clean React Compiler / ESLint pass without warnings.

### 3.2. Playwright Page Objects & Tests Migrated to Accessible Shell Locators
- [`DriversPage.ts`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/e2e/pages/DriversPage.ts): `this.page.getByRole('heading', { name: 'Drivers', level: 2 })`
- [`FleetVehiclesPage.ts`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/e2e/pages/FleetVehiclesPage.ts): `this.page.getByRole('heading', { name: 'Vehicles', level: 2 })`
- [`RoutesPage.ts`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/e2e/pages/RoutesPage.ts): `this.page.getByRole('heading', { name: 'Routes', level: 2 })`
- [`NotificationRulesPage.ts`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/e2e/pages/NotificationRulesPage.ts): `this.page.getByRole('heading', { name: 'Notification Rules', level: 2 })`
- [`FuelIssuesPage.ts`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/e2e/pages/FuelIssuesPage.ts): `this.page.getByRole('heading', { name: 'Fuel Issues', level: 2 })`
- [`BunkerTanksPage.ts`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/e2e/pages/BunkerTanksPage.ts): `this.page.getByRole('heading', { name: 'Bunker Tanks', level: 2 })`
- [`TripsPage.ts`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/e2e/pages/TripsPage.ts): `this.page.getByRole('heading', { name: 'Trips', level: 2 })`
- [`driver.spec.ts`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/test/driver.spec.ts): `page.getByRole('heading', { name: 'Drivers', level: 2 })`
- [`vehicle.spec.ts`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/test/vehicle.spec.ts): `page.getByRole('heading', { name: 'Vehicles', level: 2 })`
- [`AppLayout.test.tsx`](file:///d:/transport-logistics-modulith/transport-logistics-modulith/frontend/src/layout/AppLayout.test.tsx): Verified single heading level 2 per page.

---

## 4. Verification Results

### 4.1. Static Analysis & Build Gates
- **ESLint (`npm run lint`):** PASS (0 errors, 0 warnings with `--max-warnings=0`)
- **Production Build (`npm run build`):** PASS (`tsc -b && vite build` completed in 33.5s)

### 4.2. Vitest Unit & Integration Suite
- **Command:** `npm test`
- **Result:** PASS
- **Test Files:** 33 passed / 33 total
- **Tests:** 170 passed / 170 total

### 4.3. Playwright End-to-End Test Suite
- **Smoke Suite (`npm run test:e2e:smoke`):** 9 passed / 9 total (100%)
- **Focused Suites (Vehicles, Drivers, Routes, Notification Rules, Policies):** 100% passed across Chromium, Firefox, WebKit
- **Full Chromium Suite (`npm run test:e2e:chromium`):** 69 passed / 69 total (100%)
- **Full WebKit Suite (`npm run test:e2e:webkit`):** 69 passed / 69 total (100%)
- **Full Firefox Suite (`npm run test:e2e:firefox`):** 69 passed / 69 total (100%)

---

## 5. Next Steps

- Proceed to **`UI-HARDEN-002`**: Padding & Wrapper Normalization (clean up redundant `<div style={{ padding: 24 }}>` wrappers in Bunker and Notification modules).
