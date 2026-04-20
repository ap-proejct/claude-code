package com.googledrive.search;

import com.googledrive.common.ApiResponse;
import com.googledrive.file.FileService;
import com.googledrive.file.dto.FileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final FileService fileService;

    /**
     * 파일/폴더 이름 검색. 키워드 2글자 미만이면 빈 리스트 반환.
     * GET /api/search?q={keyword}
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FileResponse>>> search(
            @RequestParam(value = "q", required = false) String keyword) {

        if (keyword == null || keyword.trim().length() < 2) {
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }

        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(fileService.search(userId, keyword)));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getName());
    }
}
