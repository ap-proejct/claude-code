// App.jsx - 앱 전체 라우팅 설정
// React Router로 URL에 따라 다른 페이지를 보여줍니다
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import DrivePage from './pages/DrivePage'

// 로그인된 사용자만 접근 가능한 라우트
// 토큰이 없으면 로그인 페이지로 자동 이동합니다
function PrivateRoute({ children }) {
  const { token } = useAuth()
  return token ? children : <Navigate to="/login" replace />
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route
        path="/"
        element={
          <PrivateRoute>
            <DrivePage />
          </PrivateRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    // AuthProvider로 감싸면 모든 자식 컴포넌트에서 useAuth()를 사용할 수 있습니다
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  )
}
