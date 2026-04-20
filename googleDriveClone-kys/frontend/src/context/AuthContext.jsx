// AuthContext.jsx - 로그인 상태를 앱 전체에서 공유하는 파일
// Context(컨텍스트)는 props를 일일이 내려주지 않아도
// 어느 컴포넌트에서나 공통 데이터를 꺼내 쓸 수 있게 해주는 React 기능입니다
import { createContext, useContext, useState } from 'react'

// 1. Context 생성 - 공유할 데이터의 그릇을 만듭니다
const AuthContext = createContext(null)

// JWT 토큰의 만료 여부를 확인해 유효한 토큰만 반환합니다.
// JWT 구조: header.payload.signature (Base64로 인코딩되어 .로 구분)
// payload를 디코드해서 exp(만료 시각, 초 단위)를 읽고 현재 시간과 비교합니다.
// 만료된 경우 localStorage에서도 제거합니다.
function loadValidToken() {
  const token = localStorage.getItem('token')
  if (!token) return null
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    if (!payload.exp || payload.exp * 1000 < Date.now()) {
      // 만료됨 또는 exp 없음 → 정리
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      return null
    }
    return token
  } catch {
    // 토큰 형식이 깨진 경우도 제거
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    return null
  }
}

// 2. Provider 컴포넌트 - 이 컴포넌트로 감싼 자식들은 로그인 상태에 접근할 수 있습니다
export function AuthProvider({ children }) {
  // useState: 컴포넌트 안에서 변할 수 있는 값을 관리합니다
  // 초기값으로 localStorage의 토큰을 읽되, 만료된 경우 null 반환
  const [token, setToken] = useState(loadValidToken())
  const [user, setUser] = useState(() => {
    // 토큰이 유효하지 않으면 user 정보도 무시
    if (!localStorage.getItem('token')) return null
    return JSON.parse(localStorage.getItem('user') || 'null')
  })

  // 로그인 함수 - 토큰과 사용자 정보를 저장합니다
  const login = (token, user) => {
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))
    setToken(token)
    setUser(user)
  }

  // 로그아웃 함수 - 저장된 정보를 모두 지웁니다
  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setToken(null)
    setUser(null)
  }

  return (
    // value에 넣은 값들이 자식 컴포넌트에서 useAuth()로 꺼내 쓸 수 있습니다
    <AuthContext.Provider value={{ token, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

// 3. 커스텀 훅 - useAuth()로 간편하게 로그인 상태를 꺼낼 수 있습니다
export function useAuth() {
  return useContext(AuthContext)
}
