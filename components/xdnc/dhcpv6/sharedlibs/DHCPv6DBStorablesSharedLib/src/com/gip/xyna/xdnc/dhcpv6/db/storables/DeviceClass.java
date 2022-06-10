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
package com.gip.xyna.xdnc.dhcpv6.db.storables;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import xdnc.dhcp.Node;


import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdnc.dhcpv6.db.storables.utils.ParenthesisMatcher;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = DeviceClass.COL_CLASSID, tableName = DeviceClass.TABLENAME)
public class DeviceClass extends Storable<DeviceClass> implements Comparable<DeviceClass>{
  
  private static final Logger logger = CentralFactoryLogging.getLogger(DeviceClass.class);
  
  public static final String TABLENAME = "class";
  public static final String COL_CLASSID = "classID";
  public static final String COL_NAME = "name";
  public static final String COL_ATTRIBUTES = "attributes";
  public static final String COL_FIXEDATTRIBUTES = "fixedAttributes";
  public static final String COL_CONDITIONAL = "conditional";
  public static final String COL_PRIORITY = "priority";
  
  @Column(name = COL_CLASSID)
  int classId;
  
  @Column(name = COL_NAME)
  String name;
  
  @Column(name = COL_ATTRIBUTES)
  String attributes;
  
  @Column(name = COL_FIXEDATTRIBUTES)
  String fixedAttributes;
  
  @Column(name = COL_CONDITIONAL)
  String conditional;
  
  @Column(name = COL_PRIORITY)
  int priority;
  
  public int getClassID(){
    return classId;
  }
  
  public String getName(){
    return name;
  }
  
  public String getAttributes(){
    return attributes;
  }
  
  public String getFixedAttributes(){
    return fixedAttributes;
  }
  
  public String getConditional(){
    return conditional;
  }
  
  public int getPriority(){
    return priority;
  }
  
  @Override
  public Object getPrimaryKey() {
    return classId;
  }
  
  private static class DeviceClassReader implements ResultSetReader<DeviceClass> {

    public DeviceClass read(ResultSet rs) throws SQLException {
      DeviceClass dc = new DeviceClass();
      DeviceClass.fillByResultSet(dc, rs);
      return dc;
    }

  }
  
  private static final DeviceClassReader reader = new DeviceClassReader();
  
  @Override
  public ResultSetReader<? extends DeviceClass> getReader() {
    return reader;
  }
  
  @Override
  public <U extends DeviceClass> void setAllFieldsFromData(U data) {
    classId = data.classId;
    name = data.name;
    attributes = data.attributes;
    fixedAttributes = data.fixedAttributes;
    conditional = data.conditional;
    priority = data.priority;
  }
  
  public int compareTo(DeviceClass dc) {
    if (priority > dc.getPriority()) return 1;
    else if (priority < dc.getPriority()) return -1;
    else return 0;
  }
  
  public static void fillByResultSet(DeviceClass dc, ResultSet rs) throws SQLException {
    dc.classId = rs.getInt(COL_CLASSID);
    dc.name = rs.getString(COL_NAME);
    dc.attributes = rs.getString(COL_ATTRIBUTES);
    dc.fixedAttributes = rs.getString(COL_FIXEDATTRIBUTES);
    dc.conditional = rs.getString(COL_CONDITIONAL);
    try
    {
      dc.priority = rs.getInt(COL_PRIORITY);
    }
    catch(Exception e){ // vorlauefiges Fangen von nicht vorhandener Spalte fuer DHCPv4
      
    }
  }
  
  
  public static final char OPENINGPARANTHESIS = '(';
//  /**
//   * Wertet einen conditional-String aus 
//   */
//  public static boolean parseAndEvaluateConditional(String conditional, List<? extends Node> inputoptions){
////    ParenthesisMatcher paranthesisMatcher = new ParenthesisMatcher(conditional);
////    Map<String, String> subConditionalHash = new HashMap<String, String>();
////    int counter = 0;
////    String resultingConditional = conditional;
////    
////    for (int i = 0; i < conditional.length(); i++){
////      System.out.println("i = " +i);
////      char c = conditional.charAt(i);
////      
////      if (c == OPENINGPARANTHESIS){
////        int close = paranthesisMatcher.findMatchingClose(i);
////        String subConditional = conditional.substring(i+1, close);
////        subConditionalHash.put(String.valueOf(counter), subConditional);
////        resultingConditional = resultingConditional.replace(subConditional, String.valueOf(counter));
////        counter++;
////        System.out.println("conditional = " +conditional+ ", subConditional = " +subConditional);
////        System.out.println("resultingConditional = " +resultingConditional);
////        parseConditional(subConditional);
////        System.out.println("returning recursive call for subConditional = " +subConditional);
////        i = close;
////        System.out.println("geshiftetes i = " +i);
////      } else {
////        System.out.println("keine Klammer");
////      }
////      
////      
////    }
//    
//    Map<String, String> subConditionalHash = new HashMap<String, String>();
//    int counter = 0;
//    String resultingConditional = parseConditional(conditional, subConditionalHash, counter);
//    System.out.println("Evaluation of " +resultingConditional);
//    boolean fulfilled = evaluateConditionalString(resultingConditional, subConditionalHash, inputoptions);
//    return fulfilled;
//  }
  
