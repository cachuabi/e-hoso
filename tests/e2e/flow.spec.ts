// Acceptance criteria 1 (CK-E01 to CK-E03) and the file checks K3, K4, K5 of D17 on Docker Compose.
import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { writeFileSync, mkdirSync } from 'node:fs';

const PASSWORD = process.env.DEMO_PASSWORD ?? 'Demo12345';
const SHOTS = 'screenshots';
mkdirSync(SHOTS, { recursive: true });
const PDF = Buffer.from('%PDF-1.7\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n');

async function login(request: APIRequestContext, email: string): Promise<string> {
  const response = await request.post('/api/v1/auth/login', {
    data: { email, password: PASSWORD },
  });
  expect(response.ok(), await response.text()).toBeTruthy();
  return `Bearer ${(await response.json()).accessToken}`;
}

function auth(token: string) {
  return { headers: { Authorization: token } };
}

const FORMS: Record<string, Record<string, unknown>> = {
  CHUNG_THUC: { hoTen: 'Nguyễn Văn A', tenGiayTo: 'Bằng tốt nghiệp', soBan: 2 },
  KHAI_SINH: {
    hoTenTre: 'Bé A',
    ngaySinh: '2026-10-01',
    gioiTinh: 'Nam',
    noiSinh: 'Hà Nội',
    hoTenMe: 'Trần Thị B',
  },
  XAC_NHAN_HON_NHAN: {
    hoTen: 'Lê Văn C',
    ngaySinh: '1990-01-01',
    tinhTrang: 'Chưa kết hôn',
    mucDich: 'Kết hôn',
  },
};

for (const code of Object.keys(FORMS)) {
  test(`[FR-09] [FR-16] [FR-19] [FR-21] [FR-22] ${code}: Nháp đến Đã trả kết quả`, async ({
    request,
  }) => {
    const citizen = await login(request, 'dan@ehoso.local');
    const cbmc = await login(request, 'motcua@ehoso.local');
    const cctp = await login(request, 'tuphap@ehoso.local');
    const leader = await login(request, 'lanhdao@ehoso.local');
    const staffUrl = (id: string) => `/api/v1/staff/dossiers/${id}`;
    const version = async (token: string, id: string) =>
      (await (await request.get(staffUrl(id), auth(token))).json()).version;
    const ok = async (response: { ok(): boolean; text(): Promise<string> }) =>
      expect(response.ok(), await response.text()).toBeTruthy();

    const procedure = await (await request.get(`/api/v1/procedures/${code}`)).json();
    const draft = await (
      await request.post('/api/v1/dossiers', {
        ...auth(citizen),
        data: { procedureCode: code, formData: FORMS[code] },
      })
    ).json();
    const id = draft.summary.id;
    let firstFile = '';
    for (const component of procedure.components) {
      if (component.requirement !== 'REQUIRED') continue;
      const uploaded = await request.post(`/api/v1/dossiers/${id}/attachments`, {
        ...auth(citizen),
        multipart: {
          kind: 'DOSSIER',
          componentId: component.id,
          file: { name: 'tep.pdf', mimeType: 'application/pdf', buffer: PDF },
        },
      });
      await ok(uploaded);
      firstFile = (await uploaded.json()).id;
    }

    // K3, K4: the signed link works through the public HTTPS endpoint and returns the same bytes.
    const link = await (
      await request.get(`/api/v1/attachments/${firstFile}/download-url`, auth(citizen))
    ).json();
    expect(link.url).toContain('https://localhost:9443/');
    const download = await request.get(link.url);
    expect(download.status()).toBe(200);
    expect((await download.body()).equals(PDF)).toBeTruthy();
    expect((await request.get(`${link.url}&tamper=1`)).status()).toBe(403);

    await ok(await request.post(`/api/v1/dossiers/${id}/submit`, { ...auth(citizen), data: {} }));
    await ok(
      await request.post(`${staffUrl(id)}/accept`, {
        ...auth(cbmc),
        data: { originalChecked: true, version: await version(cbmc, id) },
      }),
    );
    const officers = await (await request.get('/api/v1/staff/officers', auth(cbmc))).json();
    await ok(
      await request.post(`${staffUrl(id)}/assign`, {
        ...auth(cbmc),
        data: { officerId: officers[0].id, version: await version(cbmc, id) },
      }),
    );
    await ok(
      await request.post(`/api/v1/dossiers/${id}/attachments`, {
        ...auth(cctp),
        multipart: {
          kind: 'DRAFT',
          file: { name: 'du-thao.pdf', mimeType: 'application/pdf', buffer: PDF },
        },
      }),
    );
    await ok(
      await request.post(`${staffUrl(id)}/submit-approval`, {
        ...auth(cctp),
        data: { opinion: 'Hợp lệ', resultKind: 'ISSUED', version: await version(cctp, id) },
      }),
    );
    await ok(
      await request.post(`${staffUrl(id)}/approve`, {
        ...auth(leader),
        data: { password: PASSWORD, version: await version(leader, id) },
      }),
    );
    await ok(
      await request.post(`${staffUrl(id)}/return-result`, {
        ...auth(cbmc),
        data: { method: 'ELECTRONIC', version: await version(cbmc, id) },
      }),
    );
    const finished = await (await request.get(`/api/v1/dossiers/${id}`, auth(citizen))).json();
    expect(finished.summary.status).toBe('RETURNED');
    const result = finished.attachments.find((a: { kind: string }) => a.kind === 'RESULT');
    const resultLink = await (
      await request.get(`/api/v1/attachments/${result.id}/download-url`, auth(citizen))
    ).json();
    expect((await request.get(resultLink.url)).status()).toBe(200);

    // FR-14: Mailpit received the citizen's emails.
    const mail = await request.get('http://localhost:8025/api/v1/search?query=to:dan@ehoso.local');
    await expect.poll(async () => (await mail.json()).total, { timeout: 15_000 }).toBeGreaterThan(0);
  });
}

