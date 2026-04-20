// LoginPage.jsx - 로그인 화면
import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import api from '../api/axios'

export default function LoginPage() {
  // useNavigate: 다른 페이지로 이동할 때 사용합니다
  const navigate = useNavigate()
  const { login } = useAuth()

  // 입력 폼 상태 관리
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  // 입력값이 바뀔 때마다 form 상태를 업데이트합니다
  const handleChange = (e) => {
    // ...form: 기존 값을 복사하고, 변경된 필드만 덮어씁니다
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  // 폼 제출 처리
  const handleSubmit = async (e) => {
    // 기본 동작(페이지 새로고침)을 막습니다
    e.preventDefault()
    setLoading(true)
    setError('')

    try {
      const response = await api.post('/api/auth/login', form)
      const { token, userId, email, name } = response.data.data
      // 로그인 성공 → 토큰과 사용자 정보를 저장하고 메인 화면으로 이동
      login(token, { userId, email, name })
      navigate('/')
    } catch (err) {
      // 에러 메시지를 화면에 표시합니다
      setError(err.response?.data?.message || '로그인에 실패했습니다')
    } finally {
      setLoading(false)
    }
  }

  return (
    // 화면 전체를 중앙 정렬
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="bg-white rounded-2xl shadow-md p-8 w-full max-w-sm">
        {/* 로고 */}
        <div className="text-center mb-8">
          <span className="text-4xl">📁</span>
          <h1 className="text-2xl font-bold text-gray-800 mt-2">Google Drive</h1>
          <p className="text-gray-500 text-sm mt-1">클론 프로젝트</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              이메일
            </label>
            <input
              type="email"
              name="email"
              value={form.email}
              onChange={handleChange}
              placeholder="example@email.com"
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              비밀번호
            </label>
            <input
              type="password"
              name="password"
              value={form.password}
              onChange={handleChange}
              placeholder="비밀번호 입력"
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* 에러 메시지 - error 값이 있을 때만 표시 */}
          {error && (
            <p className="text-red-500 text-sm">{error}</p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white rounded-lg py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors"
          >
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>

        {/* 비밀번호 찾기 링크 */}
        <p className="text-center text-sm text-gray-500 mt-4">
          <Link to="/forgot-password" className="text-gray-500 hover:text-blue-600 hover:underline">
            비밀번호를 잊으셨나요?
          </Link>
        </p>

        <p className="text-center text-sm text-gray-500 mt-3 pt-3 border-t border-gray-100">
          계정이 없으신가요?{' '}
          <Link to="/signup" className="text-blue-600 hover:underline font-medium">
            회원가입
          </Link>
        </p>
      </div>
    </div>
  )
}
