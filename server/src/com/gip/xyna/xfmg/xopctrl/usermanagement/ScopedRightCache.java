/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xopctrl.usermanagement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.ScopedRight;


public class ScopedRightCache {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(ScopedRightCache.class);

  private Map<String, BranchingTreeNode> keysToScopeTrees = new HashMap<String, BranchingTreeNode>();
  private final String roleName;
  
  
  public ScopedRightCache(String name, Set<String> scopedRights, Map<String, RightScope> rightScopeMap) {
    this.roleName = name;
    for (String sr : scopedRights) {
      String[] parts = RightScopeBuilder.splitScopedRightIntoParts(sr);
      String[] partsWithoutScopeName = new String[parts.length - 1];
      System.arraycopy(parts, 1, partsWithoutScopeName, 0, partsWithoutScopeName.length);
      if( rightScopeMap.get(parts[0]) == null ) {
        logger.warn( "no RightScope for \"" + parts[0]+"\"");
      } else {
        addRightFromScope(rightScopeMap.get(parts[0]), partsWithoutScopeName);
      }
    }
  }

  protected void addRightFromScope(RightScope scope, String... scopeParts) {
    String scopeName = scope.getName();
    BranchingTreeNode rootNode = keysToScopeTrees.get(scopeName);
    if (rootNode == null) {
      rootNode = new BranchingTreeNode();
      keysToScopeTrees.put(scopeName, rootNode);
    }
    distributeRightNodes(scopeParts, 0, rootNode);
  }
  
  private void distributeRightNodes(String[] scopeParts, int index, BranchingTreeNode currentNode) {
    BranchingTreeNode nextNode;
    
    //abhängig vom Typ wird der Rechtebereich nun in eine der Maps des aktuellen Knotens eingetragen
    ScopedRightPartType type = ScopedRightPartType.determineType(scopeParts[index]);
    switch (type) {
      case EXCLUSION :
        if (!currentNode.excludingBranches.containsKey(scopeParts[index])) {
          Pair<Pattern, List<Pattern>> pattern = generatePatternForExcluding(scopeParts[index]);
          currentNode.excludingBranches.put(scopeParts[index], new Pair<Pair<Pattern, List<Pattern>>, BranchingTreeNode>(pattern, new BranchingTreeNode()));
        }
        nextNode = currentNode.excludingBranches.get(scopeParts[index]).getSecond();
        break;
      case WILD :
        if (currentNode.wildBranch == null) {
          currentNode.wildBranch = new BranchingTreeNode();
        }
        nextNode = currentNode.wildBranch;
        break;
      case PARTIAL_WILD :
        if (!currentNode.partiallyWildBranches.containsKey(scopeParts[index])) {
          currentNode.partiallyWildBranches.put(scopeParts[index], 
              Pair.of( new SRCPattern(scopeParts[index]), new BranchingTreeNode() ) );
        }
        nextNode = currentNode.partiallyWildBranches.get(scopeParts[index]).getSecond();
        break;
      case PRIMITIVE :
        if (!currentNode.branches.containsKey(scopeParts[index])) {
          currentNode.branches.put(scopeParts[index], new BranchingTreeNode());
        }
        nextNode = currentNode.branches.get(scopeParts[index]);
        break;
      default :
        throw new RuntimeException("Unknown ScopePartType: " + type);
    }
    
    //den nächsten Teil des Rechts untersuchen
    if (index + 1 < scopeParts.length) {
      distributeRightNodes(scopeParts, index + 1, nextNode);
    }
  }
  
  
  public String getRoleName() {
    return roleName;
  }
  
  public boolean hasRight(String key, String... scopeParts) {
    BranchingTreeNode node = keysToScopeTrees.get(key);
    if (node == null) {
      return false;
    }
    return node.covers(scopeParts, 0);
  }
  
  
  private static final Pattern RIGHT_PART_SEPERATION_PATTERN = Pattern.compile("(?<!\\\\)[:]");
  private static final Pattern PARTIAL_WILD_SEPERATION_PATTERN = Pattern.compile("(?<!\\\\)[*]");
  private static final Pattern COMMA_SEPERATION_PATTERN = Pattern.compile("(?<!\\\\)[,]");
  private static final Pattern EXCLUSION_PATTERN = Pattern.compile("(.*)(?<!\\\\)[!][\\{](.*)\\}");

