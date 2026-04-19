import { Page } from '@playwright/test';

const ADMIN_EMAIL = 'admin@drive.com';
const ADMIN_PASSWORD = 'admin1234';

/**
 * 로그인 헬퍼 — Spring Security form login
 */
export async function login(page: Page, email = ADMIN_EMAIL, password = ADMIN_PASSWORD) {
  await page.goto('/auth/login');
  await page.fill('input[name="email"]', email);
  await page.fill('input[name="password"]', password);
  await page.click('button[type="submit"]');
  await page.waitForURL('**/drive**');
}

/**
 * 회원가입 헬퍼
 */
export async function register(page: Page, email: string, password: string, name: string) {
  await page.goto('/auth/register');
  await page.fill('input[name="name"]', name);
  await page.fill('input[name="email"]', email);
  await page.fill('input[name="password"]', password);
  await page.click('button[type="submit"]');
}

/**
 * API 호출 헬퍼 — CSRF 토큰 자동 추출 후 fetch
 */
export async function apiFetch(
  page: Page,
  url: string,
  options: { method: string; body?: string },
) {
  return page.evaluate(
    async ({ url, options }) => {
      const csrfToken = decodeURIComponent(
        document.cookie
          .split('; ')
          .find((row) => row.startsWith('XSRF-TOKEN='))
          ?.split('=')[1] ?? ''
      );
      const res = await fetch(url, {
        method: options.method,
        headers: {
          'Content-Type': 'application/json',
          'X-XSRF-TOKEN': csrfToken,
        },
        body: options.body,
      });
      return { status: res.status, body: await res.json().catch(() => null) };
    },
    { url, options },
  );
}
