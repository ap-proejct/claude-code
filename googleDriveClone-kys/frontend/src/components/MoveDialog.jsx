// MoveDialog.jsx - 파일/폴더 이동 대화상자
// 이동할 위치(폴더)를 직접 선택할 수 있습니다
import { useState, useEffect } from 'react'
import api from '../api/axios'

// props:
// - file: 이동할 파일/폴더 객체
// - onClose: 다이얼로그 닫기 함수
// - onMoved: 이동 완료 후 목록 새로고침 함수
export default function MoveDialog({ file, onClose, onMoved }) {
  // 현재 보고 있는 폴더의 내용
  const [folders, setFolders] = useState([])
  // 이동 경로 추적 (breadcrumb용)
  // { id: null, name: '내 드라이브' } 가 루트입니다
  const [path, setPath] = useState([{ id: null, name: '내 드라이브' }])
  const [loading, setLoading] = useState(false)
  const [moving, setMoving] = useState(false)

  // 현재 경로의 마지막 폴더 ID (null이면 루트)
  const currentFolderId = path[path.length - 1].id

  // 경로가 바뀔 때마다 해당 폴더의 하위 폴더 목록을 불러옵니다
  useEffect(() => {
    fetchFolders(currentFolderId)
  }, [currentFolderId])

  const fetchFolders = async (parentId) => {
    setLoading(true)
    try {
      const params = parentId ? { parentId } : {}
      const res = await api.get('/api/files', { params })
      // 폴더만 필터링하고, 이동 대상 본인은 제외합니다
      const folderList = (res.data.data || []).filter(
        (f) => f.itemType === 'FOLDER' && f.id !== file.id
      )
      setFolders(folderList)
    } catch {
      setFolders([])
    } finally {
      setLoading(false)
    }
  }

  // 폴더 클릭 시 해당 폴더 안으로 들어갑니다
  const handleEnterFolder = (folder) => {
    setPath((prev) => [...prev, { id: folder.id, name: folder.name }])
  }

  // breadcrumb 클릭으로 상위 폴더로 돌아갑니다
  const handlePathClick = (index) => {
    setPath((prev) => prev.slice(0, index + 1))
  }

  // 이동 실행
  const handleMove = async () => {
    // 현재 위치와 동일하면 무시
    if (currentFolderId === file.parentId) {
      alert('현재 위치와 같은 폴더입니다')
      return
    }
    setMoving(true)
    try {
      await api.patch(`/api/files/${file.id}/move`, {
        parentId: currentFolderId,
      })
      onMoved?.()
      onClose()
    } catch (err) {
      alert(err.response?.data?.message || '이동에 실패했습니다')
    } finally {
      setMoving(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div
        className="bg-white rounded-2xl shadow-xl w-full max-w-sm"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 헤더 */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <div>
            <h3 className="font-semibold text-gray-800">이동</h3>
            <p className="text-xs text-gray-400 truncate max-w-56 mt-0.5">{file.name}</p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl leading-none">✕</button>
        </div>

        {/* 경로(Breadcrumb) */}
        <div className="px-6 py-3 border-b border-gray-100">
          <div className="flex items-center gap-1 flex-wrap text-sm">
            {path.map((p, i) => (
              <span key={i} className="flex items-center gap-1">
                {i > 0 && <span className="text-gray-300">›</span>}
                <button
                  onClick={() => handlePathClick(i)}
                  className={`hover:text-blue-600 transition-colors
                    ${i === path.length - 1 ? 'text-gray-800 font-medium' : 'text-gray-500'}`}
                >
                  {p.name}
                </button>
              </span>
            ))}
          </div>
        </div>

        {/* 폴더 목록 */}
        <div className="px-4 py-2 max-h-56 overflow-y-auto">
          {loading ? (
            <p className="text-sm text-gray-400 text-center py-6">불러오는 중...</p>
          ) : folders.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-6">하위 폴더가 없습니다</p>
          ) : (
            folders.map((folder) => (
              <button
                key={folder.id}
                onClick={() => handleEnterFolder(folder)}
                className="w-full flex items-center gap-3 px-3 py-2 rounded-lg hover:bg-gray-50 text-left"
              >
                <span className="text-xl">📁</span>
                <span className="text-sm text-gray-700 flex-1 truncate">{folder.name}</span>
                {/* 화살표: 클릭하면 이 폴더 안으로 들어갑니다 */}
                <span className="text-gray-300 text-sm">›</span>
              </button>
            ))
          )}
        </div>

        {/* 현재 선택된 위치 표시 */}
        <div className="px-6 py-3 bg-gray-50 border-t border-gray-100">
          <p className="text-xs text-gray-500">
            이동할 위치: <span className="font-medium text-gray-700">{path[path.length - 1].name}</span>
          </p>
        </div>

        {/* 버튼 */}
        <div className="flex gap-2 px-6 py-4">
          <button
            onClick={onClose}
            className="flex-1 py-2 rounded-lg text-sm border border-gray-300 text-gray-700 hover:bg-gray-50"
          >
            취소
          </button>
          <button
            onClick={handleMove}
            disabled={moving || currentFolderId === file.parentId}
            className="flex-1 py-2 rounded-lg text-sm bg-blue-600 text-white font-medium hover:bg-blue-700 disabled:opacity-50"
          >
            {moving ? '이동 중...' : '여기로 이동'}
          </button>
        </div>
      </div>
    </div>
  )
}
