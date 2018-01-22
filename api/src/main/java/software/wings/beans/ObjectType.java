/**
 *
 */

package software.wings.beans;

/**
 * Some of the bean objects are not entities in mongo db. They are used as embedded objects.
 * In other words, they don't extend Base or UuidAware.
 *
 * @author rktummala on 10/28/17
 */
public interface ObjectType {
  String PHASE = "PHASE";
  String PHASE_STEP = "PHASE_STEP";
  String STEP = "STEP";
  String NAME_VALUE_PAIR = "NAME_VALUE_PAIR";
  String TEMPLATE_EXPRESSION = "TEMPLATE_EXPRESSION";
  String NOTIFICATION_RULE = "NOTIFICATION_RULE";

  String VARIABLE = "VARIABLE";
  String FAILURE_STRATEGY = "FAILURE_STRATEGY";
  String NOTIFICATION_GROUP = "NOTIFICATION_GROUP";
  String PIPELINE_STAGE = "PIPELINE_STAGE";
  String COMMAND_UNIT = "COMMAND_UNIT";
  String ECS_CONTAINER_TASK = "ECS_CONTAINER_TASK";
  String KUBERNETES_CONTAINER_TASK = "KUBERNETES_CONTAINER_TASK";
  String AWS_LAMBDA_SPEC = "AWS_LAMBDA_SPEC";
  String CONTAINER_DEFINITION = "CONTAINER_DEFINITION";
  String LOG_CONFIGURATION = "LOG_CONFIGURATION";
  String PORT_MAPPING = "PORT_MAPPING";
  String STORAGE_CONFIGURATION = "STORAGE_CONFIGURATION";
  String LAMBDA_SPECIFICATION = "LAMBDA_SPECIFICATION";
  String DEFAULT_SPECIFICATION = "DEFAULT_SPECIFICATION";
  String FUNCTION_SPECIFICATION = "FUNCTION_SPECIFICATION";
  String SETTING_ATTRIBUTE = "SETTING_ATTRIBUTE";
  String SETTING_VALUE = "SETTING_VALUE";
  String APPLICATION_DEFAULTS = "APPLICATION_DEFAULTS";
  String ACCOUNT_DEFAULTS = "ACCOUNT_DEFAULTS";
}
