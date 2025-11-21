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

import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;

import xmcp.gitintegration.impl.processing.XynaContentProcessingPortal;
import xmcp.gitintegration.impl.processing.XynaObjectDifferenceSelector;



public class OutputCreator <ITEM, DIFFERENCE, SELECTOR extends XynaObjectDifferenceSelector<ITEM, DIFFERENCE>>{

  public static final XynaPropertyInt TABLE_LIMIT = new XynaPropertyInt("xmcp.gitintegration.table.limit", 5)
    .setDefaultDocumentation(DocumentationLanguage.EN,
    "Table limit")
    .setDefaultDocumentation(DocumentationLanguage.DE,
    "Table limit");
  
  //private static final int TABLE_LIMIT = 5;
  protected XynaObjectDifferenceSelector<ITEM, DIFFERENCE> selector;
  
  public OutputCreator(XynaObjectDifferenceSelector<ITEM, DIFFERENCE> selector) {
    this.selector = selector;
  }

  protected final HashMap<XynaContentDifferenceType, BiFunction<DIFFERENCE, XynaContentProcessingPortal<ITEM, DIFFERENCE>, String>> differenceStringFunctions =
      createDifferenceStringFunctions();


  protected final HashMap<XynaContentDifferenceType, BiFunction<DIFFERENCE, XynaContentProcessingPortal<ITEM, DIFFERENCE>, String>> createDifferenceStringFunctions() {
    HashMap<XynaContentDifferenceType, BiFunction<DIFFERENCE, XynaContentProcessingPortal<ITEM, DIFFERENCE>, String>> result = new HashMap<>();
    result.put(XynaContentDifferenceType.CREATE, this::createCreateString);
    result.put(XynaContentDifferenceType.MODIFY, this::createModifyString);
    result.put(XynaContentDifferenceType.DELETE, this::createDeleteString);
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
    sb.append(selector.selectId(diff)).append(" ");
    sb.append(selector.selectContentType(diff)).append(" ");
    sb.append(selector.selectDifferenceType(diff)).append(" ");
    String differenceString = createDifferenceString(diff, portal);
    sb.append(differenceString);
    sb.append("\n");
    return sb.toString();
  }


  private String createDifferenceString(DIFFERENCE diff, XynaContentProcessingPortal<ITEM, DIFFERENCE> portal) {
    XynaContentDifferenceType key = selector.selectDifferenceType(diff);
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
      if (tableCount > TABLE_LIMIT.get()) {
        appendTruncatedList(ds);
        break;
      }

      ds.append("  ").append(difference.getType()).append(": ");
      formatter.accept(ds, difference);
    }
  }


  private static void appendTruncatedList(StringBuilder ds) {
    ds.append("    ").append("...");
    ds.append("\n");
  }


  public static <T extends ItemDifference<?>> void appendDifferenceTableHeader(StringBuilder sb, List<T> list, String tag) {
    Map<XynaContentDifferenceType, Integer> counts = new HashMap<>();
    counts.put(XynaContentDifferenceType.CREATE, 0);
    counts.put(XynaContentDifferenceType.MODIFY, 0);
    counts.put(XynaContentDifferenceType.DELETE, 0);
    list.stream().forEach(x -> counts.put(x.getType(), counts.get(x.getType()) + 1));
    sb.append("  ").append(tag).append(": ");
    sb.append(XynaContentDifferenceType.CREATE).append(": ").append(counts.get(XynaContentDifferenceType.CREATE)).append(", ");
    sb.append(XynaContentDifferenceType.MODIFY).append(": ").append(counts.get(XynaContentDifferenceType.MODIFY)).append(", ");
    sb.append(XynaContentDifferenceType.DELETE).append(": ").append(counts.get(XynaContentDifferenceType.DELETE)).append(", ");
    sb.append("\n");
  }
}
