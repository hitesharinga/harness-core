package software.wings.integration;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import software.wings.beans.RestResponse;
import software.wings.beans.VaultConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.security.SecretManagementDelegateService;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 9/21/18.
 */
public class VaultIntegrationTest extends BaseIntegrationTest {
  private static final String VAULT_BASE_PATH = "/foo/bar";

  @Inject private SecretManagementDelegateService secretManagementDelegateService;

  private String vaultToken;
  private VaultConfig vaultConfig;
  private VaultConfig vaultConfig2;

  private VaultConfig vaultConfigWithBasePath;

  @Before
  public void setUp() {
    super.loginAdminUser();
    this.vaultToken = System.getProperty("vault.token", "root");
    Preconditions.checkState(isNotEmpty(vaultToken));

    vaultConfig = VaultConfig.builder()
                      .accountId(accountId)
                      .name("TestVault")
                      .vaultUrl("http://127.0.0.1:8200")
                      .authToken(vaultToken)
                      .isDefault(true)
                      .build();

    vaultConfig2 = VaultConfig.builder()
                       .accountId(accountId)
                       .name("TestVault2")
                       .vaultUrl("http://127.0.0.1:8300")
                       .authToken(vaultToken)
                       .isDefault(true)
                       .build();

    vaultConfigWithBasePath = VaultConfig.builder()
                                  .accountId(accountId)
                                  .name("TestVaultWithBasePath")
                                  .vaultUrl("http://127.0.0.1:8200")
                                  .authToken(vaultToken)
                                  .basePath(VAULT_BASE_PATH)
                                  .isDefault(true)
                                  .build();
  }

  @Test
  public void testCreateUpdateDeleteVaultConfig_shouldSucceed() {
    // 1. Create a new Vault config.
    String vaultConfigId = createVaultConfig(vaultConfig);

    try {
      VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
      assertNotNull(savedVaultConfig);

      // 2. Update the existing vault config to make it default
      savedVaultConfig.setAuthToken(vaultToken);
      updateVaultConfig(savedVaultConfig);

      savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
      assertNotNull(savedVaultConfig);
      assertTrue(savedVaultConfig.isDefault());
    } finally {
      // 3. Delete the vault config
      deleteVaultConfig(vaultConfigId);
    }
  }

  @Test
  public void test_createDuplicateVaultSecretManager_shouldFail() {
    // 1. Create a new Vault config.
    String vaultConfigId = createVaultConfig(vaultConfig);

    // 2. Create the same Vault config with a different name.
    try {
      updateVaultConfig(vaultConfig);
      fail("Exception is expected when creating the same Vault secret manager with a different name");
    } catch (Exception e) {
      // Ignore. Expected.
    } finally {
      // 3. Delete the vault config
      deleteVaultConfig(vaultConfigId);
    }
  }

  @Test
  public void test_createNewDefaultVault_shouldUnsetPreviousDefaultVaultConfig() {
    // Create the first default vault config
    String vaultConfigId = createVaultConfig(vaultConfig);
    // Create 2nd default vault config. The 1 vault will be set to be non-default.
    String vaultConfig2Id = createVaultConfig(vaultConfig2);

    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertNotNull(savedVaultConfig);

    VaultConfig savedVaultConfig2 = wingsPersistence.get(VaultConfig.class, vaultConfig2Id);
    assertNotNull(savedVaultConfig2);

    try {
      assertFalse(savedVaultConfig.isDefault());
      assertTrue(savedVaultConfig2.isDefault());

      // Update 1st vault config to be default again. 2nd vault config will be set to be non-default.
      savedVaultConfig.setDefault(true);
      savedVaultConfig.setAuthToken(vaultToken);
      updateVaultConfig(savedVaultConfig);

      savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
      assertTrue(savedVaultConfig.isDefault());

      savedVaultConfig2 = wingsPersistence.get(VaultConfig.class, vaultConfig2Id);
      assertFalse(savedVaultConfig2.isDefault());
    } finally {
      // Delete both vault configs.
      deleteVaultConfig(vaultConfigId);
      deleteVaultConfig(vaultConfig2Id);
    }
  }

