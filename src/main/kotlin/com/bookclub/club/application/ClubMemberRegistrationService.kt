package com.bookclub.club.application

import com.bookclub.club.infrastructure.persistence.ClubMemberEntity
import com.bookclub.club.infrastructure.persistence.ClubMemberRepository
import com.bookclub.club.infrastructure.persistence.ClubRepository
import com.bookclub.user.infrastructure.persistence.UserEntity
import com.bookclub.user.infrastructure.persistence.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClubMemberRegistrationService(
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun registerMember(
        telegramChatId: Long,
        telegramUserId: Long,
        username: String?,
        firstName: String
    ) {
        val club = clubRepository.findByTelegramChatId(telegramChatId)
            ?: return

        var user = userRepository.findByTelegramUserId(telegramUserId)

        if (user == null) {
            user = userRepository.save(
                UserEntity(
                    telegramUserId = telegramUserId,
                    username = username,
                    firstName = firstName
                )
            )
        } else {
            user.username = username
            user.firstName = firstName
            user = userRepository.save(user)
        }

        val alreadyRegistered =
            clubMemberRepository.existsByClubTelegramChatIdAndUserTelegramUserId(
                telegramChatId,
                telegramUserId
            )

        if (!alreadyRegistered) {
            clubMemberRepository.save(
                ClubMemberEntity(
                    club = club,
                    user = user
                )
            )
        }
    }
}