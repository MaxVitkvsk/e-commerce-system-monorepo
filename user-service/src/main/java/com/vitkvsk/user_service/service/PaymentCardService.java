package com.vitkvsk.user_service.service;

import com.vitkvsk.user_service.cache.UserCacheEvictor;
import com.vitkvsk.user_service.exception.EntityAlreadyExistsException;
import com.vitkvsk.user_service.exception.ResourceNotFoundException;
import com.vitkvsk.user_service.repository.PaymentCardRepository;
import com.vitkvsk.user_service.repository.UserRepository;
import com.vitkvsk.user_service.dto.paymentcard.PaymentCardCreateDto;
import com.vitkvsk.user_service.dto.paymentcard.PaymentCardResponseDto;
import com.vitkvsk.user_service.dto.paymentcard.PaymentCardUpdateDto;
import com.vitkvsk.user_service.entity.PaymentCard;
import com.vitkvsk.user_service.entity.User;
import com.vitkvsk.user_service.exception.CardLimitExceededException;
import com.vitkvsk.user_service.mapper.PaymentCardMapper;
import com.vitkvsk.user_service.specification.CardSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCardService {

    private static final String CARD_NOT_FOUND = "Card not found with id: ";
    private static final String USER_NOT_FOUND = "User not found with id: ";

    private final PaymentCardRepository cardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper cardMapper;
    private final UserCacheEvictor userCacheEvictor;

    @Transactional
    public PaymentCardResponseDto createCard(PaymentCardCreateDto dto) {
        log.debug("Creating card for userId={}", dto.userId());

        User user = userRepository.findByIdWithCards(dto.userId())
                .orElseThrow(() -> {
                    log.debug("User not found for card creation: userId={}", dto.userId());
                    return new ResourceNotFoundException(USER_NOT_FOUND + dto.userId());
                });

        if (user.getCards().size() >= User.MAX_CARDS) {
            log.warn("Card limit exceeded: userId={}, limit={}", dto.userId(), User.MAX_CARDS);
            throw new CardLimitExceededException(User.MAX_CARDS);
        }

        if (cardRepository.existsByNumber(dto.number())) {
            log.warn("Rejected card creation: card number already exists");
            throw new EntityAlreadyExistsException("Card with number '" + dto.number() + "' already exists");
        }

        PaymentCard card = cardMapper.toEntity(dto);
        card.setUser(user);
        user.getCards().add(card);

        PaymentCard saved = cardRepository.save(card);
        log.info("Card created: id={}, userId={}", saved.getId(), dto.userId());
        userCacheEvictor.evict(dto.userId());
        return cardMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public PaymentCardResponseDto getCardById(Long id) {
        log.debug("Fetching card by id={}", id);
        return cardRepository.findById(id)
                .map(cardMapper::toResponseDto)
                .orElseThrow(() -> {
                    log.debug("Card not found: id={}", id);
                    return new ResourceNotFoundException(CARD_NOT_FOUND + id);
                });
    }

    @Transactional(readOnly = true)
    public List<PaymentCardResponseDto> getCardsByUserId(Long userId) {
        log.debug("Fetching cards for userId={}", userId);
        return cardRepository.findAllByUserId(userId).stream()
                .map(cardMapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PaymentCardResponseDto> getAllCards(String holderName, String holderSurname, Pageable pageable) {
        log.debug("Fetching cards: holderName={}, holderSurname={}, page={}",
                holderName, holderSurname, pageable.getPageNumber());
        Specification<PaymentCard> spec = Specification.where(CardSpecifications.hasUserName(holderName))
                .and(CardSpecifications.hasUserSurname(holderSurname));
        return cardRepository.findAll(spec, pageable).map(cardMapper::toResponseDto);
    }

    @Transactional
    public PaymentCardResponseDto updateCard(Long id, PaymentCardUpdateDto dto) {
        log.info("Updating card: id={}", id);
        PaymentCard card = cardRepository.findById(id)
                .orElseThrow(() -> {
                    log.debug("Card not found for update: id={}", id);
                    return new ResourceNotFoundException(CARD_NOT_FOUND + id);
                });
        Long userId = card.getUser().getId();

        cardMapper.updateEntityFromDto(dto, card);

        userCacheEvictor.evict(userId);
        return cardMapper.toResponseDto(card);
    }

    @Transactional
    public void updateActiveStatus(Long id, boolean active) {
        log.info("Changing card status: id={}, active={}", id, active);
        PaymentCard card = cardRepository.findById(id)
                .orElseThrow(() -> {
                    log.debug("Card not found for status change: id={}", id);
                    return new ResourceNotFoundException(CARD_NOT_FOUND + id);
                });
        Long userId = card.getUser().getId();

        cardRepository.updateActiveStatus(active, id);

        userCacheEvictor.evict(userId);
    }

    @Transactional
    public void deleteCard(Long id) {
        log.info("Deleting card: id={}", id);
        PaymentCard card = cardRepository.findById(id)
                .orElseThrow(() -> {
                    log.debug("Card not found for delete: id={}", id);
                    return new ResourceNotFoundException(CARD_NOT_FOUND + id);
                });
        Long userId = card.getUser().getId();

        cardRepository.deleteById(id);
        log.info("Card deleted: id={}, userId={}", id, userId);
        userCacheEvictor.evict(userId);
    }
}