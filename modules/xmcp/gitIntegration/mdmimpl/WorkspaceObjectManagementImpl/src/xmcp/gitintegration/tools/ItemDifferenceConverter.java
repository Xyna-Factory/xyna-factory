/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xmcp.gitintegration.tools;



import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.WorkspaceContentDifferenceType;
import xmcp.gitintegration.WorkspaceContentItem;
import xmcp.gitintegration.impl.ItemDifference;
import xmcp.gitintegration.impl.XynaContentDifferenceType;



public class ItemDifferenceConverter {

  private static final Map<XynaContentDifferenceType, WorkspaceContentDifferenceType> typeMap =
      Map.of(XynaContentDifferenceType.CREATE, new CREATE(), //
             XynaContentDifferenceType.DELETE, new DELETE(), //
             XynaContentDifferenceType.MODIFY, new MODIFY());//


  public <T extends WorkspaceContentItem> WorkspaceContentDifference convert(ItemDifference<T> item, String contentType) {
    WorkspaceContentDifference.Builder result = new WorkspaceContentDifference.Builder();
    result.contentType(contentType);
    result.differenceType(typeMap.get(item.getType()));
    result.existingItem(item.getFrom());
    result.newItem(item.getTo());
    return result.instance();
  }


  public <T extends WorkspaceContentItem> List<WorkspaceContentDifference> convert(List<ItemDifference<T>> items, String contentType) {
    List<WorkspaceContentDifference> result = new ArrayList<>();
    for (ItemDifference<T> item : items) {
      result.add(convert(item, contentType));
    }
    return result;
  }
}
