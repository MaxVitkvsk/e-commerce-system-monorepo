package com.vitkvsk.user_service.specification;

import com.vitkvsk.user_service.entities.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {
    public static Specification<User> hasName(String name) {
        return (root, query, cb) ->
                (name == null || name.isBlank())
                        ? null
                        : cb.equal(root.get("name"), name);
    }

    public static Specification<User> hasSurname(String surname){
        return (root, query, cb) ->
                ( surname == null || surname.isBlank())
                        ? null
                        : cb.equal(root.get("surname"), surname);

    }
}
