# share 도메인

## 책임

공유 링크 토큰 생성, 링크를 통한 파일/폴더 공개 접근 처리 (로그인 불필요).

## 파일 구성

```
share/
├── controller/
│   └── ShareController.kt      GET /share/{token}
├── service/
│   └── ShareService.kt
└── domain/
    └── (토큰 생성 규칙은 PermissionService에 위임)
```

## ShareController

`/share/{token}`은 `SecurityConfig`에서 `permitAll()` 처리 — 비로그인 접근 허용.

```kotlin
@Controller
class ShareController(private val shareService: ShareService) {

    @GetMapping("/share/{token}")
    fun viewShared(@PathVariable token: String, model: Model): String {
        val result = shareService.resolveByToken(token)
            ?: return "error/404"   // 만료되었거나 존재하지 않는 토큰

        model.addAttribute("file", result.file)
        model.addAttribute("permission", result.permission)  // VIEWER or EDITOR
        model.addAttribute("isOwner", false)
        return "share/view"
    }

    @GetMapping("/share/{token}/download")
    fun downloadShared(@PathVariable token: String, response: HttpServletResponse) {
        val result = shareService.resolveByToken(token)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        // 다운로드 응답 처리
    }
}
```

## ShareService

```kotlin
@Service
class ShareService(
    private val permissionRepository: PermissionRepository,
    private val fileRepository: FileRepository,
) {
    data class SharedFileResult(val file: File, val permission: Permission)

    fun resolveByToken(token: String): SharedFileResult? {
        val perm = permissionRepository.findByShareToken(token) ?: return null
        // 만료 확인
        if (perm.expiresAt != null && perm.expiresAt!! < Instant.now()) return null
        val file = fileRepository.findById(perm.fileId) ?: return null
        if (file.isTrashed) return null
        return SharedFileResult(file, perm.permission)
    }
}
```

## share/view.html 핵심 요소

- 파일 정보 (이름, 크기, 업로드 날짜)
- 다운로드 버튼 (VIEWER, EDITOR 모두 가능)
- `permission == EDITOR`이면 파일 업로드 영역 표시
- 로그인 유도 배너 (로그인 시 내 드라이브에 저장 가능)

## 규칙

- 토큰은 `PermissionService.generateShareToken()`으로 생성 (64자 hex, `SecureRandom`).
- 만료된 토큰으로 접근 시 404 페이지 반환 (403이 아닌 404 — 존재 여부 노출 금지).
- 휴지통에 있는 파일은 링크 공유로도 접근 불가.
- `share/view.html`은 `layout/base.html`을 상속하되, 사이드바 없이 심플한 레이아웃 사용.
