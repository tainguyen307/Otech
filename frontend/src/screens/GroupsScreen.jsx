import { useEffect, useMemo, useState } from 'react'
import { ArrowLeft, CalendarDays, ImagePlus, LoaderCircle, Plus, Search, UsersRound } from 'lucide-react'
import PostComposer from '../components/PostComposer'
import { apiErrorMessage, apiFetch } from '../lib/api'

const PAGE_SIZE = 24
const EMPTY_GROUP = {
  name: '',
  description: '',
  categoryId: '',
  visibility: 'PUBLIC',
  requirePostApproval: true,
  avatarUrl: '',
  coverUrl: '',
}

async function readResponse(response, fallback) {
  if (!response.ok) throw new Error(await apiErrorMessage(response, fallback))
  if (response.status === 204) return null
  const contentType = response.headers.get('content-type') || ''
  return contentType.includes('application/json') ? response.json() : response.text()
}

function formatDate(value) {
  return value
    ? new Intl.DateTimeFormat('vi-VN', { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(value))
    : '—'
}

function GroupForm({ group, categories, onSave, onCancel, isSaving }) {
  const [form, setForm] = useState(() => group ? {
    name: group.name,
    description: group.description || '',
    categoryId: group.categoryId || '',
    visibility: group.visibility || 'PUBLIC',
    requirePostApproval: group.requirePostApproval !== false,
    avatarUrl: group.avatarUrl || '',
    coverUrl: group.coverUrl || '',
  } : EMPTY_GROUP)
  const [uploadingField, setUploadingField] = useState('')
  const [error, setError] = useState('')

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  function submit(event) {
    event.preventDefault()
    onSave({
      ...form,
      name: form.name.trim(),
      description: form.description.trim(),
      avatarUrl: form.avatarUrl || null,
      coverUrl: form.coverUrl || null,
    })
  }

  async function uploadImage(field, event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setUploadingField(field)
    setError('')
    try {
      const body = new FormData()
      body.append('file', file)
      const response = await apiFetch('/products/images', { method: 'POST', body })
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Không thể tải ảnh nhóm.'))
      const result = await response.json()
      if (!result.imageUrl) throw new Error('Dịch vụ ảnh không trả về đường dẫn ảnh.')
      update(field, result.imageUrl)
    } catch (uploadError) {
      setError(uploadError.message || 'Không thể tải ảnh nhóm.')
    } finally {
      setUploadingField('')
    }
  }

  return <form onSubmit={submit} className="mb-5 grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900 sm:grid-cols-2">
    <label className="text-sm font-semibold text-slate-700 dark:text-slate-200">
      Tên nhóm
      <input required maxLength="255" value={form.name} onChange={(event) => update('name', event.target.value)} className="mt-1 min-h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm dark:border-slate-700 dark:bg-slate-950" />
    </label>
    <label className="text-sm font-semibold text-slate-700 dark:text-slate-200">
      Nhóm hàng hóa
      <select required value={form.categoryId} onChange={(event) => update('categoryId', event.target.value)} className="mt-1 min-h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm dark:border-slate-700 dark:bg-slate-950">
        <option value="">Chọn danh mục hàng hóa</option>
        {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
      </select>
    </label>
    <label className="text-sm font-semibold text-slate-700 dark:text-slate-200 sm:col-span-2">
      Giới thiệu
      <textarea maxLength="2000" rows="3" value={form.description} onChange={(event) => update('description', event.target.value)} className="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-950" />
    </label>
    {[
      ['avatarUrl', 'Ảnh đại diện nhóm'],
      ['coverUrl', 'Ảnh bìa nhóm'],
    ].map(([field, label]) => <div key={field} className="text-sm font-semibold text-slate-700 dark:text-slate-200">
      <label>{label}<input type="url" maxLength="2000" value={form[field]} onChange={(event) => update(field, event.target.value)} placeholder="https://…" className="mt-1 min-h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm dark:border-slate-700 dark:bg-slate-950" /></label>
      <label className={`mt-2 inline-flex min-h-9 cursor-pointer items-center gap-2 rounded-lg border border-slate-200 px-3 text-xs font-bold dark:border-slate-700 ${uploadingField ? 'pointer-events-none opacity-50' : ''}`}>
        <ImagePlus size={14} />{uploadingField === field ? 'Đang tải ảnh…' : 'Tải ảnh lên'}
        <input type="file" accept="image/jpeg,image/png,image/webp" disabled={Boolean(uploadingField)} onChange={(event) => uploadImage(field, event)} className="sr-only" />
      </label>
      {form[field] && <img src={form[field]} alt="" className="mt-2 max-h-24 rounded-lg object-cover" />}
    </div>)}
    <label className="text-sm font-semibold text-slate-700 dark:text-slate-200">
      Quyền riêng tư
      <select value={form.visibility} onChange={(event) => update('visibility', event.target.value)} className="mt-1 min-h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm dark:border-slate-700 dark:bg-slate-950">
        <option value="PUBLIC">Công khai</option>
        <option value="PRIVATE">Riêng tư — cần duyệt yêu cầu tham gia</option>
      </select>
    </label>
    <label className="flex items-center gap-2 self-end pb-3 text-sm font-semibold text-slate-700 dark:text-slate-200">
      <input type="checkbox" checked={form.requirePostApproval} onChange={(event) => update('requirePostApproval', event.target.checked)} />
      Duyệt bài thành viên trước khi đăng
    </label>
    <div className="flex gap-2 sm:col-span-2">
      <button type="submit" disabled={isSaving || Boolean(uploadingField) || !categories.length} className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-blue-600 px-4 text-sm font-bold text-white disabled:opacity-50">
        {isSaving && <LoaderCircle size={15} className="animate-spin" />}
        {group ? 'Lưu thay đổi' : 'Tạo nhóm'}
      </button>
      <button type="button" onClick={onCancel} className="min-h-10 rounded-xl border border-slate-200 px-4 text-sm font-bold text-slate-600 dark:border-slate-700 dark:text-slate-300">Hủy</button>
    </div>
    {error && <p role="alert" className="text-sm text-rose-600 sm:col-span-2 dark:text-rose-300">{error}</p>}
  </form>
}

function GroupAvatar({ group }) {
  return group.avatarUrl
    ? <img src={group.avatarUrl} alt="" className="h-full w-full object-cover" />
    : <span aria-hidden="true" className="flex h-full w-full items-center justify-center bg-blue-100 text-blue-600 dark:bg-blue-950 dark:text-blue-300"><UsersRound size={25} /></span>
}

export default function GroupsScreen({ groupId, onOpenGroup, onBack, user, onAuth }) {
  const [groups, setGroups] = useState([])
  const [myGroups, setMyGroups] = useState([])
  const [categories, setCategories] = useState([])
  const [selectedGroup, setSelectedGroup] = useState(null)
  const [posts, setPosts] = useState([])
  const [pendingPosts, setPendingPosts] = useState([])
  const [joinRequests, setJoinRequests] = useState([])
  const [search, setSearch] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [isSavingPost, setIsSavingPost] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [editingGroup, setEditingGroup] = useState(null)
  const [editingPost, setEditingPost] = useState(null)

  const canModerate = ['OWNER', 'MODERATOR'].includes(selectedGroup?.viewerRole)
  const visibleGroups = useMemo(() => {
    const unique = new Map()
    for (const group of [...myGroups, ...groups]) unique.set(group.id, group)
    return [...unique.values()]
  }, [groups, myGroups])

  useEffect(() => {
    const controller = new AbortController()
    apiFetch('/categories', { signal: controller.signal })
      .then((response) => readResponse(response, 'Không thể tải danh mục hàng hóa.'))
      .then((result) => {
        if (!controller.signal.aborted) setCategories(Array.isArray(result) ? result : [])
      })
      .catch((requestError) => {
        if (requestError.name !== 'AbortError' && !controller.signal.aborted) {
          setError(requestError.message || 'Không thể tải danh mục hàng hóa.')
        }
      })
    return () => controller.abort()
  }, [])

  useEffect(() => {
    if (groupId) return undefined
    const controller = new AbortController()
    async function loadGroups() {
      setIsLoading(true)
      setError('')
      try {
        const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) })
        if (appliedSearch) params.set('search', appliedSearch)
        if (categoryId) params.set('categoryId', categoryId)
        const requests = [
          apiFetch(`/groups?${params}`, { signal: controller.signal }).then((response) => readResponse(response, 'Không thể tải danh sách nhóm.')),
        ]
        if (user) {
          requests.push(apiFetch(`/groups/mine?page=0&size=${PAGE_SIZE}`, { signal: controller.signal })
            .then((response) => readResponse(response, 'Không thể tải nhóm của bạn.')))
        }
        const [groupResult, mineResult] = await Promise.all(requests)
        setGroups(Array.isArray(groupResult.items) ? groupResult.items : [])
        setTotalPages(groupResult.totalPages || 0)
        setTotalElements(groupResult.totalElements || 0)
        setMyGroups(Array.isArray(mineResult?.items) ? mineResult.items : [])
      } catch (requestError) {
        if (requestError.name !== 'AbortError') setError(requestError.message || 'Không thể tải danh sách nhóm.')
      } finally {
        if (!controller.signal.aborted) setIsLoading(false)
      }
    }
    loadGroups()
    return () => controller.abort()
  }, [appliedSearch, categoryId, groupId, page, refreshKey, user])

  useEffect(() => {
    if (!groupId) return undefined
    const controller = new AbortController()
    async function loadGroup() {
      setIsLoading(true)
      setError('')
      setSelectedGroup(null)
      try {
        const response = await apiFetch(`/groups/${encodeURIComponent(groupId)}`, { signal: controller.signal })
        const group = await readResponse(response, 'Không thể tải thông tin nhóm.')
        setSelectedGroup(group)
        if (group.visibility === 'PRIVATE' && !group.member) {
          setPosts([])
          setPendingPosts([])
          setJoinRequests([])
          return
        }
        const postResponse = await apiFetch(`/groups/${encodeURIComponent(groupId)}/posts`, { signal: controller.signal })
        setPosts(await readResponse(postResponse, 'Không thể tải bài viết trong nhóm.'))
        if (['OWNER', 'MODERATOR'].includes(group.viewerRole)) {
          const [pendingResponse, requestResponse] = await Promise.all([
            apiFetch(`/groups/${encodeURIComponent(groupId)}/posts/pending`, { signal: controller.signal }),
            apiFetch(`/groups/${encodeURIComponent(groupId)}/join-requests`, { signal: controller.signal }),
          ])
          setPendingPosts(await readResponse(pendingResponse, 'Không thể tải bài cần duyệt.'))
          setJoinRequests(await readResponse(requestResponse, 'Không thể tải yêu cầu tham gia.'))
        } else {
          setPendingPosts([])
          setJoinRequests([])
        }
      } catch (requestError) {
        if (requestError.name !== 'AbortError') setError(requestError.message || 'Không thể tải thông tin nhóm.')
      } finally {
        if (!controller.signal.aborted) setIsLoading(false)
      }
    }
    loadGroup()
    return () => controller.abort()
  }, [groupId, refreshKey, user])

  function refresh() {
    setRefreshKey((current) => current + 1)
  }

  function submitSearch(event) {
    event.preventDefault()
    setPage(0)
    setAppliedSearch(search.trim())
  }

  async function saveGroup(form) {
    if (!user) {
      onAuth()
      return
    }
    setIsSaving(true)
    setError('')
    try {
      const editing = Boolean(editingGroup)
      const response = await apiFetch(editing ? `/groups/${editingGroup.id}` : '/groups', {
        method: editing ? 'PUT' : 'POST',
        body: JSON.stringify(form),
      })
      const saved = await readResponse(response, 'Không thể lưu nhóm.')
      setEditingGroup(null)
      setShowCreateForm(false)
      setNotice(editing ? 'Đã cập nhật nhóm.' : 'Đã tạo nhóm.')
      if (!editing) onOpenGroup(saved.id)
      else refresh()
    } catch (requestError) {
      setError(requestError.message || 'Không thể lưu nhóm.')
    } finally {
      setIsSaving(false)
    }
  }

  async function deleteGroup() {
    if (!selectedGroup || !window.confirm('Xóa nhóm và toàn bộ bài viết, yêu cầu tham gia của nhóm này?')) return
    try {
      await readResponse(await apiFetch(`/groups/${selectedGroup.id}`, { method: 'DELETE' }), 'Không thể xóa nhóm.')
      onBack()
    } catch (requestError) {
      setError(requestError.message || 'Không thể xóa nhóm.')
    }
  }

  async function updateMembership(join) {
    if (!user) {
      onAuth()
      return
    }
    try {
      const response = await apiFetch(`/groups/${groupId}/membership`, { method: join ? 'POST' : 'DELETE' })
      const result = await readResponse(response, 'Không thể cập nhật thành viên nhóm.')
      setNotice(join && result === 'PENDING' ? 'Yêu cầu tham gia đã được gửi.' : join ? 'Bạn đã tham gia nhóm.' : 'Bạn đã rời nhóm.')
      refresh()
    } catch (requestError) {
      setError(requestError.message || 'Không thể cập nhật thành viên nhóm.')
    }
  }

  async function decideJoin(requestId, approved) {
    try {
      await readResponse(await apiFetch(`/groups/${groupId}/join-requests/${requestId}`, {
        method: 'PUT',
        body: JSON.stringify({ approved }),
      }), 'Không thể xử lý yêu cầu tham gia.')
      refresh()
    } catch (requestError) {
      setError(requestError.message || 'Không thể xử lý yêu cầu tham gia.')
    }
  }

  async function savePost(payload) {
    setIsSavingPost(true)
    try {
      const editing = Boolean(editingPost)
      const endpoint = `/groups/${groupId}/posts${editing ? `/${editingPost.id}` : ''}`
      const response = await apiFetch(endpoint, {
        method: editing ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      })
      await readResponse(response, 'Không thể lưu bài viết.')
      setEditingPost(null)
      refresh()
    } finally {
      setIsSavingPost(false)
    }
  }

  async function deletePost(postId) {
    try {
      await readResponse(await apiFetch(`/groups/${groupId}/posts/${postId}`, { method: 'DELETE' }), 'Không thể xóa bài viết.')
      refresh()
    } catch (requestError) {
      setError(requestError.message || 'Không thể xóa bài viết.')
    }
  }

  async function reviewPost(postId, status) {
    try {
      await readResponse(await apiFetch(`/groups/${groupId}/posts/${postId}/moderation?status=${status}`, { method: 'PUT' }), 'Không thể duyệt bài viết.')
      refresh()
    } catch (requestError) {
      setError(requestError.message || 'Không thể duyệt bài viết.')
    }
  }

  const groupPostComposer = selectedGroup?.member && <PostComposer
    post={editingPost ? { copy: editingPost.content, image: editingPost.imageUrls?.[0] } : null}
    isSaving={isSavingPost}
    onSave={savePost}
    onCancel={() => setEditingPost(null)}
  />

  return <main className="min-h-screen px-4 pb-28 pt-7 sm:px-6 lg:ml-[248px] lg:px-10 lg:pb-10">
    <div className="mx-auto max-w-[1180px]">
      {groupId
        ? <button type="button" onClick={onBack} className="mb-5 inline-flex min-h-10 items-center gap-2 rounded-lg px-3 text-sm font-bold text-slate-500 hover:bg-white hover:text-blue-600 dark:text-slate-400 dark:hover:bg-slate-900"><ArrowLeft size={17} />Tất cả nhóm</button>
        : <header className="mb-7">
          <p className="text-xs font-bold uppercase tracking-[0.16em] text-blue-600 dark:text-blue-300">Otech · Cộng đồng</p>
          <h1 className="mt-2 text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white">Khám phá nhóm</h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500 dark:text-slate-400">Tìm cộng đồng theo danh mục hàng hóa, tham gia trao đổi và chia sẻ bài viết.</p>
        </header>}

      {notice && <p role="status" className="mb-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-300">{notice}</p>}
      {error && <div role="alert" className="mb-5 flex items-center justify-between gap-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:border-rose-900 dark:bg-rose-950/30 dark:text-rose-300"><span>{error}</span><button type="button" onClick={refresh} className="font-bold underline">Thử lại</button></div>}

      {!groupId && <>
        <div className="mb-4 flex justify-end">
          <button type="button" onClick={() => user ? setShowCreateForm((value) => !value) : onAuth()} className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-blue-600 px-4 text-sm font-bold text-white hover:bg-blue-700"><Plus size={16} />Tạo nhóm</button>
        </div>
        {showCreateForm && <GroupForm categories={categories} onSave={saveGroup} onCancel={() => setShowCreateForm(false)} isSaving={isSaving} />}
        <form onSubmit={submitSearch} className="mb-5 grid gap-3 rounded-2xl border border-slate-200 bg-white p-3 shadow-sm dark:border-slate-800 dark:bg-slate-900 sm:grid-cols-[1fr_220px_auto]">
          <label className="relative">
            <span className="sr-only">Tìm nhóm</span>
            <Search size={17} aria-hidden="true" className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Tìm tên nhóm hoặc chủ đề…" className="min-h-11 w-full rounded-xl border border-slate-200 bg-white pl-10 pr-3 text-sm dark:border-slate-700 dark:bg-slate-950" />
          </label>
          <label className="sr-only" htmlFor="group-category-filter">Lọc theo danh mục</label>
          <select id="group-category-filter" value={categoryId} onChange={(event) => { setCategoryId(event.target.value); setPage(0) }} className="min-h-11 rounded-xl border border-slate-200 bg-white px-3 text-sm dark:border-slate-700 dark:bg-slate-950">
            <option value="">Tất cả danh mục</option>
            {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
          </select>
          <button type="submit" className="min-h-11 rounded-xl bg-blue-600 px-5 text-sm font-bold text-white hover:bg-blue-700">Tìm nhóm</button>
        </form>
        {user && myGroups.length > 0 && <section className="mb-7">
          <h2 className="mb-3 text-lg font-extrabold text-slate-900 dark:text-white">Nhóm của bạn</h2>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{myGroups.map((group) => <GroupCard key={group.id} group={group} onOpen={onOpenGroup} />)}</div>
        </section>}
        <div className="mb-4 flex items-center justify-between gap-3">
          <div><h2 className="text-lg font-extrabold text-slate-900 dark:text-white">Nhóm đang hoạt động</h2><p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{totalElements} nhóm</p></div>
          {totalPages > 1 && <span className="text-xs text-slate-500 dark:text-slate-400">Trang {page + 1}/{totalPages}</span>}
        </div>
        {isLoading && <Loading />}
        {!isLoading && visibleGroups.length === 0 && <p className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center text-sm text-slate-500 dark:border-slate-700 dark:bg-slate-900">Chưa tìm thấy nhóm phù hợp.</p>}
        {!isLoading && groups.length > 0 && <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{groups.map((group) => <GroupCard key={group.id} group={group} onOpen={onOpenGroup} />)}</div>}
        {!isLoading && totalPages > 1 && <div className="mt-6 flex items-center justify-center gap-3">
          <button type="button" disabled={page === 0} onClick={() => setPage((value) => Math.max(0, value - 1))} className="min-h-10 rounded-xl border border-slate-200 bg-white px-4 text-sm font-bold disabled:opacity-40 dark:border-slate-700 dark:bg-slate-900">Trước</button>
          <button type="button" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)} className="min-h-10 rounded-xl border border-slate-200 bg-white px-4 text-sm font-bold disabled:opacity-40 dark:border-slate-700 dark:bg-slate-900">Sau</button>
        </div>}
      </>}

      {groupId && isLoading && <Loading />}
      {groupId && !isLoading && selectedGroup && <section className="space-y-5">
        <article className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="relative h-40 bg-gradient-to-br from-blue-600 via-sky-500 to-cyan-400 sm:h-56">
            {selectedGroup.coverUrl && <img src={selectedGroup.coverUrl} alt="" className="h-full w-full object-cover" />}
            <span className="absolute right-4 top-4 rounded-full border border-white/40 bg-white/90 px-3 py-1.5 text-xs font-bold text-blue-800">{selectedGroup.visibility === 'PRIVATE' ? 'Nhóm riêng tư' : 'Nhóm công khai'}</span>
          </div>
          <div className="px-5 pb-6 sm:px-8">
            <div className="-mt-10 flex h-20 w-20 overflow-hidden rounded-2xl border-4 border-white bg-white shadow-md dark:border-slate-900 dark:bg-slate-800"><GroupAvatar group={selectedGroup} /></div>
            <div className="mt-4 flex flex-wrap items-start justify-between gap-3">
              <div><h1 className="text-2xl font-extrabold tracking-tight text-slate-900 dark:text-white sm:text-3xl">{selectedGroup.name}</h1>
                <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">Quản trị bởi <span className="font-semibold text-slate-700 dark:text-slate-200">{selectedGroup.ownerName}</span></p>
              </div>
              <div className="flex flex-wrap gap-2">
                {!selectedGroup.member && <button type="button" disabled={selectedGroup.joinRequestStatus === 'PENDING'} onClick={() => updateMembership(true)} className="min-h-10 rounded-xl bg-blue-600 px-4 text-sm font-bold text-white disabled:opacity-50">{selectedGroup.joinRequestStatus === 'PENDING' ? 'Đã gửi yêu cầu' : selectedGroup.visibility === 'PRIVATE' ? 'Yêu cầu tham gia' : 'Tham gia nhóm'}</button>}
                {selectedGroup.member && selectedGroup.viewerRole !== 'OWNER' && <button type="button" onClick={() => updateMembership(false)} className="min-h-10 rounded-xl border border-slate-200 px-4 text-sm font-bold dark:border-slate-700">Rời nhóm</button>}
                {selectedGroup.viewerRole === 'OWNER' && <>
                  <button type="button" onClick={() => setEditingGroup(selectedGroup)} className="min-h-10 rounded-xl border border-slate-200 px-4 text-sm font-bold dark:border-slate-700">Chỉnh sửa</button>
                  <button type="button" onClick={deleteGroup} className="min-h-10 rounded-xl border border-rose-200 px-4 text-sm font-bold text-rose-700 dark:border-rose-900 dark:text-rose-300">Xóa nhóm</button>
                </>}
              </div>
            </div>
            <div className="mt-5 flex flex-wrap gap-x-6 gap-y-3 text-sm text-slate-500 dark:text-slate-400">
              <span className="inline-flex items-center gap-2"><UsersRound size={16} />{selectedGroup.memberCount} thành viên</span>
              <span>{selectedGroup.postCount} bài viết</span>
              <span>{selectedGroup.categoryName || 'Chưa phân loại'}</span>
              <span className="inline-flex items-center gap-2"><CalendarDays size={16} />Tạo {formatDate(selectedGroup.createdAt)}</span>
            </div>
            <div className="mt-5 border-t border-slate-100 pt-5 dark:border-slate-800">
              <h2 className="font-bold text-slate-900 dark:text-white">Giới thiệu</h2>
              <p className="mt-2 whitespace-pre-wrap text-sm leading-7 text-slate-600 dark:text-slate-300">{selectedGroup.description || 'Nhóm chưa có phần giới thiệu.'}</p>
            </div>
          </div>
        </article>

        {editingGroup && <GroupForm group={editingGroup} categories={categories} onSave={saveGroup} onCancel={() => setEditingGroup(null)} isSaving={isSaving} />}
        {selectedGroup.visibility === 'PRIVATE' && !selectedGroup.member
          ? <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-600 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300">Bài viết chỉ hiển thị với thành viên. Gửi yêu cầu để quản trị viên duyệt tham gia.</p>
          : <>
            {canModerate && joinRequests.length > 0 && <section className="rounded-2xl border border-amber-200 bg-amber-50 p-4 dark:border-amber-900 dark:bg-amber-950/20">
              <h2 className="font-bold text-slate-900 dark:text-white">Yêu cầu tham gia ({joinRequests.length})</h2>
              <div className="mt-3 space-y-2">{joinRequests.map((request) => <div key={request.id} className="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-white p-3 dark:bg-slate-900">
                <span className="text-sm font-semibold">{request.fullName}</span>
                <div className="flex gap-2"><button type="button" onClick={() => decideJoin(request.id, true)} className="rounded-lg bg-blue-600 px-3 py-2 text-xs font-bold text-white">Chấp nhận</button><button type="button" onClick={() => decideJoin(request.id, false)} className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-bold dark:border-slate-700">Từ chối</button></div>
              </div>)}</div>
            </section>}
            {canModerate && pendingPosts.length > 0 && <section className="rounded-2xl border border-amber-200 bg-amber-50 p-4 dark:border-amber-900 dark:bg-amber-950/20"><h2 className="font-bold text-slate-900 dark:text-white">Bài viết chờ duyệt ({pendingPosts.length})</h2></section>}
            {groupPostComposer}
            <div className="space-y-4">{posts.map((post) => <article key={post.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <div className="flex items-start justify-between gap-3">
                <div><p className="font-bold text-slate-900 dark:text-white">{post.authorName}</p><p className="mt-1 text-xs text-slate-500">{formatDate(post.createdAt)}</p></div>
                <div className="flex flex-wrap justify-end gap-2">
                  {post.status === 'PENDING' && <span className="rounded-full bg-amber-100 px-2.5 py-1 text-xs font-bold text-amber-800">Chờ duyệt</span>}
                  {post.status === 'REJECTED' && <span className="rounded-full bg-rose-100 px-2.5 py-1 text-xs font-bold text-rose-800">Đã từ chối</span>}
                  {(canModerate || post.authorId === user?.id) && <button type="button" onClick={() => setEditingPost(post)} className="text-xs font-bold text-blue-600">Sửa</button>}
                  {(canModerate || post.authorId === user?.id) && <button type="button" onClick={() => deletePost(post.id)} className="text-xs font-bold text-rose-600">Xóa</button>}
                  {canModerate && post.status === 'PENDING' && <>
                    <button type="button" onClick={() => reviewPost(post.id, 'APPROVED')} className="text-xs font-bold text-emerald-700">Duyệt</button>
                    <button type="button" onClick={() => reviewPost(post.id, 'REJECTED')} className="text-xs font-bold text-rose-700">Từ chối</button>
                  </>}
                </div>
              </div>
              <p className="mt-4 whitespace-pre-wrap text-sm leading-6 text-slate-700 dark:text-slate-200">{post.content}</p>
              {post.imageUrls?.map((image) => <img key={image} src={image} alt="" className="mt-3 max-h-[480px] w-full rounded-xl object-cover" />)}
            </article>)}</div>
            {!posts.length && !groupPostComposer && <p className="rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center text-sm text-slate-500 dark:border-slate-700 dark:bg-slate-900">Chưa có bài viết nào trong nhóm.</p>}
          </>}
      </section>}
    </div>
  </main>
}

