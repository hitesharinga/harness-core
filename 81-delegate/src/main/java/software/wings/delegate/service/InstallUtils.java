package software.wings.delegate.service;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.network.Http.getBaseUrl;
import static java.lang.String.format;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import software.wings.delegate.app.DelegateConfiguration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Slf4j
public class InstallUtils {
  private static final String defaultKubectlVersion = "v1.13.2";
  private static final String kubectlBaseDir = "./client-tools/kubectl/";

  private static final String goTemplateClientVersion = "v0.3";
  private static final String goTemplateClientBaseDir = "./client-tools/go-template/";

  private static final String helmVersion = "v2.13.1";
  private static final String helmBaseDir = "./client-tools/helm/";

  private static final String chartMuseumVersion = "v0.8.2";
  private static final String chartMuseumBaseDir = "./client-tools/chartmuseum/";

  private static String kubectlPath = "kubectl";
  private static String goTemplateToolPath = "go-template";
  private static String helmPath = "helm";
  private static String chartMuseumPath = "chartmuseum";

  public static String getKubectlPath() {
    return kubectlPath;
  }

  public static String getGoTemplateToolPath() {
    return goTemplateToolPath;
  }

  public static String getHelmPath() {
    return helmPath;
  }

  public static String getChartMuseumPath() {
    return chartMuseumPath;
  }

