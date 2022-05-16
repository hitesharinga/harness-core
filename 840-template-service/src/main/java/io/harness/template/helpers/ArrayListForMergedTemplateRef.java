/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.template.helpers;

import java.util.List;

public class ArrayListForMergedTemplateRef {
  List<Object> arrayList;
  List<Object> arrayListWithTemplateRef;

  public ArrayListForMergedTemplateRef(List<Object> arrayList, List<Object> arrayListWithTemplateRef) {
    this.arrayList = arrayList;
    this.arrayListWithTemplateRef = arrayListWithTemplateRef;
  }
}
