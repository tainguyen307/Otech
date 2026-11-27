import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  ArrowLeft,
  ArrowRight,
  BarChart3,
  Check,
  CreditCard,
  Eye,
  FileText,
  Flag,
  LayoutDashboard,
  LoaderCircle,
  LockKeyhole,
  Plus,
  ImagePlus,
  RefreshCw,
  Search,
  Shield,
  ShoppingBag,
  Trash2,
  Users,
  UsersRound,
  Zap,
  X,
} from 'lucide-react'
import { apiErrorMessage, apiFetch } from '../lib/api'

const PAGE_SIZE = 15

const managementTabs = [
  { id: 'overview', label: 'Overview', icon: LayoutDashboard },
  { id: 'users', label: 'Users', icon: Users },
  { id: 'products', label: 'Products', icon: ShoppingBag },
  { id: 'posts', label: 'Posts', icon: FileText },
  { id: 'groups', label: 'Groups', icon: UsersRound },
  { id: 'reports', label: 'Reports', icon: Flag },
  { id: 'billing', label: 'Billing', icon: CreditCard },
]

const adminTabs = [
  { id: 'managers', label: 'Managers', icon: Shield },
  { id: 'packages', label: 'Service packages', icon: Zap },
]

async function requestJson(path, options, fallback) {
  const response = await apiFetch(path, options)
  if (!response.ok) throw new Error(await apiErrorMessage(response, fallback))
  return response.status === 204 ? null : response.json()
}

function formatDate(value) {
  return value ? new Date(value).toLocaleDateString('en-US') : '—'
}

function formatPrice(value) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'VND' }).format(value || 0)
}

