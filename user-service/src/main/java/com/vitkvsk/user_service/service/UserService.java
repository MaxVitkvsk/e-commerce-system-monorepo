package com.vitkvsk.user_service.service;

import com.vitkvsk.user_service.exception.ResourceNotFoundException;
import com.vitkvsk.user_service.repository.PaymentCardRepository;
import com.vitkvsk.user_service.repository.UserRepository;

import com.vitkvsk.user_service.dto.paymentcard.PaymentCardCreateDto;
import com.vitkvsk.user_service.dto.paymentcard.PaymentCardResponseDto;
import com.vitkvsk.user_service.dto.user.UserCreateDto;
import com.vitkvsk.user_service.dto.user.UserResponseDto;
import com.vitkvsk.user_service.dto.user.UserUpdateDto;
import com.vitkvsk.user_service.entity.PaymentCard;
import com.vitkvsk.user_service.entity.User;
import com.vitkvsk.user_service.exception.CardLimitExceededException;
import com.vitkvsk.user_service.mapper.PaymentCardMapper;
import com.vitkvsk.user_service.mapper.UserMapper;
import com.vitkvsk.user_service.specification.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String CACHE = "usersWithCards";

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponseDto createUser(UserCreateDto dto) {
        User saved = userRepository.save(userMapper.toEntity(dto));
        return userMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE, key = "#id")
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findByIdWithCards(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toResponseDto(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> getAllUsers(String name, String surname, Pageable pageable) {
        Specification<User> spec = Specification.where(UserSpecifications.hasName(name))
                .and(UserSpecifications.hasSurname(surname));
        return userRepository.findAll(spec, pageable).map(userMapper::toResponseDto);
    }

    @Transactional
    @CacheEvict(value = CACHE, key = "#id")
    public UserResponseDto updateUser(Long id, UserUpdateDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userMapper.updateEntityFromDto(dto, user);
        return userMapper.toResponseDto(user);
    }

    @Transactional
    @CacheEvict(value = CACHE, key = "#id")
    public void changeUserStatus(Long id, boolean active) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.updateActiveStatus(id, active);
    }

    @Transactional
    @CacheEvict(value = CACHE, key = "#id")
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
