package io.harness.jira;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.codehaus.jackson.annotate.JsonTypeName;

@Data
@JsonTypeName("jiraUserSearch")
public class JiraUserSearchResponse {
  private List<JiraUserData> userDataList = new ArrayList<>();

  public JiraUserSearchResponse(JSONArray userDataListObj) {
    for (int i = 0; i < userDataListObj.size(); i++) {
      JSONObject userDataObj = userDataListObj.getJSONObject(i);
      if (userDataObj.getBoolean("active")) {
        this.userDataList.add(new JiraUserData(userDataObj));
      }
    }
  }
}
