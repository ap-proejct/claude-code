// FileList.jsx - 파일/폴더 목록 컴포넌트
// 그리드 뷰와 리스트 뷰 전환 기능을 포함합니다
import { useState } from 'react'
import FileItem from './FileItem'
import ShareDialog from './ShareDialog'
import MoveDialog from './MoveDialog'
import ImagePreviewDialog from './ImagePreviewDialog'
import api from '../api/axios'

// props:
// - files: 표시할 파일/폴더 목록 배열
// - loading: 데이터 로딩 중 여부
// - section: 현재 섹션 (drive/recent/starred/shared/trash)
// - onFolderClick: 폴더 클릭 시 호출할 함수
// - onRefresh: 목록 새로고침 함수
export default function FileList({ files, loading, section, onFolderClick, onRefresh }) {
  // isGrid: true면 그리드 뷰, false면 리스트 뷰
  const [isGrid, setIsGrid] = useState(true)
  // 액션 메뉴 상태 (어떤 파일의 메뉴가 열렸는지)
  const [actionFile, setActionFile] = useState(null)
  const [shareFile, setShareFile] = useState(null)
  const [moveFile, setMoveFile] = useState(null)
  // 이미지 미리보기 상태 - 이미지 파일 클릭 시 설정됩니다
  const [previewFile, setPreviewFile] = useState(null)

  // 파일 클릭 시:
  // - 이미지/PDF 파일이면 미리보기 모달 열기
  // - 그 외에는 다운로드 URL 받아서 새 탭에서 다운로드
  const handleFileClick = async (fileId) => {
    const file = files.find((f) => f.id === fileId)
    if (file?.mimeType?.startsWith('image/') || file?.mimeType === 'application/pdf') {
      setPreviewFile(file)
      return
    }
    try {
      const res = await api.get(`/api/files/${fileId}/download`)
      const url = res.data.data?.downloadUrl
      if (url) {
        window.open(url, '_blank')
      }
    } catch {
      alert('다운로드 URL을 가져오지 못했습니다')
    }
  }

  // 별표 토글
  const handleStar = async (file) => {
    try {
      await api.patch(`/api/files/${file.id}/star`)
      onRefresh()
    } catch {
      alert('별표 설정에 실패했습니다')
    }
    setActionFile(null)
  }

  // 휴지통으로 이동 / 복원 / 영구 삭제
  const handleTrash = async (file) => {
    if (!confirm(`"${file.name}"을 휴지통으로 이동하시겠습니까?`)) return
    try {
      await api.delete(`/api/files/${file.id}`)
      onRefresh()
    } catch {
      alert('삭제에 실패했습니다')
    }
    setActionFile(null)
  }

  const handleRestore = async (file) => {
    try {
      await api.patch(`/api/files/${file.id}/restore`)
      onRefresh()
    } catch {
      alert('복원에 실패했습니다')
    }
    setActionFile(null)
  }

  const handlePermanentDelete = async (file) => {
    if (!confirm(`"${file.name}"을 영구 삭제하시겠습니까? 복원할 수 없습니다.`)) return
    try {
      await api.delete(`/api/files/${file.id}/permanent`)
      onRefresh()
    } catch {
      alert('영구 삭제에 실패했습니다')
    }
    setActionFile(null)
  }

  // 이름 변경
  const handleRename = async (file) => {
    const newName = prompt('새 이름을 입력하세요', file.name)
    if (!newName || newName === file.name) return
    try {
      await api.patch(`/api/files/${file.id}/rename`, { name: newName })
      onRefresh()
    } catch {
      alert('이름 변경에 실패했습니다')
    }
    setActionFile(null)
  }

  // 로딩 중일 때 스피너 표시
  if (loading) {
    return (
      <div className="flex items-center justify-center h-48 text-gray-400">
        <span className="animate-spin text-2xl mr-2">⟳</span>
        <span>불러오는 중...</span>
      </div>
    )
  }

  // 파일이 없을 때 안내 메시지
  if (files.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-48 text-gray-400">
        <span className="text-4xl mb-2">📭</span>
        <p className="text-sm">파일이 없습니다</p>
      </div>
    )
  }

  return (
    <div>
      {/* 뷰 전환 버튼 (우상단) */}
      <div className="flex justify-end mb-3 gap-1">
        <button
          onClick={() => setIsGrid(true)}
          className={`px-2 py-1 rounded text-sm ${isGrid ? 'bg-gray-200' : 'hover:bg-gray-100'}`}
          title="그리드 뷰"
        >
          ⊞
        </button>
        <button
          onClick={() => setIsGrid(false)}
          className={`px-2 py-1 rounded text-sm ${!isGrid ? 'bg-gray-200' : 'hover:bg-gray-100'}`}
          title="리스트 뷰"
        >
          ☰
        </button>
      </div>

      {/* 파일 목록 */}
      {isGrid ? (
        // 그리드 뷰: 반응형 그리드
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
          {files.map((file) => (
            <FileItem
              key={file.id}
              file={file}
              isGrid={true}
              onFolderClick={onFolderClick}
              onFileClick={handleFileClick}
              onAction={setActionFile}
            />
          ))}
        </div>
      ) : (
        // 리스트 뷰
        <div className="flex flex-col">
          {/* 헤더 행 */}
          <div className="flex items-center gap-3 px-4 py-1 text-xs text-gray-400 border-b border-gray-100 mb-1">
            <span className="w-5"></span>
            <span className="flex-1">이름</span>
            <span className="w-20 text-right hidden sm:block">크기</span>
            <span className="w-24 text-right hidden md:block">수정일</span>
            <span className="w-5"></span>
          </div>
          {files.map((file) => (
            <FileItem
              key={file.id}
              file={file}
              isGrid={false}
              onFolderClick={onFolderClick}
              onFileClick={handleFileClick}
              onAction={setActionFile}
            />
          ))}
        </div>
      )}

      {/* 액션 메뉴 모달 */}
      {actionFile && (
        <div
          className="fixed inset-0 z-40"
          onClick={() => setActionFile(null)}
        >
          <div
            className="absolute bg-white rounded-xl shadow-lg border border-gray-200 py-1 z-50 min-w-36"
            style={{ top: '50%', left: '50%', transform: 'translate(-50%, -50%)' }}
            onClick={(e) => e.stopPropagation()}
          >
            <p className="px-4 py-2 text-xs text-gray-400 border-b border-gray-100 truncate max-w-48">
              {actionFile.name}
            </p>
            {/* 파일이면 다운로드 항목 표시 */}
            {actionFile.itemType === 'FILE' && (
              <button
                onClick={() => { handleFileClick(actionFile.id); setActionFile(null) }}
                className="w-full px-4 py-2 text-sm text-left hover:bg-gray-50 text-blue-600"
              >
                ⬇️ 다운로드
              </button>
            )}
            <button onClick={() => handleRename(actionFile)} className="w-full px-4 py-2 text-sm text-left hover:bg-gray-50">✏️ 이름 변경</button>
            <button onClick={() => { setShareFile(actionFile); setActionFile(null) }} className="w-full px-4 py-2 text-sm text-left hover:bg-gray-50">🔗 공유</button>
            {section !== 'trash' && (
              <button onClick={() => { setMoveFile(actionFile); setActionFile(null) }} className="w-full px-4 py-2 text-sm text-left hover:bg-gray-50">📂 이동</button>
            )}
            <button onClick={() => handleStar(actionFile)} className="w-full px-4 py-2 text-sm text-left hover:bg-gray-50">
              {actionFile.isStarred ? '⭐ 별표 해제' : '☆ 별표 추가'}
            </button>
            {section === 'trash' ? (
              <>
                <button onClick={() => handleRestore(actionFile)} className="w-full px-4 py-2 text-sm text-left hover:bg-gray-50 text-blue-600">♻️ 복원</button>
                <button onClick={() => handlePermanentDelete(actionFile)} className="w-full px-4 py-2 text-sm text-left hover:bg-gray-50 text-red-500">🗑 영구 삭제</button>
              </>
            ) : (
              <button onClick={() => handleTrash(actionFile)} className="w-full px-4 py-2 text-sm text-left hover:bg-gray-50 text-red-500">🗑 휴지통으로 이동</button>
            )}
          </div>
        </div>
      )}

      {/* 공유 다이얼로그 */}
      {shareFile && (
        <ShareDialog file={shareFile} onClose={() => setShareFile(null)} />
      )}

      {/* 이동 다이얼로그 */}
      {moveFile && (
        <MoveDialog file={moveFile} onClose={() => setMoveFile(null)} onMoved={onRefresh} />
      )}

      {/* 이미지 미리보기 다이얼로그 */}
      {previewFile && (
        <ImagePreviewDialog file={previewFile} onClose={() => setPreviewFile(null)} />
      )}
    </div>
  )
}
