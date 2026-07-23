package com.vitkvsk.user_service.unit;

import com.vitkvsk.user_service.dao.PaymentCardRepository;
import com.vitkvsk.user_service.dao.UserRepository;
import com.vitkvsk.user_service.dto.PaymentCardCreateDto;
import com.vitkvsk.user_service.dto.UserCreateDto;
import com.vitkvsk.user_service.dto.UserResponseDto;
import com.vitkvsk.user_service.dto.UserUpdateDto;
import com.vitkvsk.user_service.entities.PaymentCard;
import com.vitkvsk.user_service.entities.User;
import com.vitkvsk.user_service.exception.CardLimitExceededException;
import com.vitkvsk.user_service.mapper.PaymentCardMapper;
import com.vitkvsk.user_service.mapper.UserMapper;
import com.vitkvsk.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

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
    }

    @Test
    void addCardToUser_shouldThrowExceptionWhenCardLimitExceeded() {
        Long userId = 1L;
        PaymentCardCreateDto cardDto = new PaymentCardCreateDto(userId, "1234567890123456", "John Doe", LocalDate.of(2025, 12, 31));

        when(paymentCardRepository.countByUserId(userId)).thenReturn((long) User.MAX_CARDS);

        assertThrows(CardLimitExceededException.class, () -> userService.addCardToUser(userId, cardDto));
        verify(paymentCardRepository, never()).save(any(PaymentCard.class));
    }

    @Test
    void updateUser_shouldUpdateUserSuccessfully() {
        Long userId = 1L;
        UserUpdateDto updateDto = new UserUpdateDto("Jane", "Doe", LocalDate.of(1990, 1, 1), "jane@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        doAnswer(invocation -> {
            UserUpdateDto dto = invocation.getArgument(0);
            User user = invocation.getArgument(1);
            user.setName(dto.name());
            user.setSurname(dto.surname());
            user.setBirthDate(dto.birthDate());
            user.setEmail(dto.email());
            return null;
        }).when(userMapper).updateEntityFromDto(any(UserUpdateDto.class), any(User.class));

        when(userMapper.toResponseDto(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return new UserResponseDto(
                    user.getId(), user.getName(), user.getSurname(), user.getBirthDate(),
                    user.getEmail(), user.isActive(), user.getCreatedAt(), user.getUpdatedAt()
            );
        });

        var result = userService.updateUser(userId, updateDto);

        assertNotNull(result);
        assertEquals("Jane", result.name());
        assertEquals("jane@example.com", result.email());
        verify(userRepository).findById(userId);
        verify(userMapper).updateEntityFromDto(eq(updateDto), eq(testUser));
    }

    @Test
    void changeUserStatus_shouldThrowExceptionWhenUserNotFound() {
        Long userId = 999L;
        when(userRepository.existsById(userId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.changeUserStatus(userId, false));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(userRepository, never()).updateActiveStatus(anyLong(), anyBoolean());
    }
}