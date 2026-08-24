
import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';
import { UiHelpers } from '../utils/uiHelpers';

export class RoutesPage extends BasePage {
  readonly createButton: Locator;
  readonly routeTable: Locator;

  constructor(page: Page) {
    super(page);
    this.createButton = page.locator('button:has-text("Create")');
    this.routeTable = page.locator('.resource-list-card table');
  }

  async goto() {
    await this.page.goto('/routes');
    await expect(this.page.getByRole('heading', { name: 'Routes', level: 2 })).toBeVisible({ timeout: 15000 });
  }

  async createRoute(payload: { code: string; name: string; distance: number; duration: number }) {
    await this.createButton.click();
    await UiHelpers.fillResourceInput(this.page, 'code', payload.code);
    await UiHelpers.fillResourceInput(this.page, 'name', payload.name);
    await UiHelpers.selectOption(this.page, '#resource-originLocationId', 'Colombo Hub');
    await UiHelpers.selectOption(this.page, '#resource-destinationLocationId', 'Kandy Depot');
    await UiHelpers.fillResourceInput(this.page, 'plannedDistanceKm', payload.distance);
    await UiHelpers.fillResourceInput(this.page, 'estimatedDurationMinutes', payload.duration);
    await UiHelpers.submitModal(this.page);
    await UiHelpers.expectSuccessMessage(this.page, 'saved');
  }
}