  static boolean installKubectl(DelegateConfiguration configuration) {
    try {
      if (StringUtils.isNotEmpty(configuration.getKubectlPath())) {
        kubectlPath = configuration.getKubectlPath();
        logger.info("Found user configured kubectl at {}. Skipping Install.", kubectlPath);
        return true;
      }

      if (isWindows()) {
        logger.info("Skipping kubectl install on Windows");
        return true;
      }

      String version = System.getenv().get("KUBECTL_VERSION");

      if (StringUtils.isEmpty(version)) {
        version = defaultKubectlVersion;
        logger.info("No version configured. Using default kubectl version", version);
      }

      String kubectlDirectory = kubectlBaseDir + version;

      if (validateKubectlExists(kubectlDirectory)) {
        kubectlPath = Paths.get(kubectlDirectory + "/kubectl").toAbsolutePath().normalize().toString();
        logger.info("kubectl version {} already installed", version);
        return true;
      }

      logger.info("Installing kubectl");

      createDirectoryIfDoesNotExist(kubectlDirectory);

      String downloadUrl = getKubectlDownloadUrl(getManagerBaseUrl(configuration.getManagerUrl()), version);

      logger.info("download Url is {}", downloadUrl);

      String script = "curl $PROXY_CURL -LO " + downloadUrl + "\n"
          + "chmod +x ./kubectl\n"
          + "./kubectl version --short --client\n";

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(kubectlDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();

      if (result.getExitValue() == 0) {
        kubectlPath = Paths.get(kubectlDirectory + "/kubectl").toAbsolutePath().normalize().toString();
        logger.info(result.outputString());
        if (validateKubectlExists(kubectlDirectory)) {
          logger.info("kubectl path: {}", kubectlPath);
          return true;
        } else {
          logger.error("kubectl not validated after download: {}", kubectlPath);
          return false;
        }
      } else {
        logger.error("kubectl install failed");
        logger.error(result.outputString());
        return false;
      }
    } catch (Exception e) {
      logger.error("Error installing kubectl", e);
      return false;
    }
  }

  private static boolean validateKubectlExists(String kubectlDirectory) {
    try {
      if (!Files.exists(Paths.get(kubectlDirectory + "/kubectl"))) {
        return false;
      }

      String script = "./kubectl version --short --client\n";
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(1, TimeUnit.MINUTES)
                                            .directory(new File(kubectlDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();

      if (result.getExitValue() == 0) {
        logger.info(result.outputString());
        return true;
      } else {
        logger.error(result.outputString());
        return false;
      }
    } catch (Exception e) {
      logger.error("Error checking kubectl", e);
      return false;
    }
  }

  private static String getKubectlDownloadUrl(String managerBaseUrl, String version) {
    return managerBaseUrl + "storage/harness-download/kubernetes-release/release/" + version + "/bin/" + getOsPath()
        + "/amd64/kubectl";
  }

  static boolean installGoTemplateTool(DelegateConfiguration configuration) {
    try {
      if (isWindows()) {
        logger.info("Skipping go-template install on Windows");
        return true;
      }

      String goTemplateClientDirectory = goTemplateClientBaseDir + goTemplateClientVersion;

      if (validateGoTemplateClientExists(goTemplateClientDirectory)) {
        goTemplateToolPath =
            Paths.get(goTemplateClientDirectory + "/go-template").toAbsolutePath().normalize().toString();
        logger.info("go-template version {} already installed", goTemplateClientVersion);
        return true;
      }

      logger.info("Installing go-template");

      createDirectoryIfDoesNotExist(goTemplateClientDirectory);

      String downloadUrl =
          getGoTemplateDownloadUrl(getManagerBaseUrl(configuration.getManagerUrl()), goTemplateClientVersion);

      logger.info("download Url is {}", downloadUrl);

      String script = "curl $PROXY_CURL -LO " + downloadUrl + "\n"
          + "chmod +x ./go-template\n"
          + "./go-template -v\n";

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(goTemplateClientDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();

      if (result.getExitValue() == 0) {
        goTemplateToolPath =
            Paths.get(goTemplateClientDirectory + "/go-template").toAbsolutePath().normalize().toString();
        logger.info(result.outputString());
        if (validateGoTemplateClientExists(goTemplateClientDirectory)) {
          logger.info("go-template path: {}", goTemplateToolPath);
          return true;
        } else {
          logger.error("go-template not validated after download: {}", goTemplateToolPath);
          return false;
        }
      } else {
        logger.error("go-template install failed");
        logger.error(result.outputString());
        return false;
      }
    } catch (Exception e) {
      logger.error("Error installing go-template", e);
      return false;
    }
  }

  private static boolean validateGoTemplateClientExists(String goTemplateClientDirectory) {
    try {
      if (!Files.exists(Paths.get(goTemplateClientDirectory + "/go-template"))) {
        return false;
      }

      String script = "./go-template -v\n";
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(1, TimeUnit.MINUTES)
                                            .directory(new File(goTemplateClientDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();

      if (result.getExitValue() == 0) {
        logger.info(result.outputString());
        return true;
      } else {
        logger.error(result.outputString());
        return false;
      }
    } catch (Exception e) {
      logger.error("Error checking go-template", e);
      return false;
    }
  }

  private static String getGoTemplateDownloadUrl(String managerBaseUrl, String version) {
    return managerBaseUrl + "storage/harness-download/snapshot-go-template/release/" + version + "/bin/" + getOsPath()
        + "/amd64/go-template";
  }

  private static String getManagerBaseUrl(String managerUrl) {
    if (managerUrl.contains("localhost") || managerUrl.contains("127.0.0.1")) {
      return "https://app.harness.io/";
    }

    return getBaseUrl(managerUrl);
  }

  private static String getOsPath() {
    String osName = System.getProperty("os.name").toLowerCase();

    if (osName.startsWith("windows")) {
      return "windows";
    }

    if (osName.startsWith("mac")) {
      return "darwin";
    }

    return "linux";
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").startsWith("Windows");
  }

  private static boolean validateHelmExists(String helmDirectory) {
    try {
      if (!Files.exists(Paths.get(helmDirectory + "/helm"))) {
        return false;
      }

      String script = "./helm version -c";
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(1, TimeUnit.MINUTES)
                                            .directory(new File(helmDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);

      ProcessResult result = processExecutor.execute();
      if (result.getExitValue() == 0) {
        logger.info(result.outputString());
        return true;
      } else {
        logger.error(result.outputString());
        return false;
      }

    } catch (Exception e) {
      logger.error("Error checking helm", e);
      return false;
    }
  }

  private static String getHelmDownloadUrl(String managerBaseUrl, String version) {
    return managerBaseUrl + "storage/harness-download/harness-helm/release/" + version + "/bin/" + getOsPath()
        + "/amd64/helm";
  }

  private static boolean initHelmClient() throws Exception {
    logger.info("Init helm client only");

    String helmDirectory = helmBaseDir + helmVersion;
    String script = "./helm init -c --skip-refresh \n";

    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .timeout(10, TimeUnit.MINUTES)
                                          .directory(new File(helmDirectory))
                                          .command("/bin/bash", "-c", script)
                                          .readOutput(true);
    ProcessResult result = processExecutor.execute();
    if (result.getExitValue() == 0) {
      logger.info("Successfully init helm client");
      return true;
    } else {
      logger.error("Helm client init failed");
      logger.error(result.outputString());
      return false;
    }
  }

  static boolean installHelm(DelegateConfiguration configuration) {
    try {
      if (isNotEmpty(configuration.getHelmPath())) {
        helmPath = configuration.getHelmPath();
        logger.info("Found user configured helm at {}. Skipping Install.", helmPath);
        return true;
      }

      if (isWindows()) {
        logger.info("Skipping helm install on Windows");
        return true;
      }

      String helmDirectory = helmBaseDir + helmVersion;
      if (validateHelmExists(helmDirectory)) {
        helmPath = Paths.get(helmDirectory + "/helm").toAbsolutePath().normalize().toString();
        logger.info(format("helm version %s already installed", helmVersion));

        return initHelmClient();
      }

      logger.info("Installing helm");
      createDirectoryIfDoesNotExist(helmDirectory);

      String downloadUrl = getHelmDownloadUrl(getManagerBaseUrl(configuration.getManagerUrl()), helmVersion);
      logger.info("Download Url is " + downloadUrl);

      String script = "curl $PROXY_CURL -LO " + downloadUrl + " \n"
          + "chmod +x ./helm \n"
          + "./helm version -c \n"
          + "./helm init -c --skip-refresh \n";

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(helmDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();

      if (result.getExitValue() == 0) {
        helmPath = Paths.get(helmDirectory + "/helm").toAbsolutePath().normalize().toString();
        logger.info(result.outputString());

        if (validateHelmExists(helmDirectory)) {
          logger.info("helm path: {}", helmPath);
          return true;
        } else {
          logger.error("helm not validated after download: {}", helmPath);
          return false;
        }
      } else {
        logger.error("helm install failed");
        logger.error(result.outputString());
        return false;
      }
    } catch (Exception e) {
      logger.error("Error installing helm", e);
      return false;
    }
  }

  private static boolean validateChartMuseumExists(String chartMuseumDirectory) {
    try {
      if (!Files.exists(Paths.get(chartMuseumDirectory + "/chartmuseum"))) {
        return false;
      }

      String script = "./chartmuseum -v";
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(1, TimeUnit.MINUTES)
                                            .directory(new File(chartMuseumDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);

      ProcessResult result = processExecutor.execute();
      if (result.getExitValue() == 0) {
        logger.info(result.outputString());
        return true;
      } else {
        logger.error(result.outputString());
        return false;
      }

    } catch (Exception e) {
      logger.error("Error checking chart museum", e);
      return false;
    }
  }

  private static String getChartMuseumDownloadUrl(String managerBaseUrl, String version) {
    return managerBaseUrl + "storage/harness-download/harness-chartmuseum/release/" + version + "/bin/" + getOsPath()
        + "/amd64/chartmuseum";
  }

  static boolean installChartMuseum(DelegateConfiguration configuration) {
    try {
      if (isWindows()) {
        logger.info("Skipping chart museum install on Windows");
        return true;
      }

      String chartMuseumDirectory = chartMuseumBaseDir + chartMuseumVersion;
      if (validateChartMuseumExists(chartMuseumDirectory)) {
        chartMuseumPath = Paths.get(chartMuseumDirectory + "/chartmuseum").toAbsolutePath().normalize().toString();
        logger.info(format("chartmuseum version %s already installed", chartMuseumVersion));
        return true;
      }

      logger.info("Installing chartmuseum");
      createDirectoryIfDoesNotExist(chartMuseumDirectory);

      String downloadUrl =
          getChartMuseumDownloadUrl(getManagerBaseUrl(configuration.getManagerUrl()), chartMuseumVersion);
      logger.info("Download Url is " + downloadUrl);

      String script = "curl $PROXY_CURL -LO " + downloadUrl + "\n"
          + "chmod +x ./chartmuseum \n"
          + "./chartmuseum -v \n";

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(chartMuseumDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);

      ProcessResult result = processExecutor.execute();
      if (result.getExitValue() == 0) {
        chartMuseumPath = Paths.get(chartMuseumDirectory + "/chartmuseum").toAbsolutePath().normalize().toString();
        logger.info(result.outputString());

        if (validateChartMuseumExists(chartMuseumDirectory)) {
          logger.info("chartmuseum path: {}", chartMuseumPath);
          return true;
        } else {
          logger.error("chartmuseum not validated after download: {}", chartMuseumPath);
          return false;
        }
      } else {
        logger.error("chart museum install failed");
        logger.error(result.outputString());
        return false;
      }

    } catch (Exception e) {
      logger.error("Error installing chart museum", e);
      return false;
    }
  }
}
