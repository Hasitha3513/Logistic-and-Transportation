import type { ReactNode } from 'react';
import {
  BellOutlined,
  CarOutlined,
  DashboardOutlined,
  EnvironmentOutlined,
  SafetyCertificateOutlined,
  ScheduleOutlined,
  SettingOutlined,
  TeamOutlined,
  UserOutlined,
  ThunderboltOutlined,
  ContainerOutlined,
} from '@ant-design/icons';

export interface NavigationItem {
  key: string;
  label: string;
  route?: string;
  icon?: ReactNode;
  requiredPermission?: string;
  children?: NavigationItem[];
}

export const navigation: NavigationItem[] = [
  {
    key: 'dashboard',
    label: 'Dashboard',
    route: '/',
    icon: <DashboardOutlined />,
    requiredPermission: 'DASHBOARD_VIEW',
  },
  {
    key: 'fleet',
    label: 'Fleet Management',
    icon: <CarOutlined />,
    children: [
      { key: 'fleet-vehicle-master', label: 'Vehicle Master', route: '/fleet/vehicles', requiredPermission: 'VEHICLE_VIEW' },
      {
        key: 'fleet-categories',
        label: 'Fleet Categories',
        children: [
          {
            key: 'vehicle-categories',
            label: 'Vehicle Categories',
            route: '/fleet/vehicle-categories',
            requiredPermission: 'VEHICLE_VIEW',
          },
          {
            key: 'vehicle-types',
            label: 'Vehicle Types',
            route: '/fleet/vehicle-types',
            requiredPermission: 'VEHICLE_VIEW',
          },
        ],
      },
    ],
  },
  {
    key: 'drivers',
    label: 'Drivers',
    icon: <UserOutlined />,
    children: [
      { key: 'driver-list', label: 'Drivers', route: '/drivers', requiredPermission: 'DRIVER_VIEW' },
    ],
  },
  {
    key: 'routes',
    label: 'Routes',
    icon: <EnvironmentOutlined />,
    children: [{ key: 'route-list', label: 'Routes', route: '/routes', requiredPermission: 'ROUTE_VIEW' }],
  },
  {
    key: 'trips',
    label: 'Trips',
    icon: <ScheduleOutlined />,
    children: [{ key: 'trip-list', label: 'Trips', route: '/trips', requiredPermission: 'TRIP_VIEW' }],
  },
  {
    key: 'fuel',
    label: 'Fuel Management',
    icon: <ThunderboltOutlined />,
    children: [
      { key: 'fuel-issues', label: 'Fuel Issues', route: '/fuel/issues', requiredPermission: 'FUEL_ISSUE_VIEW' },
      { key: 'fuel-purchases', label: 'Fuel Purchases', route: '/fuel/purchases', requiredPermission: 'FUEL_PURCHASE_VIEW' },
      { key: 'bunker-tanks', label: 'Bunker Tanks', route: '/fuel/bunker-tanks', requiredPermission: 'BUNKER_VIEW' },
      { key: 'fuel-prices', label: 'Fuel Prices', route: '/fuel/prices', requiredPermission: 'FUEL_PRICE_VIEW' },
    ],
  },
  {
    key: 'freight',
    label: 'Freight',
    icon: <ContainerOutlined />,
    children: [
      { key: 'freight-orders', label: 'Freight Orders', route: '/freight/orders', requiredPermission: 'FREIGHT_ORDER_VIEW' },
      { key: 'cargo-manifests', label: 'Cargo Manifests', route: '/freight/manifests', requiredPermission: 'CARGO_MANIFEST_VIEW' },
      { key: 'load-plans', label: 'Load Plans', route: '/freight/load-plans', requiredPermission: 'LOAD_PLAN_VIEW' },
      { key: 'freight-policies', label: 'Insurance Policies', route: '/freight/insurance/policies', requiredPermission: 'CARGO_INSURANCE_VIEW' },
      { key: 'freight-claims', label: 'Insurance Claims', route: '/freight/insurance/claims', requiredPermission: 'CARGO_INSURANCE_VIEW' },
    ],
  },
  {
    key: 'administration',
    label: 'Administration',
    icon: <SettingOutlined />,
    children: [
      { key: 'users', label: 'Users', route: '/administration/users', requiredPermission: 'IDENTITY_MANAGE', icon: <TeamOutlined /> },
      {
        key: 'roles',
        label: 'Roles',
        route: '/administration/roles',
        requiredPermission: 'IDENTITY_MANAGE',
        icon: <SafetyCertificateOutlined />,
      },
      {
        key: 'notification-rules',
        label: 'Notification Rules',
        route: '/notification-rules',
        requiredPermission: 'NOTIFICATION_RULE_VIEW',
        icon: <BellOutlined />,
      },
    ],
  },
];

export function permittedNavigation(items: NavigationItem[], permissions: string[]): NavigationItem[] {
  return items.flatMap((item) => {
    if (item.children) {
      const children = permittedNavigation(item.children, permissions);
      return children.length ? [{ ...item, children }] : [];
    }
    return !item.requiredPermission || permissions.includes(item.requiredPermission) ? [item] : [];
  });
}

export function findNavigationTrail(pathname: string, items: NavigationItem[] = navigation): NavigationItem[] {
  for (const item of items) {
    if (item.route === pathname || (item.route && item.route !== '/' && pathname.startsWith(`${item.route}/`))) {
      return [item];
    }
    const childTrail = item.children ? findNavigationTrail(pathname, item.children) : [];
    if (childTrail.length) return [item, ...childTrail];
  }
  return [];
}

export function findNavigationItem(pathname: string) {
  return findNavigationTrail(pathname).at(-1);
}
