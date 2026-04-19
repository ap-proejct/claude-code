import { test, expect } from '@playwright/test';
import { login, apiFetch } from '../../support/helpers';

/**
 * share 도메인 E2E 테스트
 * 대상: ShareApiController (/api/share/**), ShareViewController (/share/{token})
 */
test.describe('share — 공유 링크', () => {

  test.describe('인증 사용자 공유 링크 관리', () => {

    test.beforeEach(async ({ page }) => {
      await login(page);
    });

    test('공유 링크 생성 성공', async ({ page }) => {
      const folder = await apiFetch(page, '/api/files/folders', {
        method: 'POST',
        body: JSON.stringify({ name: `공유폴더_${Date.now()}` }),
      });
      const fileId = folder.body.data.id;

      const result = await apiFetch(page, `/api/share/${fileId}`, {
        method: 'POST',
        body: JSON.stringify({ permission: 'VIEWER' }),
      });
      expect(result.status).toBe(201);
      expect(result.body.data.token).toHaveLength(64);
      expect(result.body.data.permission).toBe('VIEWER');
    });

    test('공유 링크 목록 조회', async ({ page }) => {
      const folder = await apiFetch(page, '/api/files/folders', {
        method: 'POST',
        body: JSON.stringify({ name: `링크목록_${Date.now()}` }),
      });
      const fileId = folder.body.data.id;

      await apiFetch(page, `/api/share/${fileId}`, {
        method: 'POST',
        body: JSON.stringify({ permission: 'VIEWER' }),
      });

      const list = await apiFetch(page, `/api/share/${fileId}/links`, { method: 'GET' });
      expect(list.status).toBe(200);
      expect(list.body.data.length).toBeGreaterThanOrEqual(1);
    });

    test('공유 링크 취소', async ({ page }) => {
      const folder = await apiFetch(page, '/api/files/folders', {
        method: 'POST',
        body: JSON.stringify({ name: `취소폴더_${Date.now()}` }),
      });
      const fileId = folder.body.data.id;

      const created = await apiFetch(page, `/api/share/${fileId}`, {
        method: 'POST',
        body: JSON.stringify({ permission: 'VIEWER' }),
      });
      const token = created.body.data.token;

      const revoked = await apiFetch(page, `/api/share/${fileId}/${token}`, { method: 'DELETE' });
      expect(revoked.status).toBe(204);

      // 취소 후 resolve 실패 확인
      const resolved = await apiFetch(page, `/api/share/resolve/${token}`, { method: 'GET' });
      expect(resolved.status).toBe(404);
    });

  });

  test.describe('공유 링크 접근 — 인증 불필요', () => {

    let sharedToken: string;
    let sharedFileId: number;

    test.beforeAll(async ({ browser }) => {
      const page = await browser.newPage();
      await login(page);

      const folder = await apiFetch(page, '/api/files/folders', {
        method: 'POST',
        body: JSON.stringify({ name: `공개폴더_${Date.now()}` }),
      });
      sharedFileId = folder.body.data.id;

      const link = await apiFetch(page, `/api/share/${sharedFileId}`, {
        method: 'POST',
        body: JSON.stringify({ permission: 'VIEWER' }),
      });
      sharedToken = link.body.data.token;
      await page.close();
    });

    test('공유 토큰으로 파일 정보 조회 - 비인증', async ({ page }) => {
      await page.goto('/auth/login');
      const result = await page.evaluate(async (token) => {
        const res = await fetch(`/api/share/resolve/${token}`);
        return { status: res.status, body: await res.json().catch(() => null) };
      }, sharedToken);

      expect(result.status).toBe(200);
      expect(result.body.data.permission).toBe('VIEWER');
    });

    test('공유 페이지 렌더링 - 비인증', async ({ page }) => {
      await page.goto(`/share/${sharedToken}`);
      await expect(page).toHaveURL(new RegExp(`/share/${sharedToken}`));
      await expect(page.locator('h1')).toBeVisible();
    });

    test('잘못된 토큰으로 resolve 시도 - 404', async ({ page }) => {
      await page.goto('/auth/login');
      const result = await page.evaluate(async () => {
        const res = await fetch('/api/share/resolve/invalid-token-xyz-000');
        return res.status;
      });
      expect(result).toBe(404);
    });

  });

  test.describe('미인증 접근 차단', () => {

    test('공유 링크 생성은 인증 필요', async ({ page }) => {
      await page.goto('/auth/login');
      const status = await page.evaluate(async () => {
        const res = await fetch('/api/share/1', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ permission: 'VIEWER' }),
          credentials: 'omit',
          redirect: 'manual',
        });
        return res.status === 0 ? 302 : res.status;
      });
      expect([302, 401, 403]).toContain(status);
    });

  });

});
