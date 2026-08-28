
export interface TestUser {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  active: boolean;
  roles: string[];
  permissions: string[];
}

export const USERS: Record<string, TestUser> = {
  ADMIN: {
    id: '00000000-0000-0000-0000-000000000001',
    username: 'admin.user',
    email: 'admin@example.com',
    firstName: 'Admin',
    lastName: 'User',
    active: true,
    roles: ['ADMIN', 'OPERATIONS'],
    permissions: [
      'DASHBOARD_VIEW', 'VEHICLE_VIEW', 'VEHICLE_CREATE', 'VEHICLE_UPDATE', 'VEHICLE_STATUS_UPDATE',
      'VEHICLE_DOCUMENT_MANAGE', 'VEHICLE_MAINTENANCE_MANAGE', 'VEHICLE_READING_VIEW', 'VEHICLE_READING_CREATE',
      'DRIVER_VIEW', 'DRIVER_CREATE', 'DRIVER_UPDATE', 'DRIVER_LICENSE_MANAGE',
      'DRIVER_EXCEPTION_MANAGE', 'DRIVER_VIOLATION_MANAGE',
      'DRIVER_MEDICAL_VIEW', 'DRIVER_MEDICAL_MANAGE',
      'DRIVER_DRUG_TEST_VIEW', 'DRIVER_DRUG_TEST_MANAGE',
      'LUBRICANT_LOG_VIEW', 'LUBRICANT_LOG_MANAGE',
      'ROUTE_VIEW', 'ROUTE_CREATE', 'ROUTE_UPDATE', 'ROUTE_DISRUPTION_MANAGE',
      'TRIP_VIEW', 'TRIP_CREATE', 'TRIP_UPDATE', 'TRIP_ASSIGN', 'TRIP_SUBMIT', 'TRIP_APPROVE', 'TRIP_REJECT',
      'TRIP_DISPATCH', 'TRIP_START', 'TRIP_COMPLETE', 'TRIP_CLOSE', 'TRIP_CANCEL',
      'TRIP_LOG_VIEW', 'TRIP_LOG_MANAGE',
      'FUEL_ISSUE_VIEW', 'FUEL_ISSUE_CREATE', 'FUEL_ISSUE_APPROVE',
      'FUEL_PURCHASE_VIEW', 'FUEL_PURCHASE_CREATE', 'FUEL_PURCHASE_RECEIVE',
      'BUNKER_VIEW', 'BUNKER_MANAGE', 'FUEL_PRICE_VIEW', 'FUEL_PRICE_MANAGE',
      'REPORT_VIEW', 'FREIGHT_REPORT_VIEW', 'FREIGHT_REPORT_EXPORT', 'IDENTITY_MANAGE',
      'FREIGHT_ORDER_VIEW', 'FREIGHT_ORDER_MANAGE',
      'CARGO_MANIFEST_VIEW', 'CARGO_MANIFEST_MANAGE', 'CARGO_MANIFEST_FINALIZE',
      'NOTIFICATION_RULE_VIEW', 'NOTIFICATION_RULE_MANAGE', 'NOTIFICATION_VIEW',
    ],
  },
  FLEET_MANAGER: {
    id: '00000000-0000-0000-0000-000000000002',
    username: 'fleet.manager',
    email: 'fleet@example.com',
    firstName: 'Fleet',
    lastName: 'Manager',
    active: true,
    roles: ['FLEET_MANAGER'],
    permissions: [
      'DASHBOARD_VIEW', 'VEHICLE_VIEW', 'VEHICLE_CREATE', 'VEHICLE_UPDATE', 'VEHICLE_STATUS_UPDATE',
      'VEHICLE_DOCUMENT_MANAGE', 'VEHICLE_MAINTENANCE_MANAGE', 'VEHICLE_READING_VIEW', 'VEHICLE_READING_CREATE',
      'DRIVER_VIEW', 'DRIVER_CREATE', 'DRIVER_UPDATE', 'DRIVER_LICENSE_MANAGE',
      'DRIVER_EXCEPTION_MANAGE', 'DRIVER_VIOLATION_MANAGE',
      'DRIVER_MEDICAL_VIEW', 'DRIVER_MEDICAL_MANAGE',
      'DRIVER_DRUG_TEST_VIEW', 'DRIVER_DRUG_TEST_MANAGE',
      'LUBRICANT_LOG_VIEW', 'LUBRICANT_LOG_MANAGE',
      'NOTIFICATION_VIEW',
    ],
  },
  DISPATCHER: {
    id: '00000000-0000-4000-8000-000000000003',
    username: 'trip.dispatcher',
    email: 'dispatcher@example.com',
    firstName: 'Trip',
    lastName: 'Dispatcher',
    active: true,
    roles: ['DISPATCHER'],
    permissions: [
      'DASHBOARD_VIEW', 'TRIP_VIEW', 'TRIP_CREATE', 'TRIP_UPDATE', 'TRIP_ASSIGN', 'TRIP_SUBMIT', 'TRIP_APPROVE',
      'TRIP_DISPATCH', 'TRIP_START', 'TRIP_COMPLETE', 'TRIP_CLOSE', 'TRIP_CANCEL', 'TRIP_LOG_VIEW', 'TRIP_LOG_MANAGE',
      'ROUTE_VIEW', 'ROUTE_CREATE', 'ROUTE_UPDATE', 'ROUTE_DISRUPTION_MANAGE',
      'VEHICLE_VIEW', 'DRIVER_VIEW',
      'NOTIFICATION_VIEW',
    ],
  },
  FUEL_OPERATOR: {
    id: '00000000-0000-0000-0000-000000000004',
    username: 'fuel.operator',
    email: 'fuel@example.com',
    firstName: 'Fuel',
    lastName: 'Operator',
    active: true,
    roles: ['FUEL_OPERATOR'],
    permissions: [
      'DASHBOARD_VIEW', 'FUEL_ISSUE_VIEW', 'FUEL_ISSUE_CREATE', 'FUEL_ISSUE_APPROVE',
      'FUEL_PURCHASE_VIEW', 'FUEL_PURCHASE_CREATE', 'FUEL_PURCHASE_RECEIVE',
      'BUNKER_VIEW', 'BUNKER_MANAGE', 'FUEL_PRICE_VIEW', 'VEHICLE_VIEW',
    ],
  },
  VIEWER: {
    id: '00000000-0000-0000-0000-000000000005',
    username: 'read.viewer',
    email: 'viewer@example.com',
    firstName: 'Read',
    lastName: 'Viewer',
    active: true,
    roles: ['VIEWER'],
    permissions: ['DASHBOARD_VIEW'],
  },
};
