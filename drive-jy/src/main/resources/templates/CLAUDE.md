# 프론트엔드 가이드 (Thymeleaf + Tailwind CSS)

## Tailwind CSS 설정

CDN으로 사용. `layout/base.html`의 `<head>`에 한 번만 선언.

```html
<script src="https://cdn.tailwindcss.com"></script>
<script>
  tailwind.config = {
    theme: {
      extend: {
        colors: {
          brand: {
            50:  '#eff6ff',
            500: '#3b82f6',
            600: '#2563eb',
            700: '#1d4ed8',
          }
        }
      }
    }
  }
</script>
```

반복 사용 유틸리티 클래스 조합은 `<style th:inline="none">` 블록 안에 `@layer components`로 정의:

```html
<style>
  @layer components {
    .btn-primary   { @apply px-4 py-2 bg-brand-600 text-white rounded-lg hover:bg-brand-700 transition-colors text-sm font-medium; }
    .btn-secondary { @apply px-4 py-2 bg-white text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm font-medium; }
    .btn-danger    { @apply px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors text-sm font-medium; }
    .input-field   { @apply w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 text-sm; }
    .card          { @apply bg-white rounded-xl border border-gray-200 shadow-sm hover:shadow-md transition-shadow; }
    .sidebar-item  { @apply flex items-center gap-3 px-3 py-2 rounded-lg text-gray-700 hover:bg-gray-100 transition-colors text-sm cursor-pointer; }
    .sidebar-item-active { @apply flex items-center gap-3 px-3 py-2 rounded-lg bg-brand-50 text-brand-700 font-medium text-sm; }
  }
</style>
```

---

## 템플릿 디렉토리 구조

```
templates/
├── layout/
│   ├── base.html           기본 레이아웃 (head, sidebar, header, content slot)
│   ├── sidebar.html        좌측 사이드바 fragment
│   ├── header.html         상단 헤더 fragment
│   └── breadcrumb.html     breadcrumb fragment
├── components/
│   ├── file-card.html      파일 카드 (그리드 뷰)
│   ├── file-row.html       파일 행 (리스트 뷰)
│   ├── modal.html          공통 모달 래퍼
│   ├── upload-zone.html    드래그앤드롭 업로드 영역
│   ├── context-menu.html   우클릭 컨텍스트 메뉴
│   └── permission-modal.html 권한 설정 모달
├── auth/
│   ├── login.html
│   └── register.html
├── drive/
│   ├── index.html          내 드라이브 루트
│   └── folder.html         폴더 내용
├── groups/
│   ├── list.html
│   └── detail.html
├── share/
│   └── view.html           공유 링크 페이지 (로그인 불필요)
└── trash/
    └── index.html
```

---

## 레이아웃 공통화

### base.html 구조

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title th:text="${pageTitle} + ' — Drive'">Drive</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <!-- @layer components 정의 -->
  <th:block th:replace="~{layout/base :: styles}"></th:block>
  <!-- 페이지별 추가 스타일 -->
  <th:block th:replace="${extraHead} ?: ~{}"></th:block>
</head>
<body class="bg-gray-50 text-gray-900">
  <div class="flex h-screen overflow-hidden">
    <!-- 사이드바 -->
    <th:block th:replace="~{layout/sidebar :: sidebar}"></th:block>
    <!-- 메인 영역 -->
    <div class="flex-1 flex flex-col overflow-hidden">
      <th:block th:replace="~{layout/header :: header}"></th:block>
      <main class="flex-1 overflow-y-auto p-6">
        <!-- 페이지별 콘텐츠 삽입 -->
        <th:block th:replace="${content}"></th:block>
      </main>
    </div>
  </div>
  <!-- 공통 JS -->
  <script th:src="@{/js/csrf.js}"></script>
  <th:block th:replace="${extraScript} ?: ~{}"></th:block>
</body>
</html>
```

### 페이지에서 base.html 사용

```html
<!-- drive/index.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{layout/base :: layout(
          pageTitle='내 드라이브',
          content=~{::main-content}
      )}">
<body>
  <th:block th:fragment="main-content">
    <!-- 실제 내용만 여기에 -->
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-xl font-semibold">내 드라이브</h1>
      <button class="btn-primary">+ 새로 만들기</button>
    </div>
    <!-- 파일 목록 -->
    <th:block th:replace="~{components/file-card :: grid(files=${files})}"></th:block>
  </th:block>
</body>
</html>
```

---

## 공통 컴포넌트

### file-card.html (그리드 뷰)

```html
<!-- th:fragment="grid(files)" -->
<div th:fragment="grid(files)"
     class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
  <div th:each="file : ${files}"
       class="card p-3 cursor-pointer group relative"
       th:data-id="${file.id}"
       th:data-type="${file.type}">
    <!-- 파일 타입 아이콘 -->
    <div class="flex justify-center mb-2">
      <img th:src="@{/icons/__${file.iconName}__.svg}" class="w-12 h-12" alt="">
    </div>
    <!-- 파일명 -->
    <p class="text-xs text-center truncate text-gray-700" th:text="${file.name}"></p>
    <!-- 컨텍스트 메뉴 트리거 -->
    <button class="absolute top-1 right-1 opacity-0 group-hover:opacity-100
                   p-1 rounded hover:bg-gray-200 transition-opacity"
            th:data-file-id="${file.id}">
      <svg class="w-4 h-4 text-gray-500"><!-- 점 3개 아이콘 --></svg>
    </button>
  </div>
