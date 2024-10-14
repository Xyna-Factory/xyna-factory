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

package com.gip.xyna.xact.filter.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer.TokenType;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer.XFLLexem;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils.EscapableXMLEntity;

import xnwh.persistence.QueryParameter;
import xnwh.persistence.SelectionMask;

public class QueryUtils {
  
  
  private QueryUtils() {
    
  }
  
  public static void saveSelectionMask(GBSubObject stepFunctionGBSubObject, Dataflow dataflow, SelectionMask selectionMask) {
    List<AVariableIdentification> variables = stepFunctionGBSubObject.getIdentifiedVariables().getVariables(VarUsageType.input);
    for (AVariableIdentification vi : variables) {
      if(SelectionMask.class.getName().equals(vi.getIdentifiedVariable().getFQClassName())) {
        dataflow.setConstantValue(vi, null, selectionMask);
        break;
      }
    }
  }
  
  public static void refreshRootType(GBSubObject stepFunctionGBSubObject, DOM rootDatatype) {
    
    SelectionMask selectionMask = getSelectionMask(stepFunctionGBSubObject);
    if(selectionMask != null) {
      
      //update output variable
      AVariable sv = AVariable.createAVariable(String.valueOf(stepFunctionGBSubObject.getStep().getCreator().getNextXmlId()), rootDatatype, true);  
    
      ((StepFunction)stepFunctionGBSubObject.getStep()).removeOutputVarId(0);
      ((StepFunction)stepFunctionGBSubObject.getStep()).addOutputVarId(0, sv.getId());
      ((StepFunction)stepFunctionGBSubObject.getStep()).getParentScope().getChildStep().addVar(sv);
      
      stepFunctionGBSubObject.refreshIdentifiedVariables();
    }
  }
  
  public static SelectionMask getSelectionMask(GBSubObject stepFunctionGBSubObject) {
    List<AVariableIdentification> variableIdentifications = stepFunctionGBSubObject.getIdentifiedVariables().getVariables(VarUsageType.input);
    for (AVariableIdentification vi : variableIdentifications) {
      if(SelectionMask.class.getName().equals(vi.getIdentifiedVariable().getFQClassName())) {
        return (SelectionMask) vi.getConstantValue(stepFunctionGBSubObject.getRoot());
      }
    }
    return null;
  }
  
  public static void saveQueryParamater(GBSubObject stepFunctionGBSubObject, Dataflow dataflow, QueryParameter queryParameter) {
    List<AVariableIdentification> variables = stepFunctionGBSubObject.getIdentifiedVariables().getVariables(VarUsageType.input);
    for (AVariableIdentification vi : variables) {
      if (vi.getIdentifiedVariable().getVarName().equals(Tags.QUERY_CONST_QUERY_PARAMETER)) {
        dataflow.setConstantValue(vi, null, queryParameter);
        break;
      }
    }
  }
  
  public static QueryParameter getQueryParameter(GBSubObject stepFunctionGBSubObject) {
    List<AVariableIdentification> variableIdentifications = stepFunctionGBSubObject.getIdentifiedVariables().getVariables(VarUsageType.input);
    for (AVariableIdentification vi : variableIdentifications) {
      if (vi.getIdentifiedVariable().getVarName().equals(Tags.QUERY_CONST_QUERY_PARAMETER)) {
        return (QueryParameter) vi.getConstantValue(stepFunctionGBSubObject.getRoot());
      }
    }
    return null;
  }
  
  public static StepMapping findQueryHelperMapping(GBSubObject functionGBSubobject) {
    if(!(functionGBSubobject.getStep() instanceof StepFunction)) {
      return null;
    }
    Set<Step> allSteps = new HashSet<>();
    WF.addChildStepsRecursively(allSteps, functionGBSubobject.getStep().getParentWFObject().getWfAsStep());
    
    List<AVariableIdentification> variableIdentifications = functionGBSubobject.getIdentifiedVariables().getVariables(VarUsageType.input);
    for (AVariableIdentification varIdent : variableIdentifications) {
      String targetVariableId = varIdent.connectedness.getConnectedVariableId();
      for (Step _step : allSteps) {
        if(_step instanceof StepMapping) {
          StepMapping mapping = (StepMapping)_step;
          if(mapping.isConditionMapping()) {
            String[] varIds = mapping.getOutputVarIds();
            for (String id : varIds) {
              if(id.equals(targetVariableId) && varIdent.connectedness.isUserConnected())
                return mapping;
            }
          }
        }
      }
    }
    return null;
  }
  
