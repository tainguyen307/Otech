import { useEffect, useRef, useState } from 'react'
import { ArrowDownWideNarrow, LoaderCircle, MapPin, MessageCircle, Plus, RefreshCw, Search, ShieldCheck, Tag, X, Zap } from 'lucide-react'
import BillingCenter from '../components/BillingCenter'
import ProductCard from '../components/ProductCard'
import ProductForm from '../components/ProductForm'
import { apiErrorMessage, apiFetch } from '../lib/api'

const conditionOptions = [
  ['NEW', 'New'],
  ['LIKE_NEW', 'Like new'],
  ['GOOD', 'Good'],
  ['FAIR', 'Fair'],
  ['POOR', 'Well loved'],
]

const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })

export default function MarketplaceScreen({ user, onAuth, onStartConversation, onOpenMessages, initialCategoryId = '' }) {
  const [categories, setCategories] = useState([])
  const [products, setProducts] = useState([])
  const [featuredProducts, setFeaturedProducts] = useState([])
  const [featuredError, setFeaturedError] = useState('')
  const [categoriesError, setCategoriesError] = useState('')
  const [view, setView] = useState('browse')
  const [filters, setFilters] = useState(() => ({ search: '', categoryId: initialCategoryId, minPrice: '', maxPrice: '', condition: '', location: '', sort: 'newest', status: 'SELLING' }))
  const [appliedFilters, setAppliedFilters] = useState(() => ({ search: '', categoryId: initialCategoryId, minPrice: '', maxPrice: '', condition: '', location: '', sort: 'newest', status: 'SELLING' }))
  const [nextCursor, setNextCursor] = useState(null)
  const [hasMore, setHasMore] = useState(false)
  const [selectedProduct, setSelectedProduct] = useState(null)
  const [productReviews, setProductReviews] = useState(null)
  const [reviewsError, setReviewsError] = useState('')
  const [billingOpen, setBillingOpen] = useState(false)
  const [billingProduct, setBillingProduct] = useState(null)
  const [billingRefreshKey, setBillingRefreshKey] = useState(0)
  const [editingProduct, setEditingProduct] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [isLoadingMore, setIsLoadingMore] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState('')
  const [moreError, setMoreError] = useState('')
  const [actionError, setActionError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [galleryIndex, setGalleryIndex] = useState(0)
  const dialogRef = useRef(null)

  useEffect(() => {
    let active = true
    apiFetch('/categories')
      .then(async (response) => {
        if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load categories.'))
        return response.json()
      })
      .then((body) => { if (active) setCategories(Array.isArray(body) ? body : []) })
      .catch((requestError) => { if (active) setCategoriesError(requestError.message || 'Could not load categories.') })
    return () => { active = false }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    async function load() {
      setIsLoading(true)
      setError('')
      setProducts([])
      setNextCursor(null)
      setHasMore(false)
      try {
        if (view === 'mine') {
          const response = await apiFetch('/user/products?limit=24', { signal: controller.signal })
          if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load your listings.'))
          const body = await response.json()
          setProducts(Array.isArray(body.items) ? body.items : [])
          setNextCursor(body.nextCursor || null)
          setHasMore(Boolean(body.hasMore))
        } else {
          const params = new URLSearchParams()
          Object.entries(appliedFilters).forEach(([key, value]) => {
            if (value !== '' && value != null) params.set(key, value)
          })
          params.set('limit', '24')
          const response = await apiFetch(`/products?${params.toString()}`, { signal: controller.signal })
          if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load listings.'))
          const body = await response.json()
          setProducts(Array.isArray(body.items) ? body.items : [])
          setNextCursor(body.nextCursor || null)
          setHasMore(Boolean(body.hasMore))
        }
      } catch (requestError) {
        if (requestError.name !== 'AbortError') setError(requestError.message || 'Could not load listings.')
      } finally {
        if (!controller.signal.aborted) setIsLoading(false)
      }
    }
    load()
    return () => controller.abort()
  }, [appliedFilters, refreshKey, view])

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (selectedProduct && !dialog.open) dialog.showModal()
    if (!selectedProduct && dialog.open) dialog.close()
  }, [selectedProduct])

  useEffect(() => {
    if (view !== 'browse' || appliedFilters.status !== 'SELLING') return undefined
    const controller = new AbortController()
    const params = new URLSearchParams()
    if (appliedFilters.categoryId) params.set('categoryId', appliedFilters.categoryId)
    apiFetch(`/products/featured${params.size ? `?${params}` : ''}`, { signal: controller.signal })
      .then(async (response) => {
        if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load featured listings.'))
        return response.json()
      })
      .then((items) => {
        if (!controller.signal.aborted) {
          setFeaturedProducts(Array.isArray(items) ? items : [])
          setFeaturedError('')
        }
      })
      .catch((requestError) => {
        if (requestError.name !== 'AbortError' && !controller.signal.aborted) {
          setFeaturedError(requestError.message || 'Could not load featured listings.')
        }
      })
    return () => controller.abort()
  }, [appliedFilters.categoryId, appliedFilters.status, view, refreshKey])

  useEffect(() => {
    if (!selectedProduct) return undefined
    const controller = new AbortController()
    apiFetch(`/products/${selectedProduct.id}/reviews`, { signal: controller.signal })
      .then(async (response) => {
        if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load product reviews.'))
        return response.json()
      })
      .then((body) => {
        if (!controller.signal.aborted) {
          setProductReviews({ productId: selectedProduct.id, body })
          setReviewsError(null)
        }
      })
      .catch((requestError) => {
        if (requestError.name !== 'AbortError' && !controller.signal.aborted) {
          setReviewsError({ productId: selectedProduct.id, message: requestError.message || 'Could not load product reviews.' })
        }
      })
    return () => controller.abort()
  }, [selectedProduct])

  async function loadMore() {
    if (!nextCursor || isLoadingMore) return
    setIsLoadingMore(true)
    setMoreError('')
    try {
      const params = new URLSearchParams()
      Object.entries(appliedFilters).forEach(([key, value]) => {
        if (value !== '' && value != null) params.set(key, value)
      })
      params.set('limit', '24')
      params.set('cursor', nextCursor)
      const endpoint = view === 'mine' ? '/user/products' : '/products'
      const response = await apiFetch(`${endpoint}?${params.toString()}`)
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load more listings.'))
      const body = await response.json()
      setProducts((current) => [...current, ...(Array.isArray(body.items) ? body.items : [])])
      setNextCursor(body.nextCursor || null)
      setHasMore(Boolean(body.hasMore))
    } catch (requestError) {
      setMoreError(requestError.message || 'Could not load more listings.')
    } finally {
      setIsLoadingMore(false)
    }
  }

  async function saveProduct(values) {
    setIsSaving(true)
    try {
      const isEditing = Boolean(editingProduct)
      const response = await apiFetch(isEditing ? `/products/${editingProduct.id}` : '/products', {
        method: isEditing ? 'PUT' : 'POST',
        body: JSON.stringify(values),
      })
      if (!response.ok) {
        const message = await apiErrorMessage(response, 'Could not save this listing.')
        if (response.status === 402) {
          setBillingProduct(null)
          setBillingOpen(true)
        }
        throw new Error(message)
      }
      const product = await response.json()
      setProducts((current) => {
        if (!isEditing && view === 'mine') return [product, ...current]
        return current.map((item) => item.id === product.id ? product : item)
      })
      setRefreshKey((value) => value + 1)
      setBillingRefreshKey((value) => value + 1)
      setShowForm(false)
      setEditingProduct(null)
      setActionError('')
    } finally {
      setIsSaving(false)
    }
  }

  async function updateStatus(product, status) {
    setActionError('')
    try {
      const response = await apiFetch(`/products/${product.id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
      })
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not update listing status.'))
      const updated = await response.json()
      setProducts((current) => current.map((item) => item.id === updated.id ? updated : item))
      setSelectedProduct((current) => current?.id === updated.id ? updated : current)
      setRefreshKey((value) => value + 1)
    } catch (requestError) {
      setActionError(requestError.message || 'Could not update listing status.')
    }
  }

  async function archiveProduct(product) {
    setActionError('')
    try {
      const response = await apiFetch(`/products/${product.id}`, { method: 'DELETE' })
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not archive listing.'))
      setSelectedProduct(null)
      setRefreshKey((value) => value + 1)
    } catch (requestError) {
      setActionError(requestError.message || 'Could not archive listing.')
    }
  }

  async function startChat(product) {
    if (!user) {
      onAuth()
      return
    }
    setActionError('')
    try {
      const response = await apiFetch('/conversations', {
        method: 'POST',
        body: JSON.stringify({ productId: product.id }),
      })
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not start a conversation.'))
      const conversation = await response.json()
      setSelectedProduct(null)
      onStartConversation(conversation.id)
    } catch (requestError) {
      setActionError(requestError.message || 'Could not start a conversation.')
    }
  }

  async function openProductMessages(product) {
    if (!user) {
      onAuth()
      return
    }
    setActionError('')
    try {
      const response = await apiFetch('/conversations')
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load your conversations.'))
      const conversations = await response.json()
      const match = conversations.find((item) => item.product?.id === product.id)
      if (!match) {
        setActionError('No buyer conversation exists for this listing yet.')
        return
      }
      onOpenMessages(match.id)
    } catch (requestError) {
      setActionError(requestError.message || 'Could not load your conversations.')
    }
  }

  function editProduct(product) {
    setSelectedProduct(null)
    setEditingProduct(product)
    setShowForm(true)
  }

  function submitFilters(event) {
    event.preventDefault()
    const nextFilters = { ...filters }
    setAppliedFilters(nextFilters)
    const url = new URL(window.location.href)
    if (nextFilters.categoryId) url.searchParams.set('categoryId', nextFilters.categoryId)
    else url.searchParams.delete('categoryId')
    window.history.replaceState(window.history.state, '', `${url.pathname}${url.search}${url.hash}`)
  }

  function resetFilters() {
    const emptyFilters = { search: '', categoryId: '', minPrice: '', maxPrice: '', condition: '', location: '', sort: 'newest', status: 'SELLING' }
    setFilters(emptyFilters)
    setAppliedFilters(emptyFilters)
    const url = new URL(window.location.href)
    url.searchParams.delete('categoryId')
    window.history.replaceState(window.history.state, '', `${url.pathname}${url.search}${url.hash}`)
  }

  const isOwner = (product) => product.seller?.id === user?.id
  const selectedIsOwner = selectedProduct ? isOwner(selectedProduct) : false
  const selectedCategoryName = categories.find((category) => category.id === appliedFilters.categoryId)?.name
  const activeReviews = productReviews && productReviews.productId === selectedProduct?.id ? productReviews.body : null
  const activeReviewsError = reviewsError && reviewsError.productId === selectedProduct?.id ? reviewsError.message : ''
  const featuredProductIds = new Set(featuredProducts.map((product) => product.id))
  const hasFeaturedListings = view === 'browse'
    && appliedFilters.status === 'SELLING'
    && featuredProducts.length > 0
  const visibleProducts = view === 'browse' && appliedFilters.status === 'SELLING'
    ? products.filter((product) => !featuredProductIds.has(product.id))
    : products

  return <main className="px-4 pb-28 pt-7 sm:px-6 lg:ml-[248px] lg:px-10 lg:pb-10">
    <div className="mx-auto max-w-[1180px]">
      <div className="mb-7 flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
        <div><p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-[#087cf5]">Otech Marketplace</p><h1 className="text-3xl font-extrabold tracking-[-0.04em] text-[#172238] dark:text-white sm:text-4xl">Good things, passed on.</h1><p className="mt-2 max-w-xl text-sm leading-6 text-[#718097] dark:text-slate-400">Find pre-loved pieces nearby, or give something useful a second life.</p></div>
        <div className="flex flex-wrap gap-2 self-start sm:self-auto">
          {user && <button type="button" onClick={() => { setBillingProduct(null); setBillingOpen(true) }} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-[#dfe6ef] px-4 text-sm font-bold text-[#526078] hover:bg-[#f5f7fa] dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"><Zap size={16} />Posting plans</button>}
          {user && <button onClick={() => { setEditingProduct(null); setShowForm(true) }} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-[#087cf5] px-4 text-sm font-bold text-white shadow-sm hover:bg-[#006cdf]"><Plus size={17} />List an item</button>}
        </div>
      </div>
      <BillingCenter user={user} onAuth={onAuth} open={billingOpen} onClose={() => { setBillingOpen(false); setBillingProduct(null) }} productToBoost={billingProduct} refreshKey={billingRefreshKey} />

      {user && <div className="mb-5 inline-flex rounded-xl bg-[#e9eef5] p-1 dark:bg-slate-800">
        <button onClick={() => setView('browse')} aria-pressed={view === 'browse'} className={`min-h-10 rounded-lg px-4 text-sm font-bold ${view === 'browse' ? 'bg-white text-[#172238] shadow-sm dark:bg-slate-950 dark:text-white' : 'text-[#718097] dark:text-slate-400'}`}>Browse</button>
        <button onClick={() => setView('mine')} aria-pressed={view === 'mine'} className={`min-h-10 rounded-lg px-4 text-sm font-bold ${view === 'mine' ? 'bg-white text-[#172238] shadow-sm dark:bg-slate-950 dark:text-white' : 'text-[#718097] dark:text-slate-400'}`}>My listings</button>
      </div>}

      {view === 'browse' && <form onSubmit={submitFilters} className="mb-7 rounded-2xl border border-[#e3e8f0] bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900 sm:p-5">
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-[minmax(220px,1.5fr)_1fr_1fr_1fr_1fr_1fr_auto]">
          <label className="relative block"><span className="sr-only">Search listings</span><Search size={17} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-[#9aa6b8]" /><input value={filters.search} onChange={(event) => setFilters((current) => ({ ...current, search: event.target.value }))} maxLength="120" placeholder="Search products" className="h-11 w-full rounded-xl border border-[#dfe6ef] bg-white pl-10 pr-3 text-sm outline-none focus:border-[#087cf5] focus:ring-2 focus:ring-blue-100 dark:border-slate-700 dark:bg-slate-950 dark:text-white dark:focus:ring-blue-950" /></label>
          <label><span className="sr-only">Category</span><select value={filters.categoryId} onChange={(event) => setFilters((current) => ({ ...current, categoryId: event.target.value }))} className="h-11 w-full rounded-xl border border-[#dfe6ef] bg-white px-3 text-sm text-[#526078] outline-none focus:border-[#087cf5] dark:border-slate-700 dark:bg-slate-950 dark:text-slate-300"><option value="">All categories</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
          <label><span className="sr-only">Condition</span><select value={filters.condition} onChange={(event) => setFilters((current) => ({ ...current, condition: event.target.value }))} className="h-11 w-full rounded-xl border border-[#dfe6ef] bg-white px-3 text-sm text-[#526078] outline-none focus:border-[#087cf5] dark:border-slate-700 dark:bg-slate-950 dark:text-slate-300"><option value="">Any condition</option>{conditionOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
          <label><span className="sr-only">Minimum price</span><input type="number" min="0" step="1" value={filters.minPrice} onChange={(event) => setFilters((current) => ({ ...current, minPrice: event.target.value }))} placeholder="Min price" className="h-11 w-full rounded-xl border border-[#dfe6ef] bg-white px-3 text-sm outline-none focus:border-[#087cf5] dark:border-slate-700 dark:bg-slate-950 dark:text-white" /></label>
          <label><span className="sr-only">Maximum price</span><input type="number" min="0" step="1" value={filters.maxPrice} onChange={(event) => setFilters((current) => ({ ...current, maxPrice: event.target.value }))} placeholder="Max price" className="h-11 w-full rounded-xl border border-[#dfe6ef] bg-white px-3 text-sm outline-none focus:border-[#087cf5] dark:border-slate-700 dark:bg-slate-950 dark:text-white" /></label>
          <label><span className="sr-only">Location</span><input value={filters.location} onChange={(event) => setFilters((current) => ({ ...current, location: event.target.value }))} maxLength="120" placeholder="Location" className="h-11 w-full rounded-xl border border-[#dfe6ef] bg-white px-3 text-sm outline-none focus:border-[#087cf5] dark:border-slate-700 dark:bg-slate-950 dark:text-white" /></label>
          <label className="relative"><span className="sr-only">Sort listings</span><ArrowDownWideNarrow size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[#9aa6b8]" /><select value={filters.sort} onChange={(event) => setFilters((current) => ({ ...current, sort: event.target.value }))} className="h-11 w-full appearance-none rounded-xl border border-[#dfe6ef] bg-white pl-9 pr-3 text-sm text-[#526078] outline-none focus:border-[#087cf5] dark:border-slate-700 dark:bg-slate-950 dark:text-slate-300"><option value="newest">Newest</option><option value="oldest">Oldest</option><option value="price-asc">Price: low to high</option><option value="price-desc">Price: high to low</option></select></label>
        </div>
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <label className="inline-flex items-center gap-2 text-xs font-semibold text-[#718097] dark:text-slate-400"><input type="checkbox" checked={filters.status === 'SOLD'} onChange={(event) => setFilters((current) => ({ ...current, status: event.target.checked ? 'SOLD' : 'SELLING' }))} className="h-4 w-4 accent-[#087cf5]" />Include sold items</label>
          <div className="ml-auto flex gap-2">
            <button type="button" onClick={resetFilters} className="min-h-10 rounded-xl px-3 text-xs font-bold text-[#718097] hover:bg-[#f5f7fa] dark:text-slate-400 dark:hover:bg-slate-800">Reset</button>
            <button type="submit" className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-[#172238] px-4 text-xs font-bold text-white hover:bg-[#263754] dark:bg-slate-700 dark:hover:bg-slate-600"><Search size={14} />Apply filters</button>
          </div>
        </div>
      </form>}
      {categoriesError && <p role="alert" className="mb-5 rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-800 dark:bg-amber-950/30 dark:text-amber-300">Categories could not be loaded: {categoriesError}</p>}

      {showForm && user && <div className="mb-7"><ProductForm key={editingProduct?.id || 'new'} product={editingProduct} categories={categories} isSaving={isSaving} onSave={saveProduct} onCancel={() => { setShowForm(false); setEditingProduct(null) }} /></div>}
      {actionError && <div role="alert" className="mb-5 flex items-center justify-between gap-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:border-rose-900 dark:bg-rose-950/30 dark:text-rose-300"><span>{actionError}</span><button onClick={() => setActionError('')} aria-label="Dismiss error" className="rounded-lg p-1 hover:bg-rose-100 dark:hover:bg-rose-900"><X size={16} /></button></div>}

      {view === 'browse' && appliedFilters.status === 'SELLING' && (featuredProducts.length > 0 || featuredError) && <section aria-labelledby="featured-listings-title" className="mb-8">
        <div className="mb-3 flex items-center gap-2"><Zap size={17} className="text-[#087cf5]" /><h2 id="featured-listings-title" className="text-lg font-extrabold text-[#172238] dark:text-white">Featured listings</h2></div>
        {featuredError && <p role="alert" className="mb-3 text-sm text-rose-700 dark:text-rose-300">{featuredError}</p>}
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{featuredProducts.map((product) => <ProductCard key={`featured-${product.id}`} product={product} isOwner={isOwner(product)} onOpen={() => { setGalleryIndex(0); setSelectedProduct(product) }} onEdit={() => { setSelectedProduct(null); setEditingProduct(product); setShowForm(true) }} onOpenMessages={() => openProductMessages(product)} onStartChat={() => startChat(product)} onStatusChange={(status) => updateStatus(product, status)} />)}</div>
      </section>}

      <div className="mb-4 flex items-end justify-between gap-4"><div><h2 className="text-xl font-extrabold text-[#172238] dark:text-white">{view === 'mine' ? 'Your listings' : selectedCategoryName ? `${selectedCategoryName} listings` : 'Fresh finds'}</h2><p className="mt-1 text-sm text-[#718097] dark:text-slate-400">{view === 'mine' ? 'Keep your listings up to date.' : selectedCategoryName ? `Browse available items in ${selectedCategoryName}.` : 'Meet your next useful thing.'}</p></div>{!user && <button onClick={onAuth} className="text-sm font-bold text-[#087cf5] hover:underline">Sign in to list</button>}</div>
      {isLoading && <div className="flex items-center justify-center gap-2 rounded-2xl border border-dashed border-[#e3e8f0] bg-white p-12 text-sm text-[#718097] dark:border-slate-700 dark:bg-slate-900 dark:text-slate-400"><LoaderCircle size={18} className="animate-spin text-[#087cf5]" />Loading listings…</div>}
      {error && !isLoading && <div role="alert" className="rounded-2xl border border-rose-200 bg-rose-50 p-5 text-sm text-rose-700 dark:border-rose-900 dark:bg-rose-950/30 dark:text-rose-300"><p>{error}</p><button onClick={() => setRefreshKey((value) => value + 1)} className="mt-3 inline-flex min-h-10 items-center gap-2 rounded-lg bg-white px-3 text-xs font-bold text-rose-700 hover:bg-rose-100 dark:bg-slate-900 dark:text-rose-300"><RefreshCw size={14} />Try again</button></div>}
      {moreError && <p role="alert" className="mt-4 rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:bg-rose-950/30 dark:text-rose-300">{moreError}</p>}
      {!isLoading && !error && visibleProducts.length === 0 && !hasFeaturedListings && <div className="rounded-2xl border border-dashed border-[#d7e1ed] bg-white px-6 py-14 text-center dark:border-slate-700 dark:bg-slate-900"><div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-[#eaf3ff] text-[#087cf5] dark:bg-blue-950/50"><Tag size={22} /></div><h3 className="mt-4 font-extrabold text-[#172238] dark:text-white">{view === 'mine' ? 'No listings yet' : 'No items match those filters'}</h3><p className="mx-auto mt-2 max-w-md text-sm leading-6 text-[#718097] dark:text-slate-400">{view === 'mine' ? 'Share something useful with your community.' : 'Try a different keyword or widen your filters.'}</p>{view === 'mine' && user && <button onClick={() => { setEditingProduct(null); setShowForm(true) }} className="mt-5 inline-flex min-h-10 items-center gap-2 rounded-xl bg-[#087cf5] px-4 text-xs font-bold text-white hover:bg-[#006cdf]"><Plus size={15} />Create your first listing</button>}</div>}
      {!isLoading && !error && visibleProducts.length > 0 && <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{visibleProducts.map((product) => <ProductCard key={product.id} product={product} isOwner={isOwner(product)} onOpen={() => { setGalleryIndex(0); setSelectedProduct(product) }} onEdit={() => editProduct(product)} onOpenMessages={() => openProductMessages(product)} onStartChat={() => startChat(product)} onStatusChange={(status) => updateStatus(product, status)} />)}</div>}
      {!isLoading && !error && hasMore && <div className="mt-8 text-center"><button onClick={loadMore} disabled={isLoadingMore} className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-[#dfe6ef] bg-white px-5 text-sm font-bold text-[#526078] hover:bg-[#f5f7fa] disabled:opacity-60 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800">{isLoadingMore && <LoaderCircle size={16} className="animate-spin" />}Load more listings</button></div>}

      <aside className="mt-10 hidden rounded-2xl border border-[#e3e8f0] bg-white p-5 dark:border-slate-800 dark:bg-slate-900 xl:block"><div className="flex items-center gap-3"><span className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#eaf3ff] text-[#087cf5] dark:bg-blue-950/50"><ShieldCheck size={19} /></span><div><h2 className="text-sm font-extrabold text-[#172238] dark:text-white">Meet safely, trade thoughtfully</h2><p className="mt-1 text-xs text-[#718097] dark:text-slate-400">Agree on the item's condition and meet in a public place. Otech doesn't process payments.</p></div></div></aside>
    </div>

    <dialog ref={dialogRef} onClose={() => setSelectedProduct(null)} aria-labelledby="product-detail-title" className="fixed inset-0 m-auto max-h-[94vh] w-[min(1200px,calc(100%-24px))] overflow-hidden rounded-3xl border border-[#e3e8f0] bg-white p-0 text-[#172238] shadow-2xl backdrop:bg-slate-950/60 dark:border-slate-700 dark:bg-slate-900 dark:text-white">
      {selectedProduct && <div className="grid max-h-[94vh] grid-rows-[minmax(200px,36vh)_minmax(0,1fr)] md:h-[min(88vh,820px)] md:max-h-[88vh] md:grid-cols-[minmax(0,1.15fr)_minmax(360px,0.85fr)] md:grid-rows-1">
        <div className="relative min-h-0 bg-[#eff3f7] dark:bg-slate-800">
          {selectedProduct.imageUrls?.length > 0
            ? <img src={selectedProduct.imageUrls[Math.min(galleryIndex, selectedProduct.imageUrls.length - 1)]} alt={selectedProduct.title} className="h-full w-full object-contain md:absolute md:inset-0" />
            : <div className="flex h-full items-center justify-center text-sm text-[#718097]">No photos for this item</div>}
          <button type="button" onClick={() => setSelectedProduct(null)} aria-label="Close product details" className="absolute right-3 top-3 rounded-full bg-white/90 p-2 text-[#526078] hover:bg-white dark:bg-slate-950/80 dark:text-slate-200"><X size={18} /></button>
          {selectedProduct.imageUrls?.length > 1 && <div className="absolute inset-x-0 bottom-0 flex justify-center gap-2 bg-gradient-to-t from-slate-950/45 to-transparent px-4 pb-4 pt-10">{selectedProduct.imageUrls.map((image, index) => <button key={image} type="button" onClick={() => setGalleryIndex(index)} aria-label={`Show photo ${index + 1}`} aria-pressed={galleryIndex === index} className={`h-1.5 rounded-full transition-all ${galleryIndex === index ? 'w-8 bg-white' : 'w-2 bg-white/60 hover:bg-white'}`} />)}</div>}
        </div>
        <div className="flex min-h-0 flex-col">
          <div className="min-h-0 flex-1 overflow-y-auto p-5 sm:p-7 md:p-8">
            <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#087cf5]">{selectedProduct.categoryName || 'Marketplace find'}{selectedProduct.status === 'SOLD' ? ' · Sold' : ''}</p>
            <h2 id="product-detail-title" className="mt-3 text-3xl font-extrabold leading-tight tracking-tight">{selectedProduct.title}</h2>
            <p className="mt-4 text-3xl font-extrabold text-[#087cf5]">{money.format(Number(selectedProduct.price))}</p>
            <section className="mt-3" aria-label="Product reviews">
              {activeReviews && <p className="text-sm font-semibold text-[#526078] dark:text-slate-300">{activeReviews.reviewCount ? `★ ${activeReviews.averageRating} / 5 · ${activeReviews.reviewCount} verified purchase review${activeReviews.reviewCount === 1 ? '' : 's'}` : 'No verified purchase reviews yet.'}</p>}
              {activeReviews?.reviews?.slice(0, 3).map((review) => <article key={review.id} className="mt-2 rounded-lg bg-[#f5f7fa] px-3 py-2 text-xs dark:bg-slate-800"><p className="font-bold">{review.reviewerName} · ★ {review.rating}/5</p>{review.comment && <p className="mt-1 whitespace-pre-wrap text-[#718097] dark:text-slate-400">{review.comment}</p>}</article>)}
              {activeReviewsError && <p role="alert" className="text-xs text-rose-600 dark:text-rose-300">{activeReviewsError}</p>}
            </section>
            <div className="mt-5 flex flex-wrap gap-2 text-sm font-semibold text-[#526078] dark:text-slate-300"><span className="rounded-full bg-[#f2f5f8] px-3.5 py-2 dark:bg-slate-800">{conditionOptions.find(([value]) => value === selectedProduct.condition)?.[1] || selectedProduct.condition}</span>{selectedProduct.location && <span className="inline-flex items-center gap-1.5 rounded-full bg-[#f2f5f8] px-3.5 py-2 dark:bg-slate-800"><MapPin size={15} />{selectedProduct.location}</span>}</div>
            <section className="mt-8">
              <h3 className="text-sm font-extrabold uppercase tracking-wide text-[#718097] dark:text-slate-400">Description</h3>
              <p className="mt-3 whitespace-pre-wrap text-base leading-7 text-[#34445d] dark:text-slate-200">{selectedProduct.description || 'The seller has not added a description yet.'}</p>
            </section>
            <div className="mt-8 flex items-center gap-3 border-t border-[#eef1f5] pt-5 dark:border-slate-800">{selectedProduct.seller?.avatarUrl ? <img src={selectedProduct.seller.avatarUrl} alt="" className="h-12 w-12 rounded-full object-cover" /> : <span className="flex h-12 w-12 items-center justify-center rounded-full bg-[#eaf3ff] text-lg font-bold text-[#087cf5]">{selectedProduct.seller?.fullName?.[0] || 'O'}</span>}<div><p className="text-base font-extrabold">{selectedProduct.seller?.fullName || 'Otech member'}</p><p className="mt-0.5 text-sm text-[#718097] dark:text-slate-400">Seller</p></div></div>
            {actionError && <p role="alert" className="mt-4 rounded-xl bg-rose-50 px-3 py-2 text-sm text-rose-700 dark:bg-rose-950/30 dark:text-rose-300">{actionError}</p>}
          </div>
          <div className="flex shrink-0 flex-col gap-2 border-t border-[#eef1f5] bg-white p-4 dark:border-slate-800 dark:bg-slate-900 sm:flex-row sm:p-5">
            {selectedIsOwner
              ? <><button type="button" onClick={() => editProduct(selectedProduct)} className="min-h-11 flex-1 rounded-xl border border-[#dfe6ef] px-4 text-sm font-bold hover:bg-[#f5f7fa] dark:border-slate-700 dark:hover:bg-slate-800">Edit listing</button>{selectedProduct.status === 'SELLING' ? <><button type="button" onClick={() => { setBillingProduct(selectedProduct); setSelectedProduct(null); setBillingOpen(true) }} className="inline-flex min-h-11 flex-1 items-center justify-center gap-2 rounded-xl border border-blue-200 px-4 text-sm font-bold text-[#087cf5] hover:bg-blue-50 dark:border-blue-900 dark:hover:bg-blue-950/30"><Zap size={15} />Boost listing</button><button type="button" onClick={() => { setSelectedProduct(null); openProductMessages(selectedProduct) }} className="min-h-11 flex-1 rounded-xl bg-[#087cf5] px-4 text-sm font-bold text-white hover:bg-[#006cdf]">Confirm sale in messages</button></> : <button type="button" onClick={() => updateStatus(selectedProduct, 'SELLING')} className="min-h-11 flex-1 rounded-xl bg-[#087cf5] px-4 text-sm font-bold text-white hover:bg-[#006cdf]">Relist</button>}{selectedProduct.status !== 'HIDDEN' && <button type="button" onClick={() => archiveProduct(selectedProduct)} className="min-h-11 rounded-xl px-4 text-sm font-bold text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/30">Archive</button>}</>
              : selectedProduct.status === 'SELLING' && <button type="button" onClick={() => startChat(selectedProduct)} className="inline-flex min-h-11 flex-1 items-center justify-center gap-2 rounded-xl bg-[#087cf5] px-4 text-sm font-bold text-white hover:bg-[#006cdf]"><MessageCircle size={16} />Message seller</button>}
            <button type="button" onClick={() => setSelectedProduct(null)} className="min-h-11 rounded-xl px-4 text-sm font-bold text-[#718097] hover:bg-[#f5f7fa] dark:text-slate-300 dark:hover:bg-slate-800">Close</button>
          </div>
        </div>
      </div>}
    </dialog>
  </main>
}
