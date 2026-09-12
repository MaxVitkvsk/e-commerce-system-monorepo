package com.vitkvsk.payment_service.migration;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationOptions;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;


@ChangeUnit(id = "001-create-payments", order = "001", author = "vitkvsk")
public class PaymentsCollectionChange {

    private static final String COLLECTION_NAME = "payments";
    private static final String BSON_TYPE = "bsonType";
    private static final String ORDER_ID_FIELD = "order_id";
    private static final String USER_ID_FIELD = "user_id";
    private static final String STATUS_FIELD = "status";
    private static final String TIMESTAMP_FIELD = "timestamp";
    private static final String PAYMENT_AMOUNT_FIELD = "payment_amount";

    private final MongoTemplate mongoTemplate;

    public PaymentsCollectionChange(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Execution
    public void execution() {
        MongoDatabase db = mongoTemplate.getDb();

        ValidationOptions validator = new ValidationOptions()
                .validator(new Document("$jsonSchema", new Document()
                        .append(BSON_TYPE, "object")
                        .append("required", List.of(
                                ORDER_ID_FIELD, USER_ID_FIELD, STATUS_FIELD, TIMESTAMP_FIELD, PAYMENT_AMOUNT_FIELD))
                        .append("properties", new Document()
                                .append(ORDER_ID_FIELD, new Document(BSON_TYPE, "long"))
                                .append(USER_ID_FIELD, new Document(BSON_TYPE, "string"))
                                .append(STATUS_FIELD, new Document(BSON_TYPE, "string"))
                                .append(TIMESTAMP_FIELD, new Document(BSON_TYPE, "date"))
                                .append(PAYMENT_AMOUNT_FIELD, new Document(BSON_TYPE, "decimal")))))
                .validationLevel(ValidationLevel.STRICT)
                .validationAction(ValidationAction.ERROR);

        db.createCollection(COLLECTION_NAME, new CreateCollectionOptions().validationOptions(validator));

        db.getCollection(COLLECTION_NAME).createIndex(
                new Document(ORDER_ID_FIELD, 1),
                new IndexOptions().name("idx_payments_order_id").unique(true));

        db.getCollection(COLLECTION_NAME).createIndex(
                new Document(USER_ID_FIELD, 1).append(TIMESTAMP_FIELD, -1),
                new IndexOptions().name("idx_payments_user_ts"));
    }

    @RollbackExecution
    public void rollback() {
        mongoTemplate.getDb().getCollection(COLLECTION_NAME).drop();
    }
}