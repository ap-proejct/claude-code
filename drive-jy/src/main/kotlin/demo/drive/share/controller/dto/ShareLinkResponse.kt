package demo.drive.share.controller.dto

import demo.drive.permission.domain.FilePermission
import java.time.Instant

data class ShareLinkResponse(
    val token: String,
    val fileId: Long,
    val permission: String,
    val expiresAt: Instant?,
) {
    companion object {
        fun from(fp: FilePermission) = ShareLinkResponse(
            token = fp.shareToken ?: "",
            fileId = fp.fileId,
            permission = fp.permission.name,
            expiresAt = fp.expiresAt,
        )
    }
}
