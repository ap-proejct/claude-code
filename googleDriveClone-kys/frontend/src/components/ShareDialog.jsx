// ShareDialog.jsx - 구글 드라이브 스타일 공유 다이얼로그
// 하나의 화면에서 사용자 초대 + 일반 액세스(링크 공유) 설정을 합니다
// 구글 드라이브와 동일한 UX:
// 1. 상단: 이메일로 사용자 초대 (권한: 뷰어/편집자)
// 2. 중간: 이미 공유된 사용자 목록
// 3. 하단: "일반 액세스" — 제한됨 vs 링크가 있는 모든 사용자

import { useState, useEffect } from 'react'
import api from '../api/axios'

// props:
// - file: 공유할 파일/폴더 객체
// - onClose: 다이얼로그 닫기 함수
export default function ShareDialog({ file, onClose }) {
  // 공유 목록 (링크 + 사용자 모두 포함)
  const [shares, setShares] = useState([])

  // 링크 공유 상태
  const [linkShare, setLinkShare] = useState(null)
  // 일반 액세스 모드: 'restricted' (제한됨) 또는 'anyone' (링크가 있는 모든 사용자)
  const [accessMode, setAccessMode] = useState('restricted')
  // 링크 공유 권한 (anyone 모드일 때)
  const [linkPermission, setLinkPermission] = useState('VIEWER')
  // 링크 복사 완료 메시지
  const [copied, setCopied] = useState(false)

  // 사용자 초대 폼 상태
  const [inviteEmail, setInviteEmail] = useState('')
  const [invitePermission, setInvitePermission] = useState('VIEWER')
  const [inviting, setInviting] = useState(false)
  const [inviteError, setInviteError] = useState('')

  // 로딩 상태
  const [changingAccess, setChangingAccess] = useState(false)

  // 다이얼로그 열릴 때 기존 공유 목록을 불러옵니다
  useEffect(() => {
    fetchShares()
  }, [file.id])

  const fetchShares = async () => {
    try {
      const res = await api.get(`/api/files/${file.id}/shares`)
      const list = res.data.data || []
      setShares(list)
      // 기존 링크 공유가 있으면 → 'anyone' 모드
      const existing = list.find((s) => s.type === 'LINK')
      if (existing) {
        setLinkShare(existing)
        setAccessMode('anyone')
        setLinkPermission(existing.permission)
      } else {
        setLinkShare(null)
        setAccessMode('restricted')
      }
    } catch {
      // 권한 없으면 무시
    }
  }

  // ──────────────────────────────────────
  // 일반 액세스 변경 핸들러
  // ──────────────────────────────────────

  // "제한됨" ↔ "링크가 있는 모든 사용자" 전환
  const handleAccessModeChange = async (newMode) => {
    setChangingAccess(true)
    try {
      if (newMode === 'anyone' && !linkShare) {
        // 링크 공유 생성
        const res = await api.post(`/api/files/${file.id}/shares`, {
          type: 'LINK',
          permission: linkPermission,
        })
        setLinkShare(res.data.data)
      } else if (newMode === 'restricted' && linkShare) {
        // 기존 링크 공유 삭제
        await api.delete(`/api/files/${file.id}/shares/${linkShare.id}`)
        setLinkShare(null)
      }
      setAccessMode(newMode)
      await fetchShares()
    } catch (err) {
      alert(err.response?.data?.message || '액세스 설정 변경에 실패했습니다')
    } finally {
      setChangingAccess(false)
    }
  }

  // 링크 공유 권한 변경 (뷰어 ↔ 편집자)
  const handleLinkPermissionChange = async (newPerm) => {
    if (!linkShare) return
    setLinkPermission(newPerm)
    try {
      await api.patch(`/api/files/${file.id}/shares/${linkShare.id}`, {
        permission: newPerm,
      })
      await fetchShares()
    } catch {
      alert('권한 변경에 실패했습니다')
    }
  }

  // ──────────────────────────────────────
  // 사용자 초대
  // ──────────────────────────────────────

  const handleInvite = async (e) => {
    e.preventDefault()
    if (!inviteEmail.trim()) return
    setInviting(true)
    setInviteError('')
    try {
      await api.post(`/api/files/${file.id}/shares`, {
        type: 'USER',
        email: inviteEmail.trim(),
        permission: invitePermission,
      })
      setInviteEmail('')
      await fetchShares()
    } catch (err) {
      setInviteError(err.response?.data?.message || '초대에 실패했습니다')
    } finally {
      setInviting(false)
    }
  }

  // 사용자 공유 권한 변경
  const handleUserPermissionChange = async (share, newPerm) => {
    try {
      await api.patch(`/api/files/${file.id}/shares/${share.id}`, {
        permission: newPerm,
      })
      await fetchShares()
    } catch {
      alert('권한 변경에 실패했습니다')
    }
  }

  // 공유 해제
  const handleRemoveShare = async (shareId) => {
    try {
      await api.delete(`/api/files/${file.id}/shares/${shareId}`)
      if (linkShare?.id === shareId) {
        setLinkShare(null)
        setAccessMode('restricted')
      }
      await fetchShares()
    } catch {
      alert('공유 해제에 실패했습니다')
    }
  }

  // 링크 복사
  const handleCopyLink = () => {
    if (!linkShare?.shareToken) return
    const url = `${window.location.origin}/shared/${linkShare.shareToken}`
    navigator.clipboard.writeText(url).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    })
  }

  // 사용자 공유 목록만 필터링
  const userShares = shares.filter((s) => s.type === 'USER')

  return (
    // 배경 오버레이
    <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div
        className="bg-white rounded-2xl shadow-xl w-full max-w-lg"
        onClick={(e) => e.stopPropagation()}
      >
        {/* ── 헤더 ── */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <div>
            <h3 className="font-semibold text-gray-800">"{file.name}" 공유</h3>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl leading-none">✕</button>
        </div>

        <div className="px-6 py-5 space-y-5">
          {/* ── 사용자 초대 영역 ── */}
          <form onSubmit={handleInvite} className="flex items-end gap-2">
            <div className="flex-1">
              <input
                type="email"
                value={inviteEmail}
                onChange={(e) => setInviteEmail(e.target.value)}
                placeholder="사용자 추가 (이메일)"
                className="w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <select
              value={invitePermission}
              onChange={(e) => setInvitePermission(e.target.value)}
              className="border border-gray-300 rounded-lg px-2 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="VIEWER">뷰어</option>
              <option value="EDITOR">편집자</option>
            </select>
            <button
              type="submit"
              disabled={inviting || !inviteEmail.trim()}
              className="bg-blue-600 text-white rounded-lg px-4 py-2.5 text-sm font-medium hover:bg-blue-700 disabled:opacity-50 whitespace-nowrap"
            >
              {inviting ? '...' : '초대'}
            </button>
          </form>
          {inviteError && <p className="text-red-500 text-xs -mt-3">{inviteError}</p>}

          {/* ── 액세스 권한이 있는 사용자 목록 ── */}
          {userShares.length > 0 && (
            <div>
              <p className="text-xs font-medium text-gray-500 mb-2">액세스 권한이 있는 사용자</p>
              <div className="space-y-1 max-h-40 overflow-y-auto">
                {userShares.map((share) => (
                  <div key={share.id} className="flex items-center justify-between py-2 px-3 rounded-lg hover:bg-gray-50">
                    <div className="flex items-center gap-3">
                      {/* 사용자 아바타 */}
                      <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 text-sm font-medium">
                        {(share.email || '?')[0].toUpperCase()}
                      </div>
                      <p className="text-sm text-gray-700">{share.email}</p>
                    </div>
                    <div className="flex items-center gap-2">
                      <select
                        value={share.permission}
                        onChange={(e) => handleUserPermissionChange(share, e.target.value)}
                        className="text-xs border border-gray-200 rounded-md px-2 py-1 text-gray-600 focus:outline-none"
                      >
                        <option value="VIEWER">뷰어</option>
                        <option value="EDITOR">편집자</option>
                      </select>
                      <button
                        onClick={() => handleRemoveShare(share.id)}
                        className="text-gray-400 hover:text-red-500 text-sm"
                        title="액세스 삭제"
                      >
                        ✕
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* ── 일반 액세스 (구글 드라이브 핵심 기능) ── */}
          <div className="border-t border-gray-100 pt-4">
            <p className="text-xs font-medium text-gray-500 mb-3">일반 액세스</p>
            <div className="flex items-center gap-3">
              {/* 잠금/지구 아이콘 */}
              <div className={`w-9 h-9 rounded-full flex items-center justify-center text-lg shrink-0 ${
                accessMode === 'anyone' ? 'bg-green-100' : 'bg-gray-100'
              }`}>
                {accessMode === 'anyone' ? '🌐' : '🔒'}
              </div>
              <div className="flex-1">
                {/* 액세스 모드 선택 드롭다운 */}
                <select
                  value={accessMode}
                  onChange={(e) => handleAccessModeChange(e.target.value)}
                  disabled={changingAccess}
                  className="w-full text-sm font-medium text-gray-700 border border-gray-200 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
                >
                  <option value="restricted">제한됨</option>
                  <option value="anyone">링크가 있는 모든 사용자</option>
                </select>
                {/* 설명 텍스트 */}
                <p className="text-xs text-gray-400 mt-1">
                  {accessMode === 'anyone'
                    ? '링크를 가진 인터넷상의 모든 사용자가 볼 수 있습니다'
                    : '추가된 사용자만 이 항목을 열 수 있습니다'
                  }
                </p>
              </div>
              {/* anyone 모드일 때 권한 선택 */}
              {accessMode === 'anyone' && linkShare && (
                <select
                  value={linkPermission}
                  onChange={(e) => handleLinkPermissionChange(e.target.value)}
                  className="text-xs border border-gray-200 rounded-md px-2 py-1.5 text-gray-600 focus:outline-none shrink-0"
                >
                  <option value="VIEWER">뷰어</option>
                  <option value="EDITOR">편집자</option>
                </select>
              )}
            </div>
          </div>
        </div>

        {/* ── 하단 버튼 영역 ── */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-gray-100">
          {accessMode === 'anyone' && linkShare ? (
            <button
              onClick={handleCopyLink}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                copied
                  ? 'bg-green-500 text-white'
                  : 'bg-blue-50 text-blue-600 hover:bg-blue-100'
              }`}
            >
              {copied ? '✓ 복사됨!' : '🔗 링크 복사'}
            </button>
          ) : (
            <div />
          )}
          <button
            onClick={onClose}
            className="px-5 py-2 rounded-lg text-sm font-medium bg-blue-600 text-white hover:bg-blue-700"
          >
            완료
          </button>
        </div>
      </div>
    </div>
  )
}
