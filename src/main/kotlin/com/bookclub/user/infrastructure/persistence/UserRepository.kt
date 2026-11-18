package com.bookclub.user.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByTelegramUserId(telegramUserId: Long): UserEntity?
}
