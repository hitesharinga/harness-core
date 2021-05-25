package io.harness.licensing;

import io.harness.licensing.interfaces.clients.ModuleLicenseClient;
import io.harness.licensing.mappers.LicenseObjectMapper;
import io.harness.licensing.mappers.transactions.LicenseTransactionMapper;
import io.harness.licensing.scheduler.LicenseCheckProcessor;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class ModuleLicenseRegistrar {
  ModuleType moduleType;

  Class<? extends LicenseObjectMapper> objectMapper;

  Class<? extends ModuleLicenseClient> moduleLicenseClient;

  Class<? extends LicenseCheckProcessor> licenseCheckProcessor;

  Class<? extends LicenseTransactionMapper> licenseTransactionMapper;
}
