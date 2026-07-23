package com.vitkvsk.user_service.service;

import com.vitkvsk.user_service.dao.PaymentCardRepository;
import com.vitkvsk.user_service.dao.UserRepository;

import com.vitkvsk.user_service.dto.PaymentCardCreateDto;
import com.vitkvsk.user_service.dto.PaymentCardResponseDto;
import com.vitkvsk.user_service.dto.UserCreateDto;
import com.vitkvsk.user_service.dto.UserResponseDto;
import com.vitkvsk.user_service.dto.UserUpdateDto;
import com.vitkvsk.user_service.entities.PaymentCard;
import com.vitkvsk.user_service.entities.User;
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

    private final UserRepository userRepository;
    private final PaymentCardRepository paymentCardRepository;
    private final UserMapper userMapper;
    private final PaymentCardMapper paymentCardMapper;

    @Transactional
    public UserResponseDto createUser(UserCreateDto dto) {
        User user = userMapper.toEntity(dto);
        User savedUser = userRepository.save(user);
        return userMapper.toResponseDto(savedUser);
    }

    @Transactional
    @CacheEvict(value = "usersWithCards", key = "#userId")
    public PaymentCardResponseDto addCardToUser(Long userId, PaymentCardCreateDto dto) {
        long cardCount = paymentCardRepository.countByUserId(userId);
        if (cardCount >= User.MAX_CARDS) {
            throw new CardLimitExceededException(User.MAX_CARDS);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found with id: " + userId));

        PaymentCard card = paymentCardMapper.toEntity(dto);
        card.setUser(user);

        PaymentCard savedCard = paymentCardRepository.save(card);
        return paymentCardMapper.toResponseDto(savedCard);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "usersWithCards", key = "#id")
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findByIdWithCards(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found with id: " + id));
        return userMapper.toResponseDto(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> getAllUsers(String name, String surname, Pageable pageable) {
        Specification<User> spec = Specification.where(UserSpecifications.hasName(name))
                .and(UserSpecifications.hasSurname(surname));

        return userRepository.findAll(spec, pageable).map(userMapper::toResponseDto);
    }

    @Transactional
    @CacheEvict(value = "usersWithCards", key = "#id")
    public UserResponseDto updateUser(Long id, UserUpdateDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found with id: " + id));

        userMapper.updateEntityFromDto(dto, user);

        return userMapper.toResponseDto(user);
    }

    @Transactional
    @CacheEvict(value = "usersWithCards", key = "#id")
    public void changeUserStatus(Long id, boolean active) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User not found with id: " + id);
        }

        userRepository.updateActiveStatus(id, active);
    }
}
