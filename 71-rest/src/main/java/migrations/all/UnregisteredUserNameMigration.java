package migrations.all;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.beans.User;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;

@Slf4j
public class UnregisteredUserNameMigration implements Migration {
  @Inject WingsPersistence wingsPersistence;
  @Override
  public void migrate() {
    logger.info("Migrating unregistered usernames");

    Query<User> query = wingsPersistence.createQuery(User.class).field("name").equal(Constants.NOT_REGISTERED);
    logger.info("Updating " + query.count() + " user entries");
    try (HIterator<User> userIterator = new HIterator<>(query.fetch())) {
      for (User user : userIterator) {
        wingsPersistence.update(user, wingsPersistence.createUpdateOperations(User.class).set("name", user.getEmail()));
      }
    }
  }
}