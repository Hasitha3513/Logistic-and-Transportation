
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
    const logoutItem = this.page.getByRole('menuitem', { name: /log out/i });
    await expect(logoutItem).toBeVisible({ timeout: 5000 });
    await this.page.waitForTimeout(150);
    await logoutItem.click();
    await expect(this.page).toHaveURL(/.*\/login/, { timeout: 15000 });
  }

  async openAccessDrawer() {
    await this.userMenu.click();
    await this.page.locator('.ant-dropdown-menu-item:has-text("Access & permissions")').click();
    await expect(this.page.locator('.ant-drawer-title')).toContainText('Access & permissions');
  }
}
