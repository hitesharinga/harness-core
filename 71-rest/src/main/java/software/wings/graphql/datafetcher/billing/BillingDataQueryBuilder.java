package software.wings.graphql.datafetcher.billing;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.Converter;
import com.healthmarketscience.sqlbuilder.CustomExpression;
import com.healthmarketscience.sqlbuilder.FunctionCall;
import com.healthmarketscience.sqlbuilder.InCondition;
import com.healthmarketscience.sqlbuilder.OrderObject;
import com.healthmarketscience.sqlbuilder.OrderObject.Dir;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.UnaryCondition;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import io.fabric8.utils.Lists;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataQueryMetadataBuilder;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.ResultType;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLAggregationKind;
import software.wings.graphql.schema.type.aggregation.QLFilterKind;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilterType;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortType;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMTimeSeriesAggregation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class BillingDataQueryBuilder {
  private BillingDataTableSchema schema = new BillingDataTableSchema();
  private static final String DEFAULT_TIME_ZONE = "America/Los_Angeles";
  private static final String STANDARD_TIME_ZONE = "GMT";

  protected BillingDataQueryMetadata formQuery(String accountId, List<QLBillingDataFilter> filters,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLCCMEntityGroupBy> groupBy,
      QLCCMTimeSeriesAggregation groupByTime, List<QLBillingSortCriteria> sortCriteria) {
    BillingDataQueryMetadataBuilder queryMetaDataBuilder = BillingDataQueryMetadata.builder();
    SelectQuery selectQuery = new SelectQuery();

    ResultType resultType;
    resultType = ResultType.STACKED_TIME_SERIES;

    queryMetaDataBuilder.resultType(resultType);
    List<BillingDataMetaDataFields> fieldNames = new ArrayList<>();
    List<BillingDataMetaDataFields> groupByFields = new ArrayList<>();

    if (isGroupByClusterPresent(groupBy) || isNoneGroupBySelectedWithoutFilterInClusterView(groupBy, filters)) {
      addInstanceTypeFilter(filters);
    }

    decorateQueryWithAggregations(selectQuery, aggregateFunction, fieldNames);

    selectQuery.addCustomFromTable(schema.getBillingDataTable());

    if (!Lists.isNullOrEmpty(filters)) {
      decorateQueryWithFilters(selectQuery, filters);
    }

    if (isValidGroupByTime(groupByTime)) {
      decorateQueryWithGroupByTime(fieldNames, selectQuery, groupByTime, groupByFields);
    }

    if (isValidGroupBy(groupBy)) {
      decorateQueryWithGroupBy(fieldNames, selectQuery, groupBy, groupByFields);
    }

    List<QLBillingSortCriteria> finalSortCriteria = validateAndAddSortCriteria(selectQuery, sortCriteria, fieldNames);

    addAccountFilter(selectQuery, accountId);

    selectQuery.getWhereClause().setDisableParens(true);
    queryMetaDataBuilder.fieldNames(fieldNames);
    queryMetaDataBuilder.query(selectQuery.toString());
    queryMetaDataBuilder.groupByFields(groupByFields);
    queryMetaDataBuilder.sortCriteria(finalSortCriteria);
    queryMetaDataBuilder.filters(filters);
    return queryMetaDataBuilder.build();
  }

  protected BillingDataQueryMetadata formTrendStatsQuery(
      String accountId, QLCCMAggregationFunction aggregateFunction, List<QLBillingDataFilter> filters) {
    List<QLCCMAggregationFunction> aggregationFunctions = new ArrayList<>();
    aggregationFunctions.add(aggregateFunction);
    return formTrendStatsQuery(accountId, aggregationFunctions, filters);
  }

  protected BillingDataQueryMetadata formTrendStatsQuery(
      String accountId, List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingDataFilter> filters) {
    BillingDataQueryMetadataBuilder queryMetaDataBuilder = BillingDataQueryMetadata.builder();
    SelectQuery selectQuery = new SelectQuery();

    List<BillingDataMetaDataFields> fieldNames = new ArrayList<>();

    decorateQueryWithAggregations(selectQuery, aggregateFunction, fieldNames);

    decorateQueryWithMinMaxStartTime(selectQuery, fieldNames);

    selectQuery.addCustomFromTable(schema.getBillingDataTable());

    if (isClusterFilterPresent(filters) && !checkForAdditionalFilterInClusterDrillDown(filters)) {
      addInstanceTypeFilter(filters);
    }

    if (!Lists.isNullOrEmpty(filters)) {
      decorateQueryWithFilters(selectQuery, filters);
    }

    addAccountFilter(selectQuery, accountId);

    selectQuery.getWhereClause().setDisableParens(true);
    queryMetaDataBuilder.fieldNames(fieldNames);
    queryMetaDataBuilder.query(selectQuery.toString());
    queryMetaDataBuilder.filters(filters);
    return queryMetaDataBuilder.build();
  }

  protected BillingDataQueryMetadata formFilterValuesQuery(
      String accountId, List<QLBillingDataFilter> filters, List<QLCCMEntityGroupBy> groupBy) {
    BillingDataQueryMetadataBuilder queryMetaDataBuilder = BillingDataQueryMetadata.builder();
    SelectQuery selectQuery = new SelectQuery();
    ResultType resultType;
    resultType = ResultType.STACKED_TIME_SERIES;

    queryMetaDataBuilder.resultType(resultType);
    List<BillingDataMetaDataFields> fieldNames = new ArrayList<>();
    List<BillingDataMetaDataFields> groupByFields = new ArrayList<>();

    if (isGroupByClusterPresent(groupBy)) {
      if (!isGroupByClusterTypePresent(groupBy)) {
        addClusterTypeGroupBy(groupBy);
      }
      if (!isGroupByClusterNamePresent(groupBy)) {
        addClusterNameGroupBy(groupBy);
      }
    }

    selectQuery.addCustomFromTable(schema.getBillingDataTable());

    if (!Lists.isNullOrEmpty(filters)) {
      decorateQueryWithFilters(selectQuery, filters);
    }

    if (isValidGroupBy(groupBy)) {
      decorateQueryWithGroupBy(fieldNames, selectQuery, groupBy, groupByFields);
    }

    addAccountFilter(selectQuery, accountId);

    selectQuery.getWhereClause().setDisableParens(true);
    queryMetaDataBuilder.fieldNames(fieldNames);
    queryMetaDataBuilder.query(selectQuery.toString());
    queryMetaDataBuilder.groupByFields(groupByFields);
    queryMetaDataBuilder.filters(filters);
    return queryMetaDataBuilder.build();
  }

  public BillingDataQueryMetadata formBudgetInsightQuery(String accountId, List<QLBillingDataFilter> filters,
      QLCCMAggregationFunction aggregateFunction, QLCCMTimeSeriesAggregation groupBy,
      List<QLBillingSortCriteria> sortCriteria) {
    BillingDataQueryMetadataBuilder queryMetaDataBuilder = BillingDataQueryMetadata.builder();
    SelectQuery selectQuery = new SelectQuery();
    ResultType resultType;
    resultType = ResultType.STACKED_TIME_SERIES;

    queryMetaDataBuilder.resultType(resultType);
    List<BillingDataMetaDataFields> fieldNames = new ArrayList<>();
    decorateQueryWithAggregation(selectQuery, aggregateFunction, fieldNames);

    selectQuery.addCustomFromTable(schema.getBillingDataTable());

    if (!Lists.isNullOrEmpty(filters)) {
      decorateQueryWithFilters(selectQuery, filters);
    }

    if (isValidGroupByTime(groupBy)) {
      List<BillingDataMetaDataFields> groupByFields = new ArrayList<>();
      decorateQueryWithGroupByTime(fieldNames, selectQuery, groupBy, groupByFields);
    }

    List<QLBillingSortCriteria> finalSortCriteria = validateAndAddSortCriteria(selectQuery, sortCriteria, fieldNames);

    addAccountFilter(selectQuery, accountId);

    selectQuery.getWhereClause().setDisableParens(true);
    queryMetaDataBuilder.fieldNames(fieldNames);
    queryMetaDataBuilder.query(selectQuery.toString());
    queryMetaDataBuilder.sortCriteria(finalSortCriteria);
    queryMetaDataBuilder.filters(filters);
    return queryMetaDataBuilder.build();
  }

  private void addAccountFilter(SelectQuery selectQuery, String accountId) {
    selectQuery.addCondition(BinaryCondition.equalTo(schema.getAccountId(), accountId));
  }

  private void decorateQueryWithAggregations(SelectQuery selectQuery, List<QLCCMAggregationFunction> aggregateFunctions,
      List<BillingDataMetaDataFields> fieldNames) {
    for (QLCCMAggregationFunction aggregationFunction : aggregateFunctions) {
      decorateQueryWithAggregation(selectQuery, aggregationFunction, fieldNames);
    }
  }

  private void decorateQueryWithAggregation(SelectQuery selectQuery, QLCCMAggregationFunction aggregationFunction,
      List<BillingDataMetaDataFields> fieldNames) {
    if (aggregationFunction != null && aggregationFunction.getOperationType() == QLCCMAggregateOperation.SUM) {
      if (aggregationFunction.getColumnName().equals(schema.getBillingAmount().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.sum().addColumnParams(schema.getBillingAmount()),
                BillingDataMetaDataFields.SUM.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.SUM);
      } else if (aggregationFunction.getColumnName().equals(schema.getIdleCost().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.sum().addColumnParams(schema.getIdleCost()),
                BillingDataMetaDataFields.IDLECOST.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.IDLECOST);
      } else if (aggregationFunction.getColumnName().equals(schema.getCpuIdleCost().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.sum().addColumnParams(schema.getCpuIdleCost()),
                BillingDataMetaDataFields.CPUIDLECOST.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.CPUIDLECOST);
      } else if (aggregationFunction.getColumnName().equals(schema.getMemoryIdleCost().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.sum().addColumnParams(schema.getMemoryIdleCost()),
                BillingDataMetaDataFields.MEMORYIDLECOST.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.MEMORYIDLECOST);
      }
    } else if (aggregationFunction != null && aggregationFunction.getOperationType() == QLCCMAggregateOperation.MAX) {
      if (aggregationFunction.getColumnName().equals(schema.getMaxCpuUtilization().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.max().addColumnParams(schema.getMaxCpuUtilization()),
                BillingDataMetaDataFields.MAXCPUUTILIZATION.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.MAXCPUUTILIZATION);
      } else if (aggregationFunction.getColumnName().equals(schema.getMaxMemoryUtilization().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.max().addColumnParams(schema.getMaxMemoryUtilization()),
                BillingDataMetaDataFields.MAXMEMORYUTILIZATION.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.MAXMEMORYUTILIZATION);
      }
    } else if (aggregationFunction != null && aggregationFunction.getOperationType() == QLCCMAggregateOperation.AVG) {
      if (aggregationFunction.getColumnName().equals(schema.getAvgCpuUtilization().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.avg().addColumnParams(schema.getAvgCpuUtilization()),
                BillingDataMetaDataFields.AVGCPUUTILIZATION.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.AVGCPUUTILIZATION);
      } else if (aggregationFunction.getColumnName().equals(schema.getAvgMemoryUtilization().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.avg().addColumnParams(schema.getAvgMemoryUtilization()),
                BillingDataMetaDataFields.AVGMEMORYUTILIZATION.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.AVGMEMORYUTILIZATION);
      }
    }
  }

  private void decorateQueryWithMinMaxStartTime(SelectQuery selectQuery, List<BillingDataMetaDataFields> fieldNames) {
    selectQuery.addCustomColumns(Converter.toColumnSqlObject(FunctionCall.min().addColumnParams(schema.getStartTime()),
        BillingDataMetaDataFields.MIN_STARTTIME.getFieldName()));
    fieldNames.add(BillingDataMetaDataFields.MIN_STARTTIME);
    selectQuery.addCustomColumns(Converter.toColumnSqlObject(FunctionCall.max().addColumnParams(schema.getStartTime()),
        BillingDataMetaDataFields.MAX_STARTTIME.getFieldName()));
    fieldNames.add(BillingDataMetaDataFields.MAX_STARTTIME);
  }

  private void decorateQueryWithFilters(SelectQuery selectQuery, List<QLBillingDataFilter> filters) {
    for (QLBillingDataFilter filter : filters) {
      Set<QLBillingDataFilterType> filterTypes = QLBillingDataFilter.getFilterTypes(filter);
      for (QLBillingDataFilterType type : filterTypes) {
        if (type.getMetaDataFields().getFilterKind() == QLFilterKind.SIMPLE) {
          decorateSimpleFilter(selectQuery, filter, type);
        } else {
          logger.error("Failed to apply filter :[{}]", filter);
        }
      }
    }
  }

  private void decorateSimpleFilter(SelectQuery selectQuery, QLBillingDataFilter filter, QLBillingDataFilterType type) {
    Filter f = QLBillingDataFilter.getFilter(type, filter);
    if (checkFilter(f)) {
      if (isIdFilter(f)) {
        addSimpleIdOperator(selectQuery, f, type);
      } else if (isTimeFilter(f)) {
        addSimpleTimeFilter(selectQuery, f, type);
      }
    } else {
      logger.info("Not adding filter since it is not valid " + f);
    }
  }

  private void addSimpleTimeFilter(SelectQuery selectQuery, Filter filter, QLBillingDataFilterType type) {
    DbColumn key = getFilterKey(type);
    QLTimeFilter timeFilter = (QLTimeFilter) filter;
    switch (timeFilter.getOperator()) {
      case BEFORE:
        selectQuery.addCondition(BinaryCondition.lessThanOrEq(key, Instant.ofEpochMilli((Long) timeFilter.getValue())));
        break;
      case AFTER:
        selectQuery.addCondition(
            BinaryCondition.greaterThanOrEq(key, Instant.ofEpochMilli((Long) timeFilter.getValue())));
        break;
      default:
        throw new InvalidRequestException("Invalid TimeFilter operator: " + filter.getOperator());
    }
  }

  private void addSimpleIdOperator(SelectQuery selectQuery, Filter filter, QLBillingDataFilterType type) {
    DbColumn key = getFilterKey(type);
    QLIdOperator operator = (QLIdOperator) filter.getOperator();
    QLIdOperator finalOperator = operator;
    if (filter.getValues().length > 0) {
      if (operator == QLIdOperator.EQUALS) {
        finalOperator = QLIdOperator.IN;
        logger.info("Changing simpleStringOperator from [{}] to [{}]", operator, finalOperator);
      } else {
        finalOperator = operator;
      }
    }
    switch (finalOperator) {
      case EQUALS:
        selectQuery.addCondition(BinaryCondition.equalTo(key, filter.getValues()[0]));
        break;
      case IN:
        selectQuery.addCondition(new InCondition(key, (Object[]) filter.getValues()));
        break;
      case NOT_NULL:
        selectQuery.addCondition(UnaryCondition.isNotNull(key));
        break;
      case NOT_IN:
        InCondition inCondition = new InCondition(key, (Object[]) filter.getValues());
        inCondition.setNegate(true);
        selectQuery.addCondition(inCondition);
        break;
      default:
        throw new InvalidRequestException("String simple operator not supported" + operator);
    }
  }

  private boolean isIdFilter(Filter f) {
    return f instanceof QLIdFilter;
  }

  private boolean isTimeFilter(Filter f) {
    return f instanceof QLTimeFilter;
  }

  private boolean checkFilter(Filter f) {
    return f.getOperator() != null && EmptyPredicate.isNotEmpty(f.getValues());
  }

  private DbColumn getFilterKey(QLBillingDataFilterType type) {
    switch (type) {
      case EndTime:
      case StartTime:
        return schema.getStartTime();
      case Application:
        return schema.getAppId();
      case Service:
        return schema.getServiceId();
      case Environment:
        return schema.getEnvId();
      case Cluster:
        return schema.getClusterId();
      case CloudServiceName:
        return schema.getCloudServiceName();
      case LaunchType:
        return schema.getLaunchType();
      case InstanceId:
        return schema.getInstanceId();
      case InstanceType:
        return schema.getInstanceType();
      case WorkloadName:
        return schema.getWorkloadName();
      case Namespace:
        return schema.getNamespace();
      case CloudProvider:
        return schema.getCloudProviderId();
      default:
        throw new InvalidRequestException("Filter type not supported " + type);
    }
  }

  private void decorateQueryWithGroupBy(List<BillingDataMetaDataFields> fieldNames, SelectQuery selectQuery,
      List<QLCCMEntityGroupBy> groupBy, List<BillingDataMetaDataFields> groupByFields) {
    for (QLCCMEntityGroupBy aggregation : groupBy) {
      if (aggregation.getAggregationKind() == QLAggregationKind.SIMPLE) {
        decorateSimpleGroupBy(fieldNames, selectQuery, aggregation, groupByFields);
      }
    }
  }

  private void decorateSimpleGroupBy(List<BillingDataMetaDataFields> fieldNames, SelectQuery selectQuery,
      QLCCMEntityGroupBy aggregation, List<BillingDataMetaDataFields> groupByFields) {
    DbColumn groupBy;
    switch (aggregation) {
      case Application:
        groupBy = schema.getAppId();
        break;
      case StartTime:
        groupBy = schema.getStartTime();
        break;
      case Service:
        groupBy = schema.getServiceId();
        break;
      case Region:
        groupBy = schema.getRegion();
        break;
      case Cluster:
        groupBy = schema.getClusterId();
        break;
      case ClusterName:
        groupBy = schema.getClusterName();
        break;
      case Environment:
        groupBy = schema.getEnvId();
        break;
      case CloudServiceName:
        groupBy = schema.getCloudServiceName();
        break;
      case InstanceId:
        groupBy = schema.getInstanceId();
        break;
      case LaunchType:
        groupBy = schema.getLaunchType();
        break;
      case WorkloadName:
        groupBy = schema.getWorkloadName();
        break;
      case WorkloadType:
        groupBy = schema.getWorkloadType();
        break;
      case Namespace:
        groupBy = schema.getNamespace();
        break;
      case ClusterType:
        groupBy = schema.getClusterType();
        break;
      case CloudProvider:
        groupBy = schema.getCloudProviderId();
        break;
      default:
        throw new InvalidRequestException("Invalid groupBy clause");
    }
    selectQuery.addColumns(groupBy);
    selectQuery.addGroupings(groupBy);
    fieldNames.add(BillingDataMetaDataFields.valueOf(groupBy.getName().toUpperCase()));
    selectQuery.addCondition(UnaryCondition.isNotNull(groupBy));
    groupByFields.add(BillingDataMetaDataFields.valueOf(groupBy.getName().toUpperCase()));
  }

  protected List<QLCCMEntityGroupBy> getGroupByEntity(List<QLCCMGroupBy> groupBy) {
    return groupBy != null ? groupBy.stream()
                                 .filter(g -> g.getEntityGroupBy() != null)
                                 .map(QLCCMGroupBy::getEntityGroupBy)
                                 .collect(Collectors.toList())
                           : null;
  }

  private boolean isValidGroupBy(List<QLCCMEntityGroupBy> groupBy) {
    return EmptyPredicate.isNotEmpty(groupBy) && groupBy.size() <= 4;
  }

  private List<QLBillingSortCriteria> validateAndAddSortCriteria(
      SelectQuery selectQuery, List<QLBillingSortCriteria> sortCriteria, List<BillingDataMetaDataFields> fieldNames) {
    if (isEmpty(sortCriteria)) {
      return new ArrayList<>();
    }

    sortCriteria.removeIf(qlBillingSortCriteria
        -> qlBillingSortCriteria.getSortOrder() == null
            || !fieldNames.contains(qlBillingSortCriteria.getSortType().getBillingMetaData()));

    if (EmptyPredicate.isNotEmpty(sortCriteria)) {
      sortCriteria.forEach(s -> addOrderBy(selectQuery, s));
    }
    return sortCriteria;
  }

  private void addOrderBy(SelectQuery selectQuery, QLBillingSortCriteria sortCriteria) {
    QLBillingSortType sortType = sortCriteria.getSortType();
    OrderObject.Dir dir = sortCriteria.getSortOrder() == QLSortOrder.ASCENDING ? Dir.ASCENDING : Dir.DESCENDING;
    switch (sortType) {
      case Time:
        selectQuery.addCustomOrdering(BillingDataMetaDataFields.STARTTIME.getFieldName(), dir);
        break;
      case Amount:
        selectQuery.addCustomOrdering(BillingDataMetaDataFields.SUM.getFieldName(), dir);
        break;
      case IdleCost:
        selectQuery.addCustomOrdering(BillingDataMetaDataFields.IDLECOST.getFieldName(), dir);
        break;
      default:
        throw new InvalidRequestException("Order type not supported " + sortType);
    }
  }

  private boolean isGroupByClusterPresent(List<QLCCMEntityGroupBy> groupByList) {
    return groupByList.stream().anyMatch(groupBy -> groupBy == QLCCMEntityGroupBy.Cluster);
  }

  private boolean isGroupByClusterNamePresent(List<QLCCMEntityGroupBy> groupByList) {
    return groupByList.stream().anyMatch(groupBy -> groupBy == QLCCMEntityGroupBy.ClusterName);
  }

  private boolean isGroupByClusterTypePresent(List<QLCCMEntityGroupBy> groupByList) {
    return groupByList.stream().anyMatch(groupBy -> groupBy == QLCCMEntityGroupBy.ClusterType);
  }

  public void addClusterGroupBy(List<QLCCMEntityGroupBy> groupByList) {
    groupByList.add(QLCCMEntityGroupBy.Cluster);
  }

  public void addClusterNameGroupBy(List<QLCCMEntityGroupBy> groupByList) {
    groupByList.add(QLCCMEntityGroupBy.ClusterName);
  }

  public void addClusterTypeGroupBy(List<QLCCMEntityGroupBy> groupByList) {
    groupByList.add(QLCCMEntityGroupBy.ClusterType);
  }

  private boolean isGroupByStartTimePresent(List<QLCCMEntityGroupBy> groupByList) {
    return groupByList.stream().anyMatch(groupBy -> groupBy == QLCCMEntityGroupBy.StartTime);
  }

  private boolean isNoneGroupBySelectedWithoutFilterInClusterView(
      List<QLCCMEntityGroupBy> groupByList, List<QLBillingDataFilter> filters) {
    return isClusterFilterPresent(filters) && !checkForAdditionalFilterInClusterDrillDown(filters)
        && isGroupByNonePresent(groupByList);
  }

  private boolean isGroupByNonePresent(List<QLCCMEntityGroupBy> groupByList) {
    return isEmpty(groupByList) || (groupByList.size() == 1 && isGroupByStartTimePresent(groupByList));
  }

  private boolean checkForAdditionalFilterInClusterDrillDown(List<QLBillingDataFilter> filters) {
    for (QLBillingDataFilter filter : filters) {
      Set<QLBillingDataFilterType> filterTypes = QLBillingDataFilter.getFilterTypes(filter);
      if (!filterTypes.isEmpty()
          && (!(filter.getCluster() != null || filter.getStartTime() != null || filter.getEndTime() != null))) {
        return true;
      }
    }
    return false;
  }

  private boolean isClusterFilterPresent(List<QLBillingDataFilter> filters) {
    return filters.stream().anyMatch(filter -> filter.getCluster() != null);
  }

  protected boolean isUnallocatedCostAggregationPresent(List<QLCCMAggregationFunction> aggregationFunctions) {
    return aggregationFunctions.stream().anyMatch(agg -> agg.getColumnName().equals("unallocatedcost"));
  }

  private void addInstanceTypeFilter(List<QLBillingDataFilter> filters) {
    if (!isInstanceTypeFilterPresent(filters)) {
      List<String> instanceTypeValues = new ArrayList<>();
      instanceTypeValues.add("ECS_TASK_FARGATE");
      instanceTypeValues.add("ECS_CONTAINER_INSTANCE");
      instanceTypeValues.add("K8S_NODE");

      QLBillingDataFilter instanceTypeFilter = QLBillingDataFilter.builder()
                                                   .instanceType(QLIdFilter.builder()
                                                                     .operator(QLIdOperator.EQUALS)
                                                                     .values(instanceTypeValues.toArray(new String[0]))
                                                                     .build())
                                                   .build();
      filters.add(instanceTypeFilter);
    }
  }

  private boolean isInstanceTypeFilterPresent(List<QLBillingDataFilter> filters) {
    return filters.stream().anyMatch(filter -> filter.getInstanceType() != null);
  }

  protected QLCCMTimeSeriesAggregation getGroupByTime(List<QLCCMGroupBy> groupBy) {
    if (groupBy != null) {
      Optional<QLCCMTimeSeriesAggregation> first = groupBy.stream()
                                                       .filter(g -> g.getTimeAggregation() != null)
                                                       .map(QLCCMGroupBy::getTimeAggregation)
                                                       .findFirst();
      return first.orElse(null);
    }
    return null;
  }

  private void decorateQueryWithGroupByTime(List<BillingDataMetaDataFields> fieldNames, SelectQuery selectQuery,
      QLCCMTimeSeriesAggregation groupByTime, List<BillingDataMetaDataFields> groupByFields) {
    String timeBucket = getGroupByTimeQueryWithDateTrunc(groupByTime, "starttime");

    selectQuery.addCustomColumns(Converter.toCustomColumnSqlObject(
        new CustomExpression(timeBucket).setDisableParens(true), BillingDataMetaDataFields.TIME_SERIES.getFieldName()));
    selectQuery.addCustomGroupings(BillingDataMetaDataFields.TIME_SERIES.getFieldName());
    selectQuery.addCustomOrdering(BillingDataMetaDataFields.TIME_SERIES.getFieldName(), Dir.ASCENDING);
    fieldNames.add(BillingDataMetaDataFields.TIME_SERIES);
    groupByFields.add(BillingDataMetaDataFields.TIME_SERIES);
  }

  private boolean isValidGroupByTime(QLCCMTimeSeriesAggregation groupByTime) {
    return groupByTime != null && groupByTime.getTimeGroupType() != null;
  }

  public String getGroupByTimeQueryWithDateTrunc(QLCCMTimeSeriesAggregation groupByTime, String dbFieldName) {
    String unit;
    switch (groupByTime.getTimeGroupType()) {
      case DAY:
        unit = "day";
        break;
      case MONTH:
        unit = "month";
        break;
      default:
        logger.warn("Unsupported timeGroupType " + groupByTime.getTimeGroupType());
        throw new InvalidRequestException("Cant apply time group by");
    }

    return new StringBuilder("date_trunc('")
        .append(unit)
        .append("',")
        .append(dbFieldName)
        .append(" at time zone '")
        .append(STANDARD_TIME_ZONE)
        .append("')")
        .toString();
  }

  protected QLTimeFilter getStartTimeFilter(List<QLBillingDataFilter> filters) {
    Optional<QLBillingDataFilter> startTimeDataFilter =
        filters.stream().filter(qlBillingDataFilter -> qlBillingDataFilter.getStartTime() != null).findFirst();
    if (startTimeDataFilter.isPresent()) {
      return startTimeDataFilter.get().getStartTime();
    } else {
      throw new InvalidRequestException("Start time cannot be null");
    }
  }

  protected QLBillingDataFilter getInstanceTypeFilter() {
    String[] instanceTypeIdFilterValues = new String[] {"CLUSTER_UNALLOCATED"};
    QLIdFilter instanceTypeFilter =
        QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(instanceTypeIdFilterValues).build();
    return QLBillingDataFilter.builder().instanceType(instanceTypeFilter).build();
  }

  protected String getFormattedDate(Instant instant, String datePattern) {
    return instant.atZone(ZoneId.of(DEFAULT_TIME_ZONE)).format(DateTimeFormatter.ofPattern(datePattern));
  }

  protected double getRoundedDoubleValue(BigDecimal value) {
    return Math.round(value.doubleValue() * 100D) / 100D;
  }

  protected List<QLBillingDataFilter> prepareFiltersForUnallocatedCostData(List<QLBillingDataFilter> filters) {
    List<QLBillingDataFilter> modifiedFilterList =
        filters.stream()
            .filter(qlBillingDataFilter -> qlBillingDataFilter.getInstanceType() == null)
            .collect(Collectors.toList());

    modifiedFilterList.add(getInstanceTypeFilter());
    return modifiedFilterList;
  }

  protected double getRoundedDoublePercentageValue(BigDecimal value) {
    return Math.round(value.doubleValue() * 10000D) / 100D;
  }
}
