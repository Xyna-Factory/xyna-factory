/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
*/
package xmcp.gitintegration.impl;

import xmcp.gitintegration.FactoryContentDifference;
import xmcp.gitintegration.FactoryContentItem;
import xmcp.gitintegration.impl.processing.XynaObjectDifferenceSelector;

public class FactoryContentItemDifferenceSelector implements XynaObjectDifferenceSelector <FactoryContentItem, FactoryContentDifference> {

  @Override
  public FactoryContentItem selectExistingItem(FactoryContentDifference item) {
    return item.getExistingItem();
  }

  @Override
  public FactoryContentItem selectNewItem(FactoryContentDifference item) {
    return item.getNewItem();
  }

  @Override
  public String selectId(FactoryContentDifference item) {
    return String.valueOf(item.getEntryId());
  }

  @Override
  public String selectContentType(FactoryContentDifference item) {
    return item.getContentType();
  }

  @Override
  public XynaContentDifferenceType selectDifferenceType(FactoryContentDifference item) {
    return XynaContentDifferenceType.valueOf(item.getDifferenceType().getClass().getSimpleName());
  }

}