</div>
```

### file-row.html (리스트 뷰)

```html
<tr th:fragment="row(file)"
    class="hover:bg-gray-50 cursor-pointer border-b border-gray-100"
    th:data-id="${file.id}">
  <td class="py-2 px-4 flex items-center gap-3">
    <img th:src="@{/icons/__${file.iconName}__.svg}" class="w-5 h-5" alt="">
    <span class="text-sm" th:text="${file.name}"></span>
  </td>
  <td class="py-2 px-4 text-sm text-gray-500" th:text="${file.owner}"></td>
  <td class="py-2 px-4 text-sm text-gray-500" th:text="${file.updatedAtFormatted}"></td>
  <td class="py-2 px-4 text-sm text-gray-500" th:text="${file.sizeFormatted}"></td>
</tr>
```

### modal.html (공통 모달 래퍼)

```html
<div th:fragment="wrapper(modalId, title, content)"
     th:id="${modalId}"
     class="hidden fixed inset-0 z-50 flex items-center justify-center bg-black/40">
  <div class="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4">
    <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
      <h2 class="text-base font-semibold" th:text="${title}"></h2>
      <button th:onclick="|closeModal('${modalId}')|"
              class="p-1 rounded hover:bg-gray-100">
        <svg class="w-5 h-5 text-gray-500"><!-- X 아이콘 --></svg>
      </button>
    </div>
    <div class="px-6 py-4">
      <th:block th:replace="${content}"></th:block>
    </div>
  </div>
</div>
```

### upload-zone.html

```html
<div th:fragment="zone"
     id="upload-zone"
     class="border-2 border-dashed border-gray-300 rounded-xl p-8
            flex flex-col items-center justify-center gap-2
            hover:border-brand-500 hover:bg-brand-50 transition-colors
            cursor-pointer">
  <svg class="w-10 h-10 text-gray-400"><!-- 업로드 아이콘 --></svg>
  <p class="text-sm text-gray-600">파일을 드래그하거나 <span class="text-brand-600 font-medium">클릭</span>하여 업로드</p>
  <p class="text-xs text-gray-400">최대 512MB</p>
  <input type="file" id="file-input" class="hidden" multiple>
</div>
```

---

## JavaScript 공통 패턴

### CSRF 토큰 설정 (`static/js/csrf.js`)

```javascript
// 모든 fetch 요청에 CSRF 토큰 자동 포함
const csrfToken = document.cookie
  .split('; ')
  .find(row => row.startsWith('XSRF-TOKEN='))
  ?.split('=')[1];

window.apiFetch = (url, options = {}) => fetch(url, {
  ...options,
  headers: {
    'Content-Type': 'application/json',
    'X-XSRF-TOKEN': csrfToken,
    ...options.headers,
  },
});
```

### 모달 열기/닫기

```javascript
function openModal(id)  { document.getElementById(id).classList.remove('hidden'); }
function closeModal(id) { document.getElementById(id).classList.add('hidden'); }
```

### 파일 업로드 (진행률 포함)

```javascript
async function uploadFiles(files, parentId) {
  for (const file of files) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('parentId', parentId ?? '');

    const xhr = new XMLHttpRequest();
    xhr.upload.addEventListener('progress', e => {
      if (e.lengthComputable) updateProgress(file.name, e.loaded / e.total * 100);
    });
    xhr.open('POST', '/api/files/upload');
    xhr.setRequestHeader('X-XSRF-TOKEN', csrfToken);
    xhr.send(formData);
    await new Promise(resolve => xhr.addEventListener('loadend', resolve));
  }
}
```

### 그리드/리스트 뷰 토글

```javascript
const VIEW_KEY = 'drive-view-mode';
function toggleView() {
  const mode = localStorage.getItem(VIEW_KEY) === 'list' ? 'grid' : 'list';
  localStorage.setItem(VIEW_KEY, mode);
  document.getElementById('file-grid').classList.toggle('hidden', mode !== 'grid');
  document.getElementById('file-list').classList.toggle('hidden', mode !== 'list');
}
document.addEventListener('DOMContentLoaded', () => {
  const saved = localStorage.getItem(VIEW_KEY) ?? 'grid';
  document.getElementById('file-grid').classList.toggle('hidden', saved !== 'grid');
  document.getElementById('file-list').classList.toggle('hidden', saved !== 'list');
});
```

---

## 사이드바 구조

```
내 드라이브     /drive
공유 문서함     /drive/shared-with-me
최근 항목       /drive/recent
중요 항목       /drive/starred
휴지통          /trash
──────────────
그룹            /groups
──────────────
스토리지 사용량  (progress bar)
```

활성 메뉴 항목은 `sidebar-item-active` 클래스, 나머지는 `sidebar-item` 클래스 사용.

```html
<a th:href="@{/drive}"
   th:classappend="${currentPath == '/drive'} ? 'sidebar-item-active' : 'sidebar-item'">
  <svg class="w-5 h-5"><!-- 드라이브 아이콘 --></svg>
  내 드라이브
</a>
```

---

## 규칙

- 새 페이지는 반드시 `base.html`을 상속한다. 독립 HTML 파일 작성 금지.
- 공통 UI 요소(버튼, 인풋, 카드, 모달)는 반드시 위 컴포넌트 fragments를 재사용한다.
- Tailwind 클래스 조합이 3회 이상 반복되면 `@layer components`에 추가한다.
- 인라인 스타일(`style=""`) 사용 금지. Tailwind 유틸리티 클래스만 사용한다.
- JavaScript는 `static/js/` 파일로 분리. 인라인 `<script>` 블록은 페이지 초기화 코드만 허용.
- 서버 데이터 전달은 Thymeleaf `th:data-*` 속성 사용, 인라인 JS 변수 선언 금지.
