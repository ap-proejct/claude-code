package com.googledrive.file;

import com.googledrive.config.S3Service;
import com.googledrive.file.dto.CreateFolderRequest;
import com.googledrive.file.dto.FileResponse;
import com.googledrive.file.dto.MoveRequest;
import com.googledrive.file.dto.RenameRequest;
import com.googledrive.share.FileShareRepository;
import com.googledrive.user.User;
import com.googledrive.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 파일/폴더 비즈니스 로직 서비스.
 * 기본은 readOnly 트랜잭션, 쓰기 작업에는 @Transactional을 별도 지정한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    private final FileItemRepository fileItemRepository;
    private final FileAccessRepository fileAccessRepository;
    private final FileShareRepository fileShareRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    // ──────────────────────────────────────────
    // 목록 조회
    // ──────────────────────────────────────────

    /**
     * 파일/폴더 목록 조회.
     * parentId가 null이면 내 루트 목록을 반환한다.
     * parentId가 있는데 내 폴더가 아니라면, 공유된 폴더인지 확인하고 하위 전체(owner 무관)를 반환한다.
     */
    public List<FileResponse> listFiles(Long userId, Long parentId) {
        if (parentId == null) {
            return fileItemRepository.findByOwnerIdAndParentIdIsNullAndIsTrashedFalse(userId)
                    .stream().map(FileResponse::from).collect(Collectors.toList());
        }

        FileItem parent = fileItemRepository.findById(parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "폴더를 찾을 수 없습니다"));

        // 내 폴더면 내 소유 자식만 반환
        if (parent.getOwnerId().equals(userId)) {
            return fileItemRepository.findByOwnerIdAndParentIdAndIsTrashedFalse(userId, parentId)
                    .stream().map(FileResponse::from).collect(Collectors.toList());
        }

        // 내 게 아니면 공유 접근 권한 확인
        if (!hasShareAccess(parentId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다");
        }

        // 공유된 폴더의 자식 전체 반환 (원 소유자의 파일들)
        return fileItemRepository.findByParentIdAndIsTrashedFalse(parentId)
                .stream().map(FileResponse::from).collect(Collectors.toList());
    }

    /**
     * 최근 문서함 — file_accesses 기준 최신 20개, 휴지통 제외.
     */
    public List<FileResponse> listRecent(Long userId) {
        return fileAccessRepository
                .findRecentByUserId(userId, PageRequest.of(0, 20))
                .stream()
                .map(fa -> FileResponse.from(fa.getFile()))
                .collect(Collectors.toList());
    }

    /**
     * 별표 목록 — is_starred=true, 휴지통 제외.
     */
    public List<FileResponse> listStarred(Long ownerId) {
        return fileItemRepository.findByOwnerIdAndIsStarredTrueAndIsTrashedFalse(ownerId)
                .stream().map(FileResponse::from).collect(Collectors.toList());
    }

    /**
     * 공유 문서함 — 나에게 공유된 파일 목록 (만료되지 않은 것만).
     */
    public List<FileResponse> listShared(Long userId) {
        return fileShareRepository.findActiveSharesWithUser(userId).stream()
                .map(share -> fileItemRepository.findById(share.getFileId()))
                .filter(opt -> opt.isPresent() && !opt.get().isTrashed())
                .map(opt -> FileResponse.from(opt.get()))
                .collect(Collectors.toList());
    }

    /**
     * 파일/폴더 이름 검색. keyword가 없거나 공백이면 빈 리스트 반환.
     */
    public List<FileResponse> search(Long ownerId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        return fileItemRepository.searchByName(ownerId, keyword.trim())
                .stream().map(FileResponse::from).collect(Collectors.toList());
    }

    /**
     * 휴지통 목록 — is_trashed=true.
     */
    public List<FileResponse> listTrash(Long ownerId) {
        return fileItemRepository.findByOwnerIdAndIsTrashedTrue(ownerId)
                .stream().map(FileResponse::from).collect(Collectors.toList());
    }

    // ──────────────────────────────────────────
    // 단건 조회
    // ──────────────────────────────────────────

    /**
     * 파일/폴더 상세 조회.
     * 소유자 혹은 공유 받은 사용자가 접근 가능하다.
     * 조회 시 file_accesses 테이블에 접근 기록을 upsert한다.
     */
    @Transactional
    public FileResponse getFile(Long id, Long userId) {
        FileItem file = findAndVerifyAccess(id, userId);

        // file_accesses upsert: 이미 있으면 accessed_at 업데이트, 없으면 신규 생성
        fileAccessRepository.findByFileIdAndUserId(id, userId)
                .ifPresentOrElse(
                        FileAccess::updateAccessedAt,
                        () -> fileAccessRepository.save(FileAccess.of(file, userId))
                );

        return FileResponse.from(file);
    }

    // ──────────────────────────────────────────
    // 폴더 생성
    // ──────────────────────────────────────────

    /**
     * 폴더 생성.
     */
    @Transactional
    public FileResponse createFolder(CreateFolderRequest request, Long ownerId) {
        // parentId가 있으면 부모 폴더가 실제로 존재하고 소유권이 있는지 확인
        if (request.parentId() != null) {
            findAndVerifyOwner(request.parentId(), ownerId);
        }
        FileItem folder = FileItem.createFolder(request.name(), request.parentId(), ownerId);
        return FileResponse.from(fileItemRepository.save(folder));
    }

    // ──────────────────────────────────────────
    // 파일 업로드
    // ──────────────────────────────────────────

    /**
     * 파일 업로드.
     * S3에 파일을 저장하고 반환된 s3Key로 FileItem을 생성한다.
     */
    @Transactional
    public FileResponse uploadFile(MultipartFile multipartFile, Long parentId, Long ownerId) {
        String originalFilename = multipartFile.getOriginalFilename() != null
                ? multipartFile.getOriginalFilename()
                : "unknown";
        String mimeType = multipartFile.getContentType();
        long size = multipartFile.getSize();

        // parentId가 있으면 부모 폴더 소유권 확인
        if (parentId != null) {
            findAndVerifyOwner(parentId, ownerId);
        }

        // S3에 실제 업로드 후 s3Key 획득
        String s3Key;
        try {
            s3Key = s3Service.upload(multipartFile, ownerId);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다");
        }

        FileItem file = FileItem.createFile(originalFilename, mimeType, size, s3Key, parentId, ownerId);
        FileItem saved = fileItemRepository.save(file);

        // 사용자 storage_used 증가
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));
        user.addStorageUsed(size);

        return FileResponse.from(saved);
    }

    // ──────────────────────────────────────────
    // 이름 변경 / 이동 / 복사
    // ──────────────────────────────────────────

    /**
     * 이름 변경.
     */
    @Transactional
    public FileResponse renameFile(Long id, RenameRequest request, Long userId) {
        FileItem file = findAndVerifyOwner(id, userId);
        file.rename(request.name());
        return FileResponse.from(file);
    }

    /**
     * 이동 (부모 폴더 변경).
     * parentId=null이면 루트로 이동한다.
     */
    @Transactional
    public FileResponse moveFile(Long id, MoveRequest request, Long userId) {
        FileItem file = findAndVerifyOwner(id, userId);

        // 이동 대상 폴더가 있으면 소유권 확인
        if (request.parentId() != null) {
            findAndVerifyOwner(request.parentId(), userId);
        }

        // 자기 자신으로 이동 방지
        if (id.equals(request.parentId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신 안으로 이동할 수 없습니다");
        }

        file.move(request.parentId());
        return FileResponse.from(file);
    }

    /**
     * 복사 (FILE만 지원, FOLDER 미구현).
     */
    @Transactional
    public FileResponse copyFile(Long id, Long userId) {
        FileItem original = findAndVerifyOwner(id, userId);

        if (original.getItemType() == FileItem.ItemType.FOLDER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "폴더 복사는 지원하지 않습니다");
        }

        // 파일명 앞에 "복사본_" 접두사 추가
        String copiedName = "복사본_" + original.getName();
        FileItem copy = FileItem.createFile(
                copiedName,
                original.getMimeType(),
                original.getSize(),
                original.getS3Key(),
                original.getParentId(),
                userId
        );

        // 복사된 파일의 storage_used도 증가
        if (original.getSize() != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));
            user.addStorageUsed(original.getSize());
        }

        return FileResponse.from(fileItemRepository.save(copy));
    }

    // ──────────────────────────────────────────
    // 별표 / 휴지통
    // ──────────────────────────────────────────

    /**
     * 별표 토글.
     */
    @Transactional
    public FileResponse toggleStar(Long id, Long userId) {
        FileItem file = findAndVerifyOwner(id, userId);
        file.toggleStar();
        return FileResponse.from(file);
    }

    /**
     * 휴지통으로 이동 (소프트 삭제).
     * is_trashed=true, trashed_at=현재 시각으로 설정.
     */
    @Transactional
    public void trashFile(Long id, Long userId) {
        FileItem file = findAndVerifyOwner(id, userId);
        file.trash();
    }

    // ──────────────────────────────────────────
    // 다운로드
    // ──────────────────────────────────────────

    /**
     * 다운로드 URL 반환.
     * 소유자 혹은 공유 받은 사용자가 접근 가능하다.
     * S3 Presigned URL을 생성해 반환한다 (유효 기간 15분).
     *
     * @return presigned 다운로드 URL
     */
    public String getDownloadUrl(Long id, Long userId) {
        FileItem file = findAndVerifyAccess(id, userId);
        if (file.getItemType() == FileItem.ItemType.FOLDER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "폴더는 다운로드할 수 없습니다");
        }
        if (file.getS3Key() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "다운로드할 파일 정보가 없습니다");
        }
        return s3Service.generatePresignedUrl(file.getS3Key(), file.getName());
    }

    /**
     * 미리보기 URL 반환 (이미지 등 브라우저 inline 표시용).
     * 소유자 혹은 공유 받은 사용자가 접근 가능하다.
     */
    public String getViewUrl(Long id, Long userId) {
        FileItem file = findAndVerifyAccess(id, userId);
        if (file.getItemType() == FileItem.ItemType.FOLDER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "폴더는 미리보기할 수 없습니다");
        }
        if (file.getS3Key() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "미리보기할 파일 정보가 없습니다");
        }
        return s3Service.generateViewUrl(file.getS3Key(), file.getMimeType());
    }

    // ──────────────────────────────────────────
    // 휴지통 관리
    // ──────────────────────────────────────────

    /**
     * 휴지통에서 복원.
     * is_trashed=false, trashed_at=null 로 복원한다.
     */
    @Transactional
    public FileResponse restoreFile(Long id, Long userId) {
        FileItem file = findAndVerifyOwner(id, userId);
        if (!file.isTrashed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "휴지통에 있는 파일이 아닙니다");
        }
        file.restore();
        return FileResponse.from(file);
    }

    /**
     * 영구 삭제.
     * DB에서 레코드를 삭제하고 S3에서도 파일을 제거한다.
     */
    @Transactional
    public void permanentDelete(Long id, Long userId) {
        FileItem file = findAndVerifyOwner(id, userId);

        // storage_used 감소 (파일인 경우)
        if (file.getItemType() == FileItem.ItemType.FILE && file.getSize() != null) {
            userRepository.findById(userId).ifPresent(u -> u.reduceStorageUsed(file.getSize()));
        }

        // S3 파일 삭제 (파일인 경우에만 수행)
        if (file.getItemType() == FileItem.ItemType.FILE && file.getS3Key() != null) {
            s3Service.delete(file.getS3Key());
        }

        fileItemRepository.delete(file);
    }

    /**
     * 휴지통 비우기 — 현재 사용자의 모든 trashed 항목을 영구 삭제한다.
     * DB 레코드 삭제 전 S3에서도 파일을 제거한다.
     */
    @Transactional
    public void emptyTrash(Long userId) {
        List<FileItem> trashed = fileItemRepository.findByOwnerIdAndIsTrashedTrue(userId);

        // storage_used 감소 (파일인 경우)
        long totalSize = trashed.stream()
                .filter(f -> f.getItemType() == FileItem.ItemType.FILE && f.getSize() != null)
                .mapToLong(FileItem::getSize)
                .sum();

        if (totalSize > 0) {
            userRepository.findById(userId).ifPresent(u -> u.reduceStorageUsed(totalSize));
        }

        // S3 파일 삭제 (파일인 경우에만 수행)
        trashed.stream()
                .filter(f -> f.getItemType() == FileItem.ItemType.FILE && f.getS3Key() != null)
                .forEach(f -> s3Service.delete(f.getS3Key()));

        fileItemRepository.deleteAll(trashed);
    }

    // ──────────────────────────────────────────
    // 내부 공통 헬퍼
    // ──────────────────────────────────────────

    /**
     * 파일을 조회하고 소유자 여부를 검증하는 공통 메서드.
     * 파일이 없거나 소유자가 아니면 적절한 예외를 던진다.
     * 쓰기 작업(이름 변경/이동/삭제 등)에 사용한다.
     *
     * @param fileId  파일 ID
     * @param ownerId 현재 사용자 ID
     * @return FileItem 엔티티
     * @throws ResponseStatusException 404 파일 없음 / 403 권한 없음
     */
    private FileItem findAndVerifyOwner(Long fileId, Long ownerId) {
        FileItem file = fileItemRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다"));

        if (!file.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다");
        }
        return file;
    }

    /**
     * 파일을 조회하고 읽기 권한(소유자 or 공유 받은 사용자)을 검증한다.
     * 읽기 작업(조회/다운로드/미리보기)에 사용한다.
     */
    private FileItem findAndVerifyAccess(Long fileId, Long userId) {
        FileItem file = fileItemRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다"));

        if (file.getOwnerId().equals(userId) || hasShareAccess(fileId, userId)) {
            return file;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다");
    }

    /**
     * 공유 접근 권한 확인.
     * fileId 자체 또는 그 조상 폴더 중 하나라도 userId에게 직접 공유되어 있으면 true.
     * 폴더가 공유되면 그 안의 모든 하위 파일/폴더에도 접근할 수 있도록 하기 위함이다.
     * 데이터 이상으로 인한 무한 루프 방지를 위해 최대 50단계까지만 순회한다.
     */
    private boolean hasShareAccess(Long fileId, Long userId) {
        Long current = fileId;
        int depth = 0;
        while (current != null && depth++ < 50) {
            if (fileShareRepository.existsActiveShareForUser(current, userId)) {
                return true;
            }
            Optional<FileItem> item = fileItemRepository.findById(current);
            if (item.isEmpty()) return false;
            current = item.get().getParentId();
        }
        return false;
    }
}
