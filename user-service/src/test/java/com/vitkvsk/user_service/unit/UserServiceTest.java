package com.vitkvsk.user_service.unit;

import com.vitkvsk.user_service.dto.user.UserCreateDto;
import com.vitkvsk.user_service.exception.EntityAlreadyExistsException;
import com.vitkvsk.user_service.exception.ResourceNotFoundException;
import com.vitkvsk.user_service.repository.UserRepository;
import com.vitkvsk.user_service.dto.user.UserResponseDto;
import com.vitkvsk.user_service.dto.user.UserUpdateDto;
import com.vitkvsk.user_service.entity.User;
import com.vitkvsk.user_service.mapper.UserMapper;
import com.vitkvsk.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

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
        testUser.setSurname("Dod");
        testUser.setEmail("john@example.com");
        testUser.setBirthDate(LocalDate.of(1990, Month.APRIL, 1));
        testUser.setActive(true);
    }

    @Test
    void createUser_savesAndReturnsDto() {
        UserCreateDto createDto = new UserCreateDto(
                "John", "Dod", LocalDate.of(1990, Month.APRIL, 1), "john@example.com");
        UserResponseDto responseDto = new UserResponseDto(
                testUser.getId(), testUser.getName(), testUser.getSurname(), testUser.getBirthDate(),
                testUser.getEmail(), testUser.isActive(), null, null);

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userMapper.toEntity(createDto)).thenReturn(testUser);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponseDto(testUser)).thenReturn(responseDto);

        UserResponseDto result = userService.createUser(createDto);

        assertEquals("John", result.name());
        assertEquals("john@example.com", result.email());
        verify(userRepository).save(testUser);
    }

    @Test
    void createUser_duplicateEmail_throws() {
        UserCreateDto createDto = new UserCreateDto(
                "John", "Dod", LocalDate.of(1990, Month.JANUARY, 1), "john@example.com");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(EntityAlreadyExistsException.class, () -> userService.createUser(createDto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_shouldUpdateUserSuccessfully() {
        UserUpdateDto updateDto = new UserUpdateDto(
                "Jane", "Dod", LocalDate.of(1990, Month.APRIL, 1), "jane@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
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
                    user.getEmail(), user.isActive(), user.getCreatedAt(), user.getUpdatedAt());
        });

        UserResponseDto result = userService.updateUser(1L, updateDto);

        assertEquals("Jane", result.name());
        assertEquals("jane@example.com", result.email());
        verify(userRepository).findById(1L);
        verify(userMapper).updateEntityFromDto(updateDto, testUser);
    }

    @Test
    void changeUserStatus_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> userService.changeUserStatus(999L, false));

        verify(userRepository, never()).updateActiveStatus(anyLong(), anyBoolean());
    }
}