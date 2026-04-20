package com.googledrive.share;

import com.googledrive.common.ApiResponse;
import com.googledrive.file.dto.FileResponse;
import com.googledrive.share.dto.ShareRequest;
import com.googledrive.share.dto.ShareResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    /** GET /api/files/{id}/shares — 공유 목록 조회 */
    @GetMapping("/api/files/{id}/shares")
    public ResponseEntity<ApiResponse<List<ShareResponse>>> listShares(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(shareService.listShares(id, getCurrentUserId())));
    }

    /** POST /api/files/{id}/shares — 공유 추가 */
    @PostMapping("/api/files/{id}/shares")
    public ResponseEntity<ApiResponse<ShareResponse>> addShare(
            @PathVariable Long id, @RequestBody ShareRequest request) {
        return ResponseEntity.ok(ApiResponse.success(shareService.addShare(id, request, getCurrentUserId())));
    }

    /** PATCH /api/files/{id}/shares/{shareId} — 권한 변경 */
    @PatchMapping("/api/files/{id}/shares/{shareId}")
    public ResponseEntity<ApiResponse<ShareResponse>> updatePermission(
            @PathVariable Long id, @PathVariable Long shareId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                shareService.updatePermission(id, shareId, body.get("permission"), getCurrentUserId())));
    }

    /** DELETE /api/files/{id}/shares/{shareId} — 공유 해제 */
    @DeleteMapping("/api/files/{id}/shares/{shareId}")
    public ResponseEntity<ApiResponse<Void>> removeShare(
            @PathVariable Long id, @PathVariable Long shareId) {
        shareService.removeShare(id, shareId, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** GET /api/shares/{token} — 링크로 파일 접근 (인증 불필요) */
    @GetMapping("/api/shares/{token}")
    public ResponseEntity<ApiResponse<FileResponse>> getByToken(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success(shareService.getFileByToken(token)));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getName());
    }
}
