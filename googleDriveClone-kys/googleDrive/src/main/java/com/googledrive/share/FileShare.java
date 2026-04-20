package com.googledrive.share;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_shares")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "shared_with_user_id")
    private Long sharedWithUserId;

    @Column(name = "share_token", unique = true)
    private String shareToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Permission permission;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Permission { VIEWER, EDITOR }

    public static FileShare createLinkShare(Long fileId, Long ownerId, Permission permission, LocalDateTime expiresAt) {
        FileShare share = new FileShare();
        share.fileId = fileId;
        share.ownerId = ownerId;
        share.shareToken = UUID.randomUUID().toString();
        share.permission = permission;
        share.expiresAt = expiresAt;
        share.createdAt = LocalDateTime.now();
        return share;
    }

    public static FileShare createUserShare(Long fileId, Long ownerId, Long sharedWithUserId, Permission permission) {
        FileShare share = new FileShare();
        share.fileId = fileId;
        share.ownerId = ownerId;
        share.sharedWithUserId = sharedWithUserId;
        share.permission = permission;
        share.createdAt = LocalDateTime.now();
        return share;
    }

    public void changePermission(Permission permission) {
        this.permission = permission;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isLinkShare() {
        return shareToken != null;
    }
}
