import { expect, test } from '@playwright/test'

const notification = {
  id: 'notification-1',
  type: 'MESSAGE',
  title: 'New message',
  content: 'You have a message about your listing.',
  referenceId: 'conversation-1',
  read: false,
  createdAt: '2026-01-15T10:00:00Z',
}

test('navigates when marking a notification read fails and reloads unread state', async ({ page }) => {
  let notificationListRequests = 0
  let readRequestFinished = false

  await page.addInitScript(() => {
    localStorage.setItem('token', 'e2e-token')
    localStorage.setItem('user', JSON.stringify({
      id: 'user-1',
      email: 'buyer@example.com',
      fullName: 'E2E Buyer',
      role: 'USER',
    }))
  })

  page.routeWebSocket('ws://localhost:8080/ws', (socket) => {
    socket.onMessage((message) => {
      if (typeof message === 'string' && message.startsWith('CONNECT')) {
        socket.send('CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\u0000')
      }
    })
  })

  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname.replace(/^\/api/, '')

    if (request.method() === 'GET' && path === '/posts') {
      return route.fulfill({ json: [] })
    }
    if (request.method() === 'GET' && path === '/categories') {
      return route.fulfill({ json: [] })
    }
    if (request.method() === 'GET' && path === '/notifications/unread-count') {
      return route.fulfill({ json: { unreadCount: 1 } })
    }
    if (request.method() === 'GET' && path === '/notifications') {
      notificationListRequests += 1
      return route.fulfill({
        json: { items: [notification], unreadCount: 1, nextCursor: null, hasMore: false },
      })
    }
    if (request.method() === 'PATCH' && path === `/notifications/${notification.id}/read`) {
      await new Promise((resolve) => setTimeout(resolve, 250))
      await route.fulfill({
        status: 503,
        json: { error: { message: 'Simulated read receipt failure.' } },
      })
      readRequestFinished = true
      return
    }
    if (request.method() === 'GET' && path === '/conversations') {
      return route.fulfill({ json: [] })
    }

    return route.fulfill({ status: 404, json: { error: { message: 'Unexpected E2E API request.' } } })
  })

  await page.goto('/')
  await page.getByRole('button', { name: 'Notifications, 1 unread' }).click()
  const center = page.getByRole('region', { name: 'Notification history' })
  await center.getByRole('button', { name: /New message/ }).click()

  await expect(page).toHaveURL(/\/messages\/conversation-1$/)
  await expect.poll(() => readRequestFinished).toBe(true)
  await expect(page.getByRole('alert')).toHaveCount(0)

  await page.getByRole('button', { name: 'Notifications, 1 unread' }).click()
  await expect.poll(() => notificationListRequests).toBe(2)
  await expect(page.getByText('1 unread', { exact: true })).toBeVisible()
  await expect(page.getByRole('region', { name: 'Notification history' })
    .getByRole('button', { name: /New message/ })
    .locator('[aria-hidden="true"]')).toBeVisible()
  await expect(page.getByRole('alert')).toHaveCount(0)
})
