import { expect, type Locator, type Page } from '@playwright/test';

export class NotificationRulesPageObject {
  constructor(private readonly page: Page) {}

  async open() {
    const loaded = this.page.waitForResponse((response) => response.url().includes('/api/notification-rules') && response.request().method() === 'GET');
    await this.page.goto('/notification-rules');
    await loaded;
    await expect(this.page.getByRole('heading', { name: 'Notification Rules', level: 2 })).toBeVisible();
    await expect(this.page.getByRole('table')).toBeVisible();
  }

  async openCreate(): Promise<Locator> {
    await this.page.getByRole('button', { name: 'Create Notification Rule' }).click();
    const modal = this.page.getByRole('dialog', { name: 'Create Notification Rule' });
    await expect(modal).toBeVisible();
    await expect(modal.getByLabel('Template')).toBeEnabled();
    await expect(modal.getByLabel('Template').locator('xpath=../../..').locator('.ant-select-selection-item').first()).toBeVisible();
    return modal;
  }

  async select(modal: Locator, label: string, option: RegExp | string) {
    const input = modal.getByLabel(label);
    const select = input.locator('xpath=../../..');
    const selectedItem = select.locator('.ant-select-selection-item').first();
    const selectedText = await selectedItem.count() ? (await selectedItem.textContent())?.trim() : undefined;
    await select.click();
    const dropdown = this.page.locator('.ant-select-dropdown:visible').last();
    await expect(dropdown).toBeVisible();
    if (typeof option === 'string') {
      await expect(dropdown.getByText(option, { exact: true }).last()).toBeVisible();
    } else {
      await expect(dropdown.getByText(option).last()).toBeVisible();
    }
    const options = (await dropdown.locator('.ant-select-item-option-content').allTextContents()).map((value) => value.trim());
    const targetIndex = options.findIndex((value) => typeof option === 'string' ? value === option : option.test(value));
    if (targetIndex < 0) throw new Error(`Ant Select ${label} does not contain ${String(option)}`);
    const currentIndex = selectedText ? options.indexOf(selectedText) : 0;
    const steps = selectedText ? (targetIndex - Math.max(currentIndex, 0) + options.length) % options.length : targetIndex;
    for (let step = 0; step < steps; step += 1) {
      await input.press('ArrowDown');
    }
    await input.press('Enter');
    if (typeof option === 'string') {
      await expect(select.locator('.ant-select-selection-item').filter({ hasText: option }).first()).toBeVisible();
      if (label === 'Channel') {
        await expect(modal.getByLabel('Template')).toBeEnabled();
        await expect(modal.getByLabel('Template').locator('xpath=../../..').locator('.ant-select-selection-item').first()).toBeVisible();
      }
    }
  }

  async toggle(modal: Locator, label: string) {
    const item = modal.locator('.ant-form-item').filter({ hasText: label });
    const control = item.getByRole('switch');
    await control.click();
    await expect(control).toBeChecked();
  }

  async createInApp(name: string, roleName: string) {
    const modal = await this.openCreate();
    await modal.getByLabel('Rule Name').fill(name);
    await modal.getByLabel('Role Name').fill(roleName);
    await modal.getByRole('button', { name: 'Create Rule' }).click();
    await expect(modal).toBeHidden();
    await expect(await this.locateRow(name)).toBeVisible();
  }

  row(name: string) {
    return this.page.getByRole('row', { name: new RegExp(name) });
  }

  async locateRow(name: string): Promise<Locator> {
    const row = this.row(name);
    const firstPage = this.page.locator('.ant-pagination-item-1');
    if (await firstPage.count()) await firstPage.click();
    for (let pageNumber = 0; pageNumber < 5; pageNumber += 1) {
      if (await row.count()) return row;
      const next = this.page.getByTitle('Next Page');
      if (!await next.count() || await next.getAttribute('aria-disabled') === 'true') break;
      await next.click();
    }
    return row;
  }
}
