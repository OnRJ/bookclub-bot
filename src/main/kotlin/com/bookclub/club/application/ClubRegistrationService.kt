package com.bookclub.club.application

import com.bookclub.club.infrastructure.persistence.ClubEntity
import com.bookclub.club.infrastructure.persistence.ClubRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClubRegistrationService(
    private val clubRepository: ClubRepository
) {

    @Transactional
    fun registerOrActivate(
        telegramChatId: Long,
        name: String
    ) {
        val club = clubRepository.findByTelegramChatId(telegramChatId)

        if (club == null) {
            clubRepository.save(
                ClubEntity(
                    telegramChatId = telegramChatId,
                    name = name
                )
            )
        } else {
            club.name = name
            clubRepository.save(club)
        }
    }
}