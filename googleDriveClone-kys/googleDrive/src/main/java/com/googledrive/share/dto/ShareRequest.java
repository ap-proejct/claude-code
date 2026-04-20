package com.googledrive.share.dto;

import java.time.LocalDateTime;

public record ShareRequest(
        String type,           // "LINK" or "USER"
        String email,          // type=USER일 때 공유 대상 이메일
        String permission,     // "VIEWER" or "EDITOR"
        LocalDateTime expiresAt
) {}
