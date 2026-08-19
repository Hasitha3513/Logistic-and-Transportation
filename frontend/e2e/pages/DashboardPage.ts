
import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

export class DashboardPage extends BasePage {
  readonly overviewContainer: Locator;
  readonly vehiclesCard: Locator;
  readonly driversCard: Locator;
  readonly tripsCard: Locator;

  constructor(page: Page) {
    super(page);
    this.overviewContainer = page.locator('.operations-dashboard');
    this.vehiclesCard = page.locator('.ant-card', { hasText: 'Vehicles' });
    this.driversCard = page.locator('.ant-card', { hasText: 'Drivers' });
    this.tripsCard = page.locator('.ant-card', { hasText: 'Trips' });
  }

  async goto() {
    await this.page.goto('/');
  }

  async expectDashboardLoaded() {
    await expect(this.page.getByRole('heading', { name: 'Operations Overview' })).toBeVisible({ timeout: 15000 });
    await expect(this.vehiclesCard.first()).toBeVisible({ timeout: 10000 });
  }
}