  public boolean hasRight(String right) { 
    String[] parts = RIGHT_PART_SEPERATION_PATTERN.split(right, -1); //limit = -1, damit Leerstrings am Ende erhalten bleiben
    String[] scopeParts = new String[parts.length - 1];
    System.arraycopy(parts, 1, scopeParts, 0, parts.length - 1);
    return hasRight(parts[0], scopeParts);
  }
  
  
  /**
   * Ersetzt alle *, die nicht escaped sind, durch .* <br>
   * Alle anderen Metazeichen werden durch {@link Pattern#quote(String)} maskiert.
   * 
   * @param partialWildPart
   * @return
   */
  private static Pattern generatePatternForPartialWild(String partialWildPart) {
    String[] parts = PARTIAL_WILD_SEPERATION_PATTERN.split(partialWildPart, -1); //limit = -1, damit Leerstrings am Ende erhalten bleiben
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<parts.length; i++) {
      if (i > 0) {
        sb.append(".*");
      }
      sb.append(Pattern.quote(parts[i]));
    }
    
    return Pattern.compile("^" + sb.toString() + "$");
  }

  /**
   * Liefert für einen scopePart, der Anteile enthält die ausgeschlossen werden sollen,
   * ein Pair aus einem Pattern das zutreffen soll und einer Liste der Pattern, die
   * nicht zutreffen dürfen
   * @param excludingPart
   * @return
   */
  private static Pair<Pattern, List<Pattern>> generatePatternForExcluding(String excludingPart) {
    Matcher matcher = (EXCLUSION_PATTERN.matcher(excludingPart));
    
    if (!matcher.matches()) {
      throw new IllegalArgumentException("\""+excludingPart +"\" does not match pattern \""+EXCLUSION_PATTERN+"\"");
    }
    
    //dieses Pattern soll zutreffen
    Pattern match = generatePatternForPartialWild(matcher.group(1));
    
    //diese Pattern dürfen nicht zutreffen
    List<Pattern> exclude = new ArrayList<Pattern>();
    String[] split = COMMA_SEPERATION_PATTERN.split(matcher.group(2), -1);
    for (String s : split) {
      exclude.add(generatePatternForPartialWild(s.trim()));
    }
    
    return Pair.of(match, exclude);
  }
  
  
  private static enum ScopedRightPartType {
    /**
     * der scopePart besteht nur aus '*', d.h. alles ist erlaubt
     */
    WILD,
    
    /**
     * der scopePart enthält mindestens ein '*' als Wildcard
     */
    PARTIAL_WILD,
    
    /**
     * der scopePart enhält keine Wildcards oder Ausschlüsse
     */
    PRIMITIVE,
    
    /**
     * der scopePart enthält Teile, die ausgeschlossen werden sollen,
     * z.B. A*!{Ab,*e} liefert alle Strings, die mit 'A' anfangen, außer 'Ab' und denen, die mit 'e' aufhören
     */
    EXCLUSION;
    
    static ScopedRightPartType determineType(String scopePart) {
      if (EXCLUSION_PATTERN.matcher(scopePart).matches()) {
        return EXCLUSION;
      } else if (scopePart.equals("*")) {
        return WILD;
      } else if (scopePart.contains("*")) {
        return PARTIAL_WILD;
      } else {
        return PRIMITIVE;
      }
    }
    
  }
  
  private static class SRCPattern {
    private Pattern pattern;
    private String orig;
    
    public SRCPattern(String string) {
      this.orig = string;
      pattern = generatePatternForPartialWild(string);
    }
    
    public Matcher matcher(String input) {
      return pattern.matcher(input);
    }

    public String getOriginalPattern() {
      return orig;
    }
    
  }
  
  
  private static class BranchingTreeNode {
    
    private Map<String, BranchingTreeNode> branches;
    private Map<String, Pair<SRCPattern, BranchingTreeNode>> partiallyWildBranches;
    private BranchingTreeNode wildBranch;
    private Map<String, Pair<Pair<Pattern, List<Pattern>>, BranchingTreeNode>> excludingBranches;
    
    public BranchingTreeNode() {
      branches = new HashMap<String, BranchingTreeNode>();
      partiallyWildBranches  = new HashMap<String, Pair<SRCPattern, BranchingTreeNode>>();
      excludingBranches  = new HashMap<String, Pair<Pair<Pattern, List<Pattern>>, BranchingTreeNode>>();
    }
    
    public boolean covers(String[] scopeParts, int index) {
      boolean lastPart = index + 1 >= scopeParts.length;
      if (wildBranch != null) {
        if (lastPart) {
          return true; 
        } else if (wildBranch.covers(scopeParts, index+1)) {
          return true;
        }
      }
      BranchingTreeNode subBranch = branches.get(scopeParts[index]);
      if (subBranch != null) {
        if (lastPart) {
          return true;
        } else if (subBranch.covers(scopeParts, index+1)) {
          return true;
        }
      }
      for (Pair<SRCPattern, BranchingTreeNode> patternedPair : partiallyWildBranches.values()) {
        if (patternedPair.getFirst().matcher(scopeParts[index]).matches()) {
          if (lastPart) {
            return true;
          } else if (patternedPair.getSecond().covers(scopeParts, index+1)) {
            return true;
          }
        }
      }
      for (Pair<Pair<Pattern,List<Pattern>>, BranchingTreeNode> patternedPair : excludingBranches.values()) {
        //das erste Pattern muss zutreffen
        if (patternedPair.getFirst().getFirst().matcher(scopeParts[index]).matches()) {
          //die Pattern aus dem zweiten Teil dürfen nicht zutreffen
          for (Pattern pattern : patternedPair.getFirst().getSecond()) {
            if (pattern.matcher(scopeParts[index]).matches()) {
              return false; //der scopePart ist ausgeschlossen worden
            }
          }
          if (lastPart) {
            return true;
          } else if (patternedPair.getSecond().covers(scopeParts, index+1)) {
            return true;
          }
        }
      }
      return false;
    }

    //TODO unvollständig...
    public void fillRightsCovering(List<String> list, String prefix, String[] scopeParts, int index) {
      String sp = null;
      if( index <  scopeParts.length) {
        sp = scopeParts[index];
      }
      if( wildBranch == null && branches.isEmpty() && partiallyWildBranches.isEmpty() ) {
        list.add( prefix );
        return;
      }
      
      if (wildBranch != null) {
        wildBranch.fillRightsCovering(list, prefix +":*", scopeParts, index+1 );
      }
      if( sp != null ) {
        BranchingTreeNode subBranch = branches.get(sp);
        if (subBranch != null) {
          subBranch.fillRightsCovering(list, prefix +":"+sp, scopeParts, index+1 );
        }
      } else {
        for( Map.Entry<String, BranchingTreeNode> entry : branches.entrySet() ) {
          entry.getValue().fillRightsCovering(list, prefix +":"+entry.getKey(), scopeParts, index+1 );
        }
        for (Pair<SRCPattern, BranchingTreeNode> patternedPair : partiallyWildBranches.values()) {
          String next = prefix +":"+ patternedPair.getFirst().getOriginalPattern() ;
          patternedPair.getSecond().fillRightsCovering(list, next, scopeParts, index+1 );
        }
       
      }
    }
    
  }

  public boolean hasRightCoveredBy(ScopedRight scopedRight, Action action) {
    BranchingTreeNode node = keysToScopeTrees.get(scopedRight.getKey());
    if (node == null) {
      return false;
    }
    String[] scopeParts = new String[] {action.name()};
    return node.covers(scopeParts, 0);
  }
  
  public List<String> getRightsCovering(ScopedRight scopedRight, Action action) {
    List<String> list = new ArrayList<String>();
    BranchingTreeNode node = keysToScopeTrees.get(scopedRight.getKey());
    if (node == null) {
      return list;
    }
    String[] scopeParts = new String[] {action.name()};
    node.fillRightsCovering( list, scopedRight.getKey(), scopeParts, 0 );
    return list;
  }
  
  public Set<String> getKeySet() {
    return keysToScopeTrees.keySet();
  }
  
}
