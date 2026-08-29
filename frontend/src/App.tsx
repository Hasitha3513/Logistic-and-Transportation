import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './layout/AppLayout';
import DashboardPage from './pages/DashboardPage';
import WorkspacePage from './pages/WorkspacePage';
import ResourceListPage, { resourcePages } from './pages/ResourceListPage';
import VehicleListPage from './features/fleet/vehicleMaster/pages/VehicleListPage';
import TripListPage from './trips/TripListPage';
import TripDetailsPage from './trips/TripDetailsPage';
import TripEditorPage from './trips/TripEditorPage';
import LoginPage from './auth/LoginPage';
import { useAuth } from './auth/AuthContext';
import FuelIssueListPage from './fuel/FuelIssueListPage';
import FuelIssueEditorPage from './fuel/FuelIssueEditorPage';
import FuelIssueDetailsPage from './fuel/FuelIssueDetailsPage';
import FuelPurchaseListPage from './fuel/FuelPurchaseListPage';
import FuelPurchaseEditorPage from './fuel/FuelPurchaseEditorPage';
import FuelPurchaseDetailsPage from './fuel/FuelPurchaseDetailsPage';
import FuelPricePage from './fuel/FuelPricePage';
import BunkerTankListPage from './fuel/BunkerTankListPage';
import BunkerTankDetailsPage from './fuel/BunkerTankDetailsPage';
import NotificationRulesPage from './notifications/NotificationRulesPage';
import FreightOrderListPage from './features/freight/orders/pages/FreightOrderListPage';
import FreightOrderFormPage from './features/freight/orders/pages/FreightOrderFormPage';
import FreightOrderDetailsPage from './features/freight/orders/pages/FreightOrderDetailsPage';
import CargoManifestListPage from './features/freight/manifests/pages/CargoManifestListPage';
import CargoManifestCreatePage from './features/freight/manifests/pages/CargoManifestCreatePage';
import CargoManifestDetailsPage from './features/freight/manifests/pages/CargoManifestDetailsPage';
import LoadPlanListPage from './features/freight/loadPlanning/pages/LoadPlanListPage';
import LoadPlanCreatePage from './features/freight/loadPlanning/pages/LoadPlanCreatePage';
import LoadPlanDetailsPage from './features/freight/loadPlanning/pages/LoadPlanDetailsPage';
import { PolicyListPage } from './features/freight/insurance/pages/PolicyListPage';
import { PolicyCreatePage } from './features/freight/insurance/pages/PolicyCreatePage';
import { PolicyDetailsPage } from './features/freight/insurance/pages/PolicyDetailsPage';
import { ClaimListPage } from './features/freight/insurance/pages/ClaimListPage';
import { ClaimCreatePage } from './features/freight/insurance/pages/ClaimCreatePage';
import { ClaimDetailsPage } from './features/freight/insurance/pages/ClaimDetailsPage';
import { CargoExceptionListPage } from './features/freight/exceptions/pages/CargoExceptionListPage';
import { CargoExceptionCreatePage } from './features/freight/exceptions/pages/CargoExceptionCreatePage';
import { CargoExceptionDetailsPage } from './features/freight/exceptions/pages/CargoExceptionDetailsPage';
import FreightReportsPage from './features/freight/reports/pages/FreightReportsPage';
import DeliveryOrderListPage from './features/delivery/orders/pages/DeliveryOrderListPage';
import DeliveryOrderFormPage from './features/delivery/orders/pages/DeliveryOrderFormPage';
import DeliveryOrderDetailsPage from './features/delivery/orders/pages/DeliveryOrderDetailsPage';

function ProtectedRoute() {
  const { user, isLoading } = useAuth();
  if (isLoading) return null;
  return user ? <AppLayout /> : <Navigate to="/login" replace />;
}

function HomePage() {
  const { hasPermission } = useAuth();
  if (hasPermission('DASHBOARD_VIEW')) return <DashboardPage />;
  if (hasPermission('VEHICLE_VIEW')) return <Navigate to="/fleet/vehicles" replace />;
  if (hasPermission('DRIVER_VIEW')) return <Navigate to="/drivers" replace />;
  if (hasPermission('ROUTE_VIEW')) return <Navigate to="/routes" replace />;
  if (hasPermission('TRIP_VIEW')) return <Navigate to="/trips" replace />;
  if (hasPermission('FUEL_ISSUE_VIEW')) return <Navigate to="/fuel/issues" replace />;
  if (hasPermission('FUEL_PURCHASE_VIEW')) return <Navigate to="/fuel/purchases" replace />;
  if (hasPermission('BUNKER_VIEW')) return <Navigate to="/fuel/bunker-tanks" replace />;
  if (hasPermission('FUEL_PRICE_VIEW')) return <Navigate to="/fuel/prices" replace />;
  if (hasPermission('FREIGHT_ORDER_VIEW')) return <Navigate to="/freight/orders" replace />;
  if (hasPermission('CARGO_MANIFEST_VIEW')) return <Navigate to="/freight/manifests" replace />;
  if (hasPermission('LOAD_PLAN_VIEW')) return <Navigate to="/freight/load-plans" replace />;
  if (hasPermission('CARGO_INSURANCE_VIEW')) return <Navigate to="/freight/insurance/policies" replace />;
  if (hasPermission('CARGO_EXCEPTION_VIEW')) return <Navigate to="/freight/exceptions" replace />;
  if (hasPermission('FREIGHT_REPORT_VIEW')) return <Navigate to="/freight/reports" replace />;
  if (hasPermission('DELIVERY_VIEW')) return <Navigate to="/deliveries" replace />;
  if (hasPermission('IDENTITY_MANAGE')) return <Navigate to="/administration/users" replace />;
  return <Navigate to="/workspace" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route index element={<HomePage />} />
        <Route path="fleet/vehicles" element={<VehicleListPage />} />
        <Route path="fleet/vehicle-categories" element={<ResourceListPage {...resourcePages.categories} />} />
        <Route path="fleet/vehicle-types" element={<ResourceListPage {...resourcePages.types} />} />
        <Route path="drivers" element={<ResourceListPage {...resourcePages.drivers} />} />
        <Route path="routes" element={<ResourceListPage {...resourcePages.routes} />} />
        <Route path="trips" element={<TripListPage />} />
        <Route path="trips/new" element={<TripEditorPage />} />
        <Route path="trips/:tripId/edit" element={<TripEditorPage />} />
        <Route path="trips/:tripId" element={<TripDetailsPage />} />
        <Route path="fuel/issues" element={<FuelIssueListPage />} />
        <Route path="fuel/issues/new" element={<FuelIssueEditorPage />} />
        <Route path="fuel/issues/:fuelIssueId/edit" element={<FuelIssueEditorPage />} />
        <Route path="fuel/issues/:fuelIssueId" element={<FuelIssueDetailsPage />} />
        <Route path="fuel/purchases" element={<FuelPurchaseListPage />} />
        <Route path="fuel/purchases/new" element={<FuelPurchaseEditorPage />} />
        <Route path="fuel/purchases/:fuelPurchaseId/edit" element={<FuelPurchaseEditorPage />} />
        <Route path="fuel/purchases/:fuelPurchaseId" element={<FuelPurchaseDetailsPage />} />
        <Route path="fuel/bunker-tanks" element={<BunkerTankListPage />} />
        <Route path="fuel/bunker-tanks/:bunkerTankId" element={<BunkerTankDetailsPage />} />
        <Route path="fuel/prices" element={<FuelPricePage />} />
        <Route path="freight/orders" element={<FreightOrderListPage />} />
        <Route path="freight/orders/new" element={<FreightOrderFormPage />} />
        <Route path="freight/orders/:freightOrderId/edit" element={<FreightOrderFormPage />} />
        <Route path="freight/orders/:freightOrderId" element={<FreightOrderDetailsPage />} />
        <Route path="freight/manifests" element={<CargoManifestListPage />} />
        <Route path="freight/manifests/new" element={<CargoManifestCreatePage />} />
        <Route path="freight/manifests/:cargoManifestId/edit" element={<CargoManifestDetailsPage />} />
        <Route path="freight/manifests/:cargoManifestId" element={<CargoManifestDetailsPage />} />
        <Route path="freight/load-plans" element={<LoadPlanListPage />} />
        <Route path="freight/load-plans/new" element={<LoadPlanCreatePage />} />
        <Route path="freight/load-plans/:id" element={<LoadPlanDetailsPage />} />
        <Route path="freight/insurance/policies" element={<PolicyListPage />} />
        <Route path="freight/insurance/policies/new" element={<PolicyCreatePage />} />
        <Route path="freight/insurance/policies/:id" element={<PolicyDetailsPage />} />
        <Route path="freight/insurance/claims" element={<ClaimListPage />} />
        <Route path="freight/insurance/claims/new" element={<ClaimCreatePage />} />
        <Route path="freight/insurance/claims/:id" element={<ClaimDetailsPage />} />
        <Route path="freight/exceptions" element={<CargoExceptionListPage />} />
        <Route path="freight/exceptions/new" element={<CargoExceptionCreatePage />} />
        <Route path="freight/exceptions/:id" element={<CargoExceptionDetailsPage />} />
        <Route path="freight/reports" element={<FreightReportsPage />} />
        <Route path="deliveries" element={<DeliveryOrderListPage />} />
        <Route path="deliveries/new" element={<DeliveryOrderFormPage />} />
        <Route path="deliveries/:deliveryId/edit" element={<DeliveryOrderFormPage />} />
        <Route path="deliveries/:deliveryId" element={<DeliveryOrderDetailsPage />} />
        <Route path="administration/users" element={<ResourceListPage {...resourcePages.users} />} />
        <Route path="administration/roles" element={<ResourceListPage {...resourcePages.roles} />} />
        <Route path="notification-rules" element={<NotificationRulesPage />} />
        <Route path="workspace" element={<WorkspacePage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
