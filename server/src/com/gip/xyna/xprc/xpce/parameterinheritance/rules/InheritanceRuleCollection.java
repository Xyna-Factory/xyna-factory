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
package com.gip.xyna.xprc.xpce.parameterinheritance.rules;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule.ChildFilterComparator;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule.PrecedenceComparator;

/**
 * InheritanceRuleCollection fasst eine Menge von InheritanceRules zusammen.
 *
 */
public class InheritanceRuleCollection implements Serializable {

  private static final long serialVersionUID = 1L;

  //Map mit InheritanceRules. Als Key dient der jeweilige childFilter (z.B. *).
  private ConcurrentMap<String, InheritanceRule> inheritanceRules = new ConcurrentHashMap<String, InheritanceRule>();
  
  //Cache für bereits ermittelte Regeln für eine Hierarchie von OrderTypes.
  //Hier ist der Key die String-Darstellung einer konkreten Aufrufhierarchie (z.B. :OTA:OTB).
  private transient ConcurrentMap<String, InheritanceRule> preferredRulesCache = new ConcurrentHashMap<String, InheritanceRule>();
  
  public InheritanceRule add(InheritanceRule rule) {
    preferredRulesCache.clear();
    return inheritanceRules.put(rule.getChildFilter(), rule);
  }

  public InheritanceRule remove(String childFilter) {
    preferredRulesCache.clear();
    return inheritanceRules.remove(childFilter);
  }
  
  public Collection<InheritanceRule> getAllInheritanceRules() {
    return Collections.unmodifiableCollection(inheritanceRules.values());
  }

  public List<InheritanceRule> getInheritanceRulesOrderedByChildFilter() {
    List<InheritanceRule> ret = new ArrayList<InheritanceRule>(inheritanceRules.values());
    Collections.sort(ret, new ChildFilterComparator());
    
    return ret;
  }
  
  public boolean isEmpty() {
    return inheritanceRules.isEmpty();
  }
  
  public void clear() {
    preferredRulesCache.clear();
    inheritanceRules.clear();
  }

  /**
   * Liefert die Regel mit der höchsten Precedence, die in der InheritanceRuleCollection enthalten ist
   * und die zur übergebenen Aufrufhierarchie passt.
   * @param childHierarchy
   * @return
   */
  public InheritanceRule getPreferredInheritanceRule(String childHierarchy) {
    
    InheritanceRule preferredRule = preferredRulesCache.get(childHierarchy);
    
    if (preferredRule == null) {
      PrecedenceComparator comparator = new PrecedenceComparator();
      //bevorzugte InheritanceRule neu ermitteln
      for (InheritanceRule rule : inheritanceRules.values()) {
        if (rule.matches(childHierarchy)) { //Regel muss zur aktuellen Aufrufhierarchie passen
          if (comparator.compare(rule, preferredRule) > 0) {
            //Regel mit höherer Precedence gefunden
            preferredRule = rule;
          }
        }
      }
      
      if (preferredRule != null) {
        //gefundene Regel in den Cache eintragen
        preferredRulesCache.put(childHierarchy, preferredRule);
      }
    }
    
    return preferredRule;
  }
  
  
  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    preferredRulesCache = new ConcurrentHashMap<String, InheritanceRule>();
  }
}
