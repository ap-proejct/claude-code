package com.googledrive.file;

import com.googledrive.common.ApiResponse;
import com.googledrive.file.dto.CreateFolderRequest;
import com.googledrive.file.dto.FileResponse;
import com.googledrive.file.dto.MoveRequest;
import com.googledrive.file.dto.RenameRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 파일/폴더 관련 REST API 컨트롤러.
 *
 * 모든 응답은 ApiResponse<T> 래퍼로 감싸서 반환한다.
 * 현재 로그인한 사용자 ID는 SecurityContextHolder에서 추출한다.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    // ──────────────────────────────────────────
    // 특수 문서함 (경로 충돌 방지를 위해 {id} 매핑보다 먼저 선언)
    // ──────────────────────────────────────────

    /**
     * 최근 문서함.
     * GET /api/files/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<FileResponse>>> listRecent() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(fileService.listRecent(userId)));
    }

    /**
     * 별표(중요) 문서함.
     * GET /api/files/starred
     */
    @GetMapping("/starred")
    public ResponseEntity<ApiResponse<List<FileResponse>>> listStarred() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(fileService.listStarred(userId)));
    }

    /**
     * 공유 문서함 (미구현 — 빈 리스트 반환).
     * GET /api/files/shared
     */
    @GetMapping("/shared")
    public ResponseEntity<ApiResponse<List<FileResponse>>> listShared() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(fileService.listShared(userId)));
    }

    /**
     * 휴지통 목록.
     * GET /api/files/trash
     */
    @GetMapping("/trash")
    public ResponseEntity<ApiResponse<List<FileResponse>>> listTrash() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(fileService.listTrash(userId)));
    }

    // ──────────────────────────────────────────
    // 파일/폴더 기본 CRUD
    // ──────────────────────────────────────────

    /**
     * 파일/폴더 목록 조회.
     * GET /api/files?parentId=xxx  (parentId 없으면 루트)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FileResponse>>> listFiles(
            @RequestParam(required = false) Long parentId) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(fileService.listFiles(userId, parentId)));
    }

    /**
     * 폴더 생성.
     * POST /api/files/folder
     */
    @PostMapping("/folder")
    public ResponseEntity<ApiResponse<FileResponse>> createFolder(
            @Valid @RequestBody CreateFolderRequest request) {
        Long userId = getCurrentUserId();
        FileResponse response = fileService.createFolder(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 파일 업로드.
     * POST /api/files/upload
     * multipart/form-data: file (MultipartFile), parentId (optional)
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "parentId", required = false) Long parentId) {
        Long userId = getCurrentUserId();
        FileResponse response = fileService.uploadFile(file, parentId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 파일/폴더 상세 조회 (접근 기록 upsert 포함).
     * GET /api/files/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FileResponse>> getFile(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(fileService.getFile(id, userId)));
    }

    /**
     * 이름 변경.
     * PATCH /api/files/{id}/rename
     */
    @PatchMapping("/{id}/rename")
    public ResponseEntity<ApiResponse<FileResponse>> renameFile(
            @PathVariable Long id,
            @Valid @RequestBody RenameRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(fileService.renameFile(id, request, userId)));
    }

    /**
     * 이동.
     * PATCH /api/files/{id}/move
     */
    @PatchMapping("/{id}/move")
    public ResponseEntity<ApiResponse<FileResponse>> moveFile(
            @PathVariable Long id,
            @RequestBody MoveRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(fileService.moveFile(id, request, userId)));
    }

    /**
     * 복사 (FILE만 지원).
     * POST /api/files/{id}/copy
     */
    @PostMapping("/{id}/copy")
    public ResponseEntity<ApiResponse<FileResponse>> copyFile(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        FileResponse response = fileService.copyFile(id, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 다운로드 Presigned URL 반환.
     * GET /api/files/{id}/download
     * 응답 형식: {"downloadUrl": "https://..."}
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<ApiResponse<Map<String, String>>> downloadFile(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        String url = fileService.getDownloadUrl(id, userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("downloadUrl", url)));
    }

    /**
     * 미리보기 Presigned URL 반환 (이미지 등 inline 표시용).
     * GET /api/files/{id}/view
     * 응답 형식: {"viewUrl": "https://..."}
     */
    @GetMapping("/{id}/view")
    public ResponseEntity<ApiResponse<Map<String, String>>> viewFile(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        String url = fileService.getViewUrl(id, userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("viewUrl", url)));
    }

    /**
     * 별표 토글.
     * PATCH /api/files/{id}/star
     */
    @PatchMapping("/{id}/star")
    public ResponseEntity<ApiResponse<FileResponse>> toggleStar(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(fileService.toggleStar(id, userId)));
    }

    /**
     * 휴지통으로 이동 (소프트 삭제).
     * DELETE /api/files/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> trashFile(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        fileService.trashFile(id, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // ──────────────────────────────────────────
    // 휴지통 관리
    // ──────────────────────────────────────────

    /**
     * 휴지통에서 복원.
     * PATCH /api/files/{id}/restore
     */
    @PatchMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<FileResponse>> restoreFile(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(fileService.restoreFile(id, userId)));
    }

    /**
     * 영구 삭제.
     * DELETE /api/files/{id}/permanent
     */
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponse<Void>> permanentDelete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        fileService.permanentDelete(id, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 휴지통 비우기 — 현재 사용자의 모든 trashed 항목을 영구 삭제한다.
     * DELETE /api/files/trash
     */
    @DeleteMapping("/trash")
    public ResponseEntity<ApiResponse<Void>> emptyTrash() {
        Long userId = getCurrentUserId();
        fileService.emptyTrash(userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // ──────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────

    /**
     * SecurityContextHolder에서 현재 로그인한 사용자 ID를 추출한다.
     * JwtAuthenticationFilter에서 Authentication.setName(userId)로 저장했으므로
     * getName()을 Long으로 파싱해서 사용한다.
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getName());
    }
}
