
import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';
import { UiHelpers } from '../utils/uiHelpers';

export class FleetVehiclesPage extends BasePage {
  readonly createButton: Locator;
  readonly refreshButton: Locator;
  readonly vehicleTable: Locator;
  readonly detailsDrawer: Locator;

  constructor(page: Page) {
    super(page);
    this.createButton = page.locator('button:has-text("Create")');
    this.refreshButton = page.locator('button:has-text("Refresh")');
    this.vehicleTable = page.locator('.resource-list-card table');
    this.detailsDrawer = page.locator('.ant-drawer');
  }

  async goto() {
    await this.page.goto('/fleet/vehicles');
    await expect(this.page.locator('.resource-list__title')).toContainText('Vehicle registry', { timeout: 15000 });
  }

  async openCreateModal() {
    await this.createButton.click();
    await expect(this.page.locator('.ant-modal-title')).toContainText('Create Vehicle registry');
  }

  async createVehicle(payload: { registrationNumber: string; manufacturer?: string; model?: string }) {
    await this.openCreateModal();
    await UiHelpers.fillResourceInput(this.page, 'registrationNumber', payload.registrationNumber);
    await UiHelpers.selectOption(this.page, '#resource-categoryId', 'Trucks');
    await UiHelpers.selectOption(this.page, '#resource-typeId', 'Box Truck');
    await UiHelpers.selectOption(this.page, '#resource-ownershipType', 'Company owned');
    await UiHelpers.selectOption(this.page, '#resource-operationalStatus', 'Available');
    if (payload.manufacturer) await UiHelpers.fillResourceInput(this.page, 'manufacturer', payload.manufacturer);
    if (payload.model) await UiHelpers.fillResourceInput(this.page, 'model', payload.model);
    await UiHelpers.submitModal(this.page);
    await UiHelpers.expectSuccessMessage(this.page, 'saved');
  }

  async openDetails(registrationNumber: string) {
    const row = this.vehicleTable.locator('tr', { hasText: registrationNumber });
    await row.locator('button:has-text("View details")').click();
    await expect(this.detailsDrawer).toBeVisible({ timeout: 10000 });
  }
}