  public static StepMapping findQueryHelperMapping(StepFunction stepFunction) {
    Set<Step> allSteps = new HashSet<>();
    WF.addChildStepsRecursively(allSteps, stepFunction.getParentWFObject().getWfAsStep());
    
    for (Step step : allSteps) {
      if(step instanceof StepMapping) {
        StepMapping mapping = (StepMapping)step;
        if(mapping.isConditionMapping() && isInputOfQuery(mapping, stepFunction)) {
          return mapping;
        }
      }
    }
    return null;
  }
  
  private static boolean isInputOfQuery(StepMapping mapping, StepFunction query) {
    for (String mappingOutputVarId : mapping.getOutputVarIds() ) {
      for (String queryInputVarId : query.getInputVarIds() ) {
        if (mappingOutputVarId.equals(queryInputVarId)) {
          return true;
        }
      }
    }    
    return false;
  }
  
  /**
   * Aktualisiert die Expression des versteckten Mappings anhand der FilterConditions.
   * @param functionGBSubobject
   */
  public static void refreshQueryHelperMappingExpression(GBSubObject functionGBSubobject) {
    if (functionGBSubobject.getStep() instanceof StepFunction) {
      StepFunction stepFunction = (StepFunction) functionGBSubobject.getStep();
      StepMapping stepMapping = findQueryHelperMapping(functionGBSubobject);
      refreshQueryHelperMappingExpression(stepFunction, stepMapping);
    }
  }
  
  public static void refreshQueryHelperMappingExpression(StepFunction stepFunction, StepMapping stepMapping) {
    List<String> filterConditions = stepFunction.getQueryFilterConditions();
    if (stepMapping != null && filterConditions != null) {
      if(filterConditions.isEmpty() && stepMapping.getFormulaCount() > 0) {
        for(int i = 0; i < stepMapping.getFormulaCount(); i++) {
          stepMapping.removeFormula(i);
        }
        for(int i=stepMapping.getInputVarIds().length-1; i>=0; i--) {
          stepMapping.getInputConnections().removeInputConnection(i);
          stepMapping.getInputVars().remove(i);
          //do not call onInputVarRemoved -> there is no formula anyway
        }
      } else if(!filterConditions.isEmpty()){
        ExpressionUtils.cleanUpMappingInputsAndFilterConditions(filterConditions, stepMapping);
        String mappingExpression = createHelperMappingExpression(filterConditions, stepMapping);
        if (stepMapping.getFormulaCount() > 0) {
          stepMapping.replaceFormula(0, mappingExpression);
        } else {
          stepMapping.addFormula(0, mappingExpression);
        }
      }
    }
  }
  
  



