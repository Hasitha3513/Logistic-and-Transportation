
import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

export class FuelIssuesPage extends BasePage {
  readonly createButton: Locator;
  readonly issuesTable: Locator;

  constructor(page: Page) {
    super(page);
    this.createButton = page.locator('a[href="/fuel/issues/new"] button, button:has-text("Create")');
    this.issuesTable = page.locator('table');
  }

  async goto() {
    await this.page.goto('/fuel/issues');
    await expect(this.page.getByRole('heading', { name: 'Fuel Issues', level: 2 })).toBeVisible({ timeout: 15000 });
  }
}