test('[FR-08] K5: tải lên tệp 10 MB', async ({ request }) => {
  const citizen = await login(request, 'dan@ehoso.local');
  const procedure = await (await request.get('/api/v1/procedures/KHAI_SINH')).json();
  const draft = await (
    await request.post('/api/v1/dossiers', {
      ...auth(citizen),
      data: { procedureCode: 'KHAI_SINH', formData: {} },
    })
  ).json();
  const big = Buffer.alloc(10 * 1024 * 1024, 32);
  PDF.copy(big);
  const started = Date.now();
  const response = await request.post(`/api/v1/dossiers/${draft.summary.id}/attachments`, {
    ...auth(citizen),
    multipart: {
      kind: 'DOSSIER',
      componentId: procedure.components[0].id,
      file: { name: 'lon.pdf', mimeType: 'application/pdf', buffer: big },
    },
  });
  expect(response.ok(), await response.text()).toBeTruthy();
  writeFileSync('k5-upload-ms.txt', String(Date.now() - started));
});

async function noOverflow(page: Page) {
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth - window.innerWidth,
  );
  expect(overflow).toBeLessThanOrEqual(0);
}

async function uiLogin(page: Page, email: string) {
  await page.goto('/dang-nhap');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Mật khẩu').fill(PASSWORD);
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  await page.getByRole('button', { name: 'Đăng xuất' }).waitFor();
}

test('[NFR-02] [NFR-09] giao diện theo vai trò, độ rộng 360 px và ảnh chụp', async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 740 });
  await page.goto('/thu-tuc');
  await expect(page.getByText('Đăng ký khai sinh')).toBeVisible();
  await noOverflow(page);
  await page.screenshot({ path: `${SHOTS}/01-danh-muc.png`, fullPage: true });
  await page.goto('/tra-cuu');
  await noOverflow(page);
  await page.screenshot({ path: `${SHOTS}/02-tra-cuu.png`, fullPage: true });

  await uiLogin(page, 'dan@ehoso.local');
  await page.goto('/ho-so/moi/CHUNG_THUC');
  await page.getByLabel('Họ và tên người yêu cầu').fill('Nguyễn Văn A');
  await page.getByLabel('Tên giấy tờ cần chứng thực').fill('Bằng tốt nghiệp');
  await page.getByLabel('Số bản cần chứng thực').fill('2');
  await noOverflow(page);
  await page.screenshot({ path: `${SHOTS}/03-tao-ho-so.png`, fullPage: true });
  await page.getByRole('button', { name: 'Lưu nháp và tiếp tục' }).click();
  await page.getByLabel('Bản sao cần chứng thực').setInputFiles({
    name: 'ban-sao.pdf',
    mimeType: 'application/pdf',
    buffer: PDF,
  });
  await page.getByRole('button', { name: 'ban-sao.pdf' }).waitFor();
  await page.screenshot({ path: `${SHOTS}/04-dinh-kem.png`, fullPage: true });
  await page.getByRole('button', { name: 'Nộp hồ sơ' }).click();
  await page.getByText('Hồ sơ đã được nộp').waitFor();
  await noOverflow(page);
  await page.screenshot({ path: `${SHOTS}/05-da-nop.png`, fullPage: true });
  await page.goto('/ho-so');
  await page.getByRole('cell').first().waitFor();
  await noOverflow(page);
  await page.screenshot({ path: `${SHOTS}/06-ho-so-cua-toi.png`, fullPage: true });
  await page.getByRole('button', { name: 'Đăng xuất' }).click();

  await page.setViewportSize({ width: 1280, height: 800 });
  await uiLogin(page, 'motcua@ehoso.local');
  await page.getByRole('link', { name: 'Xử lý' }).first().waitFor();
  await page.screenshot({ path: `${SHOTS}/07-hang-doi-cbmc.png`, fullPage: true });
  await page.getByRole('link', { name: 'Xử lý' }).first().click();
  await page.getByRole('heading', { name: 'Tiếp nhận', exact: true }).waitFor();
  await page.screenshot({ path: `${SHOTS}/08-tiep-nhan.png`, fullPage: true });
  await page.getByLabel(/Đã đối chiếu/).check();
  await page.getByRole('button', { name: 'Tiếp nhận', exact: true }).click();
  await page.getByRole('heading', { name: 'Phiếu hẹn trả' }).waitFor();
  await page.screenshot({ path: `${SHOTS}/09-phieu-hen.png`, fullPage: true });
  await page.getByRole('link', { name: 'Quay lại hồ sơ' }).click();
  await page.getByLabel('Công chức xử lý').selectOption({ index: 1 });
  await page.screenshot({ path: `${SHOTS}/10-phan-cong.png`, fullPage: true });
  await page.getByRole('button', { name: 'Phân công' }).click();
  await page.getByText('Nhật ký xử lý').waitFor();
  await page.getByRole('button', { name: 'Đăng xuất' }).click();

  await uiLogin(page, 'tuphap@ehoso.local');
  await page.getByLabel('Trạng thái').selectOption('IN_PROGRESS');
  await page.getByRole('link', { name: 'Xử lý' }).last().click();
  await page.getByLabel('Ý kiến thẩm định').fill('Hồ sơ hợp lệ, đề nghị cấp giấy.');
  await page.getByLabel(/Dự thảo kết quả/).setInputFiles({
    name: 'du-thao.pdf',
    mimeType: 'application/pdf',
    buffer: PDF,
  });
  await page.getByRole('button', { name: /du-thao.pdf/ }).waitFor();
  await page.screenshot({ path: `${SHOTS}/11-tham-dinh.png`, fullPage: true });
  await page.getByRole('button', { name: 'Trình phê duyệt' }).click();
  await expect(page.getByText('Chờ phê duyệt').first()).toBeVisible();
  await page.getByRole('button', { name: 'Đăng xuất' }).click();

  await uiLogin(page, 'lanhdao@ehoso.local');
  await page.getByRole('link', { name: 'Xử lý' }).last().click();
  await page.getByLabel(/ký mô phỏng/).fill(PASSWORD);
  await page.screenshot({ path: `${SHOTS}/12-phe-duyet.png`, fullPage: true });
  await page.getByRole('button', { name: 'Phê duyệt' }).click();
  await expect(page.getByText('Đã có kết quả').first()).toBeVisible();
  await page.getByRole('link', { name: 'Dashboard' }).click();
  await page.getByText('Đang quá hạn').waitFor();
  await page.screenshot({ path: `${SHOTS}/13-dashboard.png`, fullPage: true });
  await page.getByRole('button', { name: 'Đăng xuất' }).click();

  await uiLogin(page, 'admin@ehoso.local');
  await page.getByText('Tạo tài khoản').waitFor();
  await page.screenshot({ path: `${SHOTS}/14-quan-tri.png`, fullPage: true });
});
