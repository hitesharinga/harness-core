package io.harness.batch.processing.dao.impl;

import com.google.inject.Inject;

import io.harness.batch.processing.dao.intfc.PublishedMessageDao;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.grpc.PublishedMessage.PublishedMessageKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
public class PublishedMessageDaoImpl implements PublishedMessageDao {
  @Autowired @Inject private HPersistence hPersistence;

  @Override
  public List<PublishedMessage> fetchPublishedMessage(
      String accountId, String messageType, Long startTime, Long endTime, int batchSize) {
    Query<PublishedMessage> query = hPersistence.createQuery(PublishedMessage.class)
                                        .filter(PublishedMessageKeys.accountId, accountId)
                                        .filter(PublishedMessageKeys.type, messageType)
                                        .order(PublishedMessageKeys.createdAt);

    query.and(query.criteria(PublishedMessageKeys.createdAt).greaterThanOrEq(startTime),
        query.criteria(PublishedMessageKeys.createdAt).lessThan(endTime));
    return query.asList(new FindOptions().limit(batchSize));
  }
}
