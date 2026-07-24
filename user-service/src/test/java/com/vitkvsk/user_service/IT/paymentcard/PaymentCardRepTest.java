package com.vitkvsk.user_service.IT.paymentcard;

import com.vitkvsk.user_service.IntegrationTest;
import com.vitkvsk.user_service.repository.PaymentCardRepository;
import com.vitkvsk.user_service.repository.UserRepository;
import com.vitkvsk.user_service.entity.PaymentCard;
import com.vitkvsk.user_service.entity.User;
import com.vitkvsk.user_service.specification.CardSpecifications;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class PaymentCardRepTest {

    @Autowired
    private PaymentCardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private User createUser(String name, String surname, String email) {
        User user = User.builder()
                .name(name)
                .surname(surname)
                .email(email)
                .birthDate(LocalDate.of(1995, 1, 1))
                .build();
        return userRepository.save(user);
    }

    private PaymentCard createCard(String number, String holder, User user) {
        return PaymentCard.builder()
                .number(number)
                .holder(holder)
                .expirationDate(LocalDate.now().plusYears(2))
                .user(user)
                .build();
    }

    @Test
    void shouldFindAllCardsByUserId() {
        User user1 = createUser("Alice", "Dod", "alice@test.com");
        User user2 = createUser("Bob", "Dod", "bob@test.com");

        cardRepository.save(createCard("1111222233334444", "ALICE DOD", user1));
        cardRepository.save(createCard("5555666677778888", "ALICE DOD", user1));
        cardRepository.save(createCard("9999000011112222", "BOB DOD", user2));

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
        User user = createUser("Sam", "Dod", "sam@test.com");
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
        User user = createUser("John", "Dod", "john@test.com");
        PaymentCard card = cardRepository.save(createCard("1234123412341234", "JOHN DOD", user));

        cardRepository.updateActiveStatus(false, card.getId());

        entityManager.flush();
        entityManager.clear();

        PaymentCard updatedCard = cardRepository.findById(card.getId()).orElseThrow();
        assertThat(updatedCard.isActive()).isFalse();
    }

    @Test
    void shouldFilterCardsByUserNameAndSurnameWithPagination() {
        User user1 = createUser("Ivan", "Dod", "ivan@test.com");
        User user2 = createUser("Petr", "Dod", "petr@test.com");

        cardRepository.save(createCard("1111222233334444", "IVAN DOD", user1));
        cardRepository.save(createCard("5555666677778888", "PETR DOD", user2));

        entityManager.flush();
        entityManager.clear();

        Specification<PaymentCard> spec = Specification
                .where(CardSpecifications.hasUserName("Ivan"))
                .and(CardSpecifications.hasUserSurname("Dod"));

        Page<PaymentCard> result = cardRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getNumber()).isEqualTo("1111222233334444");
    }
}