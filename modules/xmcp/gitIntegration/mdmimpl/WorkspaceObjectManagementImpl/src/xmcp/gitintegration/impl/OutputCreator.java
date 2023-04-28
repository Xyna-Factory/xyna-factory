/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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



import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.WorkspaceContentDifferenceType;
import xmcp.gitintegration.impl.processing.WorkspaceContentProcessingPortal;



public class OutputCreator {

  public static final int TABLE_LIMIT = 5;


  private static final HashMap<String, BiFunction<WorkspaceContentDifference, WorkspaceContentProcessingPortal, String>> differenceStringFunctions =
      createDifferenceStringFunctions();


  private static final HashMap<String, BiFunction<WorkspaceContentDifference, WorkspaceContentProcessingPortal, String>> createDifferenceStringFunctions() {
    HashMap<String, BiFunction<WorkspaceContentDifference, WorkspaceContentProcessingPortal, String>> result = new HashMap<>();
    result.put(CREATE.class.getSimpleName(), OutputCreator::createCreateString);
    result.put(MODIFY.class.getSimpleName(), OutputCreator::createModifyString);
    result.put(DELETE.class.getSimpleName(), OutputCreator::createDeleteString);
    return result;
  }


  private static String createCreateString(WorkspaceContentDifference diff, WorkspaceContentProcessingPortal portal) {
    return portal.createItemKeyString(diff.getNewItem());
  }


  private static String createDeleteString(WorkspaceContentDifference diff, WorkspaceContentProcessingPortal portal) {
    return portal.createItemKeyString(diff.getExistingItem());
  }


  private static String createModifyString(WorkspaceContentDifference diff, WorkspaceContentProcessingPortal portal) {
    return portal.createItemKeyString(diff.getExistingItem()) + portal.createDifferenceString(diff);
  }


  public String createOutput(List<? extends WorkspaceContentDifference> diffs) {
    StringBuilder sb = new StringBuilder();
    WorkspaceContentProcessingPortal portal = new WorkspaceContentProcessingPortal();
    for (WorkspaceContentDifference diff : diffs) {
      String output = createOutput(diff, portal);
      sb.append(output);
    }
    return sb.toString();
  }


  public String createOutput(WorkspaceContentDifference diff, WorkspaceContentProcessingPortal portal) {
    StringBuilder sb = new StringBuilder();
    sb.append(diff.getId()).append(" ");
    sb.append(diff.getContentType().toString()).append(" ");
    sb.append(diff.getDifferenceType().getClass().getSimpleName()).append(" ");
    String differenceString = createDifferenceString(diff, portal);
    sb.append(differenceString);
    sb.append("\n");
    return sb.toString();
  }


  private String createDifferenceString(WorkspaceContentDifference diff, WorkspaceContentProcessingPortal portal) {
    String key = diff.getDifferenceType().getClass().getSimpleName();
    BiFunction<WorkspaceContentDifference, WorkspaceContentProcessingPortal, String> f = differenceStringFunctions.get(key);
    String result = f.apply(diff, portal);
    return result;
  }



  public <C> void appendDiffs(StringBuilder ds, List<? extends ItemDifference<C>> diffs, String tag,
                              BiConsumer<StringBuilder, ItemDifference<C>> formatter) {
    if (diffs.isEmpty()) {
      return;
    }

    appendDifferenceTableHeader(ds, diffs, tag);
    int tableCount = 0;
    for (ItemDifference<C> difference : diffs) {
      tableCount++;
      if (tableCount > TABLE_LIMIT) {
        appendTruncatedList(ds);
        break;
      }

      ds.append("  ").append(difference.getType().getSimpleName()).append(": ");
      formatter.accept(ds, difference);
    }
  }


  private void appendTruncatedList(StringBuilder ds) {
    ds.append("    ").append("...");
    ds.append("\n");
  }


  public <T extends ItemDifference<?>> void appendDifferenceTableHeader(StringBuilder sb, List<T> list, String tag) {
    Map<Class<? extends WorkspaceContentDifferenceType>, Integer> counts = new HashMap<>();
    counts.put(CREATE.class, 0);
    counts.put(MODIFY.class, 0);
    counts.put(DELETE.class, 0);
    list.stream().forEach(x -> counts.put(x.getType(), counts.get(x.getType()) + 1));
    sb.append("  ").append(tag).append(": ");
    sb.append(CREATE.class.getSimpleName()).append(": ").append(counts.get(CREATE.class)).append(", ");
    sb.append(MODIFY.class.getSimpleName()).append(": ").append(counts.get(MODIFY.class)).append(", ");
    sb.append(DELETE.class.getSimpleName()).append(": ").append(counts.get(DELETE.class)).append(", ");
    sb.append("\n");
  }
}
