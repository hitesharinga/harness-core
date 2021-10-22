package io.harness.cvng.beans;

import io.harness.delegate.beans.connector.datadog.DatadogConnectorDTO;
import io.harness.delegate.beans.cvng.datadog.DatadogUtils;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
public class DatadogDataCollectionInfo extends TimeSeriesDataCollectionInfo<DatadogConnectorDTO>{
    private List<MetricCollectionInfo> metricDefinitions;

    @Override
    public Map<String, Object> getDslEnvVariables(DatadogConnectorDTO connectorConfigDTO) {
        Map<String, Object> dslEnvVariables = DatadogUtils.getCommonEnvVariables(connectorConfigDTO);
        List<String> queries = metricDefinitions.stream().map(metricCollectionInfo -> metricCollectionInfo.query)
                .collect(Collectors.toList());
        dslEnvVariables.put("queries", queries);
        return dslEnvVariables;
    }

    @Override
    public String getBaseUrl(DatadogConnectorDTO connectorConfigDTO) {
       return connectorConfigDTO.getUrl();
    }

    @Override
    public Map<String, String> collectionHeaders(DatadogConnectorDTO connectorConfigDTO) {
        return DatadogUtils.collectionHeaders(connectorConfigDTO);
    }

    @Override
    public Map<String, String> collectionParams(DatadogConnectorDTO connectorConfigDTO) {
      return DatadogUtils.collectionHeaders(connectorConfigDTO);
    }

    @Data
    @Builder
    public static class MetricCollectionInfo {
        private String query;
        private String metricName;
    }
}
