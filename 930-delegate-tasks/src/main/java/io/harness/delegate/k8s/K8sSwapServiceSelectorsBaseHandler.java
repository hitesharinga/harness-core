package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.k8s.KubernetesHelperService.toDisplayYaml;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.k8s.exception.KubernetesExceptionExplanation;
import io.harness.delegate.task.k8s.exception.KubernetesExceptionHints;
import io.harness.delegate.task.k8s.exception.KubernetesExceptionMessages;
import io.harness.exception.KubernetesTaskException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.api.model.Service;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDP)
@Singleton
@Slf4j
public class K8sSwapServiceSelectorsBaseHandler {
  @Inject private KubernetesContainerService kubernetesContainerService;

  public boolean swapServiceSelectors(
      KubernetesConfig kubernetesConfig, String serviceOne, String serviceTwo, LogCallback logCallback) {
    return swapServiceSelectors(kubernetesConfig, serviceOne, serviceTwo, logCallback, false);
  }

  public boolean swapServiceSelectors(KubernetesConfig kubernetesConfig, String serviceOne, String serviceTwo,
      LogCallback logCallback, boolean isErrorFrameworkSupported) {
    Service service1 = kubernetesContainerService.getServiceFabric8(kubernetesConfig, serviceOne);
    Service service2 = kubernetesContainerService.getServiceFabric8(kubernetesConfig, serviceTwo);

    if (service1 == null) {
      return handleServiceNotFound(serviceOne, logCallback, isErrorFrameworkSupported,
          format(KubernetesExceptionMessages.BG_SWAP_SERVICES_FAILED, serviceOne, serviceTwo));
    }

    if (service2 == null) {
      return handleServiceNotFound(serviceTwo, logCallback, isErrorFrameworkSupported,
          format(KubernetesExceptionMessages.BG_SWAP_SERVICES_FAILED, serviceOne, serviceTwo));
    }

    Map<String, String> serviceOneSelectors = service1.getSpec().getSelector();
    Map<String, String> serviceTwoSelectors = service2.getSpec().getSelector();

    logCallback.saveExecutionLog(format("%nSelectors for Service One : [name:%s]%n%s", service1.getMetadata().getName(),
                                     toDisplayYaml(service1.getSpec().getSelector())),
        LogLevel.INFO);

    logCallback.saveExecutionLog(format("%nSelectors for Service Two : [name:%s]%n%s", service2.getMetadata().getName(),
                                     toDisplayYaml(service2.getSpec().getSelector())),
        LogLevel.INFO);

    logCallback.saveExecutionLog(format("%nSwapping Service Selectors..%n"), LogLevel.INFO);

    service1.getSpec().setSelector(serviceTwoSelectors);
    service2.getSpec().setSelector(serviceOneSelectors);

    Service serviceOneUpdated = kubernetesContainerService.createOrReplaceService(kubernetesConfig, service1);

    Service serviceTwoUpdated = kubernetesContainerService.createOrReplaceService(kubernetesConfig, service2);

    logCallback.saveExecutionLog(
        format("%nUpdated Selectors for Service One : [name:%s]%n%s", serviceOneUpdated.getMetadata().getName(),
            toDisplayYaml(serviceOneUpdated.getSpec().getSelector())),
        LogLevel.INFO);

    logCallback.saveExecutionLog(
        format("%nUpdated Selectors for Service Two : [name:%s]%n%s", serviceTwoUpdated.getMetadata().getName(),
            toDisplayYaml(serviceTwoUpdated.getSpec().getSelector())),
        LogLevel.INFO);

    logCallback.saveExecutionLog("Done", LogLevel.INFO, CommandExecutionStatus.SUCCESS);

    return true;
  }

  private boolean handleServiceNotFound(
      String service, LogCallback logCallback, boolean isErrorFrameworkSupported, String message) {
    logCallback.saveExecutionLog(
        format("Service %s not found.", service), LogLevel.ERROR, CommandExecutionStatus.FAILURE);
    if (isErrorFrameworkSupported) {
      throw NestedExceptionUtils.hintWithExplanationException(
          KubernetesExceptionHints.BG_SWAP_SERVICES_SERVICE_NOT_FOUND,
          format(KubernetesExceptionExplanation.BG_SWAP_SERVICES_SERVICE_NOT_FOUND, service),
          new KubernetesTaskException(message));
    }
    return false;
  }
}
