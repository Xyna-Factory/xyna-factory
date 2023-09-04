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



import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.WorkspaceContentDifferenceType;
import xmcp.gitintegration.XynaContentDifference;
import xmcp.gitintegration.impl.processing.XynaContentProcessingPortal;
import xmcp.gitintegration.impl.processing.XynaObjectDifferenceSelector;



public class OutputCreator <ITEM, DIFFERENCE extends XynaContentDifference, SELECTOR extends XynaObjectDifferenceSelector<ITEM, DIFFERENCE>>{

  public static final int TABLE_LIMIT = 5;
  protected XynaObjectDifferenceSelector<ITEM, DIFFERENCE> selector;
  
  public OutputCreator(XynaObjectDifferenceSelector<ITEM, DIFFERENCE> selector) {
    this.selector = selector;
  }

  protected final HashMap<String, BiFunction<DIFFERENCE, XynaContentProcessingPortal<ITEM, DIFFERENCE>, String>> differenceStringFunctions =
      createDifferenceStringFunctions();


  protected final HashMap<String, BiFunction<DIFFERENCE, XynaContentProcessingPortal<ITEM, DIFFERENCE>, String>> createDifferenceStringFunctions() {
    HashMap<String, BiFunction<DIFFERENCE, XynaContentProcessingPortal<ITEM, DIFFERENCE>, String>> result = new HashMap<>();
    result.put(CREATE.class.getSimpleName(), this::createCreateString);
    result.put(MODIFY.class.getSimpleName(), this::createModifyString);
    result.put(DELETE.class.getSimpleName(), this::createDeleteString);
    return result;
  }


  protected String createCreateString(DIFFERENCE diff, XynaContentProcessingPortal<ITEM, DIFFERENCE> portal) {
    return portal.createItemKeyString(selector.selectNewItem(diff));
  }


  private String createDeleteString(DIFFERENCE diff, XynaContentProcessingPortal<ITEM, DIFFERENCE> portal) {
    return portal.createItemKeyString(selector.selectExistingItem(diff));
  }


  private String createModifyString(DIFFERENCE diff, XynaContentProcessingPortal<ITEM, DIFFERENCE> portal) {
    return portal.createItemKeyString(selector.selectExistingItem(diff)) + portal.createDifferenceString(diff);
  }


  public String createOutput(List<? extends DIFFERENCE> diffs, XynaContentProcessingPortal<ITEM, DIFFERENCE> portal) {
    StringBuilder sb = new StringBuilder();
    for (DIFFERENCE diff : diffs) {
      String output = createOutput(diff, portal);
      sb.append(output);
    }
    return sb.toString();
  }


  public String createOutput(DIFFERENCE diff, XynaContentProcessingPortal<ITEM, DIFFERENCE> portal) {
    StringBuilder sb = new StringBuilder();
    sb.append(diff.getId()).append(" ");
    sb.append(diff.getContentType().toString()).append(" ");
    sb.append(diff.getDifferenceType().getClass().getSimpleName()).append(" ");
    String differenceString = createDifferenceString(diff, portal);
    sb.append(differenceString);
    sb.append("\n");
    return sb.toString();
  }


  private String createDifferenceString(DIFFERENCE diff, XynaContentProcessingPortal<ITEM, DIFFERENCE> portal) {
    String key = diff.getDifferenceType().getClass().getSimpleName();
    BiFunction<DIFFERENCE, XynaContentProcessingPortal<ITEM, DIFFERENCE>, String> f = differenceStringFunctions.get(key);
    String result = f.apply(diff, portal);
    return result;
  }



  public static <C> void appendDiffs(StringBuilder ds, List<? extends ItemDifference<C>> diffs, String tag,
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


  private static void appendTruncatedList(StringBuilder ds) {
    ds.append("    ").append("...");
    ds.append("\n");
  }


  public static <T extends ItemDifference<?>> void appendDifferenceTableHeader(StringBuilder sb, List<T> list, String tag) {
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
