import { test, expect } from '@playwright/test';
import { login, apiFetch } from '../../support/helpers';

/**
 * file 도메인 E2E 테스트
 * 대상 컨트롤러: DriveController (/drive, /drive/folder/{id})
 *               FileController (/api/files/**, /api/files/folders)
 */
test.describe('file — 파일/폴더 관리', () => {

  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('/drive 페이지 렌더링 — 사이드바와 헤더 확인', async ({ page }) => {
    await expect(page).toHaveURL(/\/drive/);
    // 메인 헤더 확인
    await expect(page.locator('h1').filter({ hasText: '내 드라이브' })).toBeVisible();
    // 사이드바 항목 확인
    await expect(page.getByRole('link', { name: '내 드라이브' }).first()).toBeVisible();
    await expect(page.getByRole('link', { name: '휴지통' })).toBeVisible();
  });

  test('API: 폴더 생성 성공', async ({ page }) => {
    const folderName = `E2E폴더_${Date.now()}`;
    const result = await apiFetch(page, '/api/files/folders', {
      method: 'POST',
      body: JSON.stringify({ name: folderName }),
    });
    expect(result.status).toBe(201);
    expect(result.body.success).toBe(true);
    expect(result.body.data.name).toBe(folderName);
    expect(result.body.data.type).toBe('FOLDER');
  });

  test('API: 파일 업로드 성공', async ({ page }) => {
    // 파일 업로드는 multipart — fetch로 직접 전송
    const result = await page.evaluate(async () => {
      const csrfToken =
        document.cookie
          .split('; ')
          .find((row) => row.startsWith('XSRF-TOKEN='))
          ?.split('=')[1] ?? '';
      const formData = new FormData();
      const blob = new Blob(['E2E 테스트 파일 내용'], { type: 'text/plain' });
      formData.append('file', blob, 'e2e-test.txt');
      const res = await fetch('/api/files/upload', {
        method: 'POST',
        headers: { 'X-XSRF-TOKEN': csrfToken },
        body: formData,
      });
      return { status: res.status, body: await res.json().catch(() => null) };
    });
    expect(result.status).toBe(201);
    expect(result.body.success).toBe(true);
    expect(result.body.data.type).toBe('FILE');
  });

  test('API: 폴더 생성 후 이름 변경', async ({ page }) => {
    // 폴더 생성
    const created = await apiFetch(page, '/api/files/folders', {
      method: 'POST',
      body: JSON.stringify({ name: '원래이름' }),
    });
    const folderId = created.body.data.id;

    // 이름 변경
    const renamed = await apiFetch(page, `/api/files/${folderId}/rename`, {
      method: 'PATCH',
      body: JSON.stringify({ name: '바뀐이름' }),
    });
    expect(renamed.status).toBe(200);
    expect(renamed.body.data.name).toBe('바뀐이름');
  });

  test('API: 파일 휴지통 이동', async ({ page }) => {
    // 폴더 생성
    const created = await apiFetch(page, '/api/files/folders', {
      method: 'POST',
      body: JSON.stringify({ name: '삭제대상폴더' }),
    });
    const folderId = created.body.data.id;

    // 휴지통 이동
    const deleted = await apiFetch(page, `/api/files/${folderId}`, {
      method: 'DELETE',
    });
    expect(deleted.status).toBe(204);
  });

  test('API 인증 필요 — 세션 없는 요청 거부', async ({ page }) => {
    // credentials: 'omit' → 세션 쿠키 미포함, redirect: 'manual' → 302 자동 추적 차단
    // status 0 = opaque redirect (302 리다이렉트됨 = 미인증 처리 정상)
    const status = await page.evaluate(async () => {
      const r = await fetch('/api/files/1', {
        credentials: 'omit',
        redirect: 'manual',
      });
      return r.status === 0 ? 302 : r.status;
    });
    expect([302, 401, 403]).toContain(status);
  });

});
