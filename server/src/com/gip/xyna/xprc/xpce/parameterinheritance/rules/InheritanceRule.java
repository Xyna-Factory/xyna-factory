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
package com.gip.xyna.xprc.xpce.parameterinheritance.rules;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;

import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_MONITORING_TYPE;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringCodes;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspensionBackupMode;


/**
 * Basisklasse f�r Parameter-Vererbungsregeln mit verschiedenen Werten.
 * <br>
 * InheritanceRule ist immutable.
 * <br>
 * Die Erstellung einer InheritanceRule l�uft �ber einen Builder, 
 * z.B. f�r die Regel: alle Kinder sollen mit Precedence 150 mit dem MonitoringLevel 15 laufen
 * <pre>
 * InheritanceRule.createMonitoringLevelRule("15").
 *   childFilter("*").
 *   precedence(150).
 *   build();
 * </pre>
 */
public abstract class InheritanceRule implements Serializable {
  
  private static final long serialVersionUID = 1L;

  private int precedence = 0; //Priorit�t der Regel
  
  //Filter auf welche Kindauftr�ge die Regel angewendet werden soll
  //Leerstring bzw. null wenn die Regel auf den eigenen Auftrag/OrderType angewendet werden soll
  private String childFilter = "";
  private Pattern childFilterPattern;

  
  public InheritanceRule() {
  }
  
  public InheritanceRule(InheritanceRule inheritanceRule) {
    this.precedence = inheritanceRule.precedence;
    this.childFilter = inheritanceRule.childFilter;
    this.childFilterPattern = inheritanceRule.childFilterPattern;
  }

  public abstract Integer getValueAsInt();

  public abstract String getValueAsString();
  
  /**
   * Liefert den Wert der Regel, wie er bei der Definition angegeben wurde.
   * Zum Beispiel wird f�r XynaProperties der Name zur�ckgeliefert (und nicht der Wert der XynaProperty).
   * @return
   */
  public abstract String getUnevaluatedValue();
  
  protected abstract InheritanceRule clone();

  
  public String getChildFilter() {
    return childFilter;
  }
  
  
  public int getPrecedence() {
    return precedence;
  }
  
  /**
   * �berpr�ft, ob die Regel zu einer childHierarchy passt.
   * @param childHierarchy
   * @return true, wenn die Regel f�r die childHierarchy gilt.
   */
  public boolean matches(String childHierarchy) {
    if (childHierarchy == null || childHierarchy.length() == 0) {
      //Die Regel soll f�r den eigenen Auftrag gelten.
      //Dies ist der Fall, wenn childFilterPattern null ist.
      return childFilterPattern == null;
    } else {
      //Die Regel soll f�r eine Hierarchie von Kindauftr�gen gelten.
      //Daher darf childFilterPattern nicht null sein.
      if (childFilterPattern == null) {
        return false;
      }
      
      //Passt die Hierarchie zum childFilter?
      return childFilterPattern.matcher(childHierarchy).matches();
    }
  }

  
  
  @Override
  public int hashCode() {
    return Objects.hash(childFilter, precedence);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    InheritanceRule other = (InheritanceRule) obj;
    if (childFilter == null) {
      if (other.childFilter != null)
        return false;
    }
    else if (!childFilter.equals(other.childFilter))
      return false;
    if (precedence != other.precedence)
      return false;
    return true;
  }

  /**
   * Erzeugt einen Builder f�r eine InheritanceRule, die f�r das Monitoringlevel verwendet werden kann.
   * @param value Wert f�r das Monitoringlevel oder Name einer XynaProperty, deren Wert als
   * Monitoringlevel verwendet werden soll
   * @return
   * @throws XPRC_INVALID_MONITORING_TYPE 
   */
  public static  Builder createMonitoringLevelRule(String value) throws XPRC_INVALID_MONITORING_TYPE {
    Builder builder = new Builder();
    
    Integer intValue = null;
    try {
      intValue = Integer.valueOf(value);
    } catch (NumberFormatException e) {
      //ok, MonitoringLevel wird �ber eine XynaProperty definiert
    }
    
    if (intValue != null && intValue >= 0) {
      //Monitoringlevel ist direkt als Integer angegeben
      if (!(MonitoringCodes.getAllValidMonitoringLevels().contains(intValue))) {
        throw new XPRC_INVALID_MONITORING_TYPE(intValue);
      }
      builder.rule = new IntegerInheritanceRule(intValue);
    } else {
      //Monitoringlevel dynamisch �ber eine XynaProperty setzen
      if (value == null || (intValue != null && intValue < 0)) {
        //es soll das Default-Monitoringlevel verwendet werden
        value = XynaProperty.XYNA_DEFAULT_MONITORING_LEVEL.getPropertyName();
      }
      builder.rule = new XynaPropertyInheritanceRule(value);
    }
    
    return builder;
  }
  
  
  public static Builder createSuspensionBackupRule(SuspensionBackupMode value) {
    Builder builder = new Builder();
    builder.rule = new EnumInheritanceRule<SuspensionBackupMode>(value);
    return builder;
  }
  
