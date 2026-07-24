package com.vitkvsk.user_service.service;

import com.vitkvsk.user_service.cache.UserCacheEvictor;
import com.vitkvsk.user_service.exception.EntityAlreadyExistsException;
import com.vitkvsk.user_service.exception.ResourceNotFoundException;
import com.vitkvsk.user_service.repository.UserRepository;
import com.vitkvsk.user_service.dto.user.UserCreateDto;
import com.vitkvsk.user_service.dto.user.UserResponseDto;
import com.vitkvsk.user_service.dto.user.UserUpdateDto;
import com.vitkvsk.user_service.entity.User;
import com.vitkvsk.user_service.mapper.UserMapper;
import com.vitkvsk.user_service.specification.UserSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String USER_NOT_FOUND = "User not found with id: ";

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponseDto createUser(UserCreateDto dto) {
        log.debug("Creating user: name={}, surname={}", dto.name(), dto.surname());

        if (userRepository.existsByEmail(dto.email())) {
            log.warn("Rejected user creation: email already exists");
            throw new EntityAlreadyExistsException("User with email '" + dto.email() + "' already exists");
        }

        User saved = userRepository.save(userMapper.toEntity(dto));
        log.info("User created: id={}", saved.getId());
        return userMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = UserCacheEvictor.CACHE_NAME, key = "#id")
    public UserResponseDto getUserById(Long id) {
        log.debug("Fetching user by id={}", id);
        User user = userRepository.findByIdWithCards(id)
                .orElseThrow(() -> {
                    log.debug("User not found: id={}", id);
                    return new ResourceNotFoundException(USER_NOT_FOUND + id);
                });
        return userMapper.toResponseDto(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> getAllUsers(String name, String surname, Pageable pageable) {
        log.debug("Fetching users: name={}, surname={}, page={}", name, surname, pageable.getPageNumber());
        Specification<User> spec = Specification.where(UserSpecifications.hasName(name))
                .and(UserSpecifications.hasSurname(surname));
        return userRepository.findAll(spec, pageable).map(userMapper::toResponseDto);
    }

    @Transactional
    @CacheEvict(value = UserCacheEvictor.CACHE_NAME, key = "#id")
    public UserResponseDto updateUser(Long id, UserUpdateDto dto) {
        log.info("Updating user: id={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.debug("User not found for update: id={}", id);
                    return new ResourceNotFoundException(USER_NOT_FOUND + id);
                });
        userMapper.updateEntityFromDto(dto, user);
        return userMapper.toResponseDto(user);
    }

    @Transactional
    @CacheEvict(value = UserCacheEvictor.CACHE_NAME, key = "#id")
    public void changeUserStatus(Long id, boolean active) {
        log.info("Changing user status: id={}, active={}", id, active);
        if (!userRepository.existsById(id)) {
            log.debug("User not found for status change: id={}", id);
            throw new ResourceNotFoundException(USER_NOT_FOUND + id);
        }
        userRepository.updateActiveStatus(id, active);
    }

    @Transactional
    @CacheEvict(value = UserCacheEvictor.CACHE_NAME, key = "#id")
    public void deleteUser(Long id) {
        log.info("Deleting user: id={}", id);
        if (!userRepository.existsById(id)) {
            log.debug("User not found for delete: id={}", id);
            throw new ResourceNotFoundException(USER_NOT_FOUND + id);
        }
        userRepository.deleteById(id);
    }
}
