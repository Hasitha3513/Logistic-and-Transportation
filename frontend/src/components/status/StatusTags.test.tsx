import { render, screen } from '@testing-library/react';
import {
  DocumentStatusTag,
  DriverStatusTag,
  PriorityTag,
  TripStatusTag,
  VehicleStatusTag,
  documentStatusPresentation,
  driverStatusPresentation,
  priorityPresentation,
  tripStatusPresentation,
  vehicleStatusPresentation,
} from './StatusTags';

describe('status tags', () => {
  it('centralizes the known presentation mappings', () => {
    expect(tripStatusPresentation('DISPATCHED')).toEqual({ color: 'purple', label: 'Dispatched' });
    expect(vehicleStatusPresentation('UNDER_MAINTENANCE')).toEqual({ color: 'gold', label: 'Under maintenance' });
    expect(driverStatusPresentation('OFF_DUTY')).toEqual({ color: 'default', label: 'Off duty' });
    expect(documentStatusPresentation('EXPIRED')).toEqual({ color: 'error', label: 'Expired' });
    expect(priorityPresentation('HIGH')).toEqual({ color: 'orange', label: 'High' });
  });

  it('renders every reusable component with Ant Design Tag content', () => {
    render(
      <>
        <TripStatusTag status="IN_PROGRESS" />
        <VehicleStatusTag status="BROKEN_DOWN" />
        <DriverStatusTag status="AVAILABLE" />
        <DocumentStatusTag status="ACTIVE" />
        <PriorityTag priority="CRITICAL" />
      </>,
    );

    expect(screen.getByText('In progress')).toBeInTheDocument();
    expect(screen.getByText('Broken down')).toBeInTheDocument();
    expect(screen.getByText('Available')).toBeInTheDocument();
    expect(screen.getByText('Active')).toBeInTheDocument();
    expect(screen.getByText('Critical')).toBeInTheDocument();
  });

  it('uses a neutral fallback without embedding business rules', () => {
    expect(vehicleStatusPresentation('inspection_pending')).toEqual({ color: 'default', label: 'Inspection pending' });
    expect(driverStatusPresentation(undefined)).toEqual({ color: 'default', label: 'Unknown' });
  });
});
