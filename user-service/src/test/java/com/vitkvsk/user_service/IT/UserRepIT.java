package com.vitkvsk.user_service.IT;


import com.vitkvsk.user_service.TestcontainersConfiguration;
import com.vitkvsk.user_service.dao.UserRepository;
import com.vitkvsk.user_service.entities.PaymentCard;
import com.vitkvsk.user_service.entities.User;
import com.vitkvsk.user_service.specification.UserSpecifications;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Import(TestcontainersConfiguration.class)
public class UserRepIT {

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private User createUser(String name, String surname, String email, LocalDate birthDate) {
        User u = new User();
        u.setName(name);
        u.setSurname(surname);
        u.setEmail(email);
        u.setBirthDate(birthDate);
        return u;
    }

    @Test
    void shouldFindUserWithCards() {
        LocalDate birthDate = LocalDate.now().minusYears(30);
        User user = createUser("John", "Dod", "john@test.com", birthDate);
        PaymentCard card = new PaymentCard();
        card.setNumber("1111222233334444");
        card.setHolder("JOHN DOE");
        card.setExpirationDate(LocalDate.now().plusYears(1));
        card.setUser(user);
        user.getCards().add(card);

        User savedUser = userRepository.save(user);

        entityManager.flush();
        entityManager.clear();

        Optional<User> result = userRepository.findByIdWithCards(savedUser.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getCards()).hasSize(1);
        assertThat(result.get().getCards().get(0).getNumber()).isEqualTo("1111222233334444");
    }

    @Test
    void shouldUpdateActiveStatusNativeSql() {
        LocalDate birthDate = LocalDate.now().minusYears(25);
        User user = createUser("Alice", "Smith", "alice@test.com", birthDate);
        User savedUser = userRepository.save(user);

        assertThat(savedUser.isActive()).isTrue();

        userRepository.updateActiveStatus(savedUser.getId(), false);

        entityManager.flush();
        entityManager.clear();

        Optional<User> updatedUser = userRepository.findById(savedUser.getId());
        assertThat(updatedUser).isPresent();
        assertThat(updatedUser.get().isActive()).isFalse();
    }

    @Test
    void shouldFilterByNameAndSurnameWithPagination() {
        LocalDate birthDate = LocalDate.now().minusYears(20);

        userRepository.saveAll(List.of(
                createUser("John", "Doe", "john.doe@test.com", birthDate),
                createUser("John", "Smith", "john.smith@test.com", birthDate),
                createUser("Alice", "Doe", "alice.doe@test.com", birthDate)
        ));

        entityManager.flush();
        entityManager.clear();

        Specification<User> spec = Specification
                .where(UserSpecifications.hasName("John"))
                .and(UserSpecifications.hasSurname("Doe"));

        Page<User> result = userRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("john.doe@test.com");
    }

    @Test
    void shouldCascadeDeleteUserWithCards() {
        LocalDate birthDate = LocalDate.now().minusYears(30);
        User user = createUser("Mark", "Zucker", "mark@test.com", birthDate);
        PaymentCard card = new PaymentCard();
        card.setNumber("9999888877776666");
        card.setHolder("MARK ZUCKER");
        card.setExpirationDate(LocalDate.now().plusYears(2));
        card.setUser(user);
        user.getCards().add(card);

        User savedUser = userRepository.save(user);
        Long cardId = savedUser.getCards().get(0).getId();

        entityManager.flush();
        entityManager.clear();

        userRepository.deleteById(savedUser.getId());

        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findById(savedUser.getId())).isEmpty();

        PaymentCard deletedCard = entityManager.find(PaymentCard.class, cardId);
        assertThat(deletedCard).isNull();
    }
}