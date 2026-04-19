package demo.drive.file.domain

import jakarta.persistence.*

@Entity
@Table(name = "files")
class File(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var name: String,

    @Column(name = "owner_id", nullable = false)
    val ownerId: Long,

    @Column(name = "parent_id")
    var parentId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: FileType,

    @Column(name = "mime_type")
    val mimeType: String? = null,          // 폴더면 null

    @Column(name = "storage_path")
    val storagePath: String? = null,       // 폴더면 null

    @Column(name = "size_bytes", nullable = false)
    var sizeBytes: Long = 0L,

    @Column(name = "is_trashed", nullable = false)
    var isTrashed: Boolean = false,

    @Column(name = "trashed_at")
    var trashedAt: java.time.Instant? = null,

    @Column(name = "is_starred", nullable = false)
    var isStarred: Boolean = false,
) {
    val iconName: String get() = when {
        type == FileType.FOLDER -> "folder"
        mimeType?.startsWith("image/") == true -> "image"
        mimeType?.startsWith("video/") == true -> "video"
        mimeType?.startsWith("audio/") == true -> "audio"
        mimeType == "application/pdf" -> "pdf"
        mimeType?.contains("spreadsheet") == true || mimeType?.contains("excel") == true -> "sheet"
        mimeType?.contains("presentation") == true -> "slides"
        mimeType?.contains("word") == true || mimeType?.contains("document") == true -> "doc"
        else -> "file"
    }
}
