
import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

export class TripsPage extends BasePage {
  readonly createTripButton: Locator;
  readonly tripTable: Locator;
  readonly searchInput: Locator;

  constructor(page: Page) {
    super(page);
    this.createTripButton = page.locator('a[href="/trips/new"] button, button:has-text("Create trip")');
    this.tripTable = page.locator('.trip-table-card table, table');
    this.searchInput = page.locator('.trip-filters__search input');
  }

  async goto() {
    await this.page.goto('/trips');
    await expect(this.page.getByRole('heading', { name: 'Trips' }).first()).toBeVisible({ timeout: 15000 });
  }
}