  /**
   * Mapping-Expression aus den FilterConditions erzeugen 
   * @param filterConditions
   * @param stepMapping
   * @return
   */
  private static String createHelperMappingExpression(List<String> filterConditions, StepMapping stepMapping) {

    if (filterConditions == null || filterConditions.isEmpty()) {
      return "";
    }
    
    /* Ermitteln des Index der ersten Output-Variablen.
     * Der Index ist abhängig von der Anzahl der Input-Variablen.
     */
    int firstOutputVarIndex = 0;
    if (stepMapping.getInputVars() != null) {
      firstOutputVarIndex = stepMapping.getInputVars().size();
    }

    /*
     * Das Mapping erzeugt eine FilterCondition, die später der Input der Query-Function wird.
     * Die FilterCondition bekommt den Member formula mit der Expression gesetzt, welche die eigentliche FilterBedingung ist.
     * Eventuell vorhandene dynamische Teile (Variablen) werden aufgelöst.
     * Dies ist notwendig, da die Query-Function dies selbst nicht kann. Die Query-Function kennt nur %0%, was das eigene Storable repräsentiert.
     * Der Wert der eventuell verwendeten Variablen wird mit Hilfe der Funktion concat in die Expression gebracht.
     * Eventuell vorhandene Quotes bzw. Backslashes werden mit replaceall gequoted.
     * 
     * Die einzelnen FilterConditions stehen jeweils in Klammern und werden durch  &amp;&amp; voneinander getrennt.
     * 
     * Beispiele:
     *  - 1 Bedingung ohne dynamische Variablen
     *    -- <Condition>%0%.name==\"Test\"</Condition>
     *    -- <Mapping>%0%.formula=concat("(%0%.name==\"Test\")")</Mapping>
     *    
     *  - 2 Bedingung ohne dynamische Variablen
     *    -- <Condition>%0%.name==\"Test\"</Condition>
     *       <Condition>%0%.testData==\"Testing\"</Condition>
     *    -- <Mapping>%0%.formula=concat("(%0%.name==\"Test\") &amp;&amp; (%0%.testData==\"Testing\")")</Mapping>
     *  
     *  - 1 Bedingung mit dynamischer Variablen
     *    -- <Condition>%0%.name==%1%.name</Condition>
     *    -- <Mapping>%1%.formula=concat("(%0%.name==\"",replaceall(replaceall(%0%.name,"\\\\","\\\\\\\\"),"\"","\\\\\""),"\")")</Mapping>
     *  
     *  - 2 Bedingungen mit dynamischer Variablen
     *    -- <Condition>%0%.name==%1%.name</Condition>
     *       <Condition>%0%.testData==%2%.name</Condition>
     *    -- <Mapping>%2%.formula=concat("(%0%.name==\"",replaceall(replaceall(%0%.name,"\\\\","\\\\\\\\"),"\"","\\\\\""),"\") &amp;&amp; (%0%.testData==\"",replaceall(replaceall(%1%.name,"\\\\","\\\\\\\\"),"\"","\\\\\""),"\")")</Mapping>
     *  
     */
    StringBuilder sb = new StringBuilder();
    sb.append("%").append(firstOutputVarIndex).append("%.formula=concat(\"");
    for (int i = 0; i < filterConditions.size(); i++) {
      String condition = filterConditions.get(i);
      if (i > 0) {
        sb.append(" && ");
      }
      if (isDynamicExpression(condition)) {
        String expression = unescapeXMLExpressionForMapping(condition);
        // Bei gemischten Expressions (dynamische und statische Inhalte) muss zusätzlich noch nicht gequotete doppelte Anführungszeichen entfernt werden.
        expression = ExpressionUtils.unescapeQueryExpression(expression);
        sb.append("(").append(createDynamicExpression(expression)).append(")");
      } else {
        sb.append("(").append(unescapeXMLExpressionForMapping(condition)).append(")"); // hier muss wieder unescaped werden, da das Mapping beim Schreiben ins XML wieder escaped
      }
    }

    sb.append("\")");
    return sb.toString();
  }
  
  /**
   * Ermittelt ob die Expression einen dynamischen Teil enthält.
   * Dies ist immer dann der Fall, wenn eine Variable mit einem Index > 0 enthalten ist.
   * @param expression
   * @return
   */
  private static boolean isDynamicExpression(String expression) {
    return Pattern.matches(".*%[1-9]{1}[0-9]*%.*", expression);
  }

