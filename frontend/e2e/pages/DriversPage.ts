
import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';
import { UiHelpers } from '../utils/uiHelpers';

export class DriversPage extends BasePage {
  readonly createButton: Locator;
  readonly driverTable: Locator;
  readonly detailsDrawer: Locator;

  constructor(page: Page) {
    super(page);
    this.createButton = page.locator('button:has-text("Create")');
    this.driverTable = page.locator('.resource-list-card table');
    this.detailsDrawer = page.locator('.ant-drawer');
  }

  async goto() {
    await this.page.goto('/drivers');
    await expect(this.page.locator('.resource-list__title')).toContainText('Driver registry', { timeout: 15000 });
  }

  async openCreateModal() {
    await this.createButton.click();
    await expect(this.page.locator('.ant-modal-title')).toContainText('Create Driver registry');
  }

  async createDriver(payload: { employeeNumber: string; firstName: string; lastName: string; email?: string }) {
    await this.openCreateModal();
    await UiHelpers.fillResourceInput(this.page, 'employeeNumber', payload.employeeNumber);
    await UiHelpers.fillResourceInput(this.page, 'firstName', payload.firstName);
    await UiHelpers.fillResourceInput(this.page, 'lastName', payload.lastName);
    if (payload.email) await UiHelpers.fillResourceInput(this.page, 'email', payload.email);
    await UiHelpers.selectOption(this.page, '#resource-status', 'Available');
    await UiHelpers.submitModal(this.page);
    await UiHelpers.expectSuccessMessage(this.page, 'saved');
  }

  async openDetails(employeeNumber: string) {
    const row = this.driverTable.locator('tr', { hasText: employeeNumber });
    await row.locator('button:has-text("View details")').click();
    await expect(this.detailsDrawer).toBeVisible({ timeout: 10000 });
  }
}
