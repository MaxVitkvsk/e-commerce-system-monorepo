package com.vitkvsk.user_service.unit;

import com.vitkvsk.user_service.dao.PaymentCardRepository;
import com.vitkvsk.user_service.dao.UserRepository;
import com.vitkvsk.user_service.dto.PaymentCardCreateDto;
import com.vitkvsk.user_service.dto.PaymentCardResponseDto;
import com.vitkvsk.user_service.dto.PaymentCardUpdateDto;
import com.vitkvsk.user_service.entities.PaymentCard;
import com.vitkvsk.user_service.entities.User;
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
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John");
        testUser.setSurname("Doe");
        testUser.setEmail("john@example.com");
        testUser.setBirthDate(LocalDate.of(1990, 1, 1));
        testUser.setActive(true);

        testCard = new PaymentCard();
        testCard.setId(1L);
        testCard.setNumber("1234567890123456");
        testCard.setHolder("John Doe");
        testCard.setExpirationDate(LocalDate.of(2025, 12, 31));
        testCard.setActive(true);
        testCard.setUser(testUser);
    }

    @Test
    void createCard_shouldThrowExceptionWhenCardLimitExceeded() {
        PaymentCardCreateDto dto = new PaymentCardCreateDto(1L, "1234567890123456", "John Doe", LocalDate.of(2025, 12, 31));

        when(cardRepository.countByUserId(1L)).thenReturn((long) User.MAX_CARDS);

        assertThrows(CardLimitExceededException.class, () -> paymentCardService.createCard(dto));
        verify(cardRepository, never()).save(any(PaymentCard.class));
    }

    @Test
    void updateCard_shouldEvictUserCacheOnSuccess() {
        Long cardId = 1L;
        PaymentCardUpdateDto dto = new PaymentCardUpdateDto("Jane Doe", LocalDate.of(2026, 12, 31), true);

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
        assertEquals("Jane Doe", result.holder());
        assertEquals(LocalDate.of(2026, 12, 31), result.expirationDate());
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
