package com.bookclub.book.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "books")
class BookEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 500)
    var title: String = "",

    @Column(length = 500)
    var author: String? = null,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(name = "page_count")
    var pageCount: Int? = null,

    @Column(name = "cover_url", columnDefinition = "text")
    var coverUrl: String? = null,

    @Column(name = "external_id")
    var externalId: String? = null,

    @Column(name = "external_source")
    var externalSource: String? = null,

    @Column(length = 20)
    var isbn: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "metadata_status", nullable = false, length = 20)
    var metadataStatus: BookMetadataStatus = BookMetadataStatus.NEW,

    @Column(name = "description_source", columnDefinition = "text")
    var descriptionSource: String? = null,

    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    @Column(name = "metadata_updated_at")
    var metadataUpdatedAt: Instant? = null,

    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    @Column(
        name = "created_at",
        nullable = false,
        insertable = false,
        updatable = false
    )
    var createdAt: Instant? = null
)