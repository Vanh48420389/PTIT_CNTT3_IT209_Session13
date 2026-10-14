import { useCallback, useEffect, useState } from 'react'
import { api, clearCredentials, setCredentials } from './api.js'

const emptyPost = {
  title: '',
  content: '',
  categoryId: '',
  isRecruiting: false,
  targetMembers: 2,
  skillsRequired: '',
  contactInfo: ''
}

function formatDate(value) {
  if (!value) return ''
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function RecruitmentStatusBadge({ status }) {
  if (status === 'OPEN') return <span className="status-badge open">● Đang mở tuyển</span>
  if (status === 'FULL') return <span className="status-badge full">✓ Đã đủ người</span>
  if (status === 'CLOSED') return <span className="status-badge closed">✕ Đã đóng tuyển</span>
  return null
}

function ApplicationStatusBadge({ status }) {
  if (status === 'PENDING') return <span className="status-badge pending">⏳ Chờ duyệt</span>
  if (status === 'ACCEPTED') return <span className="status-badge accepted">🎉 Đã được nhận</span>
  if (status === 'REJECTED') return <span className="status-badge rejected">✕ Đã từ chối</span>
  if (status === 'CANCELLED') return <span className="status-badge closed">Đã hủy đơn</span>
  return <span className="status-badge">{status}</span>
}

function App() {
  const [user, setUser] = useState(null)
  const [categories, setCategories] = useState([])
  const [posts, setPosts] = useState([])
  const [selectedPost, setSelectedPost] = useState(null)
  const [filters, setFilters] = useState({ keyword: '', categoryId: '', onlyRecruiting: false })
  const [dialog, setDialog] = useState(null) // 'login' | 'post' | 'myApps'
  const [notice, setNotice] = useState('')

  const loadPosts = useCallback(async () => {
    const query = new URLSearchParams()
    if (filters.keyword) query.set('keyword', filters.keyword)
    if (filters.categoryId) query.set('categoryId', filters.categoryId)
    try {
      const page = await api(`/posts?${query}`)
      let content = page.content || []
      if (filters.onlyRecruiting) {
        content = content.filter((p) => p.isRecruiting)
      }
      setPosts(content)
    } catch (error) {
      setNotice(error.message)
    }
  }, [filters])

  useEffect(() => {
    api('/categories').then(setCategories).catch((error) => setNotice(error.message))
    api('/auth/me').then(setUser).catch(() => clearCredentials())
  }, [])

  useEffect(() => {
    loadPosts()
  }, [loadPosts])

  async function openPost(id) {
    try {
      setSelectedPost(await api(`/posts/${id}`))
    } catch (error) {
      setNotice(error.message)
    }
  }

  function logout() {
    clearCredentials()
    setUser(null)
    setSelectedPost(null)
    setNotice('Đã đăng xuất')
  }

  async function refreshSelected() {
    if (selectedPost) await openPost(selectedPost.id)
    await loadPosts()
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <button className="brand" onClick={() => setSelectedPost(null)}>
          <span className="brand-mark">C</span>
          <span>Campus Hub & TeamMatch</span>
        </button>
        <nav>
          {user ? (
            <>
              <span className="user-name">Xin chào, {user.displayName}</span>
              <button className="button ghost sm" onClick={() => setDialog('myApps')}>Đơn ứng tuyển</button>
              <button className="button ghost sm" onClick={() => setDialog('post')}>+ Đăng bài</button>
              <button className="button secondary sm" onClick={logout}>Đăng xuất</button>
            </>
          ) : (
            <button className="button primary sm" onClick={() => setDialog('login')}>Đăng nhập</button>
          )}
        </nav>
      </header>

      <main>
        {notice && <div className="notice" onClick={() => setNotice('')}>{notice}</div>}
        {selectedPost ? (
          <PostDetail
            post={selectedPost}
            user={user}
            onBack={() => setSelectedPost(null)}
            onChanged={refreshSelected}
            onDeleted={() => { setSelectedPost(null); loadPosts() }}
            setNotice={setNotice}
          />
        ) : (
          <>
            <section className="hero">
              <div>
                <p className="eyebrow">Không gian kết nối & ghép đội sinh viên</p>
                <h1>Tìm bạn cùng làm đồ án, thi Hackathon & NCKH.</h1>
                <p>Chia sẻ thông tin, tìm đồ thất lạc, trao đổi học tập và dễ dàng tìm kiếm đồng đội phù hợp với kỹ năng bạn mong muốn.</p>
              </div>
              <div className="hero-stat">
                <strong>{posts.length}</strong>
                <span>bài đăng hiển thị</span>
              </div>
            </section>

            <section className="filters" aria-label="Tìm kiếm bài đăng">
              <input
                value={filters.keyword}
                placeholder="Tìm theo tiêu đề hoặc nội dung..."
                onChange={(event) => setFilters({ ...filters, keyword: event.target.value })}
              />
              <select
                value={filters.categoryId}
                onChange={(event) => setFilters({ ...filters, categoryId: event.target.value })}
              >
                <option value="">Tất cả chuyên mục</option>
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>{category.name}</option>
                ))}
              </select>
              <select
                value={filters.onlyRecruiting ? 'recruiting' : 'all'}
                onChange={(event) => setFilters({ ...filters, onlyRecruiting: event.target.value === 'recruiting' })}
              >
                <option value="all">Tất cả loại bài</option>
                <option value="recruiting">🚀 Chỉ tuyển nhóm</option>
              </select>
            </section>

            <section className="post-grid">
              {posts.map((post) => (
                <PostCard key={post.id} post={post} onOpen={openPost} />
              ))}
              {!posts.length && <div className="empty">Chưa có bài đăng nào phù hợp với bộ lọc.</div>}
            </section>
          </>
        )}
      </main>

      <footer>Campus Hub · Mini Project Session 13 (Ghép đội học tập & Tuyển thành viên NCKH)</footer>

      {dialog === 'login' && (
        <AuthDialog
          onClose={() => setDialog(null)}
          onLogin={(loggedInUser) => {
            setUser(loggedInUser)
            setDialog(null)
            setNotice('Đăng nhập thành công')
            loadPosts()
          }}
        />
      )}

      {dialog === 'post' && (
        <PostDialog
          categories={categories}
          onClose={() => setDialog(null)}
          onSaved={() => {
            setDialog(null)
            setNotice('Đã đăng bài thành công')
            loadPosts()
          }}
        />
      )}

      {dialog === 'myApps' && (
        <MyApplicationsDialog
          onClose={() => setDialog(null)}
          onOpenPost={(id) => {
            setDialog(null)
            openPost(id)
          }}
          setNotice={setNotice}
        />
      )}
    </div>
  )
}

