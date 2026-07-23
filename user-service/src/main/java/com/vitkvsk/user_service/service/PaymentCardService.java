
package com.vitkvsk.user_service.service;

import com.vitkvsk.user_service.dao.PaymentCardRepository;
import com.vitkvsk.user_service.dao.UserRepository;
import com.vitkvsk.user_service.dto.PaymentCardCreateDto;
import com.vitkvsk.user_service.dto.PaymentCardResponseDto;
import com.vitkvsk.user_service.dto.PaymentCardUpdateDto;
import com.vitkvsk.user_service.entities.PaymentCard;
import com.vitkvsk.user_service.entities.User;
import com.vitkvsk.user_service.exception.CardLimitExceededException;
import com.vitkvsk.user_service.mapper.PaymentCardMapper;
import com.vitkvsk.user_service.specification.CardSpecifications;
import lombok.RequiredArgsConstructor;
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

    private final PaymentCardRepository cardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper cardMapper;

    @Transactional
    public PaymentCardResponseDto createCard(PaymentCardCreateDto dto) {
        long cardCount = cardRepository.countByUserId(dto.userId());
        if (cardCount >= User.MAX_CARDS) {
            throw new CardLimitExceededException(User.MAX_CARDS);
        }

        User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found with id: " + dto.userId()));

        PaymentCard card = cardMapper.toEntity(dto);
        card.setUser(user);

        PaymentCard savedCard = cardRepository.save(card);
        return cardMapper.toResponseDto(savedCard);
    }

    @Transactional(readOnly = true)
    public PaymentCardResponseDto getCardById(Long id) {
        return cardRepository.findById(id)
                .map(cardMapper::toResponseDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Card not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<PaymentCardResponseDto> getCardsByUserId(Long userId) {
        return cardRepository.findAllByUserId(userId).stream()
                .map(cardMapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PaymentCardResponseDto> getAllCards(String holderName, String holderSurname, Pageable pageable) {
        Specification<PaymentCard> spec = Specification.where((Specification<PaymentCard>) null);

        if (holderName != null && !holderName.isBlank()) {
            spec = spec.and(CardSpecifications.hasUserName(holderName));
        }
        if (holderSurname != null && !holderSurname.isBlank()) {
            spec = spec.and(CardSpecifications.hasUserSurname(holderSurname));
        }

        return cardRepository.findAll(spec, pageable).map(cardMapper::toResponseDto);
    }

    @Transactional
    public PaymentCardResponseDto updateCard(Long id, PaymentCardUpdateDto dto) {
        PaymentCard card = cardRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Card not found with id: " + id));

        cardMapper.updateEntityFromDto(dto, card);

        return cardMapper.toResponseDto(card);
    }

    @Transactional
    public void updateActiveStatus(Long id, boolean active) {
        if (!cardRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Card not found with id: " + id);
        }

        cardRepository.updateActiveStatus(active, id);
    }

    @Transactional
    public void deleteCard(Long id) {
        if (!cardRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Card not found with id: " + id);
        }
        cardRepository.deleteById(id);
    }
}