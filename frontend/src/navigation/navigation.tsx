import type { ReactNode } from 'react';
import {
  CarOutlined,
  DashboardOutlined,
  EnvironmentOutlined,
  SafetyCertificateOutlined,
  ScheduleOutlined,
  SettingOutlined,
  TeamOutlined,
  UserOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';

export interface NavigationItem {
  key: string;
  label: string;
  path?: string;
  icon?: ReactNode;
  permission?: string;
  children?: NavigationItem[];
}

export const navigation: NavigationItem[] = [
  {
    key: 'dashboard',
    label: 'Dashboard',
    path: '/',
    icon: <DashboardOutlined />,
    permission: 'DASHBOARD_VIEW',
  },
  {
    key: 'fleet',
    label: 'Fleet',
    icon: <CarOutlined />,
    children: [
      { key: 'vehicles', label: 'Vehicles', path: '/fleet/vehicles', permission: 'VEHICLE_VIEW' },
      {
        key: 'vehicle-categories',
        label: 'Vehicle Categories',
        path: '/fleet/vehicle-categories',
        permission: 'VEHICLE_VIEW',
      },
      {
        key: 'vehicle-types',
        label: 'Vehicle Types',
        path: '/fleet/vehicle-types',
        permission: 'VEHICLE_VIEW',
      },
    ],
  },
  {
    key: 'drivers',
    label: 'Drivers',
    icon: <UserOutlined />,
    children: [
      { key: 'driver-list', label: 'Drivers', path: '/drivers', permission: 'DRIVER_VIEW' },
    ],
  },
  {
    key: 'routes',
    label: 'Routes',
    icon: <EnvironmentOutlined />,
    children: [{ key: 'route-list', label: 'Routes', path: '/routes', permission: 'ROUTE_VIEW' }],
  },
  {
    key: 'trips',
    label: 'Trips',
    icon: <ScheduleOutlined />,
    children: [{ key: 'trip-list', label: 'Trips', path: '/trips', permission: 'TRIP_VIEW' }],
  },
  {
    key: 'fuel',
    label: 'Fuel Management',
    icon: <ThunderboltOutlined />,
    children: [{ key: 'fuel-issues', label: 'Fuel Issues', path: '/fuel/issues', permission: 'FUEL_ISSUE_VIEW' }],
  },
  {
    key: 'administration',
    label: 'Administration',
    icon: <SettingOutlined />,
    children: [
      { key: 'users', label: 'Users', path: '/administration/users', permission: 'IDENTITY_MANAGE', icon: <TeamOutlined /> },
      {
        key: 'roles',
        label: 'Roles',
        path: '/administration/roles',
        permission: 'IDENTITY_MANAGE',
        icon: <SafetyCertificateOutlined />,
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
    return !item.permission || permissions.includes(item.permission) ? [item] : [];
  });
}

export function findNavigationItem(pathname: string) {
  const items = navigation.flatMap((item) => item.children ?? [item]);
  return items.find((item) => item.path === pathname)
    ?? items.find((item) => item.path && pathname.startsWith(`${item.path}/`));
}
