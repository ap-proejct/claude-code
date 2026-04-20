// SearchBar.jsx - 파일 검색창
// 입력할 때마다 즉시 검색하지 않고 300ms 기다렸다가 검색합니다 (디바운스)
import { useState, useEffect, useRef } from 'react'
import api from '../api/axios'

export default function SearchBar() {
  const [keyword, setKeyword] = useState('')
  const [results, setResults] = useState([])
  const [show, setShow] = useState(false)
  // 디바운스 타이머를 저장하는 ref
  const timerRef = useRef(null)

  useEffect(() => {
    // 이전 타이머를 취소합니다
    clearTimeout(timerRef.current)

    if (keyword.trim().length < 2) {
      setResults([])
      return
    }

    // 300ms 후에 검색 API를 호출합니다 (디바운스)
    timerRef.current = setTimeout(async () => {
      try {
        const res = await api.get('/api/search', { params: { q: keyword } })
        setResults(res.data.data || [])
        setShow(true)
      } catch {
        setResults([])
      }
    }, 300)

    // 컴포넌트가 사라지거나 keyword가 바뀌면 타이머를 정리합니다
    return () => clearTimeout(timerRef.current)
  }, [keyword])

  // 파일 타입에 따른 아이콘
  const getIcon = (file) => {
    if (file.itemType === 'FOLDER') return '📁'
    if (file.mimeType?.startsWith('image/')) return '🖼️'
    return '📎'
  }

  return (
    // relative: 드롭다운을 이 요소 기준으로 위치시키기 위해 사용합니다
    <div className="relative flex-1 max-w-xl">
      <div className="flex items-center bg-gray-100 rounded-full px-4 py-2 gap-2">
        <span className="text-gray-400">🔍</span>
        <input
          type="text"
          value={keyword}
          onChange={(e) => { setKeyword(e.target.value); setShow(true) }}
          onBlur={() => setTimeout(() => setShow(false), 200)}
          placeholder="파일 검색..."
          className="bg-transparent flex-1 text-sm outline-none text-gray-700"
        />
        {keyword && (
          <button onClick={() => { setKeyword(''); setResults([]) }} className="text-gray-400 hover:text-gray-600">✕</button>
        )}
      </div>

      {/* 검색 결과 드롭다운 - show가 true이고 결과가 있을 때만 표시 */}
      {show && results.length > 0 && (
        <div className="absolute top-full left-0 right-0 mt-1 bg-white rounded-xl shadow-lg border border-gray-200 z-50 max-h-72 overflow-y-auto">
          {results.map((file) => (
            <div
              key={file.id}
              className="flex items-center gap-3 px-4 py-2 hover:bg-gray-50 cursor-pointer"
            >
              <span>{getIcon(file)}</span>
              <div>
                <p className="text-sm text-gray-800">{file.name}</p>
                <p className="text-xs text-gray-400">{file.itemType === 'FOLDER' ? '폴더' : '파일'}</p>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 결과 없음 표시 */}
      {show && keyword.trim().length >= 2 && results.length === 0 && (
        <div className="absolute top-full left-0 right-0 mt-1 bg-white rounded-xl shadow-lg border border-gray-200 z-50 px-4 py-3">
          <p className="text-sm text-gray-500">검색 결과가 없습니다</p>
        </div>
      )}
    </div>
  )
}
