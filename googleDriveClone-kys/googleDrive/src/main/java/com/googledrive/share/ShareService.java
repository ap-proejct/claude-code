package com.googledrive.share;

import com.googledrive.file.FileItem;
import com.googledrive.file.FileItemRepository;
import com.googledrive.file.dto.FileResponse;
import com.googledrive.share.dto.ShareRequest;
import com.googledrive.share.dto.ShareResponse;
import com.googledrive.user.User;
import com.googledrive.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShareService {

    private final FileShareRepository fileShareRepository;
    private final FileItemRepository fileItemRepository;
    private final UserRepository userRepository;

    /** 특정 파일의 공유 목록 조회 (소유자만 가능) */
    public List<ShareResponse> listShares(Long fileId, Long requesterId) {
        FileItem file = getFileAndVerifyOwner(fileId, requesterId);
        return fileShareRepository.findByFileId(file.getId()).stream()
                .map(share -> {
                    String email = null;
                    if (!share.isLinkShare() && share.getSharedWithUserId() != null) {
                        email = userRepository.findById(share.getSharedWithUserId())
                                .map(User::getEmail).orElse(null);
                    }
                    return ShareResponse.from(share, email);
                })
                .collect(Collectors.toList());
    }

    /** 공유 추가 (링크 공유 or 유저 공유) */
    @Transactional
    public ShareResponse addShare(Long fileId, ShareRequest request, Long requesterId) {
        FileItem file = getFileAndVerifyOwner(fileId, requesterId);
        FileShare.Permission permission = parsePermission(request.permission());

        FileShare share;
        String email = null;

        if ("LINK".equalsIgnoreCase(request.type())) {
            share = FileShare.createLinkShare(file.getId(), requesterId, permission, request.expiresAt());
        } else if ("USER".equalsIgnoreCase(request.type())) {
            if (request.email() == null || request.email().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "공유 대상 이메일을 입력해주세요");
            }
            User target = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 이메일의 사용자를 찾을 수 없습니다"));
            if (target.getId().equals(requesterId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신에게 공유할 수 없습니다");
            }
            share = FileShare.createUserShare(file.getId(), requesterId, target.getId(), permission);
            email = target.getEmail();
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type은 LINK 또는 USER여야 합니다");
        }

        return ShareResponse.from(fileShareRepository.save(share), email);
    }

    /** 권한 변경 */
    @Transactional
    public ShareResponse updatePermission(Long fileId, Long shareId, String permissionStr, Long requesterId) {
        getFileAndVerifyOwner(fileId, requesterId);
        FileShare share = fileShareRepository.findById(shareId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공유 정보를 찾을 수 없습니다"));
        share.changePermission(parsePermission(permissionStr));
        return ShareResponse.from(share, null);
    }

    /** 공유 해제 */
    @Transactional
    public void removeShare(Long fileId, Long shareId, Long requesterId) {
        getFileAndVerifyOwner(fileId, requesterId);
        FileShare share = fileShareRepository.findById(shareId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공유 정보를 찾을 수 없습니다"));
        fileShareRepository.delete(share);
    }

    /** 공유 링크로 파일 접근 (인증 불필요) */
    public FileResponse getFileByToken(String token) {
        FileShare share = fileShareRepository.findByShareToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지 않은 공유 링크입니다"));
        if (share.isExpired()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "만료된 공유 링크입니다");
        }
        FileItem file = fileItemRepository.findById(share.getFileId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다"));
        if (file.isTrashed()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "삭제된 파일입니다");
        }
        return FileResponse.from(file);
    }

    /** 나에게 공유된 파일 목록 */
    public List<FileResponse> listSharedWithMe(Long userId) {
        return fileShareRepository.findActiveSharesWithUser(userId).stream()
                .map(share -> fileItemRepository.findById(share.getFileId()))
                .filter(opt -> opt.isPresent() && !opt.get().isTrashed())
                .map(opt -> FileResponse.from(opt.get()))
                .collect(Collectors.toList());
    }

    private FileItem getFileAndVerifyOwner(Long fileId, Long ownerId) {
        FileItem file = fileItemRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다"));
        if (!file.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다");
        }
        return file;
    }

    private FileShare.Permission parsePermission(String value) {
        try {
            return FileShare.Permission.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "permission은 VIEWER 또는 EDITOR여야 합니다");
        }
    }
}
