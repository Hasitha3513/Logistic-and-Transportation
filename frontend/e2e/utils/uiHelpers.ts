
import { Page, expect } from '@playwright/test';

export class UiHelpers {
  static async selectOption(page: Page, selectSelector: string, optionText: string) {
    const input = page.locator(selectSelector);
    const trigger = input.locator(
      'xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " ant-select ")][1]',
    );
    await expect(trigger).toBeVisible({ timeout: 10000 });
    await trigger.click();
    const dropdown = page.locator('.ant-select-dropdown:visible');
    await expect(dropdown).toBeVisible({ timeout: 10000 });
    const option = dropdown.locator('.ant-select-item-option', { hasText: optionText }).first();
    await expect(option).toBeVisible({ timeout: 10000 });
    await option.click();
    await page.waitForTimeout(300);
  }

  static async fillResourceInput(page: Page, name: string, value: string | number) {
    const input = page.locator(`#resource-${name}`);
    await input.fill(String(value));
  }

  static async submitModal(page: Page) {
    const saveBtn = page.locator('.ant-modal-footer button.ant-btn-primary');
    await expect(saveBtn).toBeVisible({ timeout: 5000 });
    await saveBtn.click();
  }

  static async expectSuccessMessage(page: Page, text: string = 'saved') {
    const msg = page.locator('.ant-message');
    await expect(msg).toContainText(text, { timeout: 10000 });
  }

  static async expectTableContains(page: Page, text: string) {
    const table = page.locator('table');
    await expect(table).toContainText(text, { timeout: 10000 });
  }
}
