
import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

export class FuelPurchasesPage extends BasePage {
  readonly createButton: Locator;
  readonly purchasesTable: Locator;

  constructor(page: Page) {
    super(page);
    this.createButton = page.locator('a[href="/fuel/purchases/new"] button, button:has-text("New purchase")');
    this.purchasesTable = page.locator('table');
  }

  async goto() {
    await this.page.goto('/fuel/purchases');
    await expect(this.page.locator('.page-heading, h2, h3, h4, .ant-typography').filter({ hasText: 'Fuel purchases' }).first()).toBeVisible({ timeout: 15000 });
  }
}
