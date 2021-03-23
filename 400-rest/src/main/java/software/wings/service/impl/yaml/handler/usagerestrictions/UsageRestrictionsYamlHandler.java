package software.wings.service.impl.yaml.handler.usagerestrictions;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.security.EnvFilterYaml.EnvFilterYamlBuilder;
import static software.wings.security.GenericEntityFilterYaml.GenericEntityFilterYamlBuilder;
import static software.wings.security.GenericEntityFilterYaml.builder;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.yaml.ChangeContext;
import software.wings.security.EnvFilter;
import software.wings.security.EnvFilter.EnvFilterBuilder;
import software.wings.security.EnvFilterYaml;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.GenericEntityFilterBuilder;
import software.wings.security.GenericEntityFilterYaml;
import software.wings.security.UsageRestrictions;
import software.wings.security.UsageRestrictions.AppEnvRestriction;
import software.wings.security.UsageRestrictions.UsageRestrictionsBuilder;
import software.wings.security.UsageRestrictionsAppEnvRestrictionYaml;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *  @author rktummala on 01/09/19
 */
@OwnedBy(CDC)
@Singleton
public class UsageRestrictionsYamlHandler extends BaseYamlHandler<UsageRestrictions.Yaml, UsageRestrictions> {
  @Inject AppService appService;
  @Inject EnvironmentService environmentService;

  private AppEnvRestriction constructAppEnvRestriction(UsageRestrictionsAppEnvRestrictionYaml yaml, String accountId) {
    GenericEntityFilter appFilter = constructGenericEntityFilter(yaml.getAppFilter(), accountId);
    return AppEnvRestriction.builder()
        .envFilter(constructEnvFilter(appFilter, yaml.getEnvFilter()))
        .appFilter(appFilter)
        .build();
  }

  private UsageRestrictionsAppEnvRestrictionYaml constructAppEnvRestrictionYaml(AppEnvRestriction appEnvRestriction) {
    GenericEntityFilterYaml appFilterYaml = constructGenericEntityFilterYaml(appEnvRestriction.getAppFilter());
    return UsageRestrictionsAppEnvRestrictionYaml.builder()
        .envFilter(constructEnvFilterYaml(appEnvRestriction.getAppFilter(), appEnvRestriction.getEnvFilter()))
        .appFilter(appFilterYaml)
        .build();
  }

  private String getAppId(GenericEntityFilter appFilter) {
    Set<String> appIds = appFilter.getIds();
    if (isEmpty(appIds)) {
      throw new WingsException("No AppIds in app filter.");
    }

    if (appIds.size() > 1) {
      throw new WingsException("More than one App exists in the app filter.");
    }

    return appIds.toArray(new String[0])[0];
  }

  private EnvFilter constructEnvFilter(GenericEntityFilter appFilter, EnvFilterYaml envFilterYaml) {
    EnvFilterBuilder builder = EnvFilter.builder();

    if (isNotEmpty(envFilterYaml.getEntityNames())) {
      String appId = getAppId(appFilter);
      Set<String> ids = envFilterYaml.getEntityNames()
                            .stream()
                            .map(entityName -> {
                              Environment environment =
                                  environmentService.getEnvironmentByName(appId, entityName, false);
                              if (environment != null) {
                                return environment.getUuid();
                              }
                              return null;
                            })
                            .collect(Collectors.toSet());

      ids.remove(null);
      builder.ids(ids);
    }

    return builder.filterTypes(Sets.newHashSet(envFilterYaml.getFilterTypes())).build();
  }

  private EnvFilterYaml constructEnvFilterYaml(GenericEntityFilter appFilter, EnvFilter envFilter) {
    EnvFilterYamlBuilder builder = EnvFilterYaml.builder();

    if (isNotEmpty(envFilter.getIds())) {
      String appId = getAppId(appFilter);
      List<String> names = envFilter.getIds()
                               .stream()
                               .map(entityId -> {
                                 Environment environment = environmentService.get(appId, entityId);
                                 if (environment != null) {
                                   return environment.getName();
                                 }
                                 return null;
                               })
                               .collect(Collectors.toList());

      names.remove(null);
      builder.entityNames(names);
    }

    return builder.filterTypes(Lists.newArrayList(envFilter.getFilterTypes())).build();
  }

  private GenericEntityFilter constructGenericEntityFilter(GenericEntityFilterYaml yaml, String accountId) {
    GenericEntityFilterBuilder builder = GenericEntityFilter.builder();

    if (isNotEmpty(yaml.getEntityNames())) {
      Set<String> ids = yaml.getEntityNames()
                            .stream()
                            .map(entityName -> {
                              Application app = appService.getAppByName(accountId, entityName);
                              if (app != null) {
                                return app.getUuid();
                              }
                              return null;
                            })
                            .collect(Collectors.toSet());

      ids.remove(null);
      builder.ids(ids);
    }

    return builder.filterType(yaml.getFilterType()).build();
  }

  private GenericEntityFilterYaml constructGenericEntityFilterYaml(GenericEntityFilter appFilter) {
    GenericEntityFilterYamlBuilder builder = builder();

    if (isNotEmpty(appFilter.getIds())) {
      List<String> names = appFilter.getIds()
                               .stream()
                               .map(entityId -> {
                                 Application app = appService.get(entityId);
                                 if (app != null) {
                                   return app.getName();
                                 }
                                 return null;
                               })
                               .collect(Collectors.toList());

      names.remove(null);
      builder.names(names);
    }

    return builder.filterType(appFilter.getFilterType()).build();
  }

  private UsageRestrictions toBean(ChangeContext<UsageRestrictions.Yaml> changeContext) {
    UsageRestrictionsBuilder usageRestrictionsBuilder = UsageRestrictions.builder();
    String accountId = changeContext.getChange().getAccountId();
    UsageRestrictions.Yaml yaml = changeContext.getYaml();

    if (yaml == null) {
      return null;
    }

    if (isEmpty(yaml.getAppEnvRestrictions())) {
      return null;
    }

    Set<AppEnvRestriction> appEnvRestrictions =
        yaml.getAppEnvRestrictions()
            .stream()
            .map(appEnvRestrictionYaml -> constructAppEnvRestriction(appEnvRestrictionYaml, accountId))
            .collect(Collectors.toSet());
    usageRestrictionsBuilder.appEnvRestrictions(appEnvRestrictions);
    return usageRestrictionsBuilder.build();
  }

  @Override
  public UsageRestrictions.Yaml toYaml(UsageRestrictions usageRestrictions, String appId) {
    if (usageRestrictions == null) {
      return null;
    }

    if (isEmpty(usageRestrictions.getAppEnvRestrictions())) {
      return null;
    }

    UsageRestrictions.Yaml.YamlBuilder usageRestrictionsYamlBuilder = UsageRestrictions.Yaml.builder();
    List<UsageRestrictionsAppEnvRestrictionYaml> appEnvRestrictionYamlList =
        usageRestrictions.getAppEnvRestrictions()
            .stream()
            .map(this::constructAppEnvRestrictionYaml)
            .collect(Collectors.toList());
    usageRestrictionsYamlBuilder.appEnvRestrictions(appEnvRestrictionYamlList);
    return usageRestrictionsYamlBuilder.build();
  }

  @Override
  public UsageRestrictions upsertFromYaml(
      ChangeContext<UsageRestrictions.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return toBean(changeContext);
  }

  @Override
  public Class getYamlClass() {
    return UsageRestrictions.Yaml.class;
  }

  @Override
  public UsageRestrictions get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<UsageRestrictions.Yaml> changeContext) {
    // do nothing
  }
}