function StatusBadge({ children }) {
  const normalized = String(children || '').toUpperCase()
  const color = normalized === 'ACTIVE' || normalized === 'SELLING' || normalized === 'APPROVED'
    ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/50 dark:text-emerald-300'
    : normalized === 'PENDING' || normalized === 'LOCKED' || normalized === 'HIDDEN'
      ? 'bg-amber-50 text-amber-700 dark:bg-amber-950/50 dark:text-amber-300'
      : 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300'
  return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ${color}`}>{children}</span>
}

function ActionButton({ children, onClick, disabled = false, danger = false, title }) {
  return <button
    type="button"
    title={title}
    disabled={disabled}
    onClick={onClick}
    className={`inline-flex min-h-10 items-center justify-center gap-1.5 rounded-lg border px-3 py-2 text-xs font-bold transition disabled:cursor-not-allowed disabled:opacity-50 ${danger
      ? 'border-rose-200 text-rose-700 hover:bg-rose-50 dark:border-rose-900 dark:text-rose-300 dark:hover:bg-rose-950/40'
      : 'border-slate-200 text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800'}`}
  >{children}</button>
}

function MetricCard({ label, value, detail, icon: Icon, onClick }) {
  return <button
    type="button"
    onClick={onClick}
    className="rounded-2xl border border-slate-200 bg-white p-5 text-left shadow-sm hover:border-blue-300 dark:border-slate-800 dark:bg-slate-900 dark:hover:border-blue-700"
  >
    <span className="flex items-center justify-between">
      <span className="text-sm font-semibold text-slate-500 dark:text-slate-400">{label}</span>
      <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-blue-600 dark:bg-blue-950/60 dark:text-blue-300"><Icon size={19} aria-hidden="true" /></span>
    </span>
    <span className="mt-4 block text-3xl font-extrabold tracking-tight">{value ?? '—'}</span>
    <span className="mt-1 block text-xs text-slate-500 dark:text-slate-400">{detail}</span>
  </button>
}

function ManagementDashboard({ user }) {
  const isAdmin = user?.role === 'ADMIN'
  const tabs = useMemo(() => isAdmin ? [...managementTabs, ...adminTabs] : managementTabs, [isAdmin])
  const [activeTab, setActiveTab] = useState('overview')
  const [summary, setSummary] = useState(null)
  const [tableData, setTableData] = useState({ content: [], page: 0, totalPages: 0, totalElements: 0 })
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('')
  const [page, setPage] = useState(0)
  const [refreshKey, setRefreshKey] = useState(0)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [managerForm, setManagerForm] = useState({ fullName: '', email: '', password: '' })
  const [showManagerForm, setShowManagerForm] = useState(false)
  const [packageForm, setPackageForm] = useState({ code: 'POST_10', name: '10 listing credits', description: '', packageType: 'LISTING_CREDITS', price: '', listingCredits: '10', durationHours: '720', active: true })
  const [editingPackageId, setEditingPackageId] = useState('')
  const [showPackageForm, setShowPackageForm] = useState(false)
  const [paymentQrUrl, setPaymentQrUrl] = useState('')
  const [isUploadingQr, setIsUploadingQr] = useState(false)

  const loadTable = useCallback(async () => {
    if (activeTab === 'overview') {
      setLoading(false)
      return
    }
    setLoading(true)
    setError('')
    const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) })
    if (search.trim()) params.set('search', search.trim())

    if (activeTab === 'billing') {
      try {
        const status = filter || 'PENDING'
        const items = await requestJson(`/manager/billing/payment-requests?status=${status}`, undefined, 'Unable to load payment requests.')
        const paymentRequests = Array.isArray(items) ? items : []
        setTableData({ content: paymentRequests, page: 0, totalPages: 1, totalElements: paymentRequests.length })
      } catch (requestError) {
        setError(requestError.message || 'Unable to load payment requests.')
      } finally {
        setLoading(false)
      }
      return
    }

    if (activeTab === 'packages') {
      try {
        const [items, qrImageUrl] = await Promise.all([
          requestJson('/admin/billing/packages', undefined, 'Unable to load service packages.'),
          requestJson('/admin/billing/qr', undefined, 'Unable to load the payment QR code.'),
        ])
        setPaymentQrUrl(qrImageUrl || '')
        setTableData({ content: Array.isArray(items) ? items : [], page: 0, totalPages: 1, totalElements: Array.isArray(items) ? items.length : 0 })
      } catch (requestError) {
        setError(requestError.message || 'Unable to load billing settings.')
      } finally {
        setLoading(false)
      }
      return
    }

    let path = `/manager/${activeTab}?${params.toString()}`
    if (activeTab === 'users') {
      if (filter === 'USER' || filter === 'MANAGER' || filter === 'ADMIN') params.set('role', filter)
      if (['ACTIVE', 'LOCKED', 'DELETED'].includes(filter)) params.set('status', filter)
    } else if (activeTab === 'managers') {
      params.set('role', 'MANAGER')
      path = `/manager/users?${params.toString()}`
    } else if (activeTab === 'products' && filter) {
      params.set('status', filter)
    } else if ((activeTab === 'posts' || activeTab === 'groups') && filter) {
      params.set('hidden', filter === 'HIDDEN' ? 'true' : 'false')
    } else if (activeTab === 'reports' && filter) {
      params.set('status', filter)
    }
    path = path.split('?')[0] + `?${params.toString()}`

    try {
      const data = await requestJson(path, undefined, 'Unable to load management data.')
      setTableData(data)
    } catch (requestError) {
      setError(requestError.message || 'Unable to load management data.')
    } finally {
      setLoading(false)
    }
  }, [activeTab, filter, page, search])

  useEffect(() => {
    let cancelled = false
    const fetchSummary = async () => {
      try {
        const data = await requestJson('/manager/dashboard', undefined, 'Unable to load dashboard data.')
        if (!cancelled) setSummary(data)
      } catch (requestError) {
        if (!cancelled) setError(requestError.message || 'Unable to load dashboard data.')
      }
    }
    fetchSummary()
    return () => { cancelled = true }
  }, [refreshKey])

  useEffect(() => {
    const timer = window.setTimeout(loadTable, 250)
    return () => window.clearTimeout(timer)
  }, [loadTable, refreshKey])

  const changeTab = (nextTab) => {
    setActiveTab(nextTab)
    setSearch('')
    setFilter(nextTab === 'billing' ? 'PENDING' : '')
    setPage(0)
    setError('')
    setNotice('')
  }

  const runAction = async (path, method, body, successMessage) => {
    setActionLoading(true)
    setError('')
    setNotice('')
    try {
      await requestJson(path, {
        method,
        ...(body ? { body: JSON.stringify(body) } : {}),
      }, 'The action failed.')
      setNotice(successMessage)
      setRefreshKey((current) => current + 1)
      return true
    } catch (requestError) {
      setError(requestError.message || 'The action failed.')
      return false
    } finally {
      setActionLoading(false)
    }
  }

  const handleCreateManager = async (event) => {
    event.preventDefault()
    const succeeded = await runAction(
      '/admin/managers',
      'POST',
      managerForm,
      'Manager account created.',
    )
    if (succeeded) {
      setManagerForm({ fullName: '', email: '', password: '' })
      setShowManagerForm(false)
    }
  }

  const saveBillingPackage = async (event) => {
    event.preventDefault()
    const body = {
      ...packageForm,
      price: Number(packageForm.price),
      listingCredits: packageForm.packageType === 'LISTING_CREDITS' ? Number(packageForm.listingCredits) : null,
      durationHours: Number(packageForm.durationHours),
    }
    const endpoint = editingPackageId
      ? `/admin/billing/packages/${editingPackageId}`
      : '/admin/billing/packages'
    const succeeded = await runAction(endpoint, editingPackageId ? 'PUT' : 'POST', body, 'Service package saved.')
    if (succeeded) {
      setEditingPackageId('')
      setShowPackageForm(false)
      setPackageForm({ code: 'POST_10', name: '10 listing credits', description: '', packageType: 'LISTING_CREDITS', price: '', listingCredits: '10', durationHours: '720', active: true })
    }
  }

  const uploadPaymentQr = async (event) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setIsUploadingQr(true)
    setError('')
    try {
      const formData = new FormData()
      formData.append('file', file)
      const uploadResponse = await apiFetch('/products/images', { method: 'POST', body: formData })
      if (!uploadResponse.ok) throw new Error(await apiErrorMessage(uploadResponse, 'Unable to upload the QR code.'))
      const result = await uploadResponse.json()
      if (!result.imageUrl) throw new Error('The image service did not return a QR code URL.')
      const success = await runAction('/admin/billing/qr', 'PUT', { qrImageUrl: result.imageUrl }, 'Payment QR code updated.')
      if (success) setPaymentQrUrl(result.imageUrl)
    } catch (requestError) {
      setError(requestError.message || 'Unable to update the QR code.')
    } finally {
      setIsUploadingQr(false)
    }
  }

  const chooseFilter = (value) => {
    setFilter(value)
    setPage(0)
  }

  const renderUserActions = (item) => {
    if (item.role === 'ADMIN') return <span className="text-xs text-slate-400">Admin account</span>
    if (activeTab === 'users' && isAdmin && item.role === 'USER') return <div className="flex flex-wrap gap-2">
      <ActionButton
        disabled={actionLoading || item.status === 'DELETED'}
        onClick={() => runAction(`/admin/users/${item.id}/role`, 'PATCH', { role: 'MANAGER' }, 'Manager role assigned.')}
      >Assign Manager</ActionButton>
      <ActionButton
        disabled={actionLoading || item.status === 'DELETED'}
        onClick={() => runAction(`/manager/users/${item.id}/status`, 'PATCH', { status: item.status === 'LOCKED' ? 'ACTIVE' : 'LOCKED' }, item.status === 'LOCKED' ? 'User account unlocked.' : 'User account locked.')}
      >{item.status === 'LOCKED' ? 'Unlock' : 'Lock'}</ActionButton>
    </div>
    if (activeTab === 'managers') {
      const nextRole = item.role === 'MANAGER' ? 'USER' : 'MANAGER'
      return <div className="flex flex-wrap gap-2">
        <ActionButton
          disabled={actionLoading || item.status === 'DELETED'}
          onClick={() => runAction(`/admin/users/${item.id}/role`, 'PATCH', { role: nextRole }, `Role changed to ${nextRole}.`)}
        >Change role to {nextRole}</ActionButton>
        <ActionButton
          disabled={actionLoading || item.status === 'DELETED'}
          onClick={() => runAction(`/admin/managers/${item.id}/status`, 'PATCH', { status: item.status === 'LOCKED' ? 'ACTIVE' : 'LOCKED' }, item.status === 'LOCKED' ? 'Manager account unlocked.' : 'Manager account locked.')}
        >{item.status === 'LOCKED' ? 'Unlock' : 'Lock'}</ActionButton>
        <ActionButton
          danger
          disabled={actionLoading || item.status === 'DELETED'}
          onClick={() => {
            if (window.confirm(`Delete Manager account ${item.fullName}? The account will be deactivated.`)) {
              runAction(`/admin/managers/${item.id}`, 'DELETE', null, 'Manager account deactivated.')
            }
          }}
        ><Trash2 size={14} aria-hidden="true" /> Delete</ActionButton>
      </div>
    }
    if (item.role !== 'USER') return <span className="text-xs text-slate-400">No actions available</span>
    return <ActionButton
      disabled={actionLoading || item.status === 'DELETED'}
      onClick={() => runAction(`/manager/users/${item.id}/status`, 'PATCH', { status: item.status === 'LOCKED' ? 'ACTIVE' : 'LOCKED' }, item.status === 'LOCKED' ? 'User account unlocked.' : 'User account locked.')}
    ><LockKeyhole size={14} aria-hidden="true" />{item.status === 'LOCKED' ? 'Unlock' : 'Lock'}</ActionButton>
  }

  const renderRows = () => {
    const rows = tableData.content || []
    if (activeTab === 'billing') return rows.map((item) => <tr key={item.id}>
      <td><span className="font-mono font-bold text-blue-700 dark:text-blue-300">{item.paymentCode}</span><span className="mt-1 block text-xs text-slate-500">{formatDate(item.createdAt)}</span></td>
      <td><span className="font-semibold">{item.userName}</span><span className="mt-1 block text-xs text-slate-500">{item.userEmail}</span></td>
      <td>{item.packageName}<span className="mt-1 block text-xs text-slate-500">{item.productTitle ? `Product: ${item.productTitle}` : item.packageType === 'BOOST' ? 'Listing boost' : 'Listing credits'}</span></td>
      <td className="font-bold">{formatPrice(item.amount)}</td>
      <td>{item.transferReference || '—'}{item.proofImageUrl && <a href={item.proofImageUrl} target="_blank" rel="noreferrer" className="mt-1 block text-xs font-bold text-blue-600 underline">View receipt</a>}</td>
      <td><StatusBadge>{item.status}</StatusBadge>{item.reviewNote && <span className="mt-1 block max-w-40 text-xs text-slate-500">{item.reviewNote}</span>}</td>
      <td>{item.status === 'PENDING' ? <div className="flex flex-wrap gap-2">
        <ActionButton disabled={actionLoading || (!item.transferReference && !item.proofImageUrl)} onClick={() => {
          if (window.confirm(`Confirm that ${formatPrice(item.amount)} was received in the bank account with payment reference ${item.paymentCode}?`)) {
            runAction(`/manager/billing/payment-requests/${item.id}`, 'PATCH', { status: 'APPROVED' }, 'Payment approved and benefits activated.')
          }
        }}><Check size={14} aria-hidden="true" />Approve</ActionButton>
        <ActionButton danger disabled={actionLoading} onClick={() => {
          const reviewNote = window.prompt('Reason for rejecting the payment request:')
          if (reviewNote !== null) runAction(`/manager/billing/payment-requests/${item.id}`, 'PATCH', { status: 'REJECTED', reviewNote }, 'Payment request rejected.')
        }}><X size={14} aria-hidden="true" />Reject</ActionButton>
        {!item.transferReference && !item.proofImageUrl && <span className="self-center text-xs text-amber-700">Awaiting proof</span>}
      </div> : <span className="text-xs text-slate-400">{item.reviewedAt ? `Processed ${formatDate(item.reviewedAt)}` : 'Processed'}</span>}</td>
    </tr>)
    if (activeTab === 'packages') return rows.map((item) => <tr key={item.id}>
      <td><span className="font-mono font-bold">{item.code}</span><span className="mt-1 block text-xs text-slate-500">{item.name}</span></td>
      <td><StatusBadge>{item.packageType}</StatusBadge><span className="mt-1 block text-xs text-slate-500">{item.packageType === 'BOOST' ? 'Listing boost package' : `${item.listingCredits} listing credits`}</span></td>
      <td className="font-bold">{formatPrice(item.price)}</td>
      <td>{item.durationHours} hours</td>
      <td><StatusBadge>{item.active ? 'ACTIVE' : 'INACTIVE'}</StatusBadge></td>
      <td><ActionButton onClick={() => {
        setEditingPackageId(item.id)
        setPackageForm({ ...item, price: String(item.price), listingCredits: String(item.listingCredits || ''), durationHours: String(item.durationHours) })
        setShowPackageForm(true)
      }}>Edit package</ActionButton></td>
    </tr>)
    if (activeTab === 'users' || activeTab === 'managers') return rows.map((item) => <tr key={item.id}>
      <td className="font-semibold">{item.fullName}</td>
      <td className="text-slate-500 dark:text-slate-400">{item.email}</td>
      <td><StatusBadge>{item.role}</StatusBadge></td>
      <td><StatusBadge>{item.status}</StatusBadge></td>
      <td>{formatDate(item.createdAt)}</td>
      <td>{renderUserActions(item)}</td>
    </tr>)
    if (activeTab === 'products') return rows.map((item) => <tr key={item.id}>
      <td className="max-w-[260px] font-semibold"><span className="line-clamp-2">{item.title}</span></td>
      <td>{item.sellerName}</td>
      <td>{formatPrice(item.price)}</td>
      <td><StatusBadge>{item.status}</StatusBadge></td>
      <td>{formatDate(item.createdAt)}</td>
      <td>{item.status === 'SOLD' ? <span className="text-xs text-slate-400">Sold</span> : <ActionButton disabled={actionLoading} onClick={() => runAction(`/manager/products/${item.id}/visibility`, 'PATCH', { hidden: item.status !== 'HIDDEN' }, item.status === 'HIDDEN' ? 'Product restored.' : 'Product hidden.')}><Eye size={14} aria-hidden="true" />{item.status === 'HIDDEN' ? 'Restore' : 'Hide'}</ActionButton>}</td>
    </tr>)
    if (activeTab === 'posts') return rows.map((item) => <tr key={item.id}>
      <td className="max-w-[360px]"><span className="line-clamp-2">{item.content}</span></td>
      <td>{item.authorName}</td>
      <td><StatusBadge>{item.hidden ? 'HIDDEN' : 'PUBLIC'}</StatusBadge></td>
      <td>{formatDate(item.createdAt)}</td>
      <td><ActionButton disabled={actionLoading} onClick={() => runAction(`/manager/posts/${item.id}/visibility`, 'PATCH', { hidden: !item.hidden }, item.hidden ? 'Post restored.' : 'Post hidden.')}><Eye size={14} aria-hidden="true" />{item.hidden ? 'Show' : 'Hide'}</ActionButton></td>
    </tr>)
    if (activeTab === 'groups') return rows.map((item) => <tr key={item.id}>
      <td className="font-semibold">{item.name}</td>
      <td>{item.ownerName}</td>
      <td><StatusBadge>{item.hidden ? 'HIDDEN' : 'PUBLIC'}</StatusBadge></td>
      <td>{formatDate(item.createdAt)}</td>
      <td><ActionButton disabled={actionLoading} onClick={() => runAction(`/manager/groups/${item.id}/visibility`, 'PATCH', { hidden: !item.hidden }, item.hidden ? 'Group restored.' : 'Group hidden.')}><Eye size={14} aria-hidden="true" />{item.hidden ? 'Show' : 'Hide'}</ActionButton></td>
    </tr>)
    if (activeTab === 'reports') return rows.map((item) => <tr key={item.id}>
      <td><StatusBadge>{item.status}</StatusBadge></td>
      <td>{item.targetType}</td>
      <td className="max-w-[180px] break-all font-mono text-xs">{item.targetId}</td>
      <td className="max-w-[220px]"><span className="line-clamp-2">{item.reason}</span><span className="mt-1 block text-xs text-slate-400">By {item.reporterName}</span></td>
      <td>{formatDate(item.createdAt)}</td>
      <td>{item.status === 'PENDING' ? <div className="flex gap-2">
        <ActionButton disabled={actionLoading} onClick={() => runAction(`/manager/reports/${item.id}`, 'PATCH', { status: 'APPROVED' }, 'Report accepted and content reviewed.')}><Check size={14} aria-hidden="true" />Review</ActionButton>
        <ActionButton danger disabled={actionLoading} onClick={() => runAction(`/manager/reports/${item.id}`, 'PATCH', { status: 'REJECTED' }, 'Report rejected.')}><X size={14} aria-hidden="true" />Reject</ActionButton>
      </div> : <span className="text-xs text-slate-400">Processed</span>}</td>
    </tr>)
    return null
  }

  const renderMobileRows = () => {
    const rows = tableData.content || []
    return rows.map((item) => {
      if (activeTab === 'billing') return <article key={item.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="flex items-start justify-between gap-3"><div className="min-w-0"><h3 className="break-all font-mono text-sm font-bold text-blue-700 dark:text-blue-300">{item.paymentCode}</h3><p className="mt-1 text-xs text-slate-500">{formatDate(item.createdAt)}</p></div><StatusBadge>{item.status}</StatusBadge></div>
        <p className="mt-3 font-semibold">{item.userName} <span className="font-normal text-slate-500">· {item.userEmail}</span></p>
        <p className="mt-1 text-sm">{item.packageName}{item.productTitle ? ` · ${item.productTitle}` : ''}</p>
        <p className="mt-1 font-extrabold text-blue-600 dark:text-blue-300">{formatPrice(item.amount)}</p>
        <p className="mt-2 text-xs text-slate-500">Bank reference: {item.transferReference || 'Not provided'}</p>
        {item.proofImageUrl && <a href={item.proofImageUrl} target="_blank" rel="noreferrer" className="mt-2 inline-block text-xs font-bold text-blue-600 underline">View receipt</a>}
        {item.status === 'PENDING' && <div className="mt-4 flex flex-wrap gap-2 border-t border-slate-100 pt-3 dark:border-slate-800">
          <ActionButton disabled={actionLoading || (!item.transferReference && !item.proofImageUrl)} onClick={() => {
            if (window.confirm(`Confirm that ${formatPrice(item.amount)} was received in the bank account with payment reference ${item.paymentCode}?`)) {
              runAction(`/manager/billing/payment-requests/${item.id}`, 'PATCH', { status: 'APPROVED' }, 'Payment approved and benefits activated.')
            }
          }}><Check size={14} aria-hidden="true" />Approve</ActionButton>
          <ActionButton danger disabled={actionLoading} onClick={() => {
            const reviewNote = window.prompt('Reason for rejecting the payment request:')
            if (reviewNote !== null) runAction(`/manager/billing/payment-requests/${item.id}`, 'PATCH', { status: 'REJECTED', reviewNote }, 'Payment request rejected.')
          }}><X size={14} aria-hidden="true" />Reject</ActionButton>
          {!item.transferReference && !item.proofImageUrl && <span className="self-center text-xs text-amber-700 dark:text-amber-300">Waiting for the user to submit proof</span>}
        </div>}
        {item.reviewNote && <p className="mt-2 text-xs text-slate-500">Review note: {item.reviewNote}</p>}
      </article>
      if (activeTab === 'packages') return <article key={item.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="flex items-start justify-between gap-3"><div><h3 className="font-bold">{item.name}</h3><p className="mt-1 font-mono text-xs text-slate-500">{item.code}</p></div><StatusBadge>{item.active ? 'ACTIVE' : 'INACTIVE'}</StatusBadge></div>
        <p className="mt-2 text-sm text-slate-500">{item.packageType === 'BOOST' ? 'Listing boost' : `${item.listingCredits} listing credits`} · {item.durationHours} hours</p>
        <p className="mt-1 font-extrabold text-blue-600 dark:text-blue-300">{formatPrice(item.price)}</p>
        <ActionButton onClick={() => {
          setEditingPackageId(item.id)
          setPackageForm({ ...item, price: String(item.price), listingCredits: String(item.listingCredits || ''), durationHours: String(item.durationHours) })
          setShowPackageForm(true)
        }}>Edit package</ActionButton>
      </article>
      if (activeTab === 'users' || activeTab === 'managers') return <article key={item.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0"><h3 className="truncate font-bold text-slate-900 dark:text-white">{item.fullName}</h3><p className="mt-1 break-all text-sm text-slate-500 dark:text-slate-400">{item.email}</p></div>
          <StatusBadge>{item.status}</StatusBadge>
        </div>
        <div className="mt-3 flex flex-wrap items-center gap-2"><StatusBadge>{item.role}</StatusBadge><span className="text-xs text-slate-500 dark:text-slate-400">Tham gia {formatDate(item.createdAt)}</span></div>
        <div className="mt-4 border-t border-slate-100 pt-3 dark:border-slate-800">{renderUserActions(item)}</div>
      </article>
      if (activeTab === 'products') return <article key={item.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="flex items-start justify-between gap-3"><h3 className="min-w-0 flex-1 font-bold text-slate-900 dark:text-white">{item.title}</h3><StatusBadge>{item.status}</StatusBadge></div>
        <p className="mt-2 text-lg font-extrabold text-blue-600 dark:text-blue-300">{formatPrice(item.price)}</p>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{item.sellerName} <span aria-hidden="true">·</span> {formatDate(item.createdAt)}</p>
        <div className="mt-4 border-t border-slate-100 pt-3 dark:border-slate-800">{item.status === 'SOLD' ? <span className="text-sm text-slate-500 dark:text-slate-400">Product sold</span> : <ActionButton disabled={actionLoading} onClick={() => runAction(`/manager/products/${item.id}/visibility`, 'PATCH', { hidden: item.status !== 'HIDDEN' }, item.status === 'HIDDEN' ? 'Product restored.' : 'Product hidden.')}><Eye size={14} aria-hidden="true" />{item.status === 'HIDDEN' ? 'Restore' : 'Hide product'}</ActionButton>}</div>
      </article>
      if (activeTab === 'posts') return <article key={item.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="flex items-start justify-between gap-3"><h3 className="line-clamp-2 min-w-0 flex-1 font-semibold leading-6 text-slate-900 dark:text-white">{item.content}</h3><StatusBadge>{item.hidden ? 'HIDDEN' : 'PUBLIC'}</StatusBadge></div>
        <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">{item.authorName} <span aria-hidden="true">·</span> {formatDate(item.createdAt)}</p>
        <div className="mt-4 border-t border-slate-100 pt-3 dark:border-slate-800"><ActionButton disabled={actionLoading} onClick={() => runAction(`/manager/posts/${item.id}/visibility`, 'PATCH', { hidden: !item.hidden }, item.hidden ? 'Post restored.' : 'Post hidden.')}><Eye size={14} aria-hidden="true" />{item.hidden ? 'Show post' : 'Hide post'}</ActionButton></div>
      </article>
      if (activeTab === 'groups') return <article key={item.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="flex items-start justify-between gap-3"><h3 className="min-w-0 flex-1 font-bold text-slate-900 dark:text-white">{item.name}</h3><StatusBadge>{item.hidden ? 'HIDDEN' : 'PUBLIC'}</StatusBadge></div>
        <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">{item.ownerName} <span aria-hidden="true">·</span> {formatDate(item.createdAt)}</p>
        <div className="mt-4 border-t border-slate-100 pt-3 dark:border-slate-800"><ActionButton disabled={actionLoading} onClick={() => runAction(`/manager/groups/${item.id}/visibility`, 'PATCH', { hidden: !item.hidden }, item.hidden ? 'Group restored.' : 'Group hidden.')}><Eye size={14} aria-hidden="true" />{item.hidden ? 'Show group' : 'Hide group'}</ActionButton></div>
      </article>
      if (activeTab === 'reports') return <article key={item.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="flex items-start justify-between gap-3"><div><p className="font-bold text-slate-900 dark:text-white">{item.targetType}</p><p className="mt-1 break-all font-mono text-xs text-slate-500 dark:text-slate-400">ID: {item.targetId}</p></div><StatusBadge>{item.status}</StatusBadge></div>
        <p className="mt-3 text-sm leading-6 text-slate-700 dark:text-slate-300">{item.reason}</p>
        <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">By {item.reporterName} <span aria-hidden="true">·</span> {formatDate(item.createdAt)}</p>
        <div className="mt-4 flex flex-wrap gap-2 border-t border-slate-100 pt-3 dark:border-slate-800">{item.status === 'PENDING' ? <>
          <ActionButton disabled={actionLoading} onClick={() => runAction(`/manager/reports/${item.id}`, 'PATCH', { status: 'APPROVED' }, 'Report accepted and content reviewed.')}><Check size={14} aria-hidden="true" />Review report</ActionButton>
          <ActionButton danger disabled={actionLoading} onClick={() => runAction(`/manager/reports/${item.id}`, 'PATCH', { status: 'REJECTED' }, 'Report rejected.')}><X size={14} aria-hidden="true" />Reject</ActionButton>
        </> : <span className="py-2 text-sm text-slate-500 dark:text-slate-400">Report processed</span>}</div>
      </article>
      return null
    })
  }

  const tableHeaders = {
    billing: ['Payment reference', 'User', 'Package', 'Amount', 'Transfer details', 'Status', 'Actions'],
    packages: ['Package code', 'Type', 'Price', 'Duration', 'Status', 'Actions'],
    users: ['Name', 'Email', 'Role', 'Status', 'Created', 'Actions'],
    managers: ['Name', 'Email', 'Role', 'Status', 'Created', 'Actions'],
    products: ['Product', 'Seller', 'Price', 'Status', 'Listed', 'Actions'],
    posts: ['Content', 'Author', 'Visibility', 'Posted', 'Actions'],
    groups: ['Group', 'Owner', 'Visibility', 'Created', 'Actions'],
    reports: ['Status', 'Target', 'Target ID', 'Report reason', 'Submitted', 'Actions'],
  }

  const filterOptions = {
    users: [['', 'All roles/statuses'], ['USER', 'User'], ['MANAGER', 'Manager'], ['ADMIN', 'Admin'], ['ACTIVE', 'Active'], ['LOCKED', 'Locked']],
    products: [['', 'All statuses'], ['SELLING', 'For sale'], ['SOLD', 'Sold'], ['HIDDEN', 'Hidden']],
    posts: [['', 'All posts'], ['VISIBLE', 'Visible'], ['HIDDEN', 'Hidden']],
    groups: [['', 'All groups'], ['VISIBLE', 'Visible'], ['HIDDEN', 'Hidden']],
    reports: [['', 'All reports'], ['PENDING', 'Pending'], ['APPROVED', 'Accepted'], ['REJECTED', 'Rejected']],
    billing: [['PENDING', 'Pending verification'], ['APPROVED', 'Approved'], ['REJECTED', 'Rejected']],
  }

  const currentTab = tabs.find((tab) => tab.id === activeTab) || tabs[0]
  const CurrentIcon = currentTab.icon

  return <main className="min-h-screen px-4 pb-28 pt-6 sm:px-6 lg:ml-[248px] lg:px-10 lg:pb-10">
    <div className="mx-auto max-w-[1500px]">
      <header className="mb-7 flex flex-wrap items-end justify-between gap-4">
        <div>
          <div className="mb-2 flex items-center gap-2 text-xs font-bold uppercase tracking-[0.16em] text-blue-600 dark:text-blue-300"><BarChart3 size={15} aria-hidden="true" /> Otech · Management</div>
          <h1 className="text-3xl font-extrabold tracking-tight">{isAdmin ? 'Admin dashboard' : 'Manager dashboard'}</h1>
          <p className="mt-2 max-w-2xl text-sm text-slate-500 dark:text-slate-400">Manage the community, moderate content, and review reports.</p>
        </div>
        <span className="rounded-full border border-blue-100 bg-blue-50 px-3 py-1.5 text-xs font-bold text-blue-700 dark:border-blue-900 dark:bg-blue-950/50 dark:text-blue-300">{user?.fullName} · {isAdmin ? 'ADMIN' : 'MANAGER'}</span>
      </header>

      <nav aria-label="Management navigation" className="mb-6 flex gap-2 overflow-x-auto border-b border-slate-200 pb-2 dark:border-slate-800">
        {tabs.map((tab) => {
          const Icon = tab.icon
          return <button
            type="button"
            key={tab.id}
            aria-current={activeTab === tab.id ? 'page' : undefined}
            onClick={() => changeTab(tab.id)}
            className={`inline-flex min-h-11 shrink-0 items-center gap-2 rounded-xl px-4 text-sm font-bold ${activeTab === tab.id ? 'bg-blue-600 text-white' : 'text-slate-500 hover:bg-white dark:text-slate-400 dark:hover:bg-slate-900'}`}
          ><Icon size={17} aria-hidden="true" />{tab.label}</button>
        })}
      </nav>

      {error && <div role="alert" className="mb-5 flex items-start justify-between gap-3 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:border-rose-900 dark:bg-rose-950/30 dark:text-rose-300"><span>{error}</span><button aria-label="Dismiss error" onClick={() => setError('')}><X size={17} /></button></div>}
      {notice && <div role="status" aria-live="polite" className="mb-5 flex items-center justify-between gap-3 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:border-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-300"><span>{notice}</span><button aria-label="Dismiss notification" onClick={() => setNotice('')}><X size={17} /></button></div>}

      {activeTab === 'overview' ? <>
        <section aria-label="Management statistics" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <MetricCard label="Users" value={summary?.users} detail={`${summary?.activeUsers ?? 0} active · ${summary?.lockedUsers ?? 0} locked`} icon={Users} onClick={() => changeTab('users')} />
          <MetricCard label="Products" value={summary?.products} detail={`${summary?.hiddenProducts ?? 0} hidden products`} icon={ShoppingBag} onClick={() => changeTab('products')} />
          <MetricCard label="Posts & groups" value={(summary?.posts ?? 0) + (summary?.groups ?? 0)} detail={`${summary?.hiddenPosts ?? 0} hidden posts · ${summary?.hiddenGroups ?? 0} hidden groups`} icon={FileText} onClick={() => changeTab('posts')} />
          <MetricCard label="Pending reports" value={summary?.pendingReports} detail="Requires review" icon={Flag} onClick={() => { changeTab('reports'); chooseFilter('PENDING') }} />
        </section>

        <section className="mt-6 grid gap-5 xl:grid-cols-[1.4fr_1fr]">
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <h2 className="text-lg font-extrabold">Current permissions</h2>
            <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{isAdmin ? 'Admins inherit Manager permissions and can also manage Manager accounts.' : 'Managers operate the community and cannot modify administrator accounts.'}</p>
            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              <div className="rounded-xl bg-slate-50 p-4 dark:bg-slate-800/70"><p className="font-bold">Content moderation</p><p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">Manage products, posts, community groups, and reports.</p></div>
              <div className="rounded-xl bg-slate-50 p-4 dark:bg-slate-800/70"><p className="font-bold">Account management</p><p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">{isAdmin ? `${summary?.managers ?? 0} Manager accounts in the system.` : 'Search, view, lock, and unlock User accounts.'}</p></div>
            </div>
          </div>
          <div className="rounded-2xl border border-blue-100 bg-blue-50 p-6 dark:border-blue-900 dark:bg-blue-950/30">
            <h2 className="text-lg font-extrabold text-blue-950 dark:text-blue-100">Quick start</h2>
            <div className="mt-4 flex flex-col gap-2">
              <button onClick={() => changeTab('reports')} className="flex min-h-11 items-center justify-between rounded-xl bg-white px-4 text-sm font-bold text-slate-700 hover:text-blue-600 dark:bg-slate-900 dark:text-slate-200"><span>Review reports</span><ArrowRight size={17} aria-hidden="true" /></button>
              <button onClick={() => changeTab('users')} className="flex min-h-11 items-center justify-between rounded-xl bg-white px-4 text-sm font-bold text-slate-700 hover:text-blue-600 dark:bg-slate-900 dark:text-slate-200"><span>Search users</span><ArrowRight size={17} aria-hidden="true" /></button>
              {isAdmin && <button onClick={() => changeTab('managers')} className="flex min-h-11 items-center justify-between rounded-xl bg-white px-4 text-sm font-bold text-slate-700 hover:text-blue-600 dark:bg-slate-900 dark:text-slate-200"><span>Manage Manager accounts</span><ArrowRight size={17} aria-hidden="true" /></button>}
            </div>
          </div>
        </section>
      </> : <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-100 p-5 dark:border-slate-800 sm:p-6">
          <div className="flex items-center gap-3">
            <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-50 text-blue-600 dark:bg-blue-950/50 dark:text-blue-300"><CurrentIcon size={20} aria-hidden="true" /></span>
            <div><h2 className="font-extrabold">{currentTab.label}</h2><p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{tableData.totalElements ?? 0} results</p></div>
          </div>
          {activeTab === 'managers' && <button type="button" onClick={() => setShowManagerForm((shown) => !shown)} className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-blue-600 px-4 text-sm font-bold text-white hover:bg-blue-700"><Plus size={17} aria-hidden="true" />Create Manager</button>}
          {activeTab === 'packages' && <button type="button" onClick={() => { setEditingPackageId(''); setPackageForm({ code: '', name: '', description: '', packageType: 'LISTING_CREDITS', price: '', listingCredits: '10', durationHours: '720', active: true }); setShowPackageForm((shown) => !shown) }} className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-blue-600 px-4 text-sm font-bold text-white hover:bg-blue-700"><Plus size={17} aria-hidden="true" />Create package</button>}
          {activeTab === 'billing' && <button type="button" onClick={() => setRefreshKey((value) => value + 1)} className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-slate-200 px-4 text-sm font-bold text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800"><RefreshCw size={15} aria-hidden="true" />Refresh</button>}
        </div>

        {activeTab === 'managers' && showManagerForm && <form onSubmit={handleCreateManager} className="grid gap-3 border-b border-slate-100 bg-slate-50 p-5 dark:border-slate-800 dark:bg-slate-800/40 sm:grid-cols-2 lg:grid-cols-4">
          <label className="text-xs font-bold text-slate-600 dark:text-slate-300">Full name<input required maxLength={255} value={managerForm.fullName} onChange={(event) => setManagerForm({ ...managerForm, fullName: event.target.value })} className="mt-1.5 min-h-11 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm font-normal dark:border-slate-700 dark:bg-slate-900" /></label>
          <label className="text-xs font-bold text-slate-600 dark:text-slate-300">Email<input required type="email" maxLength={255} autoComplete="email" value={managerForm.email} onChange={(event) => setManagerForm({ ...managerForm, email: event.target.value })} className="mt-1.5 min-h-11 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm font-normal dark:border-slate-700 dark:bg-slate-900" /></label>
          <label className="text-xs font-bold text-slate-600 dark:text-slate-300">Temporary password<input required type="password" minLength={8} maxLength={72} autoComplete="new-password" value={managerForm.password} onChange={(event) => setManagerForm({ ...managerForm, password: event.target.value })} className="mt-1.5 min-h-11 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm font-normal dark:border-slate-700 dark:bg-slate-900" /></label>
          <div className="flex items-end gap-2"><button disabled={actionLoading} className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-bold text-white disabled:opacity-50">{actionLoading ? 'Creating…' : 'Create account'}</button><ActionButton onClick={() => setShowManagerForm(false)}>Cancel</ActionButton></div>
        </form>}

        {activeTab === 'packages' && <div className="space-y-4 border-b border-slate-100 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-800/40 sm:p-5">
          <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-900">
            <div className="flex min-w-0 items-center gap-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-blue-600 dark:bg-blue-950/50 dark:text-blue-300"><CreditCard size={18} /></span>
              <div><p className="text-sm font-extrabold">Payment QR code</p><p className="text-xs text-slate-500 dark:text-slate-400">{paymentQrUrl ? 'Configured. Make sure the QR code points to the correct receiving account.' : 'Not configured — users cannot create payment requests yet.'}</p></div>
            </div>
            <label className={`inline-flex min-h-10 cursor-pointer items-center gap-2 rounded-lg border border-slate-200 px-3 text-xs font-bold dark:border-slate-700 ${isUploadingQr ? 'pointer-events-none opacity-50' : ''}`}><ImagePlus size={15} />{isUploadingQr ? 'Uploading…' : 'Upload QR code'}<input type="file" accept="image/jpeg,image/png,image/webp" disabled={isUploadingQr} onChange={uploadPaymentQr} className="sr-only" /></label>
            {paymentQrUrl && <img src={paymentQrUrl} alt="Configured payment QR code" className="h-20 w-20 rounded-lg border border-slate-200 bg-white object-contain p-1 dark:border-slate-700" />}
          </div>
          <p className="text-xs leading-5 text-amber-800 dark:text-amber-300">Payments are verified manually. Approve only after confirming the received transaction, amount, and bank transfer reference.</p>
        </div>}

        {activeTab === 'packages' && showPackageForm && <form onSubmit={saveBillingPackage} className="grid gap-3 border-b border-slate-100 bg-white p-5 dark:border-slate-800 dark:bg-slate-900 sm:grid-cols-2 lg:grid-cols-4">
          <label className="text-xs font-bold text-slate-600 dark:text-slate-300">Package code<input required maxLength={80} value={packageForm.code} onChange={(event) => setPackageForm({ ...packageForm, code: event.target.value })} className="mt-1.5 min-h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm font-normal dark:border-slate-700 dark:bg-slate-950" /></label>
          <label className="text-xs font-bold text-slate-600 dark:text-slate-300">Package name<input required maxLength={150} value={packageForm.name} onChange={(event) => setPackageForm({ ...packageForm, name: event.target.value })} className="mt-1.5 min-h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm font-normal dark:border-slate-700 dark:bg-slate-950" /></label>
          <label className="text-xs font-bold text-slate-600 dark:text-slate-300">Package type<select value={packageForm.packageType} onChange={(event) => setPackageForm({ ...packageForm, packageType: event.target.value, listingCredits: event.target.value === 'LISTING_CREDITS' ? (packageForm.listingCredits || '10') : '', durationHours: event.target.value === 'LISTING_CREDITS' ? '720' : '24' })} className="mt-1.5 min-h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm dark:border-slate-700 dark:bg-slate-950"><option value="LISTING_CREDITS">Listing credits</option><option value="BOOST">Listing boost</option></select></label>
          <label className="text-xs font-bold text-slate-600 dark:text-slate-300">Price (VND)<input required type="number" min="1" step="1" value={packageForm.price} onChange={(event) => setPackageForm({ ...packageForm, price: event.target.value })} className="mt-1.5 min-h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm font-normal dark:border-slate-700 dark:bg-slate-950" /></label>
          {packageForm.packageType === 'LISTING_CREDITS' && <label className="text-xs font-bold text-slate-600 dark:text-slate-300">Listing credits<input required type="number" min="1" step="1" value={packageForm.listingCredits} onChange={(event) => setPackageForm({ ...packageForm, listingCredits: event.target.value })} className="mt-1.5 min-h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm font-normal dark:border-slate-700 dark:bg-slate-950" /></label>}
          <label className="text-xs font-bold text-slate-600 dark:text-slate-300">Duration (hours)<input required type="number" min="1" step="1" value={packageForm.durationHours} onChange={(event) => setPackageForm({ ...packageForm, durationHours: event.target.value })} className="mt-1.5 min-h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm font-normal dark:border-slate-700 dark:bg-slate-950" /><span className="mt-1 block font-normal text-slate-500">{packageForm.packageType === 'BOOST' ? 'Featured listing duration' : 'Listing credits validity after approval'}</span></label>
          <label className="text-xs font-bold text-slate-600 dark:text-slate-300 sm:col-span-2">Description<textarea maxLength={1000} value={packageForm.description || ''} onChange={(event) => setPackageForm({ ...packageForm, description: event.target.value })} rows="2" className="mt-1.5 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-normal dark:border-slate-700 dark:bg-slate-950" /></label>
          <label className="flex items-center gap-2 self-end pb-2 text-sm font-semibold"><input type="checkbox" checked={Boolean(packageForm.active)} onChange={(event) => setPackageForm({ ...packageForm, active: event.target.checked })} />Active</label>
          <div className="flex items-end gap-2"><button disabled={actionLoading} className="min-h-10 rounded-lg bg-blue-600 px-4 text-sm font-bold text-white disabled:opacity-50">{actionLoading ? 'Saving…' : 'Save package'}</button><ActionButton onClick={() => { setShowPackageForm(false); setEditingPackageId('') }}>Cancel</ActionButton></div>
        </form>}

        <div className="flex flex-wrap gap-3 border-b border-slate-100 p-4 dark:border-slate-800 sm:p-5">
          {!['reports', 'billing', 'packages'].includes(activeTab) && <label className="relative min-w-[220px] flex-1 sm:max-w-sm"><span className="sr-only">Search {currentTab.label.toLowerCase()}</span><Search size={16} aria-hidden="true" className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" /><input value={search} onChange={(event) => { setSearch(event.target.value); setPage(0) }} placeholder="Search by name, email, or content…" className="min-h-11 w-full rounded-xl border border-slate-200 bg-white pl-9 pr-3 text-sm dark:border-slate-700 dark:bg-slate-900" /></label>}
          {filterOptions[activeTab] && <label><span className="sr-only">Filter {currentTab.label.toLowerCase()}</span><select value={filter} onChange={(event) => chooseFilter(event.target.value)} className="min-h-11 rounded-xl border border-slate-200 bg-white px-3 text-sm dark:border-slate-700 dark:bg-slate-900">{filterOptions[activeTab].map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>}
        </div>

        {loading ? <div className="flex min-h-56 items-center justify-center gap-2 text-sm text-slate-500"><LoaderCircle className="animate-spin" size={18} aria-hidden="true" />Loading data…</div>
        : tableData.content?.length ? <>
          <div className="hidden overflow-x-auto md:block">
            <table className="w-full min-w-[900px] text-left text-sm [&_tbody_tr]:transition-colors [&_tbody_tr:hover]:bg-slate-50/80 [&_td]:px-5 [&_td]:py-4 [&_td]:align-middle [&_td]:leading-6 dark:[&_tbody_tr:hover]:bg-slate-800/40">
              <caption className="sr-only">{currentTab.label}</caption>
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-800/70 dark:text-slate-400"><tr>{(tableHeaders[activeTab] || []).map((header) => <th key={header} scope="col" className="whitespace-nowrap px-5 py-3.5 font-bold">{header}</th>)}</tr></thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">{renderRows()}</tbody>
            </table>
          </div>
          <div className="grid gap-3 p-3 md:hidden">{renderMobileRows()}</div>
        </> : <div className="px-5 py-14 text-center"><span className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-slate-400 dark:bg-slate-800 dark:text-slate-500"><Search size={20} aria-hidden="true" /></span><p className="mt-3 text-sm font-bold text-slate-700 dark:text-slate-200">No results found</p><p className="mt-1 text-xs text-slate-500 dark:text-slate-400">Try changing your search terms or filters to see more results.</p></div>}

        {!['billing', 'packages'].includes(activeTab) && <footer className="flex items-center justify-between border-t border-slate-100 px-4 py-4 dark:border-slate-800 sm:px-5">
          <p className="text-xs text-slate-500 dark:text-slate-400">Page {tableData.totalPages ? page + 1 : 0} / {tableData.totalPages || 0}</p>
          <div className="flex gap-2">
            <ActionButton disabled={page === 0 || loading} onClick={() => setPage((value) => Math.max(0, value - 1))}><ArrowLeft size={14} aria-hidden="true" />Previous</ActionButton>
            <ActionButton disabled={loading || page + 1 >= tableData.totalPages} onClick={() => setPage((value) => value + 1)}>Next<ArrowRight size={14} aria-hidden="true" /></ActionButton>
          </div>
        </footer>}
      </section>}
    </div>
  </main>
}

export default ManagementDashboard
