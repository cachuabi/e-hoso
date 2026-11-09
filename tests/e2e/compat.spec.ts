// NFR-09 on three browser engines, NFR-06 with axe (CK-X01, CK-A01).
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { appendFileSync } from 'node:fs';

const PASSWORD = process.env.DEMO_PASSWORD ?? 'Demo12345';

async function login(page: Page, email: string) {
  await page.goto('/dang-nhap');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Mật khẩu').fill(PASSWORD);
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  await page.getByRole('button', { name: 'Đăng xuất' }).waitFor();
}

test('[NFR-09] catalog, lookup and sign-in work on this browser', async ({ page, browserName }) => {
  await page.goto('/thu-tuc');
  await expect(page.getByText('Đăng ký khai sinh')).toBeVisible();
  await page.goto('/tra-cuu');
  await page.getByLabel(/Mã tham chiếu/).fill('TC-00000000');
  await page.getByLabel(/4 số cuối/).fill('0000');
  await page.getByRole('button', { name: 'Tra cứu' }).click();
  await expect(page.getByText('Không tìm thấy hồ sơ.')).toBeVisible();
  await login(page, 'dan@ehoso.local');
  await expect(page.getByRole('heading', { name: 'Hồ sơ của tôi' })).toBeVisible();
  appendFileSync('browsers.txt', `${browserName} ok\n`);
});

test('[NFR-06] no serious accessibility violation on the main screens', async ({
  page,
  browserName,
}) => {
  test.skip(browserName !== 'chromium', 'axe runs once');
  const scan = async (name: string) => {
    const { violations } = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();
    const serious = violations.filter((v) => ['serious', 'critical'].includes(v.impact ?? ''));
    appendFileSync(
      'axe.txt',
      `${name}: ${violations.length} violations, ${serious.length} serious ${serious.map((v) => v.id).join(',')}\n`,
    );
    expect(serious.map((v) => `${name}: ${v.id}`)).toEqual([]);
  };
  await page.goto('/thu-tuc');
  await page.getByText('Đăng ký khai sinh').waitFor();
  await scan('danh-muc');
  await page.goto('/dang-nhap');
  await scan('dang-nhap');
  await login(page, 'dan@ehoso.local');
  await page.goto('/ho-so/moi/KHAI_SINH');
  await page.getByLabel('Họ và tên trẻ').waitFor();
  await scan('tao-ho-so');
  await page.getByRole('button', { name: 'Đăng xuất' }).click();
  await login(page, 'lanhdao@ehoso.local');
  await page.getByRole('link', { name: 'Dashboard' }).click();
  await page.getByText('Đang quá hạn').waitFor();
  await scan('dashboard');
});
