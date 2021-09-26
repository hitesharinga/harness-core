package io.harness.changehandlers;

import static java.util.Arrays.asList;

import io.harness.changestreamsframework.ChangeEvent;
import io.harness.ng.core.service.entity.ServiceEntity.ServiceEntityKeys;

import com.mongodb.DBObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServicesChangeDataHandler extends AbstractChangeDataHandler {
  @Override
  public Map<String, String> getColumnValueMapping(ChangeEvent<?> changeEvent, String[] fields) {
    if (changeEvent == null) {
      return null;
    }
    Map<String, String> columnValueMapping = new HashMap<>();
    DBObject dbObject = changeEvent.getFullDocument();

    columnValueMapping.put("id", changeEvent.getUuid());

    if (dbObject == null) {
      return columnValueMapping;
    }

    if (dbObject.get(ServiceEntityKeys.accountId) != null) {
      columnValueMapping.put("account_id", dbObject.get(ServiceEntityKeys.accountId).toString());
    }

    if (dbObject.get(ServiceEntityKeys.orgIdentifier) != null) {
      columnValueMapping.put("org_identifier", dbObject.get(ServiceEntityKeys.orgIdentifier).toString());
    }

    if (dbObject.get(ServiceEntityKeys.projectIdentifier) != null) {
      columnValueMapping.put("project_identifier", dbObject.get(ServiceEntityKeys.projectIdentifier).toString());
    }

    if (dbObject.get(ServiceEntityKeys.identifier) != null) {
      columnValueMapping.put("identifier", dbObject.get(ServiceEntityKeys.identifier).toString());
    }

    if (dbObject.get(ServiceEntityKeys.name) != null) {
      columnValueMapping.put("name", dbObject.get(ServiceEntityKeys.name).toString());
    }

    if (dbObject.get(ServiceEntityKeys.deleted) != null) {
      columnValueMapping.put("deleted", dbObject.get(ServiceEntityKeys.deleted).toString());
    }

    if (dbObject.get(ServiceEntityKeys.createdAt) != null) {
      columnValueMapping.put("created_at", dbObject.get(ServiceEntityKeys.createdAt).toString());
    }

    if (dbObject.get(ServiceEntityKeys.lastModifiedAt) != null) {
      columnValueMapping.put("last_modified_at", dbObject.get(ServiceEntityKeys.lastModifiedAt).toString());
    }

    return columnValueMapping;
  }

  @Override
  public List<String> getPrimaryKeys() {
    return asList("id");
  }
}
