// Sidebar.jsx - 왼쪽 사이드바 네비게이션
// props: 부모 컴포넌트(DrivePage)에서 전달받는 값들입니다
export default function Sidebar({ section, onSectionChange, user, onLogout }) {
  // 사이드바 메뉴 목록 - 아이콘, 이름, 섹션 키를 배열로 정의합니다
  const menus = [
    { icon: '🏠', label: '내 드라이브', key: 'drive' },
    { icon: '🕐', label: '최근 문서함', key: 'recent' },
    { icon: '⭐', label: '중요 문서함', key: 'starred' },
    { icon: '👥', label: '공유 문서함', key: 'shared' },
    { icon: '🗑', label: '휴지통', key: 'trash' },
  ]

  return (
    <aside className="w-60 min-h-screen bg-gray-50 border-r border-gray-200 flex flex-col">
      {/* 로고 영역 */}
      <div className="px-4 py-5 border-b border-gray-200">
        <div className="flex items-center gap-2">
          <span className="text-2xl">📁</span>
          <span className="font-semibold text-gray-800 text-sm">Google Drive</span>
        </div>
      </div>

      {/* 메뉴 목록 */}
      <nav className="flex-1 px-2 py-3">
        {menus.map((menu) => (
          <button
            key={menu.key}
            onClick={() => onSectionChange(menu.key)}
            className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-left mb-1 transition-colors
              ${section === menu.key
                // 현재 선택된 메뉴는 파란 배경으로 강조합니다
                ? 'bg-blue-100 text-blue-700 font-medium'
                : 'text-gray-700 hover:bg-gray-200'
              }`}
          >
            <span>{menu.icon}</span>
            <span>{menu.label}</span>
          </button>
        ))}
      </nav>

      {/* 하단 사용자 정보 및 로그아웃 */}
      {user && (
        <div className="px-4 py-4 border-t border-gray-200">
          <p className="text-xs text-gray-500 truncate">{user.name}</p>
          <p className="text-xs text-gray-400 truncate mb-2">{user.email}</p>
          <button
            onClick={onLogout}
            className="text-xs text-red-500 hover:text-red-700"
          >
            로그아웃
          </button>
        </div>
      )}
    </aside>
  )
}
