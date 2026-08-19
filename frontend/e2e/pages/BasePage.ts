
import { Page, Locator, expect } from '@playwright/test';

export class BasePage {
  readonly page: Page;
  readonly pageHeading: Locator;
  readonly breadcrumbs: Locator;
  readonly userMenu: Locator;
  readonly navSider: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pageHeading = page.locator('.page-heading h2, .page-heading h3');
    this.breadcrumbs = page.locator('.ant-breadcrumb');
    this.userMenu = page.locator('.user-menu');
    this.navSider = page.locator('.app-sider');
  }

  async navigateTo(path: string) {
    await this.page.goto(path);
  }

  async expectHeading(title: string) {
    await expect(this.page.getByRole('heading', { name: title })).toBeVisible({ timeout: 15000 });
  }

  async expectBreadcrumb(title: string) {
    await expect(this.breadcrumbs).toContainText(title, { timeout: 10000 });
  }

  async logout() {
    await this.userMenu.click();
    await this.page.locator('.ant-dropdown-menu-item:has-text("Log out")').click();
    await expect(this.page).toHaveURL(/.*\/login/);
  }

  async openAccessDrawer() {
    await this.userMenu.click();
    await this.page.locator('.ant-dropdown-menu-item:has-text("Access & permissions")').click();
    await expect(this.page.locator('.ant-drawer-title')).toContainText('Access & permissions');
  }
}
