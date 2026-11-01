import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  ArrowLeft,
  ArrowRight,
  BarChart3,
  Check,
  Eye,
  FileText,
  Flag,
  LayoutDashboard,
  LoaderCircle,
  LockKeyhole,
  Plus,
  Search,
  Shield,
  ShoppingBag,
  Trash2,
  Users,
  X,
} from 'lucide-react'
import { apiErrorMessage, apiFetch } from '../lib/api'

const PAGE_SIZE = 15

const managementTabs = [
  { id: 'overview', label: 'Tổng quan', icon: LayoutDashboard },
  { id: 'users', label: 'Người dùng', icon: Users },
  { id: 'products', label: 'Sản phẩm', icon: ShoppingBag },
  { id: 'posts', label: 'Bài viết', icon: FileText },
  { id: 'pages', label: 'Trang cộng đồng', icon: Shield },
  { id: 'reports', label: 'Báo cáo', icon: Flag },
]

const adminTab = { id: 'managers', label: 'Quản lý Manager', icon: Shield }

async function requestJson(path, options, fallback) {
  const response = await apiFetch(path, options)
  if (!response.ok) throw new Error(await apiErrorMessage(response, fallback))
  return response.status === 204 ? null : response.json()
}

function formatDate(value) {
  return value ? new Date(value).toLocaleDateString('vi-VN') : '—'
}

function formatPrice(value) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value || 0)
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
  const tabs = useMemo(() => isAdmin ? [...managementTabs, adminTab] : managementTabs, [isAdmin])
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

  const loadTable = useCallback(async () => {
    if (activeTab === 'overview') {
      setLoading(false)
      return
    }
    setLoading(true)
    setError('')
    const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) })
    if (search.trim()) params.set('search', search.trim())

    let path = `/manager/${activeTab}?${params.toString()}`
    if (activeTab === 'users') {
      if (filter === 'USER' || filter === 'MANAGER' || filter === 'ADMIN') params.set('role', filter)
      if (['ACTIVE', 'LOCKED', 'DELETED'].includes(filter)) params.set('status', filter)
    } else if (activeTab === 'managers') {
      params.set('role', 'MANAGER')
      path = `/manager/users?${params.toString()}`
    } else if (activeTab === 'products' && filter) {
      params.set('status', filter)
    } else if ((activeTab === 'posts' || activeTab === 'pages') && filter) {
      params.set('hidden', filter === 'HIDDEN' ? 'true' : 'false')
    } else if (activeTab === 'reports' && filter) {
      params.set('status', filter)
    }
    path = path.split('?')[0] + `?${params.toString()}`

    try {
      const data = await requestJson(path, undefined, 'Không thể tải danh sách quản trị.')
      setTableData(data)
    } catch (requestError) {
      setError(requestError.message || 'Không thể tải danh sách quản trị.')
    } finally {
      setLoading(false)
    }
  }, [activeTab, filter, page, search])

  useEffect(() => {
    let cancelled = false
    const fetchSummary = async () => {
      try {
        const data = await requestJson('/manager/dashboard', undefined, 'Không thể tải dữ liệu tổng quan.')
        if (!cancelled) setSummary(data)
      } catch (requestError) {
        if (!cancelled) setError(requestError.message || 'Không thể tải dữ liệu tổng quan.')
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
    setFilter('')
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
      }, 'Thao tác không thành công.')
      setNotice(successMessage)
      setRefreshKey((current) => current + 1)
      return true
    } catch (requestError) {
      setError(requestError.message || 'Thao tác không thành công.')
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
      'Đã tạo tài khoản Manager.',
    )
    if (succeeded) {
      setManagerForm({ fullName: '', email: '', password: '' })
      setShowManagerForm(false)
    }
  }

  const chooseFilter = (value) => {
    setFilter(value)
    setPage(0)
  }

  const renderUserActions = (item) => {
    if (item.role === 'ADMIN') return <span className="text-xs text-slate-400">Tài khoản Admin</span>
    if (activeTab === 'users' && isAdmin && item.role === 'USER') return <div className="flex flex-wrap gap-2">
      <ActionButton
        disabled={actionLoading || item.status === 'DELETED'}
        onClick={() => runAction(`/admin/users/${item.id}/role`, 'PATCH', { role: 'MANAGER' }, 'Đã cấp vai trò Manager.')}
      >Cấp Manager</ActionButton>
      <ActionButton
        disabled={actionLoading || item.status === 'DELETED'}
        onClick={() => runAction(`/manager/users/${item.id}/status`, 'PATCH', { status: item.status === 'LOCKED' ? 'ACTIVE' : 'LOCKED' }, item.status === 'LOCKED' ? 'Đã mở khóa người dùng.' : 'Đã khóa người dùng.')}
      >{item.status === 'LOCKED' ? 'Mở khóa' : 'Khóa'}</ActionButton>
    </div>
    if (activeTab === 'managers') {
      const nextRole = item.role === 'MANAGER' ? 'USER' : 'MANAGER'
      return <div className="flex flex-wrap gap-2">
        <ActionButton
          disabled={actionLoading || item.status === 'DELETED'}
          onClick={() => runAction(`/admin/users/${item.id}/role`, 'PATCH', { role: nextRole }, `Đã chuyển vai trò sang ${nextRole}.`)}
        >Chuyển thành {nextRole}</ActionButton>
        <ActionButton
          disabled={actionLoading || item.status === 'DELETED'}
          onClick={() => runAction(`/admin/managers/${item.id}/status`, 'PATCH', { status: item.status === 'LOCKED' ? 'ACTIVE' : 'LOCKED' }, item.status === 'LOCKED' ? 'Đã mở khóa Manager.' : 'Đã khóa Manager.')}
        >{item.status === 'LOCKED' ? 'Mở khóa' : 'Khóa'}</ActionButton>
        <ActionButton
          danger
          disabled={actionLoading || item.status === 'DELETED'}
          onClick={() => {
            if (window.confirm(`Xóa tài khoản Manager ${item.fullName}? Tài khoản sẽ bị vô hiệu hóa.`)) {
              runAction(`/admin/managers/${item.id}`, 'DELETE', null, 'Đã vô hiệu hóa tài khoản Manager.')
            }
          }}
        ><Trash2 size={14} aria-hidden="true" /> Xóa</ActionButton>
      </div>
    }
    if (item.role !== 'USER') return <span className="text-xs text-slate-400">Không thể thao tác</span>
    return <ActionButton
      disabled={actionLoading || item.status === 'DELETED'}
      onClick={() => runAction(`/manager/users/${item.id}/status`, 'PATCH', { status: item.status === 'LOCKED' ? 'ACTIVE' : 'LOCKED' }, item.status === 'LOCKED' ? 'Đã mở khóa người dùng.' : 'Đã khóa người dùng.')}
    ><LockKeyhole size={14} aria-hidden="true" />{item.status === 'LOCKED' ? 'Mở khóa' : 'Khóa'}</ActionButton>
  }

  const renderRows = () => {
    const rows = tableData.content || []
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
      <td>{item.status === 'SOLD' ? <span className="text-xs text-slate-400">Đã bán</span> : <ActionButton disabled={actionLoading} onClick={() => runAction(`/manager/products/${item.id}/visibility`, 'PATCH', { hidden: item.status !== 'HIDDEN' }, item.status === 'HIDDEN' ? 'Đã khôi phục sản phẩm.' : 'Đã ẩn sản phẩm.')}><Eye size={14} aria-hidden="true" />{item.status === 'HIDDEN' ? 'Khôi phục' : 'Ẩn'}</ActionButton>}</td>
    </tr>)
    if (activeTab === 'posts') return rows.map((item) => <tr key={item.id}>
      <td className="max-w-[360px]"><span className="line-clamp-2">{item.content}</span></td>
      <td>{item.authorName}</td>
      <td><StatusBadge>{item.hidden ? 'HIDDEN' : 'PUBLIC'}</StatusBadge></td>
      <td>{formatDate(item.createdAt)}</td>
      <td><ActionButton disabled={actionLoading} onClick={() => runAction(`/manager/posts/${item.id}/visibility`, 'PATCH', { hidden: !item.hidden }, item.hidden ? 'Đã hiển thị bài viết.' : 'Đã ẩn bài viết.')}><Eye size={14} aria-hidden="true" />{item.hidden ? 'Hiện' : 'Ẩn'}</ActionButton></td>
    </tr>)
    if (activeTab === 'pages') return rows.map((item) => <tr key={item.id}>
      <td className="font-semibold">{item.name}</td>
      <td>{item.ownerName}</td>
      <td><StatusBadge>{item.hidden ? 'HIDDEN' : 'PUBLIC'}</StatusBadge></td>
      <td>{formatDate(item.createdAt)}</td>
      <td><ActionButton disabled={actionLoading} onClick={() => runAction(`/manager/pages/${item.id}/visibility`, 'PATCH', { hidden: !item.hidden }, item.hidden ? 'Đã hiển thị trang.' : 'Đã ẩn trang.')}><Eye size={14} aria-hidden="true" />{item.hidden ? 'Hiện' : 'Ẩn'}</ActionButton></td>
    </tr>)
    if (activeTab === 'reports') return rows.map((item) => <tr key={item.id}>
      <td><StatusBadge>{item.status}</StatusBadge></td>
      <td>{item.targetType}</td>
      <td className="max-w-[180px] break-all font-mono text-xs">{item.targetId}</td>
      <td className="max-w-[220px]"><span className="line-clamp-2">{item.reason}</span><span className="mt-1 block text-xs text-slate-400">Bởi {item.reporterName}</span></td>
      <td>{formatDate(item.createdAt)}</td>
      <td>{item.status === 'PENDING' ? <div className="flex gap-2">
        <ActionButton disabled={actionLoading} onClick={() => runAction(`/manager/reports/${item.id}`, 'PATCH', { status: 'APPROVED' }, 'Đã chấp nhận báo cáo và xử lý nội dung.')}><Check size={14} aria-hidden="true" />Xử lý</ActionButton>
        <ActionButton danger disabled={actionLoading} onClick={() => runAction(`/manager/reports/${item.id}`, 'PATCH', { status: 'REJECTED' }, 'Đã từ chối báo cáo.')}><X size={14} aria-hidden="true" />Từ chối</ActionButton>
      </div> : <span className="text-xs text-slate-400">Đã xử lý</span>}</td>
    </tr>)
    return null
  }

  const tableHeaders = {
    users: ['Họ tên', 'Email', 'Vai trò', 'Trạng thái', 'Ngày tạo', 'Thao tác'],
    managers: ['Họ tên', 'Email', 'Vai trò', 'Trạng thái', 'Ngày tạo', 'Thao tác'],
    products: ['Sản phẩm', 'Người bán', 'Giá', 'Trạng thái', 'Ngày đăng', 'Thao tác'],
    posts: ['Nội dung', 'Tác giả', 'Hiển thị', 'Ngày đăng', 'Thao tác'],
    pages: ['Trang', 'Chủ sở hữu', 'Hiển thị', 'Ngày tạo', 'Thao tác'],
    reports: ['Trạng thái', 'Đối tượng', 'Mã nội dung', 'Lý do báo cáo', 'Ngày gửi', 'Thao tác'],
  }

  const filterOptions = {
    users: [['', 'Mọi vai trò/trạng thái'], ['USER', 'Người dùng'], ['MANAGER', 'Manager'], ['ADMIN', 'Admin'], ['ACTIVE', 'Đang hoạt động'], ['LOCKED', 'Đã khóa']],
    products: [['', 'Mọi trạng thái'], ['SELLING', 'Đang bán'], ['SOLD', 'Đã bán'], ['HIDDEN', 'Đã ẩn']],
    posts: [['', 'Mọi bài viết'], ['VISIBLE', 'Đang hiển thị'], ['HIDDEN', 'Đã ẩn']],
    pages: [['', 'Mọi trang'], ['VISIBLE', 'Đang hiển thị'], ['HIDDEN', 'Đã ẩn']],
    reports: [['', 'Mọi báo cáo'], ['PENDING', 'Chờ xử lý'], ['APPROVED', 'Đã chấp nhận'], ['REJECTED', 'Đã từ chối']],
  }

  const currentTab = tabs.find((tab) => tab.id === activeTab) || tabs[0]
  const CurrentIcon = currentTab.icon

  return <main className="min-h-screen px-4 pb-28 pt-6 sm:px-6 lg:ml-[248px] lg:px-10 lg:pb-10">
    <div className="mx-auto max-w-[1500px]">
      <header className="mb-7 flex flex-wrap items-end justify-between gap-4">
        <div>
          <div className="mb-2 flex items-center gap-2 text-xs font-bold uppercase tracking-[0.16em] text-blue-600 dark:text-blue-300"><BarChart3 size={15} aria-hidden="true" /> Otech · Quản trị</div>
          <h1 className="text-3xl font-extrabold tracking-tight">{isAdmin ? 'Bảng điều khiển Admin' : 'Bảng điều khiển Manager'}</h1>
          <p className="mt-2 max-w-2xl text-sm text-slate-500 dark:text-slate-400">Theo dõi cộng đồng, kiểm duyệt nội dung và xử lý các báo cáo vi phạm.</p>
        </div>
        <span className="rounded-full border border-blue-100 bg-blue-50 px-3 py-1.5 text-xs font-bold text-blue-700 dark:border-blue-900 dark:bg-blue-950/50 dark:text-blue-300">{user?.fullName} · {isAdmin ? 'ADMIN' : 'MANAGER'}</span>
      </header>

      <nav aria-label="Các chức năng quản trị" className="mb-6 flex gap-2 overflow-x-auto border-b border-slate-200 pb-2 dark:border-slate-800">
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

      {error && <div role="alert" className="mb-5 flex items-start justify-between gap-3 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:border-rose-900 dark:bg-rose-950/30 dark:text-rose-300"><span>{error}</span><button aria-label="Đóng thông báo lỗi" onClick={() => setError('')}><X size={17} /></button></div>}
      {notice && <div role="status" aria-live="polite" className="mb-5 flex items-center justify-between gap-3 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:border-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-300"><span>{notice}</span><button aria-label="Đóng thông báo" onClick={() => setNotice('')}><X size={17} /></button></div>}

      {activeTab === 'overview' ? <>
        <section aria-label="Thống kê quản trị" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <MetricCard label="Người dùng" value={summary?.users} detail={`${summary?.activeUsers ?? 0} đang hoạt động · ${summary?.lockedUsers ?? 0} đã khóa`} icon={Users} onClick={() => changeTab('users')} />
          <MetricCard label="Sản phẩm" value={summary?.products} detail={`${summary?.hiddenProducts ?? 0} sản phẩm đang ẩn`} icon={ShoppingBag} onClick={() => changeTab('products')} />
          <MetricCard label="Bài viết & trang" value={(summary?.posts ?? 0) + (summary?.pages ?? 0)} detail={`${summary?.hiddenPosts ?? 0} bài viết · ${summary?.hiddenPages ?? 0} trang đang ẩn`} icon={FileText} onClick={() => changeTab('posts')} />
          <MetricCard label="Báo cáo chờ xử lý" value={summary?.pendingReports} detail="Cần được xem xét" icon={Flag} onClick={() => { changeTab('reports'); chooseFilter('PENDING') }} />
        </section>

        <section className="mt-6 grid gap-5 xl:grid-cols-[1.4fr_1fr]">
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <h2 className="text-lg font-extrabold">Quyền hạn hiện tại</h2>
            <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{isAdmin ? 'Admin kế thừa các quyền vận hành của Manager và có thêm quyền quản trị tài khoản Manager.' : 'Manager tập trung vận hành cộng đồng và không thể thay đổi tài khoản quản trị.'}</p>
            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              <div className="rounded-xl bg-slate-50 p-4 dark:bg-slate-800/70"><p className="font-bold">Kiểm duyệt nội dung</p><p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">Quản lý sản phẩm, bài viết, trang cộng đồng và báo cáo.</p></div>
              <div className="rounded-xl bg-slate-50 p-4 dark:bg-slate-800/70"><p className="font-bold">Quản lý tài khoản</p><p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">{isAdmin ? `${summary?.managers ?? 0} tài khoản Manager trong hệ thống.` : 'Tìm kiếm, xem và khóa/mở khóa tài khoản User.'}</p></div>
            </div>
          </div>
          <div className="rounded-2xl border border-blue-100 bg-blue-50 p-6 dark:border-blue-900 dark:bg-blue-950/30">
            <h2 className="text-lg font-extrabold text-blue-950 dark:text-blue-100">Bắt đầu nhanh</h2>
            <div className="mt-4 flex flex-col gap-2">
              <button onClick={() => changeTab('reports')} className="flex min-h-11 items-center justify-between rounded-xl bg-white px-4 text-sm font-bold text-slate-700 hover:text-blue-600 dark:bg-slate-900 dark:text-slate-200"><span>Xem báo cáo vi phạm</span><ArrowRight size={17} aria-hidden="true" /></button>
              <button onClick={() => changeTab('users')} className="flex min-h-11 items-center justify-between rounded-xl bg-white px-4 text-sm font-bold text-slate-700 hover:text-blue-600 dark:bg-slate-900 dark:text-slate-200"><span>Tìm kiếm người dùng</span><ArrowRight size={17} aria-hidden="true" /></button>
              {isAdmin && <button onClick={() => changeTab('managers')} className="flex min-h-11 items-center justify-between rounded-xl bg-white px-4 text-sm font-bold text-slate-700 hover:text-blue-600 dark:bg-slate-900 dark:text-slate-200"><span>Quản lý tài khoản Manager</span><ArrowRight size={17} aria-hidden="true" /></button>}
            </div>
          </div>
        </section>
      </> : <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-100 p-5 dark:border-slate-800 sm:p-6">
          <div className="flex items-center gap-3">
            <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-50 text-blue-600 dark:bg-blue-950/50 dark:text-blue-300"><CurrentIcon size={20} aria-hidden="true" /></span>
            <div><h2 className="font-extrabold">{currentTab.label}</h2><p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{tableData.totalElements ?? 0} kết quả</p></div>
          </div>
          {activeTab === 'managers' && <button type="button" onClick={() => setShowManagerForm((shown) => !shown)} className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-blue-600 px-4 text-sm font-bold text-white hover:bg-blue-700"><Plus size={17} aria-hidden="true" />Tạo Manager</button>}
        </div>

        {activeTab === 'managers' && showManagerForm && <form onSubmit={handleCreateManager} className="grid gap-3 border-b border-slate-100 bg-slate-50 p-5 dark:border-slate-800 dark:bg-slate-800/40 sm:grid-cols-2 lg:grid-cols-4">
          <label className="text-xs font-bold text-slate-600 dark:text-slate-300">Họ tên<input required maxLength={255} value={managerForm.fullName} onChange={(event) => setManagerForm({ ...managerForm, fullName: event.target.value })} className="mt-1.5 min-h-11 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm font-normal dark:border-slate-700 dark:bg-slate-900" /></label>
          <label className="text-xs font-bold text-slate-600 dark:text-slate-300">Email<input required type="email" maxLength={255} autoComplete="email" value={managerForm.email} onChange={(event) => setManagerForm({ ...managerForm, email: event.target.value })} className="mt-1.5 min-h-11 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm font-normal dark:border-slate-700 dark:bg-slate-900" /></label>
          <label className="text-xs font-bold text-slate-600 dark:text-slate-300">Mật khẩu tạm thời<input required type="password" minLength={8} maxLength={72} autoComplete="new-password" value={managerForm.password} onChange={(event) => setManagerForm({ ...managerForm, password: event.target.value })} className="mt-1.5 min-h-11 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm font-normal dark:border-slate-700 dark:bg-slate-900" /></label>
          <div className="flex items-end gap-2"><button disabled={actionLoading} className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-bold text-white disabled:opacity-50">{actionLoading ? 'Đang tạo…' : 'Tạo tài khoản'}</button><ActionButton onClick={() => setShowManagerForm(false)}>Hủy</ActionButton></div>
        </form>}

        <div className="flex flex-wrap gap-3 border-b border-slate-100 p-4 dark:border-slate-800 sm:p-5">
          {activeTab !== 'reports' && <label className="relative min-w-[220px] flex-1 sm:max-w-sm"><span className="sr-only">Tìm kiếm {currentTab.label.toLowerCase()}</span><Search size={16} aria-hidden="true" className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" /><input value={search} onChange={(event) => { setSearch(event.target.value); setPage(0) }} placeholder="Tìm theo tên, email hoặc nội dung…" className="min-h-11 w-full rounded-xl border border-slate-200 bg-white pl-9 pr-3 text-sm dark:border-slate-700 dark:bg-slate-900" /></label>}
          {filterOptions[activeTab] && <label><span className="sr-only">Lọc {currentTab.label.toLowerCase()}</span><select value={filter} onChange={(event) => chooseFilter(event.target.value)} className="min-h-11 rounded-xl border border-slate-200 bg-white px-3 text-sm dark:border-slate-700 dark:bg-slate-900">{filterOptions[activeTab].map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>}
        </div>

        {loading ? <div className="flex min-h-56 items-center justify-center gap-2 text-sm text-slate-500"><LoaderCircle className="animate-spin" size={18} aria-hidden="true" />Đang tải dữ liệu…</div>
          : <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-sm">
              <caption className="sr-only">{currentTab.label}</caption>
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-800/70 dark:text-slate-400"><tr>{(tableHeaders[activeTab] || []).map((header) => <th key={header} scope="col" className="px-4 py-3 font-bold">{header}</th>)}</tr></thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">{renderRows()}</tbody>
            </table>
            {!tableData.content?.length && <div className="px-5 py-14 text-center text-sm text-slate-500 dark:text-slate-400">Không có dữ liệu phù hợp.</div>}
          </div>}

        <footer className="flex items-center justify-between border-t border-slate-100 px-4 py-4 dark:border-slate-800 sm:px-5">
          <p className="text-xs text-slate-500 dark:text-slate-400">Trang {tableData.totalPages ? page + 1 : 0} / {tableData.totalPages || 0}</p>
          <div className="flex gap-2">
            <ActionButton disabled={page === 0 || loading} onClick={() => setPage((value) => Math.max(0, value - 1))}><ArrowLeft size={14} aria-hidden="true" />Trước</ActionButton>
            <ActionButton disabled={loading || page + 1 >= tableData.totalPages} onClick={() => setPage((value) => value + 1)}>Sau<ArrowRight size={14} aria-hidden="true" /></ActionButton>
          </div>
        </footer>
      </section>}
    </div>
  </main>
}

export default ManagementDashboard
