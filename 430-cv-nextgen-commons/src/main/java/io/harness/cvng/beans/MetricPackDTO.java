package io.harness.cvng.beans;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MetricPackDTO {
  @TaggedFieldSerializer.Tag(1) String uuid;
  @TaggedFieldSerializer.Tag(2) String accountId;
  String orgIdentifier;
  String projectIdentifier;
  DataSourceType dataSourceType;
  String identifier;
  CVMonitoringCategory category;
  Set<MetricDefinitionDTO> metrics;
  List<TimeSeriesThresholdDTO> thresholds;

  @Value
  @Builder
  public static class MetricDefinitionDTO {
    String name;
    TimeSeriesMetricType type;
    String path;
    String validationPath;
    List<TimeSeriesThresholdDTO> thresholds;
    boolean included;
  }
}
