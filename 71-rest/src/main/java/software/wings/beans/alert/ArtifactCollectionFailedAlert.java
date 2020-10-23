package software.wings.beans.alert;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.alert.AlertData;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;

@OwnedBy(CDC)
@Data
@Builder
public class ArtifactCollectionFailedAlert implements AlertData {
  // TODO: ASR: refactor this class for connector level artifact streams
  @Inject @Transient @SchemaIgnore private transient AppService appService;
  @Inject @Transient @SchemaIgnore private transient ArtifactStreamService artifactStreamService;
  @Inject @Transient @SchemaIgnore private transient ServiceResourceService serviceResourceService;
  @Inject @Transient @SchemaIgnore private transient SettingsService settingsService;

  private String appId;
  private String serviceId;
  private String artifactStreamId;
  private String settingId;

  @Override
  public boolean matches(AlertData alertData) {
    ArtifactCollectionFailedAlert otherAlert = (ArtifactCollectionFailedAlert) alertData;
    return StringUtils.equals(otherAlert.getArtifactStreamId(), artifactStreamId);
  }

  @Override
  public String buildTitle() {
    StringBuilder title = new StringBuilder(128);
    title.append("Artifact collection ");
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
    if (artifactStream != null) {
      title.append("of source ").append(artifactStream.getName()).append(' ');
      if (isNotBlank(appId) && !appId.equals(GLOBAL_APP_ID)) {
        if (isNotBlank(serviceId)) {
          Service service = serviceResourceService.getWithDetails(appId, serviceId);
          title.append("in service ").append(service.getName()).append(' ');
        }
        Application app = appService.get(appId);
        title.append("for application ").append(app.getName()).append(' ');
      } else {
        if (isNotBlank(settingId)) {
          SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
          title.append("under artifact server ").append(settingAttribute.getName()).append(' ');
        }
      }
    }

    title.append("is disabled because of multiple failed attempts. Fix artifact source setup or do a manual pull");
    return title.toString();
  }
}
