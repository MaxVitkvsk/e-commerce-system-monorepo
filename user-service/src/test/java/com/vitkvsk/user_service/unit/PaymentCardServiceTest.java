package com.vitkvsk.user_service.unit;

import com.vitkvsk.user_service.repository.PaymentCardRepository;
import com.vitkvsk.user_service.dto.paymentcard.PaymentCardCreateDto;
import com.vitkvsk.user_service.dto.paymentcard.PaymentCardResponseDto;
import com.vitkvsk.user_service.dto.paymentcard.PaymentCardUpdateDto;
import com.vitkvsk.user_service.entity.PaymentCard;
import com.vitkvsk.user_service.entity.User;
import com.vitkvsk.user_service.exception.CardLimitExceededException;
import com.vitkvsk.user_service.mapper.PaymentCardMapper;
import com.vitkvsk.user_service.service.PaymentCardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {

    @Mock
    private PaymentCardRepository cardRepository;

    @Mock
    private PaymentCardMapper cardMapper;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private PaymentCardService paymentCardService;

    private PaymentCard testCard;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("John")
                .surname("Dod")
                .email("john@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .active(true)
                .build();

        testCard = PaymentCard.builder()
                .id(1L)
                .number("1234567890123456")
                .holder("John Dod")
                .expirationDate(LocalDate.of(2028, 12, 31))
                .active(true)
                .user(testUser)
                .build();
    }

    @Test
    void createCard_shouldThrowExceptionWhenCardLimitExceeded() {
        PaymentCardCreateDto dto = new PaymentCardCreateDto(1L, "1234567890123456", "John Dod", LocalDate.of(2027, 12, 31));

        when(cardRepository.countByUserId(1L)).thenReturn((long) User.MAX_CARDS);

        assertThrows(CardLimitExceededException.class, () -> paymentCardService.createCard(dto));
        verify(cardRepository, never()).save(any(PaymentCard.class));
    }

    @Test
    void updateCard_shouldEvictUserCacheOnSuccess() {
        Long cardId = 1L;
        PaymentCardUpdateDto dto = new PaymentCardUpdateDto("Jane Dod", LocalDate.of(2027, 12, 31), true);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        doAnswer(invocation -> {
            PaymentCardUpdateDto updateDto = invocation.getArgument(0);
            PaymentCard card = invocation.getArgument(1);
            card.setHolder(updateDto.holder());
            card.setExpirationDate(updateDto.expirationDate());
            if (updateDto.active() != null) {
                card.setActive(updateDto.active());
            }
            return null;
        }).when(cardMapper).updateEntityFromDto(any(PaymentCardUpdateDto.class), any(PaymentCard.class));

        when(cardMapper.toResponseDto(any(PaymentCard.class))).thenAnswer(invocation -> {
            PaymentCard card = invocation.getArgument(0);
            return new PaymentCardResponseDto(
                    card.getId(), card.getUser().getId(), card.getNumber(),
                    card.getHolder(), card.getExpirationDate(), card.isActive()
            );
        });

        when(cacheManager.getCache("usersWithCards")).thenReturn(cache);

        var result = paymentCardService.updateCard(cardId, dto);

        assertNotNull(result);
        assertEquals("Jane Dod", result.holder());
        assertEquals(LocalDate.of(2027, 12, 31), result.expirationDate());
        verify(cache).evict(1L);
        verify(cardMapper).updateEntityFromDto(eq(dto), eq(testCard));
    }

    @Test
    void deleteCard_shouldEvictUserCacheOnSuccess() {
        Long cardId = 1L;
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(cacheManager.getCache("usersWithCards")).thenReturn(cache);

        paymentCardService.deleteCard(cardId);

        verify(cardRepository).deleteById(cardId);
        verify(cache).evict(1L);
    }
}
