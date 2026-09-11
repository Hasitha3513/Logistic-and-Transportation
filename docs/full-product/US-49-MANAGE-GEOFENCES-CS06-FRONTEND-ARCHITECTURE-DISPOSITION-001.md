# US-49 CS06 Frontend Architecture Disposition

**Task:** `US-49-MANAGE-GEOFENCES-CS06-FRONTEND-ARCHITECTURE-DISPOSITION-001`  
**Decision:** `APPROVED_EXISTING_FRONTEND_STACK`  
**Date:** 2026-09-11

## Original conflict

The original CS06 task required Refine, `@refinedev/antd`, and Ant Design Pro Components while also prohibiting dependency changes. Those packages and abstractions do not exist in the repository. Introducing them would be a frontend-platform migration and would conflict with the repository rule that prohibits adding Refine or Ant Design Pro Components without separate architecture approval.

## Inspected repository evidence

- `frontend/package.json` and `frontend/package-lock.json` contain React 19, React Router, Ant Design, TanStack Query, React Hook Form, Zod, Axios, Vitest, React Testing Library, and Playwright.
- Refine, `@refinedev/antd`, and `@ant-design/pro-components` are absent.
- `frontend/src/main.tsx` composes `BrowserRouter`, `QueryClientProvider`, `AuthProvider`, Ant Design configuration, and the existing application providers.
- `frontend/src/App.tsx` owns the React Router route tree and protects the authenticated branch through `AppLayout`.
- `frontend/src/layout/AppLayout.tsx` is the sole owner of sidebar navigation, header, breadcrumb/page context, and global content layout.
- `frontend/src/auth/AuthContext.tsx` exposes authenticated user state and permission-code checks backed by the server-provided permission set.
- Existing Tracking pages use feature-owned typed API/hooks, TanStack Query, Ant Design tables/forms/drawers/modals, React Router navigation, and `AuthContext` permission checks.

## Approved CS06 stack

US-49 CS06 must use:

- React 19 and TypeScript;
- existing React Router routes;
- Ant Design and established internal UI abstractions;
- TanStack Query for server state, mutations, and scoped invalidation;
- React Hook Form with Zod for forms and client validation;
- the shared Axios client;
- `AuthContext` and existing permission helpers;
- Vitest, React Testing Library, and Playwright.

Refine, `@refinedev/antd`, Ant Design Pro Components, Material UI, another router, another global-state framework, and new frontend dependencies are not approved.

## Preserved requirements

This disposition changes no US-49 product behavior. CS06 still must implement the accessible polygon vertex editor and local preview, list/detail/create/edit/lifecycle workflows, stable memberships, transition and unauthorized-transition history, exact `GEOFENCE_VIEW`, `GEOFENCE_MANAGE`, and `GEOFENCE_EVENT_VIEW` affordances, optimistic-version and idempotency behavior, Tenant-safe forms, privacy-minimized display, real PostgreSQL-backed Chromium evidence, and the existing `AppLayout` ownership rule.

Backend APIs, public contracts, permissions, event/Notification behavior, Flyway V79, US-49 status, US-48 external hold, and 72/87 accounting remain unchanged.

## Next task

`US-49-MANAGE-GEOFENCES-CS06-FRONTEND-001-RERUN`
