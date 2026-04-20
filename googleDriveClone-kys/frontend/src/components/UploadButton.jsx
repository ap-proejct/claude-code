// UploadButton.jsx - 파일 업로드 버튼
// useRef: DOM 요소에 직접 접근할 때 사용합니다 (여기서는 숨겨진 input 클릭용)
import { useRef, useState } from 'react'
import api from '../api/axios'

// props: parentId(현재 폴더 ID), onUploadDone(업로드 완료 후 목록 새로고침 함수)
export default function UploadButton({ parentId, onUploadDone }) {
  // input[type=file] 요소에 접근하기 위한 ref
  const inputRef = useRef(null)
  const [uploading, setUploading] = useState(false)

  // 실제 파일 input을 클릭합니다 (숨겨진 input을 버튼으로 트리거)
  const handleClick = () => {
    inputRef.current?.click()
  }

  // 파일이 선택됐을 때 업로드를 시작합니다
  const handleFileChange = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    setUploading(true)
    try {
      // FormData: 파일을 서버로 전송할 때 사용하는 객체입니다
      const formData = new FormData()
      formData.append('file', file)
      if (parentId) {
        formData.append('parentId', parentId)
      }

      await api.post('/api/files/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })

      // 업로드 완료 후 파일 목록을 다시 불러옵니다
      onUploadDone?.()
    } catch (err) {
      alert('파일 업로드에 실패했습니다: ' + (err.response?.data?.message || err.message))
    } finally {
      setUploading(false)
      // input 값을 초기화해서 같은 파일을 다시 선택할 수 있게 합니다
      e.target.value = ''
    }
  }

  return (
    <>
      {/* 숨겨진 파일 선택 input */}
      <input
        type="file"
        ref={inputRef}
        onChange={handleFileChange}
        className="hidden"
      />

      {/* 업로드 버튼 */}
      <button
        onClick={handleClick}
        disabled={uploading}
        className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors"
      >
        <span>⬆️</span>
        {uploading ? '업로드 중...' : '파일 업로드'}
      </button>
    </>
  )
}
