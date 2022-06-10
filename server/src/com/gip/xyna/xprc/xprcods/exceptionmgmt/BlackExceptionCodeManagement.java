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
package com.gip.xyna.xprc.xprcods.exceptionmgmt;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.exceptions.exceptioncode.CodeGroupUnknownException;
import com.gip.xyna.utils.exceptions.exceptioncode.DuplicateCodeGroupException;
import com.gip.xyna.utils.exceptions.exceptioncode.InvalidPatternException;
import com.gip.xyna.utils.exceptions.exceptioncode.NoCodeAvailableException;
import com.gip.xyna.utils.exceptions.exceptioncode.OverlappingCodePatternException;
import com.gip.xyna.utils.exceptions.utils.XMLUtils;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;


//FIXME achtung wegen deadlockgefahr mit connections innerhalb von synchronized blocks
public class BlackExceptionCodeManagement extends com.gip.xyna.utils.exceptions.exceptioncode.ExceptionCodeManagement
                implements
                  ExceptionCodeManagement {

  //private final static Logger logger = CentralFactoryLogging.getLogger(BlackExceptionCodeManagement.class);
  private final static String XYNA_PATTERN = "XYNA-[[]]";
  private final static int XYNA_PADDING = 5;
  private final static int DEFAULT_RANGE = 100;
  private final ODS ods;

  private static Comparator<Pattern> startIndexComparator = new Comparator<Pattern>() {

    public int compare(Pattern o1, Pattern o2) {
      return o1.getStartIndex() - o2.getStartIndex();
    }

  };
  
  
  public static class CodeGroupPattern implements StringSerializable<CodeGroupPattern> {
    private String pattern;
    
    public CodeGroupPattern(String string) {
      try {
        new Pattern(string, 0, 10, 1);
      } catch (InvalidPatternException e) {
        throw new IllegalArgumentException("Invalid CodeGroupPattern", e);
      }
      this.pattern = string;
    }

    public CodeGroupPattern deserializeFromString(String string) {
      return new CodeGroupPattern(string);
    }

    public String serializeToString() {
      return pattern;
    }
    
    public String getPattern() {
      return pattern;
    }
    
    @Override
    public String toString() {
      return "CodeGroupPattern("+pattern+")";
    }
    
  }
  

  

  protected BlackExceptionCodeManagement(ODS ods) throws XynaException {
    super();
    this.ods = ods;
    ods.registerStorable(CodeGroupStorable.class);
    ods.registerStorable(CodePatternStorable.class);
    String user = getClass().getSimpleName();
    XynaProperty.RELOAD_FROM_STORAGE_EACH_ACTION.registerDependency(user);
    XynaProperty.AUTOMATIC_CODEGROUP_GENERATION_AND_EXTENSION.registerDependency(user);
    XynaProperty.AUTOMATIC_CODEGROUP_GENERATION_DEFAULTPATTERN.registerDependency(user);
    XynaProperty.AUTOMATIC_CODEGROUP_GENERATION_DEFAULTPADDING.registerDependency(user);
  }


  private void initExistingExceptionCodes() {
    CodeGroup[] codeGroups = getCodeGroups();
    if (codeGroups.length == 0) {
      //nicht initialisiert
      try {
        String utils = "xynautils";
        createCodeGroup(utils);
        addExceptionCodePattern(utils, XYNA_PATTERN, 0, 1000, XYNA_PADDING);

        //intern verwendete fehlermeldungen und reservierte bereiche
        String black_xprc = "black_xprc";
        createCodeGroup(black_xprc);
        addExceptionCodePattern(black_xprc, XYNA_PATTERN, 1000, 1600 - 1000, XYNA_PADDING);
               
        String reserved = "reserved";
        createCodeGroup(reserved);
        addExceptionCodePattern(reserved, XYNA_PATTERN, 1600, 2200 - 1600, XYNA_PADDING);     

        String black_xnwh = "black_xnwh";
        createCodeGroup(black_xnwh);
        addExceptionCodePattern(black_xnwh, XYNA_PATTERN, 2200, 2500 - 2200, XYNA_PADDING);

        addExceptionCodePattern(reserved, XYNA_PATTERN, 2500, 3000 - 2500, XYNA_PADDING);     

        String black_xdev = "black_xdev";
        createCodeGroup(black_xdev);
        addExceptionCodePattern(black_xdev, XYNA_PATTERN, 3000, 3500 - 3000, XYNA_PADDING);

        addExceptionCodePattern(reserved, XYNA_PATTERN, 3500, 4000 - 3500, XYNA_PADDING);     

        String black_xfmg = "black_xfmg";
        createCodeGroup(black_xfmg);
        addExceptionCodePattern(black_xfmg, XYNA_PATTERN, 4000, 4300 - 4000, XYNA_PADDING);

        addExceptionCodePattern(reserved, XYNA_PATTERN, 4300, 8000 - 4300, XYNA_PADDING);     

        String black_global = "black_global";
        createCodeGroup(black_global);
        addExceptionCodePattern(black_global, XYNA_PATTERN, 8000, 8200 - 8000, XYNA_PADDING);     
        
        addExceptionCodePattern(reserved, XYNA_PATTERN, 8200, 9200 - 8200, XYNA_PADDING);     

        String black_xact = "black_xact";
        createCodeGroup(black_xact);
        addExceptionCodePattern(black_xact, XYNA_PATTERN, 9200, 9600 - 9200, XYNA_PADDING);

        addExceptionCodePattern(reserved, XYNA_PATTERN, 9600, 10000 - 9600, XYNA_PADDING);   //trigger  

        String black_xmcp = "black_xmcp";
        createCodeGroup(black_xmcp);
        addExceptionCodePattern(black_xmcp, XYNA_PATTERN, 10000, 10300 - 10000, XYNA_PADDING);

        addExceptionCodePattern(reserved, XYNA_PATTERN, 10300, 15000 - 10300, XYNA_PADDING);     

        addExceptionCodePattern(reserved, XYNA_PATTERN, 0, 1000000, XYNA_PADDING+1);

      } catch (XynaException e) {
        //programmierfehler?!
        throw new RuntimeException(e);
      }
    }
  }


  private synchronized void saveExceptionManagement(ODSConnection con) throws PersistenceLayerException {
    List<CodeGroupStorable> codeGroups = new ArrayList<CodeGroupStorable>();
    List<CodePatternStorable> codePatterns = new ArrayList<CodePatternStorable>();
    for (CodeGroup codeGroup : getCodeGroups()) {
      CodeGroupStorable cgs = new CodeGroupStorable(codeGroup);
      codeGroups.add(cgs);
      int cnt = 0;
      for (Pattern pattern : codeGroup.getPatterns()) {
        CodePatternStorable cps = new CodePatternStorable(codeGroup.getCodeGroupName() + "-" + pattern.getPrefix()
                        + "-" + pattern.getSuffix() + "-" + pattern.getStartIndex(), cnt, codeGroup.getCodeGroupName(),
                                                          pattern);
        codePatterns.add(cps);
        cnt++;
      }
    }
    con.deleteAll(CodeGroupStorable.class);
    con.deleteAll(CodePatternStorable.class);
    con.persistCollection(codeGroups);
    con.persistCollection(codePatterns);
  }
  
  private void loadExceptionManagement(ODSConnection con) throws PersistenceLayerException {
    Collection<CodeGroupStorable> codeGroups = con.loadCollection(CodeGroupStorable.class);
    Collection<CodePatternStorable> codePatterns = con.loadCollection(CodePatternStorable.class);
    Pattern[] nulls = new Pattern[codePatterns.size()];
    Map<String, CodeGroup> codeGroupMap = new HashMap<String, CodeGroup>();
    for (CodeGroupStorable codeGroup : codeGroups) {
      CodeGroup cg = codeGroup.getAsCodeGroup();
      List<Pattern> patterns = new ArrayList<Pattern>(Arrays.asList(nulls));
      for (CodePatternStorable codePattern : codePatterns) {
        if (codePattern.getCodeGroupName().equals(codeGroup.getCodeGroupName())) {
          patterns.set(codePattern.getPatternIndex(), codePattern.getAsPattern());
        }
      }
      patterns.removeAll(Collections.singleton(null));
      cg.setPatterns(patterns);
      codeGroupMap.put(codeGroup.getCodeGroupName(), cg);
    }
    super.init(codeGroupMap);
    initExistingExceptionCodes();
  }

  public synchronized void init() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      loadExceptionManagement(con);
    } finally {
      con.closeConnection();
    }
  }


  public synchronized String createNewExceptionCode(String codeGroupName) throws CodeGroupUnknownException,
                  NoCodeAvailableException, PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      if (XynaProperty.RELOAD_FROM_STORAGE_EACH_ACTION.get()) {
        loadExceptionManagement(con);
      }
      /*
       * XynaProperty.AUTOMATIC_CODEGROUP_GENERATION_AND_EXTENSION
       * default = true.
       * 
       * auswirkungen für createNewExceptionCode:
       * falls true, wird:
       * 1) bei einer nicht gefundenen codegruppe eine neue erstellt
       * 2) falls keine codes für das codepattern verfügbar: ein neues codepattern mit länge 100 wird erstellt.
       *    das codepattern sieht genauso aus wie ein schon vorhandenes zur codegruppe. falls die codegruppe noch kein
       *    pattern enthält, wird ein pattern der form XYNA-[[]] angelegt.
       * falls false, wird in den oben genannten fällen ein fehler geworfen
       */
      String ret = null;
      try {
        ret = createExceptionCode(codeGroupName);
      } catch (CodeGroupUnknownException e) {
        if (XynaProperty.AUTOMATIC_CODEGROUP_GENERATION_AND_EXTENSION.get()) {
          initNewCodeGroup(codeGroupName);
          ret = createExceptionCode(codeGroupName);
        } else {
          throw e;
        }
      } catch (NoCodeAvailableException e) {
        if (XynaProperty.AUTOMATIC_CODEGROUP_GENERATION_AND_EXTENSION.get()) {
          CodeRange range = null;
          try {
            CodeGroup[] codeGroups = getCodeGroups();
            for (CodeGroup codeGroup : codeGroups) {
              if (codeGroup.getCodeGroupName().equals(codeGroupName) && codeGroup.getPatterns().size() > 0) {
                range = findFreeCodeRange(codeGroupName, codeGroup.getPattern(0).getPrefix() + "[[]]"
                                + codeGroup.getPattern(0).getSuffix(), codeGroup.getPattern(0).getPadding(),
                                          DEFAULT_RANGE);
                break;
              }
            }          
          } catch (InvalidPatternException e1) {
            //programmierfehler
            throw new RuntimeException(e1);
          }
          addExceptionCodePattern( codeGroupName, range );
          
          ret = createExceptionCode(codeGroupName);
        } else {
          throw e;
        }        
      }
      saveExceptionManagement(con);
      con.commit();
      return ret;
    } finally {
      con.closeConnection();
    }
  }


  private void addExceptionCodePattern(String codeGroupName, CodeRange range ) throws NoCodeAvailableException, CodeGroupUnknownException {
    try {
      if( range == null ) {
        range = findFreeCodeRange(codeGroupName, 
                                  XynaProperty.AUTOMATIC_CODEGROUP_GENERATION_DEFAULTPATTERN.get().getPattern(),
                                  XynaProperty.AUTOMATIC_CODEGROUP_GENERATION_DEFAULTPADDING.get(),
                                  DEFAULT_RANGE);
      }
      
      addExceptionCodePattern(codeGroupName, 
                              XynaProperty.AUTOMATIC_CODEGROUP_GENERATION_DEFAULTPATTERN.get().getPattern(),
                              range.startIndex,
                              range.endIndex - range.startIndex + 1,
                              XynaProperty.AUTOMATIC_CODEGROUP_GENERATION_DEFAULTPADDING.get());
    } catch (InvalidPatternException e1) {
      //programmierfehler
      throw new RuntimeException(e1);
    } catch (OverlappingCodePatternException e1) {
      //programmierfehler
      throw new RuntimeException(e1);
    }
  }

  public void checkExceptionCode(Document doc) throws CodeGroupUnknownException, NoCodeAvailableException,
                  PersistenceLayerException {
    Element root = doc.getDocumentElement();
    List<Element> exceptions = XMLUtils.getChildElementsByName(root, GenerationBase.EL.EXCEPTIONTYPE);    
    for (Element ex : exceptions) {
      boolean isAbstract = com.gip.xyna.xprc.xfractwfe.generation.XMLUtils.isTrue(ex, GenerationBase.ATT.ABSTRACT);
      if (isAbstract) {
        continue;
      }
      String code = ex.getAttribute(GenerationBase.ATT.EXCEPTION_CODE);
      if (code == null || code.length() == 0) {
        String packageName = ex.getAttribute(GenerationBase.ATT.TYPEPATH);
        if (packageName == null || packageName.length() == 0) {
          packageName = GenerationBase.DEFAULT_PACKAGE;
        }
        int pIndex = packageName.indexOf(".");
        if (pIndex > -1) {
          packageName = packageName.substring(0, pIndex);
        }
        //erster teil des packages
        String codeGroupName = packageName;
        code = createNewExceptionCode(codeGroupName);
        ex.setAttribute(GenerationBase.ATT.EXCEPTION_CODE, code);
      }
    }
  }


  private void initNewCodeGroup(String codeGroupName) throws NoCodeAvailableException {
    try {
      createCodeGroup(codeGroupName);
    } catch (DuplicateCodeGroupException e) {
      throw new RuntimeException(e); //sollte nicht passieren, weil vorher gefragt wird, ob die codegroup bereits vorhanden ist
    }
    try {
      addExceptionCodePattern( codeGroupName, null );
    } catch (CodeGroupUnknownException e) {
      //programmierfehler
      throw new RuntimeException(e);
    }
  }


  private CodeRange findFreeCodeRange(String codeGroupName, String pattern, int padding, int minSize)
                  throws NoCodeAvailableException, InvalidPatternException {
    Pattern tempPattern = new Pattern(pattern, 0, minSize, padding);
    CodeGroup[] codeGroups = getCodeGroups();
    List<Pattern> allPatterns = new ArrayList<Pattern>();
    for (int i = 0; i < codeGroups.length; i++) {
      for (Pattern p : codeGroups[i].getPatterns()) {
        if (p.getPadding() == padding && p.getPrefix().equals(tempPattern.getPrefix())
                        && p.getSuffix().equals(tempPattern.getSuffix())) {
          allPatterns.add(p);
        }
      }
    }
    if (allPatterns.size() == 0) {
      return new CodeRange(0, minSize - 1);
    }
    Collections.sort(allPatterns, startIndexComparator);
    if (allPatterns.get(0).getStartIndex() >= minSize) {
      return new CodeRange(0, minSize - 1);
    }
    for (int i = 0; i < allPatterns.size() - 1; i++) {
      Pattern p1 = allPatterns.get(i);
      Pattern p2 = allPatterns.get(i + 1);
      if (p2.getStartIndex() - p1.getEndIndex() >= minSize) {
        return new CodeRange(p1.getEndIndex() + 1, p1.getEndIndex() + minSize);
      }
    }
    int startIndex = allPatterns.get(allPatterns.size() - 1).getEndIndex() + 1;
    int endIndex = startIndex + minSize - 1;
    if ((Integer.toString(endIndex)).length() > padding) {
      throw new NoCodeAvailableException(codeGroupName);
    }
    return new CodeRange(startIndex, endIndex);
  }


  private static class CodeRange {

    private int startIndex;
    private int endIndex;


    public CodeRange(int startIndex, int endIndex) {
      this.startIndex = startIndex;
      this.endIndex = endIndex;
    }
  }


  public void addCodeGroup(String codeGroupName) throws DuplicateCodeGroupException, PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      if (XynaProperty.RELOAD_FROM_STORAGE_EACH_ACTION.get()) {
        loadExceptionManagement(con);
      }
      createCodeGroup(codeGroupName);
      saveExceptionManagement(con);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  public void addCodePattern(String codeGroupName, String pattern, int startIndex, int endIndex, int padding) throws CodeGroupUnknownException, InvalidPatternException, OverlappingCodePatternException, PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      if (XynaProperty.RELOAD_FROM_STORAGE_EACH_ACTION.get()) {
        loadExceptionManagement(con);
      }
      addExceptionCodePattern(codeGroupName, pattern, startIndex, endIndex-startIndex+1, padding);
      saveExceptionManagement(con);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  public CodeGroup[] listCodeGroups() throws PersistenceLayerException {
    if (XynaProperty.RELOAD_FROM_STORAGE_EACH_ACTION.get()) {
      ODSConnection con = ods.openConnection();
      try {
        loadExceptionManagement(con);
      } finally {
        con.closeConnection();
      }
    }
    return getCodeGroups();
  }


  public void removeCodePattern2(String codeGroupName, int patternIndex) throws CodeGroupUnknownException, PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      if (XynaProperty.RELOAD_FROM_STORAGE_EACH_ACTION.get()) {
        loadExceptionManagement(con);
      }
      removeCodePattern(codeGroupName, patternIndex);
      saveExceptionManagement(con);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }



  public void removeCodeGroup2(String codeGroupName) throws CodeGroupUnknownException, PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      if (XynaProperty.RELOAD_FROM_STORAGE_EACH_ACTION.get()) {
        loadExceptionManagement(con);
      }
      removeCodeGroup(codeGroupName);
      saveExceptionManagement(con);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }
  
  public void exportToXml() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      Collection<CodeGroupStorable> codeGroups = con.loadCollection(CodeGroupStorable.class);
      Collection<CodePatternStorable> codePatterns = con.loadCollection(CodePatternStorable.class);
      ODSConnection conTemp = ods.openConnection(ODSConnectionType.INTERNALLY_USED);
      try {
        //nicht alte exportierte daten übernehmen
        conTemp.deleteAll(CodeGroupStorable.class);
        conTemp.deleteAll(CodePatternStorable.class);
        
        conTemp.persistCollection(codeGroups);
        conTemp.persistCollection(codePatterns);
        conTemp.commit();
      } finally {
        conTemp.closeConnection();
      }
    } finally {
      con.closeConnection();
    }
  }
  
  /**
   * upsert
   */
  public void importFromXml() throws PersistenceLayerException {
    ODSConnection conTemp = ods.openConnection(ODSConnectionType.INTERNALLY_USED);
    try {
      Collection<CodeGroupStorable> codeGroups = conTemp.loadCollection(CodeGroupStorable.class);      
      Collection<CodePatternStorable> codePatterns = conTemp.loadCollection(CodePatternStorable.class);
      ODSConnection con = ods.openConnection();
      try {
        con.persistCollection(codeGroups);
        con.persistCollection(codePatterns);
        con.commit();
      } finally {
        con.closeConnection();
      }
    } finally {
      conTemp.closeConnection();
    }
  }

}
