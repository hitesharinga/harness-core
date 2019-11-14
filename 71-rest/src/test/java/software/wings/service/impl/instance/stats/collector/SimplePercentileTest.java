package software.wings.service.impl.instance.stats.collector;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SimplePercentileTest extends CategoryTest {
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testPercentile() {
    List<Integer> numbers = new ArrayList<>(100);
    IntStream.range(1, 101).forEach(numbers::add);
    Collections.shuffle(numbers);

    Integer percentile = new SimplePercentile(numbers).evaluate(95);
    assertThat(percentile).isEqualTo(sorted(numbers).get(95));

    percentile = new SimplePercentile(numbers).evaluate(20);
    assertThat(percentile).isEqualTo(sorted(numbers).get(20));

    numbers = new ArrayList<>(20);
    IntStream.range(1, 21).forEach(numbers::add);
    Collections.shuffle(numbers);
    percentile = new SimplePercentile(numbers).evaluate(95);
    assertThat(percentile).isEqualTo(sorted(numbers).get(19));
  }

  private static List<Integer> sorted(List<Integer> list) {
    return list.stream().sorted().collect(Collectors.toList());
  }
}
