
import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

export class BunkerTanksPage extends BasePage {
  readonly tankTable: Locator;

  constructor(page: Page) {
    super(page);
    this.tankTable = page.locator('table');
  }

  async goto() {
    await this.page.goto('/fuel/bunker-tanks');
    await expect(this.page.getByRole('heading', { name: 'Bunker Tanks' }).first()).toBeVisible({ timeout: 15000 });
  }
}
