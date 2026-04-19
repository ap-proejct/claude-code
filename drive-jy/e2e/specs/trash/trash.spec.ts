import { test, expect } from '@playwright/test';
import { login, apiFetch } from '../../support/helpers';

/**
 * trash 도메인 E2E 테스트
 * 대상: TrashController (/trash, /api/trash/**)
 */
test.describe('trash — 휴지통', () => {

  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('GET /trash - 휴지통 페이지 렌더링', async ({ page }) => {
    await page.goto('/trash');
    await expect(page).toHaveURL(/\/trash/);
  });

  test('API: 폴더 생성 후 휴지통 이동', async ({ page }) => {
    const created = await apiFetch(page, '/api/files/folders', {
      method: 'POST',
      body: JSON.stringify({ name: `휴지통이동폴더_${Date.now()}` }),
    });
    expect(created.status).toBe(201);
    const folderId = created.body.data.id;

    const trashed = await apiFetch(page, `/api/files/${folderId}`, { method: 'DELETE' });
    expect(trashed.status).toBe(204);

    const list = await apiFetch(page, '/api/trash', { method: 'GET' });
    expect(list.status).toBe(200);
    expect(list.body.data.some((f: { id: number }) => f.id === folderId)).toBe(true);
  });

  test('API: 휴지통 파일 복구', async ({ page }) => {
    const created = await apiFetch(page, '/api/files/folders', {
      method: 'POST',
      body: JSON.stringify({ name: `복구폴더_${Date.now()}` }),
    });
    const folderId = created.body.data.id;

    await apiFetch(page, `/api/files/${folderId}`, { method: 'DELETE' });

    const restored = await apiFetch(page, `/api/trash/${folderId}/restore`, { method: 'POST' });
    expect(restored.status).toBe(204);

    const list = await apiFetch(page, '/api/trash', { method: 'GET' });
    expect(list.body.data.some((f: { id: number }) => f.id === folderId)).toBe(false);
  });

  test('API: 휴지통 파일 영구 삭제', async ({ page }) => {
    const created = await apiFetch(page, '/api/files/folders', {
      method: 'POST',
      body: JSON.stringify({ name: `영구삭제폴더_${Date.now()}` }),
    });
    const folderId = created.body.data.id;

    await apiFetch(page, `/api/files/${folderId}`, { method: 'DELETE' });

    const deleted = await apiFetch(page, `/api/trash/${folderId}`, { method: 'DELETE' });
    expect(deleted.status).toBe(204);

    const list = await apiFetch(page, '/api/trash', { method: 'GET' });
    expect(list.body.data.some((f: { id: number }) => f.id === folderId)).toBe(false);
  });

  test('API: 휴지통 전체 비우기', async ({ page }) => {
    await apiFetch(page, '/api/files/folders', {
      method: 'POST',
      body: JSON.stringify({ name: `비우기대상_${Date.now()}` }),
    }).then(r => apiFetch(page, `/api/files/${r.body.data.id}`, { method: 'DELETE' }));

    const emptied = await apiFetch(page, '/api/trash', { method: 'DELETE' });
    expect(emptied.status).toBe(204);

    const list = await apiFetch(page, '/api/trash', { method: 'GET' });
    expect(list.body.data).toHaveLength(0);
  });

  test('API: 휴지통이 아닌 파일 영구 삭제 시도 - 실패', async ({ page }) => {
    const created = await apiFetch(page, '/api/files/folders', {
      method: 'POST',
      body: JSON.stringify({ name: `정상폴더_${Date.now()}` }),
    });
    const folderId = created.body.data.id;

    const result = await apiFetch(page, `/api/trash/${folderId}`, { method: 'DELETE' });
    expect([400, 422]).toContain(result.status);
  });

  test('미인증 사용자는 휴지통 API 접근 불가', async ({ page }) => {
    const status = await page.evaluate(async () => {
      const res = await fetch('/api/trash', { credentials: 'omit', redirect: 'manual' });
      return res.status === 0 ? 302 : res.status;
    });
    expect([302, 401, 403]).toContain(status);
  });

});