function PostCard({ post, onOpen }) {
  const skills = post.skillsRequired ? post.skillsRequired.split(',').map((s) => s.trim()).filter(Boolean) : []

  return (
    <article className="post-card" onClick={() => onOpen(post.id)}>
      <div className="card-top">
        <span className="tag">{post.category.name}</span>
        <span>{formatDate(post.createdAt)}</span>
      </div>

      <h2>{post.title}</h2>
      <p>{post.content}</p>

      {post.isRecruiting && (
        <div style={{ marginBottom: 12 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
            <RecruitmentStatusBadge status={post.recruitmentStatus} />
            <span style={{ fontSize: 12, color: 'var(--muted)', fontWeight: 600 }}>
              {post.acceptedMembersCount} / {post.targetMembers || '?'} thành viên
            </span>
          </div>
          {skills.length > 0 && (
            <div className="skill-tags">
              {skills.slice(0, 3).map((skill, index) => (
                <span className="skill-tag" key={index}>{skill}</span>
              ))}
              {skills.length > 3 && <span className="skill-tag">+{skills.length - 3}</span>}
            </div>
          )}
        </div>
      )}

      <div className="card-footer">
        <span>{post.author.displayName}</span>
        <span>♥ {post.reactionCount} · {post.commentCount} bình luận</span>
      </div>
    </article>
  )
}

function PostDetail({ post, user, onBack, onChanged, onDeleted, setNotice }) {
  const [comment, setComment] = useState('')
  const [applyModalOpen, setApplyModalOpen] = useState(false)
  const canManage = user && (user.id === post.author.id || user.role === 'ADMIN')
  const isAuthor = user && user.id === post.author.id
  const skills = post.skillsRequired ? post.skillsRequired.split(',').map((s) => s.trim()).filter(Boolean) : []

  async function act(action, success) {
    try {
      await action()
      if (success) setNotice(success)
      await onChanged()
    } catch (error) {
      setNotice(error.message)
    }
  }

  const accepted = post.acceptedMembersCount || 0
  const target = post.targetMembers || 1
  const percent = Math.min(100, Math.round((accepted / target) * 100))

  return (
    <section className="detail">
      <button className="back" onClick={onBack}>← Quay lại danh sách bài đăng</button>

      <div className="detail-card">
        <div className="card-top">
          <span className="tag">{post.category.name}</span>
          <span>{formatDate(post.createdAt)}</span>
        </div>

        <h1>{post.title}</h1>
        <p className="author">Đăng bởi <strong>{post.author.displayName}</strong> · Trạng thái bài: {post.status}</p>

        {/* TEAM RECRUITMENT WIDGET */}
        {post.isRecruiting && (
          <div className="recruitment-widget">
            <div className="recruitment-header">
              <h3>
                <span>🎯 Tuyển thành viên nhóm</span>
                <RecruitmentStatusBadge status={post.recruitmentStatus} />
              </h3>

              {isAuthor && (
                <div style={{ display: 'flex', gap: 6 }}>
                  {post.recruitmentStatus !== 'OPEN' && (
                    <button
                      className="button secondary sm"
                      onClick={() => act(() => api(`/posts/${post.id}/recruitment-status`, {
                        method: 'PATCH',
                        body: JSON.stringify({ recruitmentStatus: 'OPEN' })
                      }), 'Đã mở lại tuyển thành viên')}
                    >
                      Mở lại tuyển
                    </button>
                  )}
                  {post.recruitmentStatus === 'OPEN' && (
                    <button
                      className="button secondary sm"
                      onClick={() => act(() => api(`/posts/${post.id}/recruitment-status`, {
                        method: 'PATCH',
                        body: JSON.stringify({ recruitmentStatus: 'CLOSED' })
                      }), 'Đã tạm đóng tuyển thành viên')}
                    >
                      Đóng tuyển
                    </button>
                  )}
                </div>
              )}
            </div>

            <div className="progress-container">
              <div className="progress-label">
                <span>Tiến độ tuyển thành viên:</span>
                <strong>{accepted} / {target} bạn ({percent}%)</strong>
              </div>
              <div className="progress-bar">
                <div
                  className={`progress-fill ${accepted >= target ? 'full' : ''}`}
                  style={{ width: `${percent}%` }}
                />
              </div>
            </div>

            <div className="recruitment-meta">
              {skills.length > 0 && (
                <div>
                  <strong>Kỹ năng mong muốn: </strong>
                  <div className="skill-tags" style={{ display: 'inline-flex', marginLeft: 6 }}>
                    {skills.map((skill, index) => (
                      <span className="skill-tag" key={index}>{skill}</span>
                    ))}
                  </div>
                </div>
              )}
              {post.contactInfo && (
                <div>
                  <strong>Thông tin liên hệ: </strong>
                  <span>{post.contactInfo}</span>
                </div>
              )}
            </div>

            {/* Applicant Flow */}
            {user && !isAuthor && (
              <div>
                {post.myApplication ? (
                  <div className="my-application-box">
                    <h4>
                      <span>Trạng thái đơn của bạn</span>
                      <ApplicationStatusBadge status={post.myApplication.status} />
                    </h4>
                    <p style={{ margin: '4px 0', fontSize: 13, color: '#334155' }}>
                      <strong>Lời nhắn:</strong> {post.myApplication.message}
                    </p>
                    <p style={{ margin: '4px 0', fontSize: 13, color: '#334155' }}>
                      <strong>Liên hệ:</strong> {post.myApplication.contactInfo}
                    </p>
                    {post.myApplication.status === 'PENDING' && (
                      <button
                        className="button danger sm"
                        style={{ marginTop: 8 }}
                        onClick={() => act(() => api(`/posts/${post.id}/apply`, { method: 'DELETE' }), 'Đã hủy đơn ứng tuyển')}
                      >
                        Hủy đơn ứng tuyển
                      </button>
                    )}
                  </div>
                ) : (
                  <div>
                    {post.recruitmentStatus === 'OPEN' ? (
                      <button className="button primary" onClick={() => setApplyModalOpen(true)}>
                        🚀 Nộp đơn ứng tuyển vào nhóm
                      </button>
                    ) : (
                      <p className="muted" style={{ margin: 0 }}>
                        Nhóm hiện đã nhận đủ thành viên hoặc tạm đóng nhận đơn.
                      </p>
                    )}
                  </div>
                )}
              </div>
            )}

            {!user && (
              <p className="muted" style={{ margin: 0 }}>
                💡 Vui lòng đăng nhập để ứng tuyển vào nhóm này.
              </p>
            )}
          </div>
        )}

        <div className="content">{post.content}</div>

        <div className="actions">
          <button
            className={`button ${post.reactedByCurrentUser ? 'primary' : 'secondary'}`}
            disabled={!user}
            onClick={() => act(() => api(`/posts/${post.id}/reaction`, { method: 'POST' }), 'Đã cập nhật quan tâm')}
          >
            ♥ {post.reactionCount} quan tâm
          </button>

          {canManage && post.status !== 'RESOLVED' && (
            <button
              className="button secondary"
              onClick={() => act(() => api(`/posts/${post.id}/status`, {
                method: 'PATCH',
                body: JSON.stringify({ status: 'RESOLVED' })
              }), 'Đã đánh dấu hoàn tất')}
            >
              Đánh dấu hoàn tất
            </button>
          )}

          {user?.role === 'ADMIN' && post.status !== 'HIDDEN' && (
            <button
              className="button secondary"
              onClick={() => act(() => api(`/posts/${post.id}/status`, {
                method: 'PATCH',
                body: JSON.stringify({ status: 'HIDDEN' })
              }), 'Đã ẩn bài đăng')}
            >
              Ẩn bài
            </button>
          )}

          {canManage && (
            <button
              className="button danger"
              onClick={async () => {
                if (!window.confirm('Xác nhận xóa bài đăng này?')) return
                try {
                  await api(`/posts/${post.id}`, { method: 'DELETE' })
                  setNotice('Đã xóa bài đăng')
                  onDeleted()
                } catch (error) {
                  setNotice(error.message)
                }
              }}
            >
              Xóa bài
            </button>
          )}
        </div>

        {/* AUTHOR APPLICANT MANAGEMENT */}
        {canManage && post.isRecruiting && (
          <section className="applicants-section">
            <h3>
              <span>Danh sách ứng viên ({post.applications?.length || 0})</span>
            </h3>

            {(!post.applications || post.applications.length === 0) ? (
              <p className="muted">Chưa có ứng viên nào gửi đơn tham gia nhóm.</p>
            ) : (
              <div>
                {post.applications.map((app) => (
                  <div className="applicant-card" key={app.id}>
                    <div className="applicant-header">
                      <div>
                        <span className="applicant-name">{app.applicant.displayName} (@{app.applicant.username})</span>
                        <div className="applicant-contact">📞 {app.contactInfo} · {formatDate(app.createdAt)}</div>
                      </div>
                      <ApplicationStatusBadge status={app.status} />
                    </div>

                    <p className="applicant-msg">"{app.message}"</p>

                    {app.status === 'PENDING' && (
                      <div className="applicant-actions">
                        <button
                          className="button danger sm"
                          onClick={() => act(() => api(`/posts/${post.id}/applications/${app.id}/status`, {
                            method: 'PATCH',
                            body: JSON.stringify({ status: 'REJECTED' })
                          }), 'Đã từ chối ứng viên')}
                        >
                          ✕ Từ chối
                        </button>
                        <button
                          className="button primary sm"
                          onClick={() => act(() => api(`/posts/${post.id}/applications/${app.id}/status`, {
                            method: 'PATCH',
                            body: JSON.stringify({ status: 'ACCEPTED' })
                          }), 'Đã duyệt nhận ứng viên vào nhóm!')}
                        >
                          ✓ Chấp nhận vào nhóm
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </section>
        )}
      </div>

      {/* COMMENTS SECTION */}
      <div className="comments">
        <h2>Bình luận ({post.commentCount})</h2>
        {user ? (
          <form
            onSubmit={(event) => {
              event.preventDefault()
              act(
                () => api(`/posts/${post.id}/comments`, { method: 'POST', body: JSON.stringify({ content: comment }) }),
                'Đã thêm bình luận'
              ).then(() => setComment(''))
            }}
          >
            <textarea
              value={comment}
              onChange={(event) => setComment(event.target.value)}
              placeholder="Viết bình luận hoặc trao đổi thông tin..."
              required
              maxLength={1000}
            />
            <button className="button primary">Gửi bình luận</button>
          </form>
        ) : (
          <p className="muted">Đăng nhập để bình luận hoặc bày tỏ quan tâm.</p>
        )}

        <div className="comment-list">
          {post.comments.map((item) => (
            <div className="comment" key={item.id}>
              <strong>{item.author.displayName}</strong>
              <span>{formatDate(item.createdAt)}</span>
              <p>{item.content}</p>
            </div>
          ))}
        </div>
      </div>

      {/* APPLY MODAL */}
      {applyModalOpen && (
        <ApplyDialog
          postId={post.id}
          onClose={() => setApplyModalOpen(false)}
          onSuccess={() => {
            setApplyModalOpen(false)
            setNotice('Gửi đơn ứng tuyển thành công!')
            onChanged()
          }}
        />
      )}
    </section>
  )
}

function ApplyDialog({ postId, onClose, onSuccess }) {
  const [form, setForm] = useState({ message: '', contactInfo: '' })
  const [error, setError] = useState('')

  async function submit(event) {
    event.preventDefault()
    try {
      await api(`/posts/${postId}/apply`, {
        method: 'POST',
        body: JSON.stringify(form)
      })
      onSuccess()
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  return (
    <Dialog title="Ứng tuyển vào nhóm" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        {error && <p className="form-error">{error}</p>}
        <label>
          Thông tin liên hệ (SĐT / Zalo / Email / Telegram)
          <input
            required
            maxLength={120}
            placeholder="Ví dụ: Zalo 0912345678, email nam@ptit.edu.vn"
            value={form.contactInfo}
            onChange={(event) => setForm({ ...form, contactInfo: event.target.value })}
          />
        </label>
        <label>
          Lời nhắn giới thiệu bản thân & kỹ năng
          <textarea
            required
            rows={4}
            maxLength={500}
            placeholder="Giới thiệu về kỹ năng, kinh nghiệm và mong muốn đóng góp cho dự án của nhóm..."
            value={form.message}
            onChange={(event) => setForm({ ...form, message: event.target.value })}
          />
        </label>
        <button className="button primary">🚀 Gửi đơn ứng tuyển</button>
      </form>
    </Dialog>
  )
}

function MyApplicationsDialog({ onClose, onOpenPost, setNotice }) {
  const [apps, setApps] = useState([])
  const [loading, setLoading] = useState(true)

  const fetchApps = useCallback(async () => {
    try {
      const data = await api('/me/applications')
      setApps(data || [])
    } catch (error) {
      setNotice(error.message)
    } finally {
      setLoading(false)
    }
  }, [setNotice])

  useEffect(() => {
    fetchApps()
  }, [fetchApps])

  async function cancelApp(postId) {
    if (!window.confirm('Hủy đơn ứng tuyển này?')) return
    try {
      await api(`/posts/${postId}/apply`, { method: 'DELETE' })
      setNotice('Đã rút đơn ứng tuyển')
      await fetchApps()
    } catch (err) {
      setNotice(err.message)
    }
  }

  return (
    <Dialog title="Đơn ứng tuyển của tôi" onClose={onClose}>
      {loading ? (
        <p className="muted">Đang tải...</p>
      ) : apps.length === 0 ? (
        <p className="muted">Bạn chưa gửi đơn ứng tuyển vào nhóm nào.</p>
      ) : (
        <div>
          {apps.map((item) => (
            <div className="my-apps-item" key={item.id}>
              <div className="my-apps-item-top">
                <h4 onClick={() => onOpenPost(item.postId)}>📌 {item.postTitle}</h4>
                <ApplicationStatusBadge status={item.status} />
              </div>
              <p style={{ margin: '4px 0', fontSize: 13, color: '#475569' }}>"{item.message}"</p>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 12, color: 'var(--muted)' }}>
                <span>Liên hệ: {item.contactInfo}</span>
                {item.status === 'PENDING' && (
                  <button className="button danger sm" onClick={() => cancelApp(item.postId)}>Hủy đơn</button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </Dialog>
  )
}

function AuthDialog({ onClose, onLogin }) {
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState({ username: '', displayName: '', password: '' })
  const [error, setError] = useState('')

  async function submit(event) {
    event.preventDefault()
    try {
      if (mode === 'register') {
        await api('/auth/register', { method: 'POST', body: JSON.stringify(form) })
      }
      setCredentials(form.username, form.password)
      onLogin(await api('/auth/me'))
    } catch (requestError) {
      clearCredentials()
      setError(requestError.message)
    }
  }

  return (
    <Dialog title={mode === 'login' ? 'Đăng nhập Campus Hub' : 'Tạo tài khoản sinh viên'} onClose={onClose}>
      <form className="form" onSubmit={submit}>
        {error && <p className="form-error">{error}</p>}
        <label>
          Tên đăng nhập
          <input
            required
            minLength={3}
            value={form.username}
            onChange={(event) => setForm({ ...form, username: event.target.value })}
          />
        </label>
        {mode === 'register' && (
          <label>
            Họ và tên / Tên hiển thị
            <input
              required
              value={form.displayName}
              onChange={(event) => setForm({ ...form, displayName: event.target.value })}
            />
          </label>
        )}
        <label>
          Mật khẩu
          <input
            required
            minLength={6}
            type="password"
            value={form.password}
            onChange={(event) => setForm({ ...form, password: event.target.value })}
          />
        </label>
        <button className="button primary">{mode === 'login' ? 'Đăng nhập' : 'Đăng ký tài khoản'}</button>
        <button
          type="button"
          className="text-button"
          onClick={() => {
            setMode(mode === 'login' ? 'register' : 'login')
            setError('')
          }}
        >
          {mode === 'login' ? 'Chưa có tài khoản? Đăng ký ngay' : 'Đã có tài khoản? Đăng nhập'}
        </button>
      </form>
    </Dialog>
  )
}

function PostDialog({ categories, onClose, onSaved }) {
  const [form, setForm] = useState(emptyPost)
  const [error, setError] = useState('')

  async function submit(event) {
    event.preventDefault()
    try {
      await api('/posts', {
        method: 'POST',
        body: JSON.stringify({
          ...form,
          categoryId: Number(form.categoryId),
          targetMembers: form.isRecruiting ? Number(form.targetMembers) : null
        })
      })
      onSaved()
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  return (
    <Dialog title="Tạo bài đăng mới" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        {error && <p className="form-error">{error}</p>}
        <label>
          Tiêu đề
          <input
            required
            maxLength={160}
            placeholder="Ví dụ: Tìm 2 bạn làm đồ án tốt nghiệp React & Spring Boot"
            value={form.title}
            onChange={(event) => setForm({ ...form, title: event.target.value })}
          />
        </label>
        <label>
          Chuyên mục
          <select
            required
            value={form.categoryId}
            onChange={(event) => {
              const val = event.target.value
              const isTeamCategory = categories.find((c) => String(c.id) === val)?.slug === 'tim-nhom'
              setForm({
                ...form,
                categoryId: val,
                isRecruiting: isTeamCategory ? true : form.isRecruiting
              })
            }}
          >
            <option value="">Chọn chuyên mục</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>{category.name}</option>
            ))}
          </select>
        </label>

        {/* Checkbox tuyển nhóm */}
        <div className="form-checkbox">
          <input
            type="checkbox"
            id="isRecruitingCheck"
            checked={form.isRecruiting}
            onChange={(e) => setForm({ ...form, isRecruiting: e.target.checked })}
          />
          <label htmlFor="isRecruitingCheck" style={{ cursor: 'pointer', margin: 0, fontWeight: 600 }}>
            🚀 Đây là bài đăng tuyển thành viên nhóm / NCKH / Cuộc thi
          </label>
        </div>

        {form.isRecruiting && (
          <div style={{ display: 'grid', gap: 12, padding: 14, background: '#f8fafc', borderRadius: 12, border: '1px solid #e2e8f0' }}>
            <label>
              Số lượng thành viên cần tuyển
              <input
                type="number"
                min={1}
                max={20}
                required={form.isRecruiting}
                value={form.targetMembers}
                onChange={(e) => setForm({ ...form, targetMembers: e.target.value })}
              />
            </label>
            <label>
              Kỹ năng yêu cầu (ngăn cách bằng dấu phẩy)
              <input
                maxLength={250}
                placeholder="Ví dụ: React, Spring Boot, Docker, UI/UX"
                value={form.skillsRequired}
                onChange={(e) => setForm({ ...form, skillsRequired: e.target.value })}
              />
            </label>
            <label>
              Thông tin liên hệ nhóm
              <input
                maxLength={250}
                placeholder="Ví dụ: Zalo: 0987654321, email: leader@ptit.edu.vn"
                value={form.contactInfo}
                onChange={(e) => setForm({ ...form, contactInfo: e.target.value })}
              />
            </label>
          </div>
        )}

        <label>
          Nội dung chi tiết
          <textarea
            required
            maxLength={10000}
            rows={6}
            placeholder="Mô tả chi tiết mục tiêu đề tài, yêu cầu, thời gian biểu và quyền lợi..."
            value={form.content}
            onChange={(event) => setForm({ ...form, content: event.target.value })}
          />
        </label>
        <button className="button primary">Đăng bài ngay</button>
      </form>
    </Dialog>
  )
}

function Dialog({ title, onClose, children }) {
  return (
    <div className="dialog-backdrop" onMouseDown={onClose}>
      <section className="dialog" role="dialog" aria-modal="true" onMouseDown={(event) => event.stopPropagation()}>
        <div className="dialog-title">
          <h2>{title}</h2>
          <button onClick={onClose} aria-label="Đóng">×</button>
        </div>
        {children}
      </section>
    </div>
  )
}

export default App
