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
package com.gip.xyna.xfmg.xfmon.fruntimestats.path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.StatisticsAggregator;



public class StatisticsPathImpl implements StatisticsPath {


  private static Pattern DOT_PATTERN = Pattern.compile("(?<!\\\\)[.]");
  private static Pattern ESCAPED_DOT_PATTERN = Pattern.compile("\\\\.");

  private final List<StatisticsPathPart> path;


  public StatisticsPathImpl() {
    path = new ArrayList<StatisticsPathPart>();
  }
  
  public StatisticsPathImpl(StatisticsPathPart... path) {
    this(Arrays.asList(path));
  }
  
  public StatisticsPathImpl(List<StatisticsPathPart> path) {
    this.path = path;
  }
  
  public StatisticsPathImpl(String... path) {
    this(new ArrayList<StatisticsPathPart>());
    for (String part : path) {
      this.path.add(new SimplePathPart(part));
    }
  }

  public List<StatisticsPathPart> getPath() {
    return path;
  }


  public StatisticsPathPart getPathPart(int index) {
    return path.get(index);
  }
  
  
  public StatisticsPathImpl append(StatisticsPathPart part) {
    List<StatisticsPathPart> newPath = new ArrayList<StatisticsPathPart>(path);
    newPath.add(part);
    return new StatisticsPathImpl(newPath);
  }
  
  
  public StatisticsPathImpl append(StatisticsPath path) {
    List<StatisticsPathPart> newPath = new ArrayList<StatisticsPathPart>(this.path);
    newPath.addAll(path.getPath());
    return new StatisticsPathImpl(newPath);
  }
  
  
  public StatisticsPath append(String part) {
    return append(simplePathPart(part));
  }
  
  

  public boolean isSimple() {
    for (StatisticsPathPart part : path) {
      if (part.getStatisticsNodeTraversal() != StatisticsNodeTraversal.SINGLE) {
        return false;
      }
    }
    return true;
  }
  
  
  public int length() {
    return path.size();
  }

  
  
  private Integer hashCode;
  
