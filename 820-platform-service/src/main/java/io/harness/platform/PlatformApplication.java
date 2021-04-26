package io.harness.platform;

import static io.harness.AuthorizationServiceHeader.BEARER;
import static io.harness.AuthorizationServiceHeader.DEFAULT;
import static io.harness.AuthorizationServiceHeader.IDENTITY_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.platform.PlatformConfiguration.getPlatformServiceCombinedResourceClasses;
import static io.harness.platform.audit.AuditServiceSetup.AUDIT_SERVICE;
import static io.harness.platform.notification.NotificationServiceSetup.NOTIFICATION_SERVICE;
import static io.harness.platform.resourcegroup.ResourceGroupServiceSetup.RESOURCE_GROUP_SERVICE;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.stream.Collectors.toSet;

import io.harness.GodInjector;
import io.harness.annotations.dev.OwnedBy;
import io.harness.health.HealthService;
import io.harness.maintenance.MaintenanceController;
import io.harness.metrics.MetricRegistryModule;
import io.harness.ng.core.exceptionmappers.GenericExceptionMapperV2;
import io.harness.ng.core.exceptionmappers.JerseyViolationExceptionMapperV2;
import io.harness.ng.core.exceptionmappers.WingsExceptionMapperV2;
import io.harness.notification.eventbackbone.MessageConsumer;
import io.harness.notification.exception.NotificationExceptionMapper;
import io.harness.platform.audit.AuditServiceModule;
import io.harness.platform.audit.AuditServiceSetup;
import io.harness.platform.notification.NotificationServiceModule;
import io.harness.platform.notification.NotificationServiceSetup;
import io.harness.platform.remote.HealthResource;
import io.harness.platform.resourcegroup.ResourceGroupServiceModule;
import io.harness.platform.resourcegroup.ResourceGroupServiceSetup;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.security.InternalApiAuthFilter;
import io.harness.security.NextGenAuthenticationFilter;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.PublicApi;
import io.harness.threading.ExecutorModule;
import io.harness.threading.ThreadPool;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;

