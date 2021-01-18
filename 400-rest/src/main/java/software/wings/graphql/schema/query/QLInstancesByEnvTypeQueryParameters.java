package software.wings.graphql.schema.query;

import io.harness.beans.EnvironmentType;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLInstancesByEnvTypeQueryParameters implements QLPageQueryParameters {
  int limit;
  int offset;
  EnvironmentType envType;
  String accountId;
  DataFetchingFieldSelectionSet selectionSet;
}