  public static Builder createBackupWhenRemoteCallRule(SuspensionBackupMode value) {
    Builder builder = new Builder();
    builder.rule = new EnumInheritanceRule<SuspensionBackupMode>(value);
    return builder;
  }
  
  public static class Builder {
    
    InheritanceRule rule;
    
    public Builder childFilter(String childFilter) {
      if (childFilter == null || childFilter.length() == 0) {
        //Regel soll f�r den Auftrag selbst gelten
        rule.childFilter = "";
        rule.childFilterPattern = null;
      } else {
        //Regel soll f�r Kindauftr�ge gelten
        rule.childFilter = childFilter;
        
        String regex = buildRegEx(childFilter);
        rule.childFilterPattern = Pattern.compile(regex);
      }
      
      return this;
    }

    private String buildRegEx(String childFilter) {
      if (childFilter.equals("*")) {
        return ".*";
      } else {
        StringBuilder regExpBuilder = new StringBuilder();
        String[] filterParts = childFilter.split("(?<![\\\\\\\\]*[\\\\])\\:");
        for (int i = 0; i<filterParts.length; i++) {
          if (filterParts[i].equals("*")) {
            if (i == 0) {
              regExpBuilder.append("(^.*($|(?<![\\\\\\\\]*[\\\\])[:]))?");
            } else if (i+1 == filterParts.length) {
              if (regExpBuilder.charAt(regExpBuilder.length() - 1) == ':') {
                regExpBuilder.deleteCharAt(regExpBuilder.length() - 1);
                regExpBuilder.append("((?<![\\\\\\\\]*[\\\\])[:].*)?$");
              } else {
                throw new IllegalArgumentException("Invalid childFilter format '" + childFilter + "'");
              }
            } else {
              if (regExpBuilder.charAt(regExpBuilder.length() - 1) == ':') {
                regExpBuilder.deleteCharAt(regExpBuilder.length() - 1);
                regExpBuilder.append("(?<![\\\\\\\\]*[\\\\])[:].*?(?<![\\\\\\\\]*[\\\\])[:]");
              } else {
                throw new IllegalArgumentException("Invalid childFilter format '" + childFilter + "'");
              }
            }
          } else {
            String[] wildCardSperatedParts = filterParts[i].split("(?<![\\\\\\\\]*[\\\\])\\*");
            for (int j = 0; j<wildCardSperatedParts.length; j++) {
              wildCardSperatedParts[j] = Pattern.quote(wildCardSperatedParts[j]);
            }
            regExpBuilder.append(StringUtils.joinStringArray(wildCardSperatedParts, "([^:]|(?<![\\\\\\\\]*[\\\\])[:])*?"));
            if (i+1 < filterParts.length) {
              regExpBuilder.append(':');
            }
          }
        }
        return regExpBuilder.toString();
      }
    }
    
    public Builder precedence(int precedence) {
      rule.precedence = precedence;
      return this;
    }

    public InheritanceRule build() {
      return rule.clone();
    }
  }
  
  
  public static class ChildFilterComparator implements Comparator<InheritanceRule> {

    public int compare(InheritanceRule o1, InheritanceRule o2) {
      //Regeln f�r eigenen Auftrag nach vorne sortieren
      if (o1.getChildFilter().length() == 0) {
        return o2.getChildFilter().length() == 0 ? 0 : -1;
      }
      if (o2.getChildFilter().length() == 0) {
        return 1;
      }
      
      //Regeln f�r Kindauftr�ge alphabetisch sortieren
      return o1.getChildFilter().compareTo(o2.getChildFilter());
    }
  }

  public static class PrecedenceComparator implements Comparator<InheritanceRule> {
    
    public int compare(InheritanceRule o1, InheritanceRule o2) {
      if (o1 == null && o2 == null) {
        return 0;
      }
      
      if (o1 == null) {
        return -1;
      }

      if (o2 == null) {
        return 1;
      }
      
      if (o1.precedence == o2.precedence) {
        return 0;
      }
      
      return o1.precedence > o2.precedence ? 1 : -1;
    }
  }
}
