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


package com.gip.xyna.xprc.xfractwfe.generation;



import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xprc.xfractwfe.generation.Step.DistinctionType;



public interface Distinction {
  public static final String FORMULA_GUI_DELIMITER_START = "¿";
  public static final String FORMULA_GUI_DELIMITER_END = "?";


  public List<CaseInfo> getHandledCases();
  public List<CaseInfo> getUnhandledCases(boolean considerRetryAsHandled);
  public void addBranch(int branchNo, String expression, String name);
  public void addMissingBranches();
  public BranchInfo removeBranch(int index);
  public List<BranchInfo> getBranchesForGUI();
  public String getOuterConditionForGUI();
  public DistinctionType getDistinctionType();
  public String getOuterCondition();


  /**
   * kapselt attribute, die an case element hängen, also eine lane
   */
  public static class CaseInfo {
    /**
     * ~ label
     */
    private String name;
    /**
     * ~ premise
     */
    private String complexName;
    private String alias; //zeigt auf id (ConditionalBranch) oder complexname einer anderen Lane
    private Distinction distinctionStep;
    private String id;
    private boolean isDefault;


    public CaseInfo(String id, String name, String complexName, String alias, Distinction distinctionStep) {
      this(id, name, complexName, alias, distinctionStep, false);
    }


    public CaseInfo(String id, String name, String alias, Distinction distinctionStep) {
      this(id, name, null, alias, distinctionStep, true);
    }


    protected CaseInfo(String id, String name, String complexName, String alias, Distinction distinctionStep, boolean isDefault) {
      this.id = id;
      this.name = name;
      this.complexName = complexName;
      this.isDefault = isDefault;
      this.distinctionStep = distinctionStep;
      
      //soll null bleiben, auch wenn attribut gesetzt wurde
      if (alias != null && alias.length() > 0) {
        this.alias = alias;
      }
    }


    public String getId() {
      return id;
    }
    
    public void setId(String id) {
      this.id = id;
    }


    /**
     * Returns either the alias of a case (if set) or the name another case would use as a reference via its alias.
     */
    public String getBranchName() {
      String alias = getAlias();
      if ( (alias != null) && (alias.length() > 0) ) {
        return alias;
      } else {
        if (distinctionStep.getDistinctionType() == DistinctionType.ConditionalBranch) {
          return getId();
        } else {
          return getComplexName();
        }
      }
    }


    public String getName() {
      return name;
    }


    public String getComplexName() {
      return complexName;
    }


    protected void setComplexName(String complexName) {
      this.complexName = complexName;
    }


    /**
     * Gives an enhanced version of the complex name that makes formula handling in the GUI easier.<br><br>
     * 
     * For conditional branching, this returns the outer condition with the question mark being replaced by {@link StepChoice#FORMULA_GUI_DELIMITER_END} + case-specific-condition + {@link StepChoice#FORMULA_GUI_DELIMITER_END}.<br><br>
     * 
     * For other choices, this just returns the complex name.
     */
    public String getGuiName() {
      if (distinctionStep.getDistinctionType() != DistinctionType.ConditionalBranch) {
        return getComplexName();
      }

      String outerCondition = distinctionStep.getOuterCondition();
      int questionMarkPos = StepChoice.calcIndexOfFormulaDelimiter(outerCondition);
      String beforeQuestionMark = outerCondition.substring(0, questionMarkPos);
      String afterQuestionMark = outerCondition.substring(questionMarkPos+1);

      String caseCondition = "";
      String complexName = getComplexName();
      if (complexName != null && !complexName.isEmpty()) {
        caseCondition = complexName.substring(beforeQuestionMark.length(), complexName.lastIndexOf(afterQuestionMark));
      }

      return beforeQuestionMark + FORMULA_GUI_DELIMITER_START + caseCondition + FORMULA_GUI_DELIMITER_END + afterQuestionMark;
    }


    public String getAlias() {
      return alias;
    }


    public void setAlias(String alias) {
      this.alias = alias;
    }


    public boolean isMainCaseOfItsBranch() {
      String alias = getAlias();
      return ( (alias == null) || (alias.length() == 0) );
    }


    public boolean isDefault() {
      return isDefault;
    }


    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof CaseInfo)) {
        return false;
      }
      if (id != null) {
        return id.equals(((CaseInfo) obj).id);
      } else {
        if (alias == null) {
          return (name.equals(((CaseInfo) obj).name) && complexName.equals(((CaseInfo) obj).complexName) && ((CaseInfo) obj).alias == null);
        } else {
          return (name.equals(((CaseInfo) obj).name) && complexName.equals(((CaseInfo) obj).complexName) && alias.equals(((CaseInfo) obj).alias));
        }
      }

    }


    private transient int hash;


    @Override
    public int hashCode() {
      int h = hash;
      if (h == 0) {
        h = name.hashCode() ^ complexName.hashCode();
        if (alias != null) {
          h ^= alias.hashCode();
        }
        hash = h;
      }
      return h;
    }
  }


  public class BranchInfo {

    private List<CaseInfo> cases = new ArrayList<CaseInfo>();


    public List<CaseInfo> getCases() {
      return cases;
    }

    public CaseInfo getMainCase() {
      for (CaseInfo curCase : cases) {
        if (curCase.isMainCaseOfItsBranch()) {
          return curCase;
        }
      }

      return null;
    }

    public void addCase(CaseInfo newCase) {
      cases.add(newCase);
    }

    public Step getMainStep() {
      return null;
    }

    public Step getExecutedStep() {
      Step mainStep = getMainStep();
      if (mainStep != null && mainStep.hasBeenExecuted()) {
        return mainStep;
      }

      return null;
    }

    public String getBranchName() {
      CaseInfo mainCase = getMainCase();
      if (mainCase == null) {
        return null;
      }

      return mainCase.getBranchName();
    }

    public boolean isFakeBranchForOldGUI() {
     return false;
    }
  }

}
