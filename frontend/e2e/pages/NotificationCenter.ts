import { expect, type Page } from '@playwright/test';

export class NotificationCenterObject {
  constructor(private readonly page: Page) {}

  bell() { return this.page.getByRole('button', { name: /Open notifications/ }); }

  async open() {
    await this.bell().click();
    await expect(this.page.getByText('Notifications', { exact: true }).last()).toBeVisible();
  }

  item(title: string) {
    return this.page.locator('.ant-list-item').filter({ hasText: title });
  }
}