function GroupCard({ group, onOpen }) {
  return <article className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-md dark:border-slate-800 dark:bg-slate-900">
    <div className="h-24 bg-gradient-to-br from-blue-600 via-sky-500 to-cyan-400">{group.coverUrl && <img src={group.coverUrl} alt="" className="h-full w-full object-cover" />}</div>
    <div className="px-4 pb-4">
      <div className="-mt-8 h-16 w-16 overflow-hidden rounded-xl border-4 border-white bg-white shadow-sm dark:border-slate-900 dark:bg-slate-800"><GroupAvatar group={group} /></div>
      <h3 className="mt-3 line-clamp-1 text-base font-extrabold text-slate-900 dark:text-white">{group.name}</h3>
      <p className="mt-1 text-xs font-semibold text-blue-600 dark:text-blue-300">{group.categoryName || 'Chưa phân loại'} · {group.visibility === 'PRIVATE' ? 'Riêng tư' : 'Công khai'}</p>
      <p className="mt-2 line-clamp-2 min-h-10 text-sm leading-5 text-slate-500 dark:text-slate-400">{group.description || 'Chưa có giới thiệu.'}</p>
      <div className="mt-3 flex items-center justify-between border-t border-slate-100 pt-3 text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400">
        <span>{group.memberCount} thành viên · {group.postCount} bài</span>
        <button type="button" onClick={() => onOpen(group.id)} className="font-bold text-blue-600 hover:underline dark:text-blue-300">Xem nhóm</button>
      </div>
    </div>
  </article>
}

function Loading() {
  return <div className="flex min-h-56 items-center justify-center gap-2 text-sm text-slate-500"><LoaderCircle size={18} className="animate-spin text-blue-600" />Đang tải nhóm…</div>
}
