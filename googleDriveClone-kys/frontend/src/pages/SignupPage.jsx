// SignupPage.jsx - 회원가입 화면
// 보안 질문/답변 입력을 포함합니다 (비밀번호 찾기용)
import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import api from '../api/axios'

// 미리 제공되는 보안 질문 목록
// 사용자가 직접 입력할 수도 있고, 이 중에서 선택할 수도 있게 합니다
const PRESET_QUESTIONS = [
  '가장 좋아하는 음식은?',
  '태어난 도시는?',
  '어릴 적 애완동물의 이름은?',
  '출신 초등학교 이름은?',
  '가장 좋아하는 영화는?',
]

export default function SignupPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    securityQuestion: PRESET_QUESTIONS[0],
    securityAnswer: '',
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    try {
      await api.post('/api/auth/signup', form)
      alert('회원가입이 완료됐습니다! 로그인해주세요.')
      navigate('/login')
    } catch (err) {
      setError(err.response?.data?.message || '회원가입에 실패했습니다')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center py-6">
      <div className="bg-white rounded-2xl shadow-md p-8 w-full max-w-sm">
        <div className="text-center mb-6">
          <span className="text-4xl">📁</span>
          <h1 className="text-2xl font-bold text-gray-800 mt-2">회원가입</h1>
        </div>

        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">이름</label>
            <input
              type="text"
              name="name"
              value={form.name}
              onChange={handleChange}
              placeholder="홍길동"
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">이메일</label>
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
            <label className="block text-sm font-medium text-gray-700 mb-1">비밀번호</label>
            <input
              type="password"
              name="password"
              value={form.password}
              onChange={handleChange}
              placeholder="8자 이상"
              required
              minLength={8}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* 보안 질문 영역 - 비밀번호 찾기용 */}
          <div className="border-t border-gray-100 pt-3 mt-3">
            <p className="text-xs text-gray-400 mb-2">
              비밀번호를 잊었을 때 사용할 보안 질문을 설정합니다
            </p>
            <label className="block text-sm font-medium text-gray-700 mb-1">보안 질문</label>
            <select
              name="securityQuestion"
              value={form.securityQuestion}
              onChange={handleChange}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 mb-2"
            >
              {PRESET_QUESTIONS.map((q) => (
                <option key={q} value={q}>{q}</option>
              ))}
            </select>
            <input
              type="text"
              name="securityAnswer"
              value={form.securityAnswer}
              onChange={handleChange}
              placeholder="답변 (대소문자 구분 없음)"
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {error && <p className="text-red-500 text-sm">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white rounded-lg py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors mt-4"
          >
            {loading ? '처리 중...' : '가입하기'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-6">
          이미 계정이 있으신가요?{' '}
          <Link to="/login" className="text-blue-600 hover:underline font-medium">
            로그인
          </Link>
        </p>
      </div>
    </div>
  )
}
