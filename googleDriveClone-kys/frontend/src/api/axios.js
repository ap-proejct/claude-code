// Axios 기본 설정 파일
// 모든 API 요청에 공통으로 적용되는 설정을 모아놓은 파일입니다
import axios from 'axios'

// API 기본 URL - 모든 요청은 이 주소를 앞에 붙입니다
const api = axios.create({
  baseURL: 'http://localhost:8080',
})

// 요청 인터셉터 - API를 호출하기 직전에 자동으로 실행됩니다
// localStorage에 저장된 JWT 토큰을 Authorization 헤더에 자동으로 추가합니다
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 응답 인터셉터 - API 응답을 받을 때마다 자동으로 실행됩니다
// 401(인증 실패) 응답이 오면 토큰을 삭제하고 로그인 페이지로 이동합니다
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
