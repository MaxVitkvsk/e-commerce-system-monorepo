package com.vitkvsk.user_service.unit;

import com.vitkvsk.user_service.cache.UserCacheEvictor;
import com.vitkvsk.user_service.exception.EntityAlreadyExistsException;
import com.vitkvsk.user_service.repository.PaymentCardRepository;
import com.vitkvsk.user_service.dto.paymentcard.PaymentCardCreateDto;
import com.vitkvsk.user_service.dto.paymentcard.PaymentCardResponseDto;
import com.vitkvsk.user_service.dto.paymentcard.PaymentCardUpdateDto;
import com.vitkvsk.user_service.entity.PaymentCard;
import com.vitkvsk.user_service.entity.User;
import com.vitkvsk.user_service.exception.CardLimitExceededException;
import com.vitkvsk.user_service.mapper.PaymentCardMapper;
import com.vitkvsk.user_service.repository.UserRepository;
import com.vitkvsk.user_service.service.PaymentCardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {

    @Mock private PaymentCardRepository cardRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentCardMapper cardMapper;
    @Mock private UserCacheEvictor userCacheEvictor;
    @InjectMocks private PaymentCardService paymentCardService;

    private final PaymentCardCreateDto createDto = new PaymentCardCreateDto(
            1L, "1234567890123456", "JOHN DOD", LocalDate.of(2030, Month.DECEMBER, 31));

    private PaymentCard testCard;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("John")
                .surname("Dod")
                .email("john@example.com")
                .birthDate(LocalDate.of(1990, Month.APRIL, 1))
                .active(true)
                .build();

        testCard = PaymentCard.builder()
                .id(1L)
                .number("1234567890123456")
                .holder("John Dod")
                .expirationDate(LocalDate.of(2028, Month.APRIL, 28))
                .active(true)
                .user(testUser)
                .build();
    }

    private User userWithCards(int count) {
        List<PaymentCard> cards = new ArrayList<>();
        IntStream.range(0, count).forEach(i -> cards.add(PaymentCard.builder().id((long) i).build()));
        return User.builder().id(1L).cards(cards).build();
    }

    @Test
    void createCard_duplicateNumber_throws() {
        when(userRepository.findByIdWithCards(1L)).thenReturn(Optional.of(testUser));
        when(cardRepository.existsByNumber(createDto.number())).thenReturn(true);

        assertThrows(EntityAlreadyExistsException.class, () -> paymentCardService.createCard(createDto));
    }

    @Test
    void createCard_shouldThrowExceptionWhenCardLimitExceeded() {
        when(userRepository.findByIdWithCards(1L)).thenReturn(Optional.of(userWithCards(User.MAX_CARDS)));

        assertThrows(CardLimitExceededException.class, () -> paymentCardService.createCard(createDto));
        verify(cardRepository, never()).save(any(PaymentCard.class));
    }

    @Test
    void updateCard_shouldEvictUserCacheOnSuccess() {
        Long cardId = 1L;
        PaymentCardUpdateDto dto = new PaymentCardUpdateDto("Jane Dod", LocalDate.of(2027, Month.APRIL, 28), true);

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

        var result = paymentCardService.updateCard(cardId, dto);

        assertNotNull(result);
        assertEquals("Jane Dod", result.holder());
        assertEquals(LocalDate.of(2027, Month.APRIL, 28), result.expirationDate());
        verify(userCacheEvictor).evict(1L);
        verify(cardMapper).updateEntityFromDto(dto, testCard);
    }

    @Test
    void deleteCard_shouldEvictUserCacheOnSuccess() {
        Long cardId = 1L;
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));

        paymentCardService.deleteCard(cardId);

        verify(cardRepository).deleteById(cardId);
        verify(userCacheEvictor).evict(1L);
    }
}