  /**
   * Erzeugt mit Hilfe des XFLLexer die Expression, welche den dynmischen Teil auflöst und quoted.
   * @param expression
   * @return
   */
  private static String createDynamicExpression(String expression) {
    StringBuilder sb = new StringBuilder();
    List<XFLLexem> lexems = XFLLexer.lex(expression, true);
    for (int i = 0; i < lexems.size(); i++) {
      XFLLexem lexem = lexems.get(i);
      if (lexem.getType() == TokenType.VARIABLE && isDynamicExpression(lexem.getToken())) {
        StringBuilder varAndAccessPart = new StringBuilder();
        varAndAccessPart.append(lexem.getToken());
        // find potential following access parts
        int openBraces = 0;
        int openListAccesses = 0;
        for (int j = i + 1; j < lexems.size(); j++) {
          XFLLexem lexem2 = lexems.get(j);
//          if (lexem2.getType() == TokenType.ACCESS_PART 
//              || lexem2.getType() == TokenType.BRACE 
//              || lexem2.getType() == TokenType.LIST_ACCESS
//            || lexem2.getType() == TokenType.LITERAL
//            || lexem2.getType() == TokenType.FUNCTION
//            || lexem2.getType() == TokenType.ARGUMENT_SEPERATOR) {
          if(lexem2.getType() == TokenType.BRACE) {
            if(lexem2.getToken().equals("(")) {
              openBraces++;
            } else if(lexem2.getToken().equals(")")) {
              openBraces--;
            }
            if(openBraces < 0) {
              break; //we closed braces outside of our scope.
            }
          }

          if (lexem2.getType() == TokenType.LIST_ACCESS) {
            if (lexem2.getToken().equals("[")) {
              openListAccesses++;
            } else if (lexem2.getToken().equals("]")) {
              openListAccesses--;
            }
            if (openListAccesses < 0) {
              break; //we closed listAccess outside of our scope
            }
          }

          if (lexem2.getType() == TokenType.VARIABLE && openBraces == 0 && openListAccesses == 0) {
            break; //invalid formula, however we have to generate a new dynamic expression for variable in lexem2
          }
          
          if(lexem2.getType() != TokenType.OPERATOR 
              && lexem2.getType() != TokenType.ASSIGNMENT) {
            varAndAccessPart.append(lexem2.getToken());
            i++;
          } else {
            break;
          }
        }
        sb.append(prepareDynamicPart(varAndAccessPart.toString()));
      } else if (lexem.getType() == TokenType.LITERAL) {
        sb.append(escapeLiteral(lexem.getToken()));
      } else {
        sb.append(lexem.getToken());
      }
    }
    return sb.toString();
  }

  /**
   * Erzeugt den Teil der Expression, der Backslashes und doppelte Anführungszeichen quoted.
   * Das Quoten erfolgt mit Hilfe von replaceall.
   * Die vielen Backslashes entstehen dadurch, dass hier bereits für die später erzeugte Java-Klasse gequoted werden muss.
   * Benötigt man in der Regex von replaceall einen Backslash zum Quoten, so braucht man im XML 4 Backslashes.
   * Zu beachten ist auch, dass der Backslash im 2. Parameter von replaceall auch gequoted werden muss. 
   * 
   * Beispiel anhand von %0%.name
   * 
   * 1. Backslash quoten
   *    replaceall(%0%.name,"\\\\","\\\\\\\\")
   *    
   * 2. Doppeltes Anführungszeichen quoten
   *    replaceall([ERGEBNIS AUS 1],"\"","\\\\\"")
   *    
   * Ergbenis:
   *    replaceall(replaceall(%0%.name,"\\\\","\\\\\\\\"),"\"","\\\\\"")
   * 
   * @param part
   * @return
   */
  private static String prepareDynamicPart(String part) {
    StringBuilder sb = new StringBuilder();
    sb.append("\\\"\",replaceall(replaceall(");
    /*
    * Passt die Indexe von Variablen in der Expression an.
    * Dies ist notwendig, da das Query  in der GUI als 1. Input-Variable eine Fakevariable bestehend aus dem Storable bekommt.
    * Dadurch verschieben sich alle Indexe. Dies wird hier wieder für das Mapping korrigiert
    * */
    sb.append(modifyVariableIndices(part, -1));
    sb.append(",\"\\\\\\\\\",\"\\\\\\\\\\\\\\\\\"),\"\\\"\",\"\\\\\\\\\\\"\"),\"\\\"");
    return sb.toString();
  }

  
  private static String unescapeXMLExpressionForMapping(String expression) {
    if(expression == null) {
      return null;
    }
    for (EscapableXMLEntity entity : EscapableXMLEntity.values()) {
      expression = expression.replace(entity.getFullEscapedRepresentation(), entity.getUnescapedRepresentation());
    }
    return expression;
  }
  
