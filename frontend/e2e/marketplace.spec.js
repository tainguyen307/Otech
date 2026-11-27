import { expect, test } from '@playwright/test'

const category = { id: 'books', name: 'Books' }
const product = {
  id: 'product-1',
  title: 'Reusable notebook',
  description: 'A lightly used notebook in good condition.',
  price: 42000,
  condition: 'GOOD',
  location: 'District 1',
  imageUrls: [],
  categoryId: category.id,
  categoryName: category.name,
  status: 'SELLING',
  seller: { id: 'seller-1', fullName: 'Test Seller', avatarUrl: null },
}

async function mockMarketplaceApi(page, { products = [product], holdListingErrors = false } = {}) {
  const requestedCategoryIds = []
  let canLoadListings = !holdListingErrors

  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname.replace(/^\/api/, '')

    if (request.method() === 'GET' && path === '/categories') {
      return route.fulfill({ json: [category] })
    }
    if (request.method() === 'GET' && path === '/posts') {
      return route.fulfill({ json: [] })
    }
    if (request.method() === 'GET' && path === '/products/featured') {
      return route.fulfill({ json: [] })
    }
    if (request.method() === 'GET' && path === '/products') {
      requestedCategoryIds.push(url.searchParams.get('categoryId'))
      if (!canLoadListings) {
        return route.fulfill({
          status: 503,
          json: { error: { message: 'Simulated listing service error.' } },
        })
      }
      return route.fulfill({ json: { items: products, nextCursor: null, hasMore: false } })
    }
    if (request.method() === 'GET' && path === `/products/${product.id}/reviews`) {
      return route.fulfill({ json: { averageRating: 0, reviewCount: 0, reviews: [] } })
    }

    return route.fulfill({ status: 404, json: { error: { message: 'Unexpected E2E API request.' } } })
  })

  return {
    requestedCategoryIds,
    allowListingSuccess() {
      canLoadListings = true
    },
  }
}

test('browses a category, opens a listing, and shows its review summary', async ({ page }) => {
  const api = await mockMarketplaceApi(page)

  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Browse by category' })).toBeVisible()

  const categoryCard = page.locator('article').filter({
    has: page.getByRole('heading', { name: category.name, exact: true }),
  })
  await categoryCard.getByRole('button', { name: 'Listings' }).click()

  await expect(page).toHaveURL(/\/marketplace\?categoryId=books$/)
  await expect(page.getByRole('heading', { name: 'Books listings' })).toBeVisible()
  await expect(page.getByRole('button', { name: /Reusable notebook/ })).toBeVisible()
  expect(api.requestedCategoryIds).toContain(category.id)

  await page.getByRole('button', { name: /Reusable notebook/ }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await expect(page.getByRole('heading', { name: product.title, level: 2 })).toBeVisible()
  await expect(page.getByText('No verified purchase reviews yet.')).toBeVisible()
})

test('recovers from a listing API error when the user retries', async ({ page }) => {
  const api = await mockMarketplaceApi(page, { holdListingErrors: true })

  await page.goto('/marketplace')
  const errorAlert = page.getByRole('alert')
  await expect(errorAlert).toContainText('Simulated listing service error.')

  api.allowListingSuccess()
  await errorAlert.getByRole('button', { name: 'Try again' }).click()

  await expect(errorAlert).toHaveCount(0)
  await expect(page.getByRole('button', { name: /Reusable notebook/ })).toBeVisible()
})

test('shows the empty state and opens sign-in when a guest tries to list', async ({ page }) => {
  await mockMarketplaceApi(page, { products: [] })

  await page.goto('/marketplace')
  await expect(page.getByText('No items match those filters')).toBeVisible()
  await page.getByRole('button', { name: 'Sign in to list' }).click()
  await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible()
})
