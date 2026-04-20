// ForgotPasswordPage.jsx - 비밀번호 찾기 화면
// 2단계로 진행합니다:
//   1) 이메일 입력 → 서버에서 해당 계정의 보안 질문을 받아옴
//   2) 답변 + 새 비밀번호 입력 → 서버가 답변을 검증 후 비밀번호 재설정
import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import api from '../api/axios'

export default function ForgotPasswordPage() {
  const navigate = useNavigate()

  // step: 'email' (1단계) → 'reset' (2단계)
  const [step, setStep] = useState('email')

  // 폼 상태
  const [email, setEmail] = useState('')
  const [securityQuestion, setSecurityQuestion] = useState('')
  const [securityAnswer, setSecurityAnswer] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  // ──────────────────────────────────────
  // 1단계: 이메일 제출 → 보안 질문 받아오기
  // ──────────────────────────────────────
  const handleEmailSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const res = await api.get('/api/auth/security-question', {
        params: { email },
      })
      setSecurityQuestion(res.data.data.securityQuestion)
      setStep('reset')
    } catch (err) {
      setError(err.response?.data?.message || '보안 질문을 가져오지 못했습니다')
    } finally {
      setLoading(false)
    }
  }

  // ──────────────────────────────────────
  // 2단계: 답변 + 새 비밀번호 제출 → 재설정
  // ──────────────────────────────────────
  const handleResetSubmit = async (e) => {
    e.preventDefault()
    setError('')

    // 비밀번호 확인 체크
    if (newPassword !== confirmPassword) {
      setError('비밀번호 확인이 일치하지 않습니다')
      return
    }
    if (newPassword.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다')
      return
    }

    setLoading(true)
    try {
      await api.post('/api/auth/reset-password', {
        email,
        securityAnswer,
        newPassword,
      })
      alert('비밀번호가 변경되었습니다! 새 비밀번호로 로그인해주세요.')
      navigate('/login')
    } catch (err) {
      setError(err.response?.data?.message || '비밀번호 재설정에 실패했습니다')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="bg-white rounded-2xl shadow-md p-8 w-full max-w-sm">
        <div className="text-center mb-8">
          <span className="text-4xl">🔑</span>
          <h1 className="text-2xl font-bold text-gray-800 mt-2">비밀번호 찾기</h1>
          <p className="text-gray-500 text-sm mt-1">
            {step === 'email'
              ? '가입 시 입력한 이메일을 입력해주세요'
              : '보안 질문에 답하고 새 비밀번호를 설정하세요'}
          </p>
        </div>

        {step === 'email' && (
          // ─── 1단계: 이메일 입력 ───
          <form onSubmit={handleEmailSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">이메일</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="example@email.com"
                required
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            {error && <p className="text-red-500 text-sm">{error}</p>}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-blue-600 text-white rounded-lg py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {loading ? '확인 중...' : '다음'}
            </button>
          </form>
        )}

        {step === 'reset' && (
          // ─── 2단계: 보안 질문 답변 + 새 비밀번호 ───
          <form onSubmit={handleResetSubmit} className="space-y-4">
            {/* 보안 질문 표시 (읽기 전용) */}
            <div className="bg-gray-50 rounded-lg px-4 py-3">
              <p className="text-xs text-gray-500 mb-1">보안 질문</p>
              <p className="text-sm text-gray-800 font-medium">{securityQuestion}</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">답변</label>
              <input
                type="text"
                value={securityAnswer}
                onChange={(e) => setSecurityAnswer(e.target.value)}
                placeholder="답변 입력"
                required
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">새 비밀번호</label>
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="8자 이상"
                required
                minLength={8}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">새 비밀번호 확인</label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="다시 입력"
                required
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            {error && <p className="text-red-500 text-sm">{error}</p>}

            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => { setStep('email'); setError('') }}
                className="flex-1 border border-gray-300 text-gray-700 rounded-lg py-2 text-sm font-medium hover:bg-gray-50"
              >
                이전
              </button>
              <button
                type="submit"
                disabled={loading}
                className="flex-1 bg-blue-600 text-white rounded-lg py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors"
              >
                {loading ? '변경 중...' : '비밀번호 변경'}
              </button>
            </div>
          </form>
        )}

        <p className="text-center text-sm text-gray-500 mt-6">
          <Link to="/login" className="text-blue-600 hover:underline font-medium">
            로그인으로 돌아가기
          </Link>
        </p>
      </div>
    </div>
  )
}