  public static String escapeExpressionForXML(String expression) {
    StringBuilder sb = new StringBuilder();
    
    List<XFLLexem> lexems = XFLLexer.lex(expression, true);
    
    for (XFLLexem lex : lexems) {
      if (lex.getType() == TokenType.LITERAL || lex.getType() == TokenType.UNKNOWN) {
        sb.append(escapeLiteral(lex.getToken()));
      } else {
        sb.append(lex.getToken());
      }
    }
    return sb.toString();
  }
  
  private static String escapeLiteral(String literal) {
    literal = literal.replaceAll("\\\\", "\\\\\\\\");       // \ -> \\
    literal = literal.replaceAll("\\\\\"", "\\\\\\\\\"");   // \" -> \\"
    if(literal.startsWith("\"") && literal.endsWith("\"")) {
      literal = "\\\"" + literal.substring(1, literal.length() - 1) + "\\\"";
    }
    return literal;
  }
  
  
  //changes the index of all variables in input
  private static String modifyVariableIndices(String input, int amount) {
    StringBuilder sb = new StringBuilder();
    List<XFLLexem> lexem = XFLLexer.lex(input);

    for (XFLLexem lex : lexem) {
      if (lex.getType() == TokenType.VARIABLE) {
        int currentIndex = Integer.parseInt(lex.getToken().substring(1, lex.getToken().length() - 1));
        sb.append("%" + (currentIndex + amount) + "%");
      } else {
        sb.append(lex.getToken());
      }
    }

    return sb.toString();
  }
  
