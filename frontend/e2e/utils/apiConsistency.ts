
import { Locator, Page, expect } from '@playwright/test';

export class ApiConsistency {
  static async verifyTableRowMatchesApi(page: Page, rowSelector: Locator, expectedData: Record<string, string>) {
    for (const value of Object.values(expectedData)) {
      await expect(rowSelector).toContainText(value);
    }
  }

  static async verifyMetricMatchesApi(page: Page, labelText: string, expectedValue: string | number) {
    const metricCard = page.locator('.ant-statistic', { hasText: labelText });
    await expect(metricCard).toContainText(String(expectedValue));
  }
}
