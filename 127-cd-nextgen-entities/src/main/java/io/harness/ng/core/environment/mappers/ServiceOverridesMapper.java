package io.harness.ng.core.environment.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.environment.beans.NGServiceOverridesEntity;
import io.harness.ng.core.environment.beans.ServiceOverrideRequestDTO;
import io.harness.ng.core.environment.beans.ServiceOverrideResponseDTO;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class ServiceOverridesMapper {
  public NGServiceOverridesEntity toServiceOverridesEntity(
      String accountId, ServiceOverrideRequestDTO serviceOverrideRequestDTO) {
    return NGServiceOverridesEntity.builder()
        .accountId(accountId)
        .orgIdentifier(serviceOverrideRequestDTO.getOrgIdentifier())
        .projectIdentifier(serviceOverrideRequestDTO.getProjectIdentifier())
        .environmentRef(serviceOverrideRequestDTO.getEnvironmentRef())
        .serviceRef(serviceOverrideRequestDTO.getServiceRef())
        .variableOverrides(serviceOverrideRequestDTO.getVariableOverrides())
        .build();
  }

  public ServiceOverrideResponseDTO toResponseWrapper(NGServiceOverridesEntity serviceOverridesEntity) {
    return ServiceOverrideResponseDTO.builder()
        .accountId(serviceOverridesEntity.getAccountId())
        .orgIdentifier(serviceOverridesEntity.getOrgIdentifier())
        .projectIdentifier(serviceOverridesEntity.getProjectIdentifier())
        .environmentRef(serviceOverridesEntity.getEnvironmentRef())
        .serviceRef(serviceOverridesEntity.getServiceRef())
        .variableOverrides(serviceOverridesEntity.getVariableOverrides())
        .build();
  }
}
