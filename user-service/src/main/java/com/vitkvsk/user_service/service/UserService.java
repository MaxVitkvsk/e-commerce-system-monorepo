package com.vitkvsk.user_service.service;

import com.vitkvsk.user_service.exception.ResourceNotFoundException;
import com.vitkvsk.user_service.repository.UserRepository;
import com.vitkvsk.user_service.dto.user.UserCreateDto;
import com.vitkvsk.user_service.dto.user.UserResponseDto;
import com.vitkvsk.user_service.dto.user.UserUpdateDto;
import com.vitkvsk.user_service.entity.User;
import com.vitkvsk.user_service.mapper.UserMapper;
import com.vitkvsk.user_service.specification.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String CACHE = "usersWithCards";
    private static final String USER_NOT_FOUND = "User not found with id: ";

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
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));
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
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));
        userMapper.updateEntityFromDto(dto, user);
        return userMapper.toResponseDto(user);
    }

    @Transactional
    @CacheEvict(value = CACHE, key = "#id")
    public void changeUserStatus(Long id, boolean active) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException(USER_NOT_FOUND + id);
        }
        userRepository.updateActiveStatus(id, active);
    }

    @Transactional
    @CacheEvict(value = CACHE, key = "#id")
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException(USER_NOT_FOUND + id);
        }
        userRepository.deleteById(id);
    }
}
