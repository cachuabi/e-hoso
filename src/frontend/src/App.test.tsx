import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from './App';

function respond(status: number, body: unknown) {
  return Promise.resolve(new Response(JSON.stringify(body), { status }));
}

describe('App shell', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('[FR-06] shows the product name and the public procedure catalog to guests', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) =>
        url.includes('/auth/refresh')
          ? respond(401, {})
          : respond(200, [
              {
                code: 'KHAI_SINH',
                name: 'Đăng ký khai sinh',
                deadlineUnit: 'SAME_DAY',
                deadlineDays: null,
                applies15hCutoff: true,
                documentsToPresent: null,
                formSchema: { fields: [] },
                components: [{ id: '1', name: 'Giấy chứng sinh', requirement: 'REQUIRED' }],
              },
            ]),
      ),
    );
    render(<App />);
    expect(await screen.findByRole('heading', { name: 'e-HoSo' })).toBeInTheDocument();
    expect(await screen.findByText('Đăng ký khai sinh')).toBeInTheDocument();
    expect(screen.getByText(/Trong ngày/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Đăng nhập' })).toBeInTheDocument();
  });
});