  @Test
  public void test_unsetOnlyDefaultVault_shouldFail() {
    // Create the first default vault config
    String vaultConfigId = createVaultConfig(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertNotNull(savedVaultConfig);

    try {
      savedVaultConfig.setDefault(false);
      savedVaultConfig.setAuthToken(vaultToken);
      updateVaultConfig(savedVaultConfig);
      fail("Unset the only default vault config manager will fail!");
    } catch (Exception e) {
      // Exception is expected.
    } finally {
      // Clean up.
      deleteVaultConfig(vaultConfigId);
    }
  }

  @Test
  public void test_UpdateSecretTextWithValue_VaultWithBasePath_shouldSucceed() {
    // Create the first default vault config
    String vaultConfigId = createVaultConfig(vaultConfigWithBasePath);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertNotNull(savedVaultConfig);

    testUpdateSecretText(savedVaultConfig);
  }

  @Test
  public void test_UpdateSecretTextWithValue_shouldSucceed() {
    // Create the first default vault config
    String vaultConfigId = createVaultConfig(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertNotNull(savedVaultConfig);

    testUpdateSecretText(savedVaultConfig);
  }

  private void testUpdateSecretText(VaultConfig savedVaultConfig) {
    String secretUuid = null;
    try {
      secretUuid = createSecretText("FooBarSecret", "MySecretValue", null);
      updateSecretText(secretUuid, "FooBarSecret_Modified", "MySecretValue_Modified", null);
      verifySecretValue(secretUuid, "MySecretValue_Modified", savedVaultConfig);
    } finally {
      // Clean up.
      if (secretUuid != null) {
        deleteSecretText(secretUuid);
      }
      deleteVaultConfig(savedVaultConfig.getUuid());
    }
  }

  @Test
  public void test_CreateSecretText_WithInvalidPath_shouldFail() {
    // Create the first default vault config
    String vaultConfigId = createVaultConfig(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertNotNull(savedVaultConfig);

    try {
      createSecretText("FooBarSecret", null, "foo/bar/InvalidSecretPath");
      fail("Expected to fail when creating a secret text pointing to an invalid Vault path.");
    } catch (Exception e) {
      // Exception expected.
    } finally {
      // Clean up.
      deleteVaultConfig(vaultConfigId);
    }
  }

  @Test
  public void test_UpdateSecretText_WithInvalidPath_shouldFail() {
    String vaultConfigId = createVaultConfig(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertNotNull(savedVaultConfig);

    String secretValue = "MySecretValue";
    String secretUuid1 = null;
    String secretUuid2 = null;
    try {
      // This will create one secret at path 'harness/SECRET_TEXT/FooSecret".
      secretUuid1 = createSecretText("FooSecret", secretValue, null);
      // Second secret will refer the first secret by relative path, as the default root is "/harness".
      secretUuid2 = createSecretText("BarSecret", null, "SECRET_TEXT/FooSecret");
      updateSecretText(secretUuid2, "BarSecret", null, "foo/bar/InvalidSecretPath");
      fail("Expected to fail when updating a secret text pointing to an invalid Vault path.");
    } catch (Exception e) {
      // Exception is expected here.
    } finally {
      if (secretUuid1 != null) {
        deleteSecretText(secretUuid1);
      }
      if (secretUuid2 != null) {
        deleteSecretText(secretUuid2);
      }
      // Clean up.
      deleteVaultConfig(vaultConfigId);
    }
  }

  @Test
  public void test_CreateSecretText_withInvalidPathReference_shouldFail() {
    String vaultConfigId = createVaultConfig(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertNotNull(savedVaultConfig);

    String secretName = "MySecret";
    String secretName2 = "AbsolutePathSecret";
    String pathPrefix = isEmpty(savedVaultConfig.getBasePath()) ? "/harness" : savedVaultConfig.getBasePath();
    String absoluteSecretPathWithNoPound = pathPrefix + "/SECRET_TEXT/" + secretName + "/FooSecret";

    try {
      testCreateSecretText(savedVaultConfig, secretName, secretName2, absoluteSecretPathWithNoPound);
      fail("Saved with secret path doesn't contain # should fail");
    } catch (Exception e) {
      // Exception is expected.
    }
  }

  @Test
  public void test_CreateSecretText_WithValidPath_shouldSucceed() {
    String vaultConfigId = createVaultConfig(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertNotNull(savedVaultConfig);

    String secretName = "FooSecret";
    String secretName2 = "AbsolutePathSecret";
    String pathPrefix = isEmpty(savedVaultConfig.getBasePath()) ? "/harness" : savedVaultConfig.getBasePath();
    String absoluteSecretPath = pathPrefix + "/SECRET_TEXT/" + secretName + "#value";
    testCreateSecretText(savedVaultConfig, secretName, secretName2, absoluteSecretPath);
  }

  @Test
  public void test_CreateSecretText_vaultWithBasePath_validPath_shouldSucceed() {
    String vaultConfigId = createVaultConfig(vaultConfigWithBasePath);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertNotNull(savedVaultConfig);

    String secretName = "FooSecret";
    String secretName2 = "AbsolutePathSecret";
    String pathPrefix = isEmpty(savedVaultConfig.getBasePath()) ? "/harness" : savedVaultConfig.getBasePath();
    String absoluteSecretPath = pathPrefix + "/SECRET_TEXT/" + secretName + "#value";
    testCreateSecretText(savedVaultConfig, secretName, secretName2, absoluteSecretPath);
  }

  private void testCreateSecretText(
      VaultConfig savedVaultConfig, String secretName, String secretName2, String absoluteSecretPath) {
    String secretValue = "MySecretValue";
    String secretUuid1 = null;
    String secretUuid2 = null;
    try {
      // This will create one secret at path 'harness/SECRET_TEXT/FooSecret".
      secretUuid1 = createSecretText(secretName, secretValue, null);
      verifySecretValue(secretUuid1, secretValue, savedVaultConfig);

      // Second secret will refer the first secret by absolute path of format "/foo/bar/FooSecret#value'.
      String pathPrefix = isEmpty(savedVaultConfig.getBasePath()) ? "/harness" : savedVaultConfig.getBasePath();
      secretUuid2 = createSecretText(secretName2, null, absoluteSecretPath);
      verifySecretValue(secretUuid2, secretValue, savedVaultConfig);

    } finally {
      if (secretUuid1 != null) {
        deleteSecretText(secretUuid1);
      }
      if (secretUuid2 != null) {
        deleteSecretText(secretUuid2);
      }
      // Clean up.
      deleteVaultConfig(savedVaultConfig.getUuid());
    }
  }

  private String createSecretText(String name, String value, String path) {
    WebTarget target = client.target(API_BASE + "/secrets/add-secret?accountId=" + accountId);
    SecretText secretText = SecretText.builder().name(name).value(value).path(path).build();
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(secretText, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    // Verify vault config was successfully created.
    assertEquals(0, restResponse.getResponseMessages().size());
    String encryptedDataId = restResponse.getResource();
    assertTrue(isNotEmpty(encryptedDataId));

    return encryptedDataId;
  }

  private void updateSecretText(String uuid, String name, String value, String path) {
    WebTarget target = client.target(API_BASE + "/secrets/update-secret?accountId=" + accountId + "&uuid=" + uuid);
    SecretText secretText = SecretText.builder().name(name).value(value).path(path).build();
    RestResponse<Boolean> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(secretText, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    // Verify vault config was successfully created.
    assertEquals(0, restResponse.getResponseMessages().size());
    Boolean updated = restResponse.getResource();
    assertTrue(updated);
  }

  private void deleteSecretText(String uuid) {
    WebTarget target = client.target(API_BASE + "/secrets/delete-secret?accountId=" + accountId + "&uuid=" + uuid);
    RestResponse<Boolean> restResponse =
        getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse<Boolean>>() {});
    // Verify vault config was successfully created.
    assertEquals(0, restResponse.getResponseMessages().size());
    Boolean deleted = restResponse.getResource();
    assertTrue(deleted);
  }

  private String createVaultConfig(VaultConfig vaultConfig) {
    WebTarget target = client.target(API_BASE + "/vault?accountId=" + accountId);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(vaultConfig, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    // Verify vault config was successfully created.
    assertEquals(0, restResponse.getResponseMessages().size());
    String vaultConfigId = restResponse.getResource();
    assertTrue(isNotEmpty(vaultConfigId));

    return vaultConfigId;
  }

  private void updateVaultConfig(VaultConfig vaultConfig) {
    WebTarget target = client.target(API_BASE + "/vault?accountId=" + accountId);
    vaultConfig.setName("TestVault_Different_Name");
    getRequestBuilderWithAuthHeader(target).post(
        entity(vaultConfig, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
  }

  private void deleteVaultConfig(String vaultConfigId) {
    WebTarget target = client.target(API_BASE + "/vault?accountId=" + accountId + "&vaultConfigId=" + vaultConfigId);
    RestResponse<Boolean> deleteRestResponse =
        getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse<Boolean>>() {});
    // Verify the vault config was deleted successfully
    assertEquals(0, deleteRestResponse.getResponseMessages().size());
    assertTrue(Boolean.valueOf(deleteRestResponse.getResource()));
    assertNull(wingsPersistence.get(VaultConfig.class, vaultConfigId));
  }

  private void verifySecretValue(String secretUuid, String expectedValue, VaultConfig vaultConfig) {
    vaultConfig.setAuthToken(this.vaultToken);
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, secretUuid);
    assertNotNull(encryptedData);

    char[] decrypted = secretManagementDelegateService.decrypt(encryptedData, vaultConfig);
    assertTrue(isNotEmpty(decrypted));
    assertEquals(expectedValue, new String(decrypted));
  }
}