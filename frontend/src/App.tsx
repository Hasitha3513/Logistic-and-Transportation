import { Navigate, Route, Routes } from 'react-router-dom';
import {
  DashboardOutlined,
} from '@ant-design/icons';
import AppLayout from './layout/AppLayout';
import DashboardPage from './pages/DashboardPage';
import ModulePage from './pages/ModulePage';
import ResourceListPage, { resourcePages } from './pages/ResourceListPage';
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
  if (hasPermission('IDENTITY_MANAGE')) return <Navigate to="/administration/users" replace />;
  return <Navigate to="/workspace" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route index element={<HomePage />} />
        <Route path="fleet/vehicles" element={<ResourceListPage {...resourcePages.vehicles} />} />
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
        <Route path="administration/users" element={<ResourceListPage {...resourcePages.users} />} />
        <Route path="administration/roles" element={<ResourceListPage {...resourcePages.roles} />} />
        <Route path="workspace" element={<ModulePage eyebrow="Operations" title="Workspace" description="Select an available module from the navigation." icon={<DashboardOutlined />} />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
