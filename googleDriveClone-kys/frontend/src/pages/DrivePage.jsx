// DrivePage.jsx - 메인 드라이브 화면
// 사이드바 + 파일 목록으로 구성된 레이아웃입니다
import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useFiles } from '../hooks/useFiles'
import Sidebar from '../components/Sidebar'
import FileList from '../components/FileList'
import UploadButton from '../components/UploadButton'
import SearchBar from '../components/SearchBar'
import api from '../api/axios'

export default function DrivePage() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()

  // useSearchParams: URL의 ?section=... ?parentId=... 값을 읽고 씁니다
  const [searchParams, setSearchParams] = useSearchParams()
  const section = searchParams.get('section') || 'drive'
  const parentId = searchParams.get('parentId') ? Number(searchParams.get('parentId')) : null

  // 폴더 경로 표시용 breadcrumb 상태
  // URL의 path 파라미터(JSON)에서 읽어옵니다 → 새로고침/뒤로가기에도 유지
  const [breadcrumb, setBreadcrumb] = useState(() => {
    const pathParam = searchParams.get('path')
    if (!pathParam) return []
    try {
      const parsed = JSON.parse(pathParam)
      if (Array.isArray(parsed)) return parsed
    } catch { /* ignore */ }
    return []
  })

  // URL이 변경되면 breadcrumb도 URL 기준으로 동기화합니다 (뒤로/앞으로 버튼 대응)
  useEffect(() => {
    const pathParam = searchParams.get('path')
    if (!pathParam) {
      setBreadcrumb([])
      return
    }
    try {
      const parsed = JSON.parse(pathParam)
      if (Array.isArray(parsed)) setBreadcrumb(parsed)
    } catch { /* ignore */ }
  }, [searchParams])

  // useFiles 훅으로 파일 목록을 가져옵니다
  const { files, loading, refetch } = useFiles(section, parentId)

  // 드래그 앤 드롭 업로드 상태
  const [isDragging, setIsDragging] = useState(false)
  const [dragUploading, setDragUploading] = useState(false)

  // 드롭된 파일들을 순차적으로 업로드합니다
  const handleDropUpload = async (fileList) => {
    if (!fileList || fileList.length === 0) return
    setDragUploading(true)
    try {
      // 여러 파일을 순차적으로 업로드 (Promise.all 쓰면 병렬 업로드도 가능)
      for (const file of Array.from(fileList)) {
        const formData = new FormData()
        formData.append('file', file)
        if (parentId) formData.append('parentId', parentId)
        await api.post('/api/files/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
      }
      refetch()
    } catch (err) {
      alert('업로드 실패: ' + (err.response?.data?.message || err.message))
    } finally {
      setDragUploading(false)
    }
  }

  // 드래그 이벤트 핸들러 - 휴지통/공유 문서함에서는 무시합니다
  const canDrop = section !== 'trash' && section !== 'shared'
  const handleDragOver = (e) => {
    if (!canDrop) return
    e.preventDefault()
    setIsDragging(true)
  }
  const handleDragLeave = (e) => {
    if (!canDrop) return
    // 실제로 영역을 벗어났을 때만 false로 (자식 요소 간 이동은 무시)
    if (e.currentTarget.contains(e.relatedTarget)) return
    setIsDragging(false)
  }
  const handleDrop = (e) => {
    if (!canDrop) return
    e.preventDefault()
    setIsDragging(false)
    handleDropUpload(e.dataTransfer.files)
  }

  // 섹션 변경 (사이드바 메뉴 클릭)
  const handleSectionChange = (newSection) => {
    if (newSection === 'drive') {
      setSearchParams({})
    } else {
      setSearchParams({ section: newSection })
    }
  }

  // 폴더 클릭 시 해당 폴더 안으로 이동
  // breadcrumb를 URL의 path 파라미터에 저장합니다
  const handleFolderClick = (folderId, folderName) => {
    const next = [...breadcrumb, { id: folderId, name: folderName }]
    setSearchParams({ parentId: String(folderId), path: JSON.stringify(next) })
  }

  // breadcrumb 클릭으로 상위 폴더로 이동
  const handleBreadcrumbClick = (index) => {
    if (index < 0) {
      setSearchParams({})
    } else {
      const next = breadcrumb.slice(0, index + 1)
      setSearchParams({
        parentId: String(next[next.length - 1].id),
        path: JSON.stringify(next),
      })
    }
  }

  // 폴더 만들기
  const handleCreateFolder = async () => {
    const name = prompt('폴더 이름을 입력하세요')
    if (!name?.trim()) return
    try {
      await api.post('/api/files/folder', {
        name: name.trim(),
        parentId: parentId || null,
      })
      refetch()
    } catch (err) {
      alert('폴더 생성에 실패했습니다: ' + (err.response?.data?.message || err.message))
    }
  }

  // 로그아웃
  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  // 현재 섹션 제목
  const sectionTitle = {
    drive: parentId ? breadcrumb[breadcrumb.length - 1]?.name || '내 드라이브' : '내 드라이브',
    recent: '최근 문서함',
    starred: '중요 문서함',
    shared: '공유 문서함',
    trash: '휴지통',
  }[section] || '내 드라이브'

  return (
    // 전체 레이아웃: 사이드바 + 메인 영역을 가로로 배치
    <div className="flex h-screen bg-white overflow-hidden">
      {/* 왼쪽 사이드바 */}
      <Sidebar
        section={section}
        onSectionChange={handleSectionChange}
        user={user}
        onLogout={handleLogout}
      />

      {/* 오른쪽 메인 영역 */}
      <main className="flex-1 flex flex-col overflow-hidden">
        {/* 상단 헤더 */}
        <header className="flex items-center gap-4 px-6 py-3 border-b border-gray-200">
          <SearchBar />
        </header>

        {/* 컨텐츠 영역 - 드래그 앤 드롭 업로드 대상 */}
        <div
          className="flex-1 overflow-y-auto px-6 py-4 relative"
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
        >
          {/* 드래그 중 오버레이 */}
          {isDragging && (
            <div className="fixed inset-0 left-64 bg-blue-500/10 border-4 border-dashed border-blue-500 z-40 flex items-center justify-center pointer-events-none">
              <div className="bg-white rounded-2xl px-8 py-6 shadow-xl">
                <p className="text-lg font-semibold text-blue-600">📤 여기에 놓아 업로드하기</p>
              </div>
            </div>
          )}

          {/* 업로드 진행 중 배너 */}
          {dragUploading && (
            <div className="fixed bottom-6 right-6 bg-blue-600 text-white px-5 py-3 rounded-xl shadow-lg z-50 text-sm">
              <span className="animate-pulse">⬆️ 업로드 중...</span>
            </div>
          )}
          {/* 제목 + 액션 버튼 */}
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-lg font-semibold text-gray-800">{sectionTitle}</h2>

              {/* Breadcrumb (내 드라이브에서만 표시) */}
              {section === 'drive' && breadcrumb.length > 0 && (
                <div className="flex items-center gap-1 mt-1 text-sm text-gray-500">
                  <button onClick={() => handleBreadcrumbClick(-1)} className="hover:text-blue-600">
                    내 드라이브
                  </button>
                  {breadcrumb.map((crumb, i) => (
                    <span key={crumb.id} className="flex items-center gap-1">
                      <span>/</span>
                      <button
                        onClick={() => handleBreadcrumbClick(i)}
                        className={i === breadcrumb.length - 1 ? 'text-gray-800 font-medium' : 'hover:text-blue-600'}
                      >
                        {crumb.name}
                      </button>
                    </span>
                  ))}
                </div>
              )}
            </div>

            {/* 업로드/폴더 생성 버튼 (휴지통/공유 문서함에서는 숨김) */}
            {section !== 'trash' && section !== 'shared' && (
              <div className="flex items-center gap-2">
                <button
                  onClick={handleCreateFolder}
                  className="flex items-center gap-2 border border-gray-300 text-gray-700 px-4 py-2 rounded-lg text-sm hover:bg-gray-50 transition-colors"
                >
                  <span>📁</span>
                  새 폴더
                </button>
                <UploadButton parentId={parentId} onUploadDone={refetch} />
              </div>
            )}

            {/* 휴지통 비우기 버튼 */}
            {section === 'trash' && files.length > 0 && (
              <button
                onClick={async () => {
                  if (!confirm('휴지통을 비우시겠습니까? 모든 파일이 영구 삭제됩니다.')) return
                  try {
                    await api.delete('/api/files/trash')
                    refetch()
                  } catch {
                    alert('휴지통 비우기에 실패했습니다')
                  }
                }}
                className="flex items-center gap-2 border border-red-300 text-red-500 px-4 py-2 rounded-lg text-sm hover:bg-red-50 transition-colors"
              >
                🗑 휴지통 비우기
              </button>
            )}
          </div>

          {/* 파일 목록 */}
          <FileList
            files={files}
            loading={loading}
            section={section}
            onFolderClick={handleFolderClick}
            onRefresh={refetch}
          />
        </div>
      </main>
    </div>
  )
}
