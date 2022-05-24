package io.harness.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.jackson.JsonNodeUtils;
import io.harness.jira.deserializer.JiraStatusDeserializer;
import io.harness.jira.deserializer.JiraUserDataDeserializer;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.sf.json.JSONObject;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = JiraUserDataDeserializer.class)
public class JiraUserData {
  private String accountId;
  private String displayName;
  private boolean active;

  public JiraUserData(JsonNode node) {
    this.accountId = JsonNodeUtils.mustGetString(node, "accountId");
    this.displayName = JsonNodeUtils.mustGetString(node, "displayName");
    this.active = JsonNodeUtils.mustGetBoolean(node, "active");
  }

  public JiraUserData(String accountId, String displayName, boolean active) {
    this.accountId = accountId;
    this.displayName = displayName;
    this.active = active;
  }
}