  @Override
  public int hashCode() {
    if (hashCode == null) {
      hashCode = calculateHashCode();
    }
    return hashCode;
  }
  
  
  private Integer calculateHashCode() {
    int value = 0;
    for (StatisticsPathPart part : path) {
      value += part.hashCode();
    }
    return new Integer(value);
  }
  
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof StatisticsPath)) {
      return false;
    } else {
      StatisticsPath otherPath = (StatisticsPath) obj;
      for (int i = 0; i < path.size(); i++) {
        if (!path.get(i).equals(otherPath.getPathPart(i))) {
          return false;
        }
      }
      return true;
    }
  }
  
  
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("Path: ");
    for (StatisticsPathPart part : path) {
      builder.append('\'')
             .append(part.toString())
             .append("' ");
    }
    return builder.toString();
  }
  
  
  public static boolean covers(StatisticsPath fuzzyMasterPath, StatisticsPath simplePathToCover) {
    if (simplePathToCover.isSimple()) {
      if (fuzzyMasterPath.length() > simplePathToCover.length()) {
        return false;
      } else {
        for (int i = 0; i < fuzzyMasterPath.length(); i++) {
          StatisticsPathPart masterPathPart = fuzzyMasterPath.getPathPart(i);
          switch (masterPathPart.getStatisticsNodeTraversal()) {
            case ALL :
            case ALL_AND_SELF :
            case ALL_OR_SELF :
              continue;
            case MULTI :
            case MULTI_AND_SELF :
            case MULTI_OR_SELF :
              FilterPathPart filter = (FilterPathPart) masterPathPart;
              if (filter.accept(simplePathToCover.getPathPart(i).getPartName())) {
                continue;
              } else {
                return false;
              }
            case SINGLE :
              if (masterPathPart.getPartName().equals(simplePathToCover.getPathPart(i).getPartName())) {
                continue;
              } else {
                return false;
              }
            default :
              throw new RuntimeException("unknown traversal: " + masterPathPart.getStatisticsNodeTraversal());
          }
        }
      }
      return true;
    } else {
      throw new RuntimeException("simplePathToCover is not simple");
    }
  }
  
  
  public static StatisticsPath aggregationStackToPath(StatisticsAggregator aggregator) {
    StatisticsPath path = new StatisticsPathImpl();
    StatisticsAggregator curr = aggregator;
    while (curr != null) {
      path = path.append(curr.getPathpart());
      if (curr == curr.getNextAggregationPart()) {
        break;
      }
      curr = curr.getNextAggregationPart();
    }
    return path;
  }
  
  
  public static StatisticsPath fromString(String... pathparts) {
    StatisticsPath conversion = new StatisticsPathImpl();
    for (int i = 0; i < pathparts.length; i++) {
      String part = pathparts[i];
      if (part.equals("*")) {
        conversion = conversion.append(ALL);
      } else {
        conversion = conversion.append(new SimplePathPart(part));
      }
    }
    return conversion;
  }
  
  
  public static StatisticsPath fromEscapedString(String path) {
    String[] split = DOT_PATTERN.split(path);
    for (int i=0; i < split.length; i++) {
      split[i] = unescapePathPart(split[i]);
    }
    return fromString(split);
  }
  
  
  public static StatisticsPathPart simplePathPart(String part) {
    return new SimplePathPart(part);
  }
  
  
  public static String pathToString(String... parts) {
    StringBuilder pathBuilder = new StringBuilder();
    for (int i=0; i < parts.length; i++) {
      pathBuilder.append(escapePathPart(parts[i]));
      if (i+1 < parts.length) {
        pathBuilder.append(".");
      }
    }
    return pathBuilder.toString();
  }
  
  
  public static String escapePathPart(String part) {
    return DOT_PATTERN.matcher(part).replaceAll("\\\\.");
  }
  
  
  public static String unescapePathPart(String part) {
    return ESCAPED_DOT_PATTERN.matcher(part).replaceAll(".");
  }
  
  
  public static class SimplePathPart implements StatisticsPathPart {

    private final String name;
    
    public SimplePathPart(String name) {
      this.name = name;
    }
    
    public String getPartName() {
      return name;
    }

    public StatisticsNodeTraversal getStatisticsNodeTraversal() {
      return StatisticsNodeTraversal.SINGLE;
    }
    
    @Override
    public int hashCode() {
      return name.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj != null && obj instanceof StatisticsPathPart) {
        return name.equals(((StatisticsPathPart) obj).getPartName());
      } else {
        return false;
      }
    }
    
    @Override
    public String toString() {
      return name;
    }

    public UnknownPathOnTraversalHandling getUnknownPathHandling() {
      return UnknownPathOnTraversalHandling.THROW_IF_ANY;
    }

  }
  
  
  
  public final static StatisticsPathPart ALL = new AllPathPart();
  
  private final static class AllPathPart implements StatisticsPathPart, UnknownPathHandlingHolder {
    public String getPartName() { return "ALL"; }
    public StatisticsNodeTraversal getStatisticsNodeTraversal() { return StatisticsNodeTraversal.ALL; }
    public String toString() { return "ALL"; };
    public UnknownPathOnTraversalHandling getUnknownPathHandling() { return UnknownPathOnTraversalHandling.THROW_IF_ALL; }
  }
  
  public final static StatisticsPath ALL_PATH = new StatisticsPathImpl(ALL);
  
  public final static StatisticsPathPart ALL_OR_SELF = new AllOrSelfPathPart();

  private final static class AllOrSelfPathPart implements StatisticsPathPart, UnknownPathHandlingHolder {
    public String getPartName() { return "ALL_OR_SELF"; }
    public StatisticsNodeTraversal getStatisticsNodeTraversal() { return StatisticsNodeTraversal.ALL_OR_SELF; }
    public String toString() { return "ALL_OR_SELF"; };
    public UnknownPathOnTraversalHandling getUnknownPathHandling() { return UnknownPathOnTraversalHandling.THROW_IF_ALL; }
  }
  
  public final static StatisticsPathPart ALL_AND_SELF = new AllAndSelfPathPart();
  
  private final static class AllAndSelfPathPart implements StatisticsPathPart, UnknownPathHandlingHolder {
    public String getPartName() { return "ALL_AND_SELF"; }
    public StatisticsNodeTraversal getStatisticsNodeTraversal() { return StatisticsNodeTraversal.ALL_AND_SELF; }
    public String toString() { return "ALL_AND_SELF"; };
    public UnknownPathOnTraversalHandling getUnknownPathHandling() { return UnknownPathOnTraversalHandling.THROW_IF_ALL; }
  }
  
  
  public static abstract class FilterPathPart implements StatisticsPathPart, UnknownPathHandlingHolder {

    private final StatisticsNodeTraversal traversal;
    private final UnknownPathOnTraversalHandling unknownHandling;
    
    public FilterPathPart(boolean orSelf) {
      this(orSelf, UnknownPathOnTraversalHandling.THROW_IF_ALL);
    }
    
    public FilterPathPart(boolean orSelf, UnknownPathOnTraversalHandling unknownHandling) {
      if (orSelf) {
        traversal = StatisticsNodeTraversal.MULTI_OR_SELF;
      } else {
        traversal = StatisticsNodeTraversal.MULTI;
      }
      this.unknownHandling = unknownHandling;
    }
    
    public String getPartName() {
      return "FILTER";
    }
    
    public abstract boolean accept(String nodename);
    
    public StatisticsNodeTraversal getStatisticsNodeTraversal() {
      return traversal;
    }
    
    public UnknownPathOnTraversalHandling getUnknownPathHandling() {
      return unknownHandling;
    }
    
  }
  
  
  public static class WhiteListFilter extends FilterPathPart {

    private final Set<String> whitelist;
    
    public WhiteListFilter(String... white) {
      this(false, UnknownPathOnTraversalHandling.THROW_IF_ALL, white);
    }
    
    public WhiteListFilter(UnknownPathOnTraversalHandling unknownHandling, String... white) {
      this(false, unknownHandling, white);
    }
    
    
    public WhiteListFilter(boolean orSelf, UnknownPathOnTraversalHandling unknownHandling, String... white) {
      super(orSelf, unknownHandling);
      whitelist = new HashSet<String>(Arrays.asList(white));
    }
    
    public WhiteListFilter(StatisticsPathPart... white) {
      this(false, UnknownPathOnTraversalHandling.THROW_IF_ALL, white);
    }
    
    public WhiteListFilter(UnknownPathOnTraversalHandling unknownHandling, StatisticsPathPart... white) {
      this(false, unknownHandling, white);
    }
    
    public WhiteListFilter(boolean orSelf, UnknownPathOnTraversalHandling unknownHandling, StatisticsPathPart... white) {
      super(orSelf, unknownHandling);
      whitelist = new HashSet<String>();
      for (StatisticsPathPart statisticsPathPart : white) {
        whitelist.add(statisticsPathPart.getPartName());
      }
    }
    
    @Override
    public boolean accept(String nodename) {
      return whitelist.contains(nodename);
    }
    
    @Override
    public String toString() {
      return getPartName() + "- WhiteList: " + whitelist;
    }
    
  }
  
  public static class BlackListFilter extends FilterPathPart {

    private final Set<String> blacklist;
    
    public BlackListFilter(String... black) {
      this(UnknownPathOnTraversalHandling.THROW_IF_ALL, black);
    }
    
    public BlackListFilter(UnknownPathOnTraversalHandling unknownHandling, String... black) {
      this(false, unknownHandling, black);
    }
    
    public BlackListFilter(boolean orSelf, UnknownPathOnTraversalHandling unknownHandling, String... black) {
      super(orSelf, unknownHandling);
      blacklist = new HashSet<String>(Arrays.asList(black));
    }
    
    public BlackListFilter(StatisticsPathPart... black) {
      this(false, UnknownPathOnTraversalHandling.THROW_IF_ALL, black);
    }
    
    public BlackListFilter(UnknownPathOnTraversalHandling unknownHandling, StatisticsPathPart... black) {
      this(false, unknownHandling, black);
    }
    
    public BlackListFilter(boolean orSelf, UnknownPathOnTraversalHandling unknownHandling, StatisticsPathPart... black) {
      super(orSelf, unknownHandling);
      blacklist = new HashSet<String>();
      for (StatisticsPathPart statisticsPathPart : black) {
        blacklist.add(statisticsPathPart.getPartName());
      }
    }
    
    @Override
    public boolean accept(String nodename) {
      return !blacklist.contains(nodename);
    }
    
    @Override
    public String toString() {
      return getPartName() + "- BlackList: " + blacklist;
    }
    
  }


}
