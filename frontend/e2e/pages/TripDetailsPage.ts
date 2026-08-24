
import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';
import { UiHelpers } from '../utils/uiHelpers';

export class TripDetailsPage extends BasePage {
  readonly lifecycleCard: Locator;
  readonly submitButton: Locator;
  readonly approveButton: Locator;
  readonly dispatchButton: Locator;
  readonly startButton: Locator;
  readonly completeButton: Locator;
  readonly closeButton: Locator;
  readonly rejectButton: Locator;
  readonly cancelButton: Locator;

  constructor(page: Page) {
    super(page);
    this.lifecycleCard = page.locator('.lifecycle-actions');
    this.submitButton = page.locator('.lifecycle-actions button:has-text("Submit")');
    this.approveButton = page.locator('.lifecycle-actions button:has-text("Approve")');
    this.dispatchButton = page.locator('.lifecycle-actions button:has-text("Dispatch")');
    this.startButton = page.locator('.lifecycle-actions button:has-text("Start trip")');
    this.completeButton = page.locator('.lifecycle-actions button:has-text("Complete trip")');
    this.closeButton = page.locator('.lifecycle-actions button:has-text("Close trip")');
    this.rejectButton = page.locator('.lifecycle-actions button:has-text("Reject")');
    this.cancelButton = page.locator('.lifecycle-actions button:has-text("Cancel trip")');
  }

  async goto(tripId: string) {
    await this.page.goto(`/trips/${tripId}`);
    await expect(this.lifecycleCard).toBeVisible({ timeout: 15000 });
  }

  async submitTrip() {
    await this.submitButton.click();
    await this.page.click('.ant-modal-footer button.ant-btn-primary');
    await UiHelpers.expectSuccessMessage(this.page, 'submitted');
  }

  async approveTrip() {
    await this.approveButton.click();
    await this.page.click('.ant-modal-footer button.ant-btn-primary');
    await UiHelpers.expectSuccessMessage(this.page, 'approved');
  }

  async dispatchTrip(remarks = 'Dispatch confirmed') {
    await this.dispatchButton.click();
    await this.page.fill('textarea[aria-label="Dispatch remarks"]', remarks);
    await this.page.click('.ant-modal-footer button.ant-btn-primary');
    await UiHelpers.expectSuccessMessage(this.page, 'dispatched');
  }

  async startTrip(startOdometerKm: number) {
    await this.startButton.click();
    await this.page.fill('input[aria-label="Start odometer (km)"]', String(startOdometerKm));
    await this.page.click('.ant-modal-footer button.ant-btn-primary');
    await UiHelpers.expectSuccessMessage(this.page, 'in progress');
  }

  async completeTrip(endOdometerKm: number, remarks = 'Delivered on time') {
    await this.completeButton.click();
    await this.page.fill('input[aria-label="End odometer (km)"]', String(endOdometerKm));
    await this.page.fill('textarea[aria-label="Completion remarks"]', remarks);
    await this.page.click('.ant-modal-footer button.ant-btn-primary');
    await UiHelpers.expectSuccessMessage(this.page, 'completed');
  }
}
