package demo.drive.share.controller.dto

import java.time.Instant

data class ShareLinkRequest(
    val permission: String = "VIEWER",
    val expiresAt: Instant? = null,
)