@Slf4j
@OwnedBy(PL)
public class PlatformApplication extends Application<PlatformConfiguration> {
  private static final String APPLICATION_NAME = "Platform Microservice";

  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));
    new PlatformApplication().run(args);
  }

  private final MetricRegistry metricRegistry = new MetricRegistry();

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<PlatformConfiguration> bootstrap) {
    initializeLogging();
    bootstrap.addBundle(new SwaggerBundle<PlatformConfiguration>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(PlatformConfiguration appConfig) {
        return getSwaggerConfiguration();
      }
    });
    bootstrap.addCommand(new InspectCommand<>(this));
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    configureObjectMapper(bootstrap.getObjectMapper());
  }

  public static void configureObjectMapper(final ObjectMapper mapper) {
    NGObjectMapperHelper.configureNGObjectMapper(mapper);
  }

  @Override
  public void run(PlatformConfiguration appConfig, Environment environment) {
    ExecutorModule.getInstance().setExecutorService(ThreadPool.create(
        20, 100, 500L, TimeUnit.MILLISECONDS, new ThreadFactoryBuilder().setNameFormat("main-app-pool-%d").build()));
    log.info("Starting Platform Application ...");
    MaintenanceController.forceMaintenance(true);
    GodInjector godInjector = new GodInjector();
    godInjector.put(NOTIFICATION_SERVICE,
        Guice.createInjector(new NotificationServiceModule(appConfig), new MetricRegistryModule(metricRegistry)));
    if (appConfig.getResoureGroupServiceConfig().isEnableResourceGroup()) {
      godInjector.put(RESOURCE_GROUP_SERVICE,
          Guice.createInjector(new ResourceGroupServiceModule(appConfig), new MetricRegistryModule(metricRegistry)));
    }
    if (appConfig.getAuditServiceConfig().isEnableAuditService()) {
      godInjector.put(AUDIT_SERVICE,
          Guice.createInjector(new AuditServiceModule(appConfig), new MetricRegistryModule(metricRegistry)));
    }

    registerCommonResources(appConfig, environment, godInjector);
    registerCorsFilter(appConfig, environment);
    registerJerseyProviders(environment);
    registerJerseyFeatures(environment);
    registerAuthFilters(appConfig, environment);

    new NotificationServiceSetup().setup(
        appConfig.getNotificationServiceConfig(), environment, godInjector.get(NOTIFICATION_SERVICE));
    if (appConfig.getResoureGroupServiceConfig().isEnableResourceGroup()) {
      new ResourceGroupServiceSetup().setup(
          appConfig.getResoureGroupServiceConfig(), environment, godInjector.get(RESOURCE_GROUP_SERVICE));
    }
    if (appConfig.getAuditServiceConfig().isEnableAuditService()) {
      new AuditServiceSetup().setup(appConfig.getAuditServiceConfig(), environment, godInjector.get(AUDIT_SERVICE));
    }
    MaintenanceController.forceMaintenance(false);
    new Thread(godInjector.get(NOTIFICATION_SERVICE).getInstance(MessageConsumer.class)).start();
  }

  private void registerCommonResources(
      PlatformConfiguration appConfig, Environment environment, GodInjector godInjector) {
    if (Resource.isAcceptable(HealthResource.class)) {
      List<HealthService> healthServices = new ArrayList<>();
      healthServices.add(godInjector.get(NOTIFICATION_SERVICE).getInstance(HealthService.class));
      if (appConfig.getResoureGroupServiceConfig().isEnableResourceGroup()) {
        healthServices.add(godInjector.get(RESOURCE_GROUP_SERVICE).getInstance(HealthService.class));
      }
      if (appConfig.getAuditServiceConfig().isEnableAuditService()) {
        healthServices.add(godInjector.get(AUDIT_SERVICE).getInstance(HealthService.class));
      }
      environment.jersey().register(new HealthResource(healthServices));
    }
  }

  private void registerJerseyFeatures(Environment environment) {
    environment.jersey().register(MultiPartFeature.class);
  }

  private void registerCorsFilter(PlatformConfiguration appConfig, Environment environment) {
    FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    String allowedOrigins = String.join(",", appConfig.getAllowedOrigins());
    cors.setInitParameters(of("allowedOrigins", allowedOrigins, "allowedHeaders",
        "X-Requested-With,Content-Type,Accept,Origin,Authorization,X-api-key", "allowedMethods",
        "OPTIONS,GET,PUT,POST,DELETE,HEAD", "preflightMaxAge", "86400"));
    cors.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
  }

  private void registerJerseyProviders(Environment environment) {
    environment.jersey().register(NotificationExceptionMapper.class);
    environment.jersey().register(JerseyViolationExceptionMapperV2.class);
    environment.jersey().register(WingsExceptionMapperV2.class);
    environment.jersey().register(GenericExceptionMapperV2.class);
  }

  public SwaggerBundleConfiguration getSwaggerConfiguration() {
    SwaggerBundleConfiguration defaultSwaggerBundleConfiguration = new SwaggerBundleConfiguration();
    String resourcePackage = String.join(",", getUniquePackages(getPlatformServiceCombinedResourceClasses()));
    defaultSwaggerBundleConfiguration.setResourcePackage(resourcePackage);
    defaultSwaggerBundleConfiguration.setSchemes(new String[] {"https", "http"});
    defaultSwaggerBundleConfiguration.setVersion("1.0");
    defaultSwaggerBundleConfiguration.setSchemes(new String[] {"https", "http"});
    defaultSwaggerBundleConfiguration.setHost("{{host}}");
    defaultSwaggerBundleConfiguration.setTitle("Platform Service API Reference");
    return defaultSwaggerBundleConfiguration;
  }

  private void registerAuthFilters(PlatformConfiguration configuration, Environment environment) {
    if (configuration.isEnableAuth()) {
      registerNextGenAuthFilter(configuration, environment);
      registerInternalApiAuthFilter(configuration, environment);
    }
  }

  private void registerNextGenAuthFilter(PlatformConfiguration configuration, Environment environment) {
    Predicate<Pair<ResourceInfo, ContainerRequestContext>> predicate =
        (getAuthenticationExemptedRequestsPredicate().negate())
            .and((getAuthFilterPredicate(InternalApi.class)).negate());
    Map<String, String> serviceToSecretMapping = new HashMap<>();
    serviceToSecretMapping.put(BEARER.getServiceId(), configuration.getPlatformSecrets().getJwtAuthSecret());
    serviceToSecretMapping.put(
        IDENTITY_SERVICE.getServiceId(), configuration.getPlatformSecrets().getJwtIdentityServiceSecret());
    serviceToSecretMapping.put(DEFAULT.getServiceId(), configuration.getPlatformSecrets().getNgManagerServiceSecret());
    environment.jersey().register(new NextGenAuthenticationFilter(predicate, null, serviceToSecretMapping));
  }

  private void registerInternalApiAuthFilter(PlatformConfiguration configuration, Environment environment) {
    Map<String, String> serviceToSecretMapping = new HashMap<>();
    serviceToSecretMapping.put(DEFAULT.getServiceId(), configuration.getPlatformSecrets().getNgManagerServiceSecret());
    environment.jersey().register(
        new InternalApiAuthFilter(getAuthFilterPredicate(InternalApi.class), null, serviceToSecretMapping));
  }

  private Predicate<Pair<ResourceInfo, ContainerRequestContext>> getAuthenticationExemptedRequestsPredicate() {
    return getAuthFilterPredicate(PublicApi.class)
        .or(resourceInfoAndRequest
            -> resourceInfoAndRequest.getValue().getUriInfo().getAbsolutePath().getPath().endsWith("api/version")
                || resourceInfoAndRequest.getValue().getUriInfo().getAbsolutePath().getPath().endsWith("api/swagger")
                || resourceInfoAndRequest.getValue().getUriInfo().getAbsolutePath().getPath().endsWith(
                    "api/swagger.json")
                || resourceInfoAndRequest.getValue().getUriInfo().getAbsolutePath().getPath().endsWith(
                    "api/swagger.yaml"));
  }

  private Predicate<Pair<ResourceInfo, ContainerRequestContext>> getAuthFilterPredicate(
      Class<? extends Annotation> annotation) {
    return resourceInfoAndRequest
        -> (resourceInfoAndRequest.getKey().getResourceMethod() != null
               && resourceInfoAndRequest.getKey().getResourceMethod().getAnnotation(annotation) != null)
        || (resourceInfoAndRequest.getKey().getResourceClass() != null
            && resourceInfoAndRequest.getKey().getResourceClass().getAnnotation(annotation) != null);
  }

  private static Set<String> getUniquePackages(Collection<Class<?>> classes) {
    return classes.stream().map(aClass -> aClass.getPackage().getName()).collect(toSet());
  }
}
