import { test, expect } from '@playwright/test';
import { login, register } from '../../support/helpers';

/**
 * auth 도메인 E2E 테스트
 * 대상 컨트롤러: AuthController (/auth/login, /auth/register, /auth/logout)
 */
test.describe('auth — 인증', () => {

  test('미인증 사용자가 /drive 접근 시 로그인 페이지로 리다이렉트', async ({ page }) => {
    await page.goto('/drive');
    await expect(page).toHaveURL(/\/auth\/login/);
  });

  test('로그인 페이지 렌더링', async ({ page }) => {
    await page.goto('/auth/login');
    await expect(page.locator('input[name="email"]')).toBeVisible();
    await expect(page.locator('input[name="password"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test('회원가입 페이지 렌더링', async ({ page }) => {
    await page.goto('/auth/register');
    await expect(page.locator('input[name="name"]')).toBeVisible();
    await expect(page.locator('input[name="email"]')).toBeVisible();
    await expect(page.locator('input[name="password"]')).toBeVisible();
  });

  test('회원가입 성공 후 로그인 페이지 리다이렉트', async ({ page }) => {
    const unique = Date.now();
    await register(page, `e2e${unique}@test.com`, 'password123', `테스터${unique}`);
    await expect(page).toHaveURL(/\/auth\/login/);
  });

  test('올바른 자격증명으로 로그인 성공', async ({ page }) => {
    await login(page);
    await expect(page).toHaveURL(/\/drive/);
  });

  test('잘못된 비밀번호로 로그인 실패', async ({ page }) => {
    await page.goto('/auth/login');
    await page.fill('input[name="email"]', 'admin@drive.com');
    await page.fill('input[name="password"]', 'wrongpassword');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/error/);
  });

  test('로그아웃 후 세션 종료', async ({ page }) => {
    await login(page);
    // 로그아웃 버튼 클릭 (form POST /auth/logout)
    await page.click('form button:has-text("로그아웃")');
    await expect(page).toHaveURL(/\/auth\/login/);
    // 세션 종료 확인: /drive 접근 시 다시 리다이렉트
    await page.goto('/drive');
    await expect(page).toHaveURL(/\/auth\/login/);
  });

});
