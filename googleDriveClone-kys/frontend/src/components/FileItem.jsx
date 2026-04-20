// FileItem.jsx - 파일/폴더 카드 한 개를 표시하는 컴포넌트
// props로 받은 file 객체의 정보를 화면에 그립니다
export default function FileItem({ file, onFolderClick, onFileClick, onAction, isGrid }) {
  // 파일 타입에 따라 아이콘을 반환하는 함수
  const getIcon = () => {
    if (file.itemType === 'FOLDER') return '📁'
    if (file.mimeType?.startsWith('image/')) return '🖼️'
    if (file.mimeType === 'application/pdf') return '📄'
    if (file.mimeType?.startsWith('video/')) return '🎬'
    if (file.mimeType?.startsWith('audio/')) return '🎵'
    return '📎'
  }

  // 파일 크기를 읽기 쉬운 단위로 변환합니다 (예: 1024 → 1 KB)
  const formatSize = (bytes) => {
    if (!bytes) return ''
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
    return `${(bytes / 1024 / 1024 / 1024).toFixed(1)} GB`
  }

  // 날짜를 '2025.04.16' 형식으로 변환합니다
  const formatDate = (dateStr) => {
    if (!dateStr) return ''
    return new Date(dateStr).toLocaleDateString('ko-KR')
  }

  // 클릭 처리: 폴더면 폴더 열기, 파일이면 다운로드
  const handleClick = () => {
    if (file.itemType === 'FOLDER') {
      onFolderClick(file.id, file.name)
    } else {
      onFileClick(file.id)
    }
  }

  if (isGrid) {
    // 그리드 뷰
    return (
      <div className="relative group">
        <div
          onClick={handleClick}
          title={file.itemType === 'FILE' ? '클릭하여 다운로드' : '폴더 열기'}
          className="flex flex-col items-center p-4 rounded-xl border border-gray-200 hover:border-blue-300 hover:bg-blue-50 cursor-pointer transition-all"
        >
          <span className="text-4xl mb-2">{getIcon()}</span>
          <p className="text-xs text-gray-700 text-center truncate w-full font-medium">
            {file.name}
          </p>
          {file.size && (
            <p className="text-xs text-gray-400 mt-1">{formatSize(file.size)}</p>
          )}
          {/* 파일이면 호버 시 다운로드 아이콘 표시 */}
          {file.itemType === 'FILE' && (
            <span className="absolute bottom-2 right-2 opacity-0 group-hover:opacity-60 text-xs text-gray-500">
              ⬇️
            </span>
          )}
          {file.isStarred && (
            <span className="absolute top-2 left-2 text-yellow-400 text-xs">⭐</span>
          )}
        </div>

        {/* 점 3개(⋮) 메뉴 버튼 - 호버 시 표시
            p-2 + w-8 h-8: 클릭 범위를 32×32px로 넓혀 실수로 놓치지 않도록 합니다 */}
        <button
          onClick={(e) => { e.stopPropagation(); onAction(file) }}
          className="absolute top-1 right-1 opacity-0 group-hover:opacity-100 text-gray-500 hover:text-gray-800 hover:bg-gray-200 rounded-full w-8 h-8 flex items-center justify-center text-lg leading-none transition-colors"
          title="더보기"
        >
          ⋮
        </button>
      </div>
    )
  }

  // 리스트 뷰
  return (
    <div
      className="flex items-center gap-3 px-4 py-2 hover:bg-gray-50 rounded-lg cursor-pointer group"
      onClick={handleClick}
      title={file.itemType === 'FILE' ? '클릭하여 다운로드' : '폴더 열기'}
    >
      <span className="text-xl flex-shrink-0">{getIcon()}</span>
      <p className="flex-1 text-sm text-gray-800 truncate">{file.name}</p>
      {/* 파일이면 호버 시 다운로드 아이콘 표시 */}
      {file.itemType === 'FILE' && (
        <span className="opacity-0 group-hover:opacity-50 text-xs text-gray-500">⬇️</span>
      )}
      {file.isStarred && <span className="text-yellow-400 text-xs">⭐</span>}
      <p className="text-xs text-gray-400 w-20 text-right hidden sm:block">
        {formatSize(file.size)}
      </p>
      <p className="text-xs text-gray-400 w-24 text-right hidden md:block">
        {formatDate(file.updatedAt)}
      </p>
      {/* 액션 버튼 - 클릭 범위 32×32px 확보 */}
      <button
        onClick={(e) => { e.stopPropagation(); onAction(file) }}
        className="opacity-0 group-hover:opacity-100 text-gray-500 hover:text-gray-800 hover:bg-gray-200 rounded-full w-8 h-8 flex items-center justify-center text-lg leading-none transition-colors flex-shrink-0"
        title="더보기"
      >
        ⋮
      </button>
    </div>
  )
}
