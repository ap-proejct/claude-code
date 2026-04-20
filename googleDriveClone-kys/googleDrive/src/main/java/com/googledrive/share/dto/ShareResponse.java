package com.googledrive.share.dto;

import com.googledrive.share.FileShare;

import java.time.LocalDateTime;

public record ShareResponse(
        Long id,
        Long fileId,
        String type,           // "LINK" or "USER"
        String email,          // 유저 공유면 대상 이메일, 링크 공유면 null
        String shareToken,     // 링크 공유면 토큰, 유저 공유면 null
        String permission,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
    public static ShareResponse from(FileShare share, String email) {
        return new ShareResponse(
                share.getId(),
                share.getFileId(),
                share.isLinkShare() ? "LINK" : "USER",
                email,
                share.getShareToken(),
                share.getPermission().name(),
                share.getExpiresAt(),
                share.getCreatedAt()
        );
    }
}
