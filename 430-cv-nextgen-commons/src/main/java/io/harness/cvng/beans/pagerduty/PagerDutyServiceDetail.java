package io.harness.cvng.beans.pagerduty;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PagerDutyServiceDetail {
  String id;
  String name;
}
