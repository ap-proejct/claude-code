package demo.drive.permission.domain

enum class Permission : Comparable<Permission> {
    VIEWER, EDITOR, OWNER   // ordinal 순서 = 우선순위 (VIEWER < EDITOR < OWNER)
}
