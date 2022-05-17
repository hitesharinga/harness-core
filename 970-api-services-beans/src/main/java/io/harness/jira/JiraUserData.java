package io.harness.jira;

import lombok.Data;
import net.sf.json.JSONObject;

@Data
public class JiraUserData {
  private String accountId;
  private String displayName;

  public JiraUserData(JSONObject jsonObject) {
    this.accountId = jsonObject.getString("accountId");
    this.displayName = jsonObject.getString("displayName");
  }
}
