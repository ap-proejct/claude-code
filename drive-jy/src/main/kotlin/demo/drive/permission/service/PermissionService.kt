package demo.drive.permission.service

import demo.drive.common.exception.DriveErrorCode
import demo.drive.common.exception.DriveException
import demo.drive.file.domain.FileRepository
import demo.drive.group.domain.GroupMemberRepository
import demo.drive.permission.domain.Permission
import demo.drive.permission.domain.PermissionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val MAX_INHERIT_DEPTH = 20

@Service
@Transactional(readOnly = true)
class PermissionService(
    private val fileRepository: FileRepository,
    private val permissionRepository: PermissionRepository,
    private val groupMemberRepository: GroupMemberRepository,
) {
    /**
     * 파일에 대해 userId가 갖는 최종 권한 반환. 없으면 null.
     */
    fun resolvePermission(fileId: Long, userId: Long): Permission? {
        val file = fileRepository.findById(fileId) ?: return null

        // 1. 소유자 → OWNER 즉시 반환
        if (file.ownerId == userId) return Permission.OWNER

        // 2. 만료되지 않은 직접 부여 권한
        val direct = permissionRepository
            .findActiveByFileIdAndUserId(fileId, userId)?.permission

        // 3. 사용자가 속한 그룹 경유 권한 (가장 높은 것)
        val myGroupIds = groupMemberRepository.findGroupIdsByUserId(userId)
        val viaGroup = if (myGroupIds.isEmpty()) null else
            permissionRepository
                .findActiveByFileIdAndGroupIdIn(fileId, myGroupIds)
                .maxOfOrNull { it.permission }

        // 4. 직접·그룹 중 높은 것 선택
        val effective = listOfNotNull(direct, viaGroup).maxOrNull()

        // 5. 없으면 부모 폴더 상속 확인 (재귀, 최대 깊이 제한)
        if (effective == null && file.parentId != null) {
            return resolveInheritedPermission(file.parentId!!, userId, myGroupIds, depth = 1)
        }

        return effective
    }

    private fun resolveInheritedPermission(
        folderId: Long, userId: Long, groupIds: List<Long>, depth: Int,
    ): Permission? {
        if (depth > MAX_INHERIT_DEPTH) return null

        val folder = fileRepository.findById(folderId) ?: return null
        if (folder.ownerId == userId) return Permission.OWNER

        val inherited = permissionRepository
            .findInheritableByFileIdAndTargets(folderId, userId, groupIds)
            .maxOfOrNull { it.permission }

        return inherited
            ?: folder.parentId?.let {
                resolveInheritedPermission(it, userId, groupIds, depth + 1)
            }
    }

    /**
     * required 이상 권한이 없으면 DriveException.AccessDenied 던지기
     */
    fun requirePermission(fileId: Long, userId: Long, required: Permission) {
        val actual = resolvePermission(fileId, userId)
            ?: throw DriveException(DriveErrorCode.ACCESS_DENIED)
        if (actual < required)
            throw DriveException(DriveErrorCode.PERMISSION_REQUIRED, "${required.name} 권한이 필요합니다.")
    }

    /**
     * 공유 토큰 생성 (64자 hex)
     */
    fun generateShareToken(): String {
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
