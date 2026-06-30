import { expect, test } from '@playwright/test';

async function openHomeAsUser(page: import('@playwright/test').Page) {
  await page.addInitScript(() => {
    localStorage.setItem('buildgraph.token', 'jwt-user-token');
  });
  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 'user-1004',
        email: 'user@example.com',
        name: '테스트 사용자',
        role: 'USER'
      })
    });
  });
  await page.goto('/');
}

test('renders the home command center with primary quote actions', async ({ page }) => {
  await openHomeAsUser(page);
  const main = page.getByRole('main');

  await expect(main.getByRole('heading', { name: '어떤 PC 견적이 필요하세요?' })).toBeVisible();
  await expect(main.getByRole('textbox', { name: '원하는 PC 사양 입력' })).toBeVisible();
  await expect(main.getByRole('button', { name: 'QHD 게임' })).toBeVisible();
  await expect(main.getByRole('button', { name: 'AI CUDA 실습' })).toBeVisible();
  await expect(main.getByRole('button', { name: '견적 상담 시작' })).toBeVisible();
});

test('keeps shared header and navigation destinations unchanged', async ({ page }) => {
  await openHomeAsUser(page);
  const header = page.locator('header');
  const nav = page.getByRole('navigation');

  await expect(header.getByRole('link', { name: 'AI 견적' })).toHaveAttribute('href', '/requirements/new');
  await expect(header.getByRole('link', { name: '내 견적함' })).toHaveAttribute('href', '/my/quotes');
  await expect(header.getByRole('link', { name: 'AS 접수' })).toHaveAttribute('href', '/support/new');
  await expect(nav.getByRole('link', { name: '홈' })).toHaveAttribute('href', '/');
  await expect(nav.getByRole('link', { name: '셀프 견적' })).toHaveAttribute('href', '/self-quote');
  await expect(nav.getByRole('link', { name: '관리자' })).toHaveAttribute('href', '/admin');
});

test('keeps the home command center usable on mobile width', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await openHomeAsUser(page);
  const main = page.getByRole('main');

  await expect(main.getByRole('heading', { name: '어떤 PC 견적이 필요하세요?' })).toBeVisible();
  await expect(main.getByRole('button', { name: '견적 상담 시작' })).toBeVisible();

  const hasBodyOverflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1);
  expect(hasBodyOverflow).toBe(false);
});

test('starts a local consultation and renders simulated recommendations', async ({ page }) => {
  await openHomeAsUser(page);
  const main = page.getByRole('main');

  await main.getByRole('textbox', { name: '원하는 PC 사양 입력' }).fill('200만원 안에서 QHD 게임과 개발을 같이 할 PC 추천해줘. NVIDIA 선호.');
  await main.getByRole('button', { name: '견적 상담 시작' }).click();

  await expect(main.getByRole('heading', { name: 'QHD 게임 추천을 조정했습니다' })).toBeVisible();
  await expect(main.getByText('QHD 게임 균형형')).toBeVisible();
  await expect(main.getByText('Tool 검증')).toBeVisible();
  await expect(page.getByTestId('assistant-bar')).toBeVisible();
  await expect(page.getByRole('textbox', { name: 'AI에게 추가 질문' })).toBeVisible();
});

test('updates recommendation cards from a follow-up chat question', async ({ page }) => {
  await openHomeAsUser(page);
  const main = page.getByRole('main');

  await main.getByRole('textbox', { name: '원하는 PC 사양 입력' }).fill('200만원 QHD 게임용 PC');
  await main.getByRole('button', { name: '견적 상담 시작' }).click();
  await page.getByRole('textbox', { name: 'AI에게 추가 질문' }).fill('저소음으로 바꿔줘');
  await page.getByRole('button', { name: '질문 보내기' }).click();

  await expect(main.getByRole('heading', { name: '저소음 추천을 조정했습니다' })).toBeVisible();
  await expect(main.getByText('저소음 균형형')).toBeVisible();
  await expect(page.getByText('쿨링 소음과 전력 여유를 우선해서 다시 정렬했습니다.')).toBeVisible();
});

test('lets desktop users drag the assistant bar', async ({ page }) => {
  await openHomeAsUser(page);
  const main = page.getByRole('main');

  await main.getByRole('textbox', { name: '원하는 PC 사양 입력' }).fill('AI CUDA 실습용 300만원 PC');
  await main.getByRole('button', { name: '견적 상담 시작' }).click();

  const assistantBar = page.getByTestId('assistant-bar');
  const before = await assistantBar.boundingBox();
  expect(before).not.toBeNull();

  await page.mouse.move(before!.x + 20, before!.y + 20);
  await page.mouse.down();
  await page.mouse.move(before!.x - 160, before!.y - 120, { steps: 8 });
  await page.mouse.up();

  const after = await assistantBar.boundingBox();
  expect(after).not.toBeNull();
  expect(Math.abs(after!.x - before!.x)).toBeGreaterThan(40);
  expect(Math.abs(after!.y - before!.y)).toBeGreaterThan(40);
});