  /**
   * Extrahiert aus der Mapping-Expression die einzelnen FilterConditions
   * @param input
   * @param inputVarCount
   * @return
   */
  public static List<String> extractQueryFormulas(String input, int inputVarCount) {
    List<String> result = new ArrayList<>(20);
    if(input == null || input.isEmpty()) {
      return result;
    }
    /*
     * Die GUI bekommt als erste Variable das Storable (Fakevariable). Dadurch müssen für die GUI alle Variablenindexe bei dynamischen FilterConditions um 1 erhöht werden.
     * Dies benötigt die GUI um die einzelnen Member des Storable ermitteln und darstellen zu können.
     */
    String prepared = modifyVariableIndices(input, 1); //increase by one
    
    inputVarCount++; //we increased indices by one
    
    /*
     * Beispiele:
     *  - 1 Bedingung ohne dynamische Variablen
     *    -- <Mapping>%0%.formula=concat("(%0%.name==\"Test\")")</Mapping>
     *    -- <Condition>%0%.name==\"Test\"</Condition>
     *    
     *  - 2 Bedingung ohne dynamische Variablen
     *    -- <Mapping>%0%.formula=concat("(%0%.name==\"Test\") &amp;&amp; (%0%.testData==\"Testing\")")</Mapping>
     *    -- <Condition>%0%.name==\"Test\"</Condition>
     *       <Condition>%0%.testData==\"Testing\"</Condition>
     *  
     *  - 1 Bedingung mit dynamischer Variablen
     *    -- <Mapping>%1%.formula=concat("(%0%.name==\"",replaceall(replaceall(%0%.name,"\\\\","\\\\\\\\"),"\"","\\\\\""),"\")")</Mapping>
     *    -- <Condition>%0%.name==%1%.name</Condition>
     *  
     *  - 1 Bedingung mit dynamischer Variablen
     *    -- <Mapping>%2%.formula=concat("(%0%.name==\"",replaceall(replaceall(%0%.name,"\\\\","\\\\\\\\"),"\"","\\\\\""),"\") && (%0%.testData==\"",replaceall(replaceall(%1%.name,"\\\\","\\\\\\\\"),"\"","\\\\\""),"\")")</Mapping>
     *    -- <Condition>%0%.name==%1%.name</Condition>
     *       <Condition>%0%.testData==%2%.name</Condition>
     *       
     * Hinweise:
     *  - Die einzelnen FilterConditions stehen im Mapping immer innerhalb von Klammern und werden durch && voneinander getrennt
     *  - Dynmische Variablen müssen vor dem Aufruf des Querys entfernt werden, da das Query keine Variablen auswerten kann.
     *  - Eventuell vorhandene Backslashes und einfache Anführungszeichen müssen entsprechend gequoted werden.
     *    Dies erfolgt durch den zweimaligen Aufruf von replaceall. Da das replaceall bereits für eine Java-Klasse gequoted ist, gibt es entsprechend viele Backslashes.
     *  - Alle dynamischen Variablen in den Query-Conditions müssen Input-Variablen des Mappings sein.
     *  - Das Mapping erzeugt ein FilterCondition-Object, welches später die erste Input-Variable der Query-Function ist. %0% innerhalb des concat steht für das Storable.
     * 
     */

    // nicht benötigte Teile aus der Expression entfernen
    
    /*
     * Erster Teil des Mappings entfernen. ( bis zum concat(" )
     * Hier ist die Besonderheit, dass die erste Variable des Mappings abhängig von der Anzahl der Input-Variablen ist.
     * Die erzeugte FilterCondition ist die erste Output-Variable des Mappings und bekommt somit die nächste fortlaufende Id nach den Input-Variablen.
     * Zum Beispiel, wenn das Mapping 2 Input-Variablen hat, so ist die Id der ersten Outputvariablen 2.
     * 
     */
    prepared = prepared.replaceFirst("%" + inputVarCount + "%.formula=concat\\(\"", "");
    
    // ") am Ende entfernen
    prepared = prepared.substring(0, prepared.length() - 2);
    
    // Expression in die einzelnen FilterConditions trennen
    String[] formulas = prepared.split("\\) && \\(");
    int i = 0;
    for (String formula : formulas) {
      formula = formula.trim();
      // Durch das Teilen in die einzelnen FilterConditions gehen Klammern verloren. --> Einheitlichen Zustand wieder herstellen um sie sicher zu entfernen.
      if(formulas.length > 1) {
        if(i == 0) {
          formula = formula + ")";
        } else if (i == formulas.length - 1) {
          formula = "(" + formula;
        } else {
          formula = "(" + formula + ")";
        }
      }
      // Klammern entfernen, wenn es am Anfang und Ende eine gibt. Wenn es nur am Ende eine geschlossene Klammer gibt, dann besser nicht.
      if(formula.startsWith("(") && formula.endsWith(")")) {
        formula = formula.substring(1, formula.length() - 1);
      }
      if(isFormulaDynamic(formula)) {
        /*
         * Bei dynamischen FilterConditions muss der Teil entfernt werden, der später zur Laufzeit die dynamischen Variablen auflöst.
         * \"",replaceall(replaceall(%0%.name,"\\\\","\\\\\\\\"),"\"","\\\\\"")," --> %0%.name
         */
        
        // \"", replaceall(replaceall( entfernen
        formula = formula.replaceAll("\\\\\"\",\\s?replaceall\\(replaceall\\(", "");
        
        // ,"\\\\","\\\\\\\\"),"\"","\\\\\""),"\") entfernen
        formula = formula.replaceAll(",\\s?\"\\\\\\\\\\\\\\\\\",\\s?\"\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"\\),\\s?\"\\\\\"\",\\s?\"\\\\\\\\\\\\\\\\\\\\\"\"\\),\\s?\"\\\\\"", "");

      }
      result.add(formula);
      i++;
    }
    return result;
  }
  
  private static boolean isFormulaDynamic(String formula) {
    if(formula == null) {
      return false;
    }
    return formula.contains("replaceall");
  }
  
  
  public static boolean isQuery(GBSubObject functionGBSubobject) {
    if(functionGBSubobject == null) {
      return false;
    }
    return QueryUtils.findQueryHelperMapping(functionGBSubobject) != null;
  }


}
