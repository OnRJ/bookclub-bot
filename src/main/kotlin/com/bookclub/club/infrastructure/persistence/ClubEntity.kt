
package com.bookclub.club.infrastructure.persistence

import jakarta.persistence.*

@Entity
@Table(name = "clubs")
class ClubEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "telegram_chat_id", nullable = false, unique = true)
    var telegramChatId: Long = 0,

    @Column(nullable = false)
    var name: String = ""
)
