import { expect, test } from '@playwright/test'

// Must be publicly reachable: the crawler refuses private and loopback targets.
const targetUrl = process.env.E2E_TARGET_URL ?? 'https://example.com'
const auditTimeout = Number(process.env.E2E_AUDIT_TIMEOUT_MS ?? 180_000)

test('register, add a website, run an audit and download the report', async ({ page }) => {
  const email = `e2e-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@example.com`

  // Behind Caddy the production CSP applies; anything it blocks is a bug.
  const cspViolations: string[] = []
  page.on('console', (message) => {
    if (message.type() === 'error' && /Content[- ]Security[- ]Policy/i.test(message.text())) {
      cspViolations.push(message.text())
    }
  })

  await test.step('register', async () => {
    await page.goto('/register')
    await page.getByLabel('Name').fill('E2E Smoke')
    await page.getByLabel('Email').fill(email)
    await page.getByLabel('Password').fill('smoke-test-passphrase-42')
    await page.getByRole('button', { name: 'Create account' }).click()
    await expect(page).toHaveURL(/\/dashboard$/)
  })

  await test.step('add website', async () => {
    await page.goto('/websites')
    await page.getByRole('main').getByRole('button', { name: 'Add website' }).first().click()
    const dialog = page.getByRole('dialog')
    await dialog.getByLabel('Website URL').fill(targetUrl)
    await dialog.getByRole('button', { name: 'Add website' }).click()
    await expect(dialog).toBeHidden()
  })

  await test.step('run audit', async () => {
    await page.goto('/audits')
    await page.getByRole('button', { name: 'Run audit' }).first().click()
    await expect(page).toHaveURL(/\/audits\/\d+$/)
  })

  await test.step('wait for completion', async () => {
    // The report button is disabled while the audit is active; failed and
    // cancelled audits also show an "Audit error" alert.
    await expect(page.getByRole('button', { name: 'Download report' }).first()).toBeEnabled({
      timeout: auditTimeout,
    })
    await expect(page.getByText('Audit error')).toHaveCount(0)
    await expect(page.getByRole('button', { name: 'Cancel audit' })).toHaveCount(0)
  })

  await test.step('download report', async () => {
    await page.getByRole('button', { name: 'Download report' }).first().click()
    const downloadPromise = page.waitForEvent('download')
    await page.getByRole('menuitem', { name: /HTML report/ }).click()
    const download = await downloadPromise
    expect(download.suggestedFilename()).toMatch(/^seopulse-audit-\d+-.*\.html$/)
  })

  await test.step('session survives a reload via the refresh cookie', async () => {
    await page.reload()
    await expect(page).toHaveURL(/\/audits\/\d+$/)
    await expect(page.getByRole('button', { name: 'Download report' }).first()).toBeVisible()
  })

  expect(cspViolations).toEqual([])
})
