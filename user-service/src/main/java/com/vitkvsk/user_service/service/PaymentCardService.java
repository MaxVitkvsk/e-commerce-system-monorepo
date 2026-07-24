package com.vitkvsk.user_service.service;

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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentCardService {

    private static final String CACHE = "usersWithCards";

    private final PaymentCardRepository cardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper cardMapper;
    private final CacheManager cacheManager;

    private void evictUserCache(Long userId) {
        Cache cache = cacheManager.getCache(CACHE);
        if (cache != null) {
            cache.evict(userId);
        }
    }

    @Transactional
    public PaymentCardResponseDto createCard(PaymentCardCreateDto dto) {
        if (cardRepository.countByUserId(dto.userId()) >= User.MAX_CARDS) {
            throw new CardLimitExceededException(User.MAX_CARDS);
        }
        User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.userId()));

        PaymentCard card = cardMapper.toEntity(dto);
        card.setUser(user);

        PaymentCard saved = cardRepository.save(card);
        evictUserCache(dto.userId());
        return cardMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public PaymentCardResponseDto getCardById(Long id) {
        return cardRepository.findById(id)
                .map(cardMapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<PaymentCardResponseDto> getCardsByUserId(Long userId) {
        return cardRepository.findAllByUserId(userId).stream()
                .map(cardMapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PaymentCardResponseDto> getAllCards(String holderName, String holderSurname, Pageable pageable) {
        Specification<PaymentCard> spec = Specification.where(CardSpecifications.hasUserName(holderName))
                .and(CardSpecifications.hasUserSurname(holderSurname));
        return cardRepository.findAll(spec, pageable).map(cardMapper::toResponseDto);
    }

    @Transactional
    public PaymentCardResponseDto updateCard(Long id, PaymentCardUpdateDto dto) {
        PaymentCard card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));
        Long userId = card.getUser().getId();

        cardMapper.updateEntityFromDto(dto, card);

        evictUserCache(userId);
        return cardMapper.toResponseDto(card);
    }

    @Transactional
    public void updateActiveStatus(Long id, boolean active) {
        PaymentCard card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));
        Long userId = card.getUser().getId();

        cardRepository.updateActiveStatus(active, id);

        evictUserCache(userId);
    }

    @Transactional
    public void deleteCard(Long id) {
        PaymentCard card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));
        Long userId = card.getUser().getId();

        cardRepository.deleteById(id);

        evictUserCache(userId);
    }
}