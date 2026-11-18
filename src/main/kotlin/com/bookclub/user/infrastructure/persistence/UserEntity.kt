
package com.bookclub.user.infrastructure.persistence

import jakarta.persistence.*

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "telegram_user_id", nullable = false, unique = true)
    var telegramUserId: Long = 0,

    @Column
    var username: String? = null,

    @Column(name = "first_name", nullable = false)
    var firstName: String = ""
)
