package com.vitkvsk.user_service.IT;

import com.vitkvsk.user_service.TestcontainersConfiguration;
import com.vitkvsk.user_service.dao.PaymentCardRepository;
import com.vitkvsk.user_service.dao.UserRepository;
import com.vitkvsk.user_service.entities.PaymentCard;
import com.vitkvsk.user_service.entities.User;
import com.vitkvsk.user_service.specification.CardSpecifications;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Import(TestcontainersConfiguration.class)
public class PaymentCardRepIT {

    @Autowired
    private PaymentCardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private User createUser(String name, String surname, String email) {
        User user = new User();
        user.setName(name);
        user.setSurname(surname);
        user.setEmail(email);
        user.setBirthDate(LocalDate.of(1995, 1, 1));
        return userRepository.save(user);
    }

    private PaymentCard createCard(String number, String holder, User user) {
        PaymentCard card = new PaymentCard();
        card.setNumber(number);
        card.setHolder(holder);
        card.setExpirationDate(LocalDate.now().plusYears(2));
        card.setUser(user);
        return card;
    }

    @Test
    void shouldFindAllCardsByUserId() {
        User user1 = createUser("Alice", "Doe", "alice@test.com");
        User user2 = createUser("Bob", "Doe", "bob@test.com");

        cardRepository.save(createCard("1111222233334444", "ALICE DOE", user1));
        cardRepository.save(createCard("5555666677778888", "ALICE DOE", user1));
        cardRepository.save(createCard("9999000011112222", "BOB DOE", user2));

        entityManager.flush();
        entityManager.clear();

        List<PaymentCard> aliceCards = cardRepository.findAllByUserId(user1.getId());

        assertThat(aliceCards).hasSize(2);
        assertThat(aliceCards)
                .extracting(PaymentCard::getNumber)
                .containsExactlyInAnyOrder("1111222233334444", "5555666677778888");
    }

    @Test
    void shouldUpdateCard() {
        User user = createUser("Sam", "Smith", "sam@test.com");
        PaymentCard card = cardRepository.save(createCard("1234567812345678", "OLD HOLDER", user));

        card.setHolder("NEW HOLDER");
        card.setExpirationDate(LocalDate.now().plusYears(5));
        cardRepository.save(card);

        entityManager.flush();
        entityManager.clear();

        PaymentCard updatedCard = cardRepository.findById(card.getId()).orElseThrow();
        assertThat(updatedCard.getHolder()).isEqualTo("NEW HOLDER");
    }

    @Test
    void shouldUpdateCardActiveStatusNativeSql() {
        User user = createUser("John", "Doe", "john@test.com");
        PaymentCard card = cardRepository.save(createCard("1234123412341234", "JOHN DOE", user));

        cardRepository.updateActiveStatus(false, card.getId());

        entityManager.flush();
        entityManager.clear();

        PaymentCard updatedCard = cardRepository.findById(card.getId()).orElseThrow();
        assertThat(updatedCard.isActive()).isFalse();
    }

    @Test
    void shouldFilterCardsByUserNameAndSurnameWithPagination() {
        User user1 = createUser("Ivan", "Ivanov", "ivan@test.com");
        User user2 = createUser("Petr", "Ivanov", "petr@test.com");

        cardRepository.save(createCard("1111222233334444", "IVAN IVANOV", user1));
        cardRepository.save(createCard("5555666677778888", "PETR IVANOV", user2));

        entityManager.flush();
        entityManager.clear();

        Specification<PaymentCard> spec = Specification
                .where(CardSpecifications.hasUserName("Ivan"))
                .and(CardSpecifications.hasUserSurname("Ivanov"));

        Page<PaymentCard> result = cardRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getNumber()).isEqualTo("1111222233334444");
    }
}