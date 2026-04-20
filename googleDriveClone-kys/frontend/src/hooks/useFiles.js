// useFiles 커스텀 훅
// 훅(Hook)은 컴포넌트 안에서 반복되는 로직을 따로 빼놓은 함수입니다
// section과 parentId에 따라 알맞은 API를 호출해서 파일 목록을 가져옵니다
import { useState, useEffect, useCallback } from 'react'
import api from '../api/axios'

export function useFiles(section, parentId) {
  // files: 파일 목록 데이터
  const [files, setFiles] = useState([])
  // loading: API 호출 중인지 여부 (true면 로딩 스피너 표시)
  const [loading, setLoading] = useState(false)
  // error: 에러 발생 시 에러 객체 저장
  const [error, setError] = useState(null)

  // useCallback: 함수를 메모이제이션해서 불필요한 재생성을 막습니다
  const fetchFiles = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      let response
      // section 값에 따라 다른 API 엔드포인트를 호출합니다
      if (section === 'recent') {
        response = await api.get('/api/files/recent')
      } else if (section === 'starred') {
        response = await api.get('/api/files/starred')
      } else if (section === 'shared') {
        response = await api.get('/api/files/shared')
      } else if (section === 'trash') {
        response = await api.get('/api/files/trash')
      } else {
        // 기본 내 드라이브: parentId가 있으면 폴더 안, 없으면 루트
        const params = parentId ? { parentId } : {}
        response = await api.get('/api/files', { params })
      }
      // response.data.data → 백엔드 ApiResponse의 data 필드
      setFiles(response.data.data || [])
    } catch (err) {
      setError(err)
      setFiles([])
    } finally {
      setLoading(false)
    }
  }, [section, parentId])

  // useEffect: section이나 parentId가 바뀔 때마다 파일 목록을 다시 불러옵니다
  useEffect(() => {
    fetchFiles()
  }, [fetchFiles])

  // 컴포넌트에서 쓸 수 있도록 값들을 반환합니다
  return { files, loading, error, refetch: fetchFiles }
}
