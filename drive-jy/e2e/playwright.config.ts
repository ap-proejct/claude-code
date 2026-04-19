import { defineConfig, devices } from '@playwright/test';

/**
 * Google Drive 클론 E2E 테스트 설정.
 *
 * 실행 전 Spring Boot 서버가 기동되어 있어야 합니다:
 *   ./gradlew.bat bootRun   (별도 터미널)
 *
 * 실행:
 *   cd e2e && npx playwright test
 *   cd e2e && npx playwright test --headed   (브라우저 화면 표시)
 *   cd e2e && npx playwright test --debug    (디버그 모드)
 */
export default defineConfig({
  testDir: './specs',
  fullyParallel: false,       // 순서 의존 시나리오 방지
  forbidOnly: !!process.env.CI,
  retries: 1,
  workers: 1,                 // 단일 워커 — 공유 DB 상태 충돌 방지
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
  ],

  use: {
    baseURL: 'http://localhost:8080',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'off',
    locale: 'ko-KR',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
