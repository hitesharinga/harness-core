package io.harness.licensing.services;

import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;

@Deprecated
public interface LicenseCrudService {
  ModuleLicenseDTO getModuleLicense(String accountId, ModuleType moduleType);
  AccountLicenseDTO getAccountLicense(String accountIdentifier);
  ModuleLicenseDTO getModuleLicenseById(String identifier);
  ModuleLicenseDTO createModuleLicense(ModuleLicenseDTO moduleLicense);
  ModuleLicenseDTO updateModuleLicense(ModuleLicenseDTO moduleLicense);
}
