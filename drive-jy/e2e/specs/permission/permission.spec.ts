import { test, expect } from '@playwright/test';
import { login, register, apiFetch } from '../../support/helpers';

/**
 * permission 도메인 E2E 테스트
 * 대상: PermissionService를 통한 파일 접근 제어 (FileController 경유)
 *
 * 시나리오:
 *   - 소유자는 자신의 파일에 모든 작업 가능
 *   - 다른 사용자는 소유자 파일에 접근/수정 불가
 *   - 권한 없는 파일 다운로드 거부
 */
test.describe('permission — 파일 접근 제어', () => {

  let otherUserEmail: string;

  test.beforeAll(async ({ browser }) => {
    // 두 번째 사용자 사전 등록
    const page = await browser.newPage();
    const ts = Date.now();
    otherUserEmail = `other_${ts}@test.com`;
    await register(page, otherUserEmail, 'password123', `다른사람_${ts}`);
    await page.close();
  });

  test.describe('소유자 권한', () => {

    test.beforeEach(async ({ page }) => {
      await login(page);
    });

    test('소유자는 자신이 만든 폴더 이름 변경 가능', async ({ page }) => {
      const created = await apiFetch(page, '/api/files/folders', {
        method: 'POST',
        body: JSON.stringify({ name: '원본폴더' }),
      });
      expect(created.status).toBe(201);
      const folderId = created.body.data.id;

      const renamed = await apiFetch(page, `/api/files/${folderId}/rename`, {
        method: 'PATCH',
        body: JSON.stringify({ name: '변경된폴더' }),
      });
      expect(renamed.status).toBe(200);
      expect(renamed.body.data.name).toBe('변경된폴더');
    });

    test('소유자는 자신이 만든 파일 휴지통 이동 가능', async ({ page }) => {
      const created = await apiFetch(page, '/api/files/folders', {
        method: 'POST',
        body: JSON.stringify({ name: '삭제할폴더' }),
      });
      const folderId = created.body.data.id;

      const deleted = await apiFetch(page, `/api/files/${folderId}`, {
        method: 'DELETE',
      });
      expect(deleted.status).toBe(204);
    });

  });

  test.describe('비소유자 접근 제어', () => {

    let ownerFileId: number;

    test.beforeAll(async ({ browser }) => {
      // admin(소유자)이 파일을 만들어 둠
      const page = await browser.newPage();
      await login(page);
      const created = await apiFetch(page, '/api/files/folders', {
        method: 'POST',
        body: JSON.stringify({ name: '소유자전용폴더' }),
      });
      ownerFileId = created.body.data.id;
      await page.close();
    });

    test('다른 사용자는 소유자 파일 이름 변경 불가 — 403 반환', async ({ page }) => {
      await login(page, otherUserEmail, 'password123');

      const result = await apiFetch(page, `/api/files/${ownerFileId}/rename`, {
        method: 'PATCH',
        body: JSON.stringify({ name: '침해시도' }),
      });
      expect([403, 404]).toContain(result.status);
    });

    test('다른 사용자는 소유자 파일 삭제 불가 — 403 반환', async ({ page }) => {
      await login(page, otherUserEmail, 'password123');

      const result = await apiFetch(page, `/api/files/${ownerFileId}`, {
        method: 'DELETE',
      });
      expect([403, 404]).toContain(result.status);
    });

    test('다른 사용자는 소유자 파일 다운로드 불가 — 403 반환', async ({ page }) => {
      await login(page, otherUserEmail, 'password123');

      const status = await page.evaluate(async (fileId) => {
        const csrfToken = decodeURIComponent(
          document.cookie
            .split('; ')
            .find((row) => row.startsWith('XSRF-TOKEN='))
            ?.split('=')[1] ?? ''
        );
        const res = await fetch(`/api/files/${fileId}/download`, {
          headers: { 'X-XSRF-TOKEN': csrfToken },
          redirect: 'manual',
        });
        return res.status === 0 ? 302 : res.status;
      }, ownerFileId);
      expect([302, 403, 404]).toContain(status);
    });

  });

  test.describe('미인증 접근 차단', () => {

    test('세션 없는 파일 API 접근은 리다이렉트 또는 거부', async ({ page }) => {
      await page.goto('/auth/login');
      const status = await page.evaluate(async () => {
        const res = await fetch('/api/files/1/rename', {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name: '무단변경' }),
          credentials: 'omit',
          redirect: 'manual',
        });
        return res.status === 0 ? 302 : res.status;
      });
      expect([302, 401, 403]).toContain(status);
    });

  });

});