  public static String parseConditional(String conditional, Map<String, String> subConditionalHash, int counter){
    
    
    ParenthesisMatcher paranthesisMatcher = new ParenthesisMatcher(conditional);

    String resultingConditional = conditional;
    
    for (int i = 0; i < conditional.length(); i++){

      char c = conditional.charAt(i);
      
      if (c == OPENINGPARANTHESIS){
        int close = paranthesisMatcher.findMatchingClose(i);
        String subConditional = conditional.substring(i+1, close);
        subConditionalHash.put(String.valueOf(counter), subConditional);
        int oldcounter = counter;
        //System.out.println("resultingConditional vor Austausch= " +resultingConditional);
        //resultingConditional = resultingConditional.replace(subConditional, String.valueOf(counter));
        Pattern SUB_PATTERN = Pattern.compile("\\s*"+Pattern.quote(subConditional)+"\\s*");
        resultingConditional = SUB_PATTERN.matcher(resultingConditional).replaceFirst(String.valueOf(counter));
        //System.out.println("resultingConditional nach Austausch= " +resultingConditional);
        counter++;
        String resultingSubConditional = parseConditional(subConditional, subConditionalHash, counter);
        
        subConditionalHash.put(String.valueOf(oldcounter), resultingSubConditional);             
        i = close;
      } else {
        
      }
    }
    
    //return new Conditional(conditional, resultingConditional, subConditionalHash);
    return resultingConditional;
  }
  
//  private static final Pattern NOT_OPTION_PATTERN = Pattern.compile("^\\s*NOT\\s+<\\d+>.*$");
//  private static final Pattern NOT_SUBCONDITION_PATTERN = Pattern.compile("^\\s*NOT\\s+\\(\\d+\\).*$");
//  private static final Pattern NOT_PATTERN = Pattern.compile("\\s*NOT\\s*");
//  private static final Pattern OR_PATTERN = Pattern.compile("\\s*OR\\s*");
//  private static final Pattern AND_PATTERN = Pattern.compile("\\s*AND\\s*");
//  private static final Pattern CONDITION_PATTERN = Pattern.compile("\\s*<(\\s*\\d+\\s*)>\\s*");
//  private static final Pattern SUBCONDITION_PATTERN = Pattern.compile("\\s*\\(\\s*(\\d+)\\s*\\)\\s*");
//  
//  public static boolean evaluateConditionalString(String resultingConditional, Map<String, String> subConditionalHash, List<? extends Node> inputoptions) {
//    
//    
//    String trimmed = resultingConditional.trim();//Abschneiden führender und abschließender Leerzeichen
//    
//    if (logger.isDebugEnabled()){
//      logger.debug("## Evaluating conditional string " +resultingConditional);
//    }
//    
//    String[] orParts = OR_PATTERN.split(trimmed);
//    
//    boolean evaluation = false;
//    if (orParts.length > 1){
//      evaluation = evaluateORs(orParts, subConditionalHash, inputoptions);
//    } else {
//      // wenn ANDs vorhanden sind
//      String[] andParts = AND_PATTERN.split(trimmed);
//      if (andParts.length > 1){
//        evaluation = evaluateANDs(andParts, subConditionalHash, inputoptions);
//      } else {
//        //wenn NOTs vorhanden sind
//        String[] notParts = NOT_PATTERN.split(trimmed);
//        if (notParts.length > 1){
//          evaluation = evaluateNOTs(notParts, subConditionalHash, inputoptions);
//        } else {
//          // einzelne Einträge wie (...) oder <..>
//          Matcher matcherSubCond = SUBCONDITION_PATTERN.matcher(trimmed);
//          Matcher matcherCond = CONDITION_PATTERN.matcher(trimmed);
//          if (matcherSubCond.matches()){// wenn Untercondition drinsteht, d.h. (...)
//            String subConditional = subConditionalHash.get(matcherSubCond.group(1));
//            evaluation = evaluateConditionalString(subConditional, subConditionalHash, inputoptions);
//          } else if (matcherCond.matches()){
//            //evaluation = Condition.evaluate(matcherCond.group(1), inputoptions);
//            evaluation = DHCPv6ServicesImpl.evaluateCondition(matcherCond.group(1), inputoptions);
//          }
//        }
//      }   
//    }
//    //System.out.println("evaluation of " +resultingConditional+ " = " +evaluation);
//    if (logger.isDebugEnabled()){
//      logger.debug("## Evaluation of " +resultingConditional+ " = " +evaluation);
//    }
//    return evaluation;
//  }
  
//  private static boolean evaluateNOTs(String[] notParts,
//      Map<String, String> subConditionalHash, List<? extends Node> inputoptions) {
//    
//    if (notParts.length > 2){
//      throw new RuntimeException("Missing AND/OR statement between NOTs in Conditional");
//    }
//    boolean notEvaluation = false;
//    String notString = notParts[1];
//    notEvaluation = evaluateConditionalString(notString, subConditionalHash, inputoptions);
//
//    return (!notEvaluation);
//  }
//
//  private static boolean evaluateANDs(String[] andParts,
//      Map<String, String> subConditionalHash, List<? extends Node> inputoptions) {
//    boolean andEvaluation = true;
//    for (int and = 0; and < andParts.length; and++){
//      String andString = andParts[and];
//      if (!andString.equals("")){
//        andEvaluation = evaluateConditionalString(andString, subConditionalHash, inputoptions);
//      }
//      
//      if (andEvaluation == false) break;
//    }
//    return andEvaluation;
//  }
//
//  private static boolean evaluateORs(String[] orParts, Map<String, String> subConditionalHash, List<? extends Node> inputoptions){
//    boolean orEvaluation = false;
//    for (int or = 0; or < orParts.length; or++){
//      
//      String orString = orParts[or];
//      if (!orString.equals("")){
//        orEvaluation = evaluateConditionalString(orString, subConditionalHash, inputoptions);
//      }
//      
//      if (orEvaluation == true){
//        break;
//      }
////      //wenn Condition drinsteht, d.h. <...>
////      Matcher matcherCond = CONDITION_PATTERN.matcher(orString);
////      Matcher matcherSubCond = SUBCONDITION_PATTERN.matcher(orString);
////      if (matcherCond.matches()){
////        orEvaluation = Condition.evaluate(matcherCond.group(1));
////      }
////      else if (matcherSubCond.matches()){// wenn Untercondition drinsteht, d.h. (...)
////        String subConditional = subConditionalHash.get(matcherSubCond.group(1));
////        
////        orEvaluation = evaluateSubConditional(subConditional, subConditionalHash);
////        
////      } else { // wenn ein String mit AND oder NOT drinsteht, z.B. (2) AND NOT <3>
////        orEvaluation = evaluateConditionalString(orString, subConditionalHash);
////      }
//    }
//    return orEvaluation;
//  }

  public static void main(String[] args) {
    ArrayList<Node> inputoptions = new ArrayList<Node>();
    //boolean result = parseAndEvaluateConditional(" <1> AND ( NOT (<2>) AND ( <1> OR <2> ) )", inputoptions);
  }
  
  

}
