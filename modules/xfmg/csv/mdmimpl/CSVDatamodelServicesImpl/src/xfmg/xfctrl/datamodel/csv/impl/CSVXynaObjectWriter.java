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
package xfmg.xfctrl.datamodel.csv.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;

import xfmg.xfctrl.datamodel.csv.impl.fields.XmomField;
import xfmg.xfctrl.datamodel.csv.impl.fields.XmomFieldParser;
import xfmg.xfctrl.datamodel.csv.impl.fields.XmomListField;
import xfmg.xfctrl.datamodel.csv.parameter.CSVColumnModification;
import xfmg.xfctrl.datamodel.csv.parameter.CSVParameter;
import xfmg.xfctrl.datamodel.csv.parameter.CSVWriteOptions;
import xfmg.xfctrl.datamodel.csv.parameter.StringTransformation;


/*
 * TODO: support für abgeleitete typen: dann muss man spalten für alle abgeleiteten typen erzeugen
 *       dafür z.b. erstmal alle objekte durchlaufen und schauen, welche subtypen verwendet werden. spalten für theoretisch vorhandene subtypen braucht man meist nicht.
 *       (aber einen usecase gibt es dafür auch: nämlich das manuelle editieren der objekte in excel - ggfs will man subtypen verwenden, die es vorher nicht gab)
 */
public class CSVXynaObjectWriter {

  /*
   * schreibt eine "box" (mehrere zeilen, die zum gleichen listenobjekt bzw zum root gehören)
   * beispiel:
   *    meta                        member1       list1.m1       list1.m2        list1.sublist.m1        list1.sublist.m2       list2.m1
   * 1 root=X                        123            
   * 2 list1[0]=X                                   123            123
   * 3 list1[0].sublist[0]=X                                                        123                     124
   * 4 list1[0].sublist[1]=X                                                        124                     124
   * 5 list1[1]=X                                   123            124
   * 6 list1[1].sublist[0]=X                                                        123                     124
   * 7 list1[1].sublist[1]=X                                                        123                     125
   * 8 list1[1].sublist[2]=X                                                        124                     124
   * 9 list1[2]=X                                   123            124
   *10 list2[0]=X                                                                                                                123
   *
   * es gibt in diesem beispiel folgende lineboxwriter instanzen:
   * 1) root           (pathElements=null)
   * 2) list1          (pathElements={"list1"})
   * 3) list1.sublist  (pathElements={"list1", "sublist"})
   * 4) list2          (pathElements={"list2"})
   * auf diesen wird jeweils in einer schleife über die entsprechenden listen writeLineContent() aufgerufen.
   * 
   */
  static class CSVLineBoxWriter {

    private final List<XmomField> xmomFields; //spalten der zeilen, die zu dieser box gehören

    private int startColumnIdx; //index der ersten spalte von der box (ohne metacolumn mitzuzählen)
    private final SortedMap<XmomListField, CSVLineBoxWriter> subBoxes;
    private final String[] pathElements; //null falls root, ansonsten membervar-names, die zu dem listenobjekt führen (ohne indizes)


    public CSVLineBoxWriter(List<XmomField> xmomFields, SortedMap<XmomListField, CSVLineBoxWriter> subBoxes, XmomListField parent) {
      this.xmomFields = xmomFields;
      this.subBoxes = subBoxes;
      if (parent == null) {
        pathElements = null;
      } else {
        pathElements = parent.getPathElements();
      }
    }


    private void writeMetaColumn(CSVWriter csvWriter, List<Integer> listIndexStack, Object obj) {
      StringBuilder metaInfo = new StringBuilder();
      if (pathElements == null) {
        metaInfo.append("root");
      } else {
        Iterator<Integer> listIdxIter = listIndexStack.iterator();
        for (int i = 0; i < pathElements.length; i++) {
          metaInfo.append(pathElements[i]);
          if (pathElements[i].endsWith("[")) {
            metaInfo.append(listIdxIter.next()).append("]");
          }
          if (i + 1 < pathElements.length) {
            metaInfo.append(".");
          }
        }
      }
      metaInfo.append("=");
      if (obj == null) {
        metaInfo.append("null");
      } else {
        metaInfo.append(obj.getClass().getCanonicalName());
      }
      csvWriter.appendString(metaInfo.toString());
    }


    public void writeLineContent(CSVWriter csvWriter, Object obj, List<Integer> listIndexStack) {
      if (csvWriter.doIncludeMetaColumn()) {
        writeMetaColumn(csvWriter, listIndexStack, obj);
      }
      for (int i = 0; i < startColumnIdx; i++) {
        csvWriter.appendString("");
      }
      if (obj instanceof GeneralXynaObject) {
        GeneralXynaObject gxo = (GeneralXynaObject) obj;
        for (XmomField xf : xmomFields) {
          csvWriter.appendObject(xf.getObject(gxo));
        }
        csvWriter.appendLineSeparator();
        for (Entry<XmomListField, CSVLineBoxWriter> e : subBoxes.entrySet()) {
          XmomListField xf = e.getKey();
          CSVLineBoxWriter w = e.getValue();
          int last = listIndexStack.size();
          listIndexStack.add(0);
          int i = 0;
          while (xf.hasMore(gxo)) {
            listIndexStack.set(last, i++);
            w.writeLineContent(csvWriter, xf.getObject(gxo), listIndexStack);
          }
          listIndexStack.remove(last);
        }
      } else {
        csvWriter.appendObject(obj); //z.b. int (das ist der fall, falls es sich um eine primitive liste handelt)
        csvWriter.appendLineSeparator();
      }
    }


    public int initStartColumnIndex(int last) {
      startColumnIdx = last + 1;
      last += xmomFields.size();
      for (Entry<XmomListField, CSVLineBoxWriter> e : subBoxes.entrySet()) {
        last = e.getValue().initStartColumnIndex(last);
        if (e.getKey().getType() != null) {
          //primitive liste hat immer eine spalte
          last++;
        }
      }
      return last;
    }


    public void writeHeader(CSVXynaObjectWriter cxow) {
      for (XmomField xf : xmomFields) {
        cxow.csvWriter.appendString(getHeaderField(cxow, xf));
      }
      for (Entry<XmomListField, CSVLineBoxWriter> e : subBoxes.entrySet()) {
        if (e.getKey().getType() != null) {
          //primitive liste
          cxow.csvWriter.appendString(getHeaderField(cxow, e.getKey()));
        } else {
          e.getValue().writeHeader(cxow);
        }
      }
    }


    private String getHeaderField(CSVXynaObjectWriter cxow, XmomField xf) {
      String renamed = cxow.renamedHeader.get(xf.getPath());
      if (renamed != null) {
        return renamed;
      }
      switch (cxow.header) {
        case LABELS :
          return xf.getLabel();
        case NONE :
          return null;
        case PATHS :
          return xf.getPath();
        default :
          throw new IllegalStateException("Unexpected case " + cxow.header);
      }
    }

  }


  private static final Logger logger = CentralFactoryLogging.getLogger(CSVXynaObjectWriter.class);
  private static final Comparator<XmomListField> COMPARATOR = new Comparator<XmomListField>() {

    @Override
    public int compare(XmomListField o1, XmomListField o2) {
      return o1.getPath().compareTo(o2.getPath());
    }

  };


  public enum Header {
    NONE, LABELS, PATHS;
  }
  
  public static final String META_HEADER = "meta";


  private Header header;
  private CSVWriter csvWriter;
  private Set<String> ignoredPaths = new HashSet<>();
  private CSVLineBoxWriter rootWriter;
  private Map<String, String> renamedHeader = new HashMap<>();
  private Map<String, StringTransformation> stringTransformations = new HashMap<>();


  public CSVXynaObjectWriter(CSVParameter csvParameter) {
    String h = csvParameter.getHeader();
    h = h == null ? "none" : h.toLowerCase();
    if (h.equals("none")) {
      this.header = Header.NONE;
    } else if (h.equals("labels")) {
      this.header = Header.LABELS;
    } else if (h.equals("paths")) {
      this.header = Header.PATHS;
    } else {
      throw new IllegalArgumentException("Invalid header type " + csvParameter.getHeader());
    }
    this.csvWriter = new CSVWriter();
    this.csvWriter.setEndOfFile(csvParameter.getEndOfFile());
    this.csvWriter.setLineSeparator(csvParameter.getLineSeparator());
    this.csvWriter.setMasker(csvParameter.getMasker());
    this.csvWriter.setNullRepresentation(csvParameter.getNullRepresentation());
    this.csvWriter.setSeparator(csvParameter.getSeparator());
  }


  public CSVXynaObjectWriter(CSVParameter csvParameter, CSVWriteOptions csvWriteOptions) {
    this(csvParameter);
    this.csvWriter.setMaskAlways(csvWriteOptions.getMaskAlways());
    if (csvWriteOptions.getIgnoredPaths() != null) {
      ignoredPaths.addAll(csvWriteOptions.getIgnoredPaths());
    }
    if (csvWriteOptions.getCSVColumnModifications() != null) {
      for (CSVColumnModification mod : csvWriteOptions.getCSVColumnModifications()) {
        String path = mod.getPath().trim();
        if (path.startsWith("%0%.")) {
          path = path.substring(4).trim();
        }
        if (mod.getHeaderName() != null) {
          String headerFieldName = mod.getHeaderName().trim();
          if (!headerFieldName.isEmpty()) {
            renamedHeader.put(path, headerFieldName);
          }
        }
        if (mod.getStringTransformation() != null) {
          stringTransformations.put(path, mod.getStringTransformation());
        }
      }
    }
    this.csvWriter.setIncludeMetaColumn(csvWriteOptions.getIncludeMetaColumn());
  }


  public void setClass(String fqTypeName) {
    try {
      XmomFieldParser xfp = new XmomFieldParser(ignoredPaths, stringTransformations);
      xfp.parse(fqTypeName);
      initializeXmomFields(xfp);
    } catch (Exception e) {
      throw new RuntimeException("Could not parse XynaObject " + fqTypeName, e);
    }
  }


  public void setClass(Class<? extends GeneralXynaObject> containedClass) {
    try {
      XmomFieldParser xfp = new XmomFieldParser(ignoredPaths, stringTransformations);
      xfp.parse(containedClass);
      initializeXmomFields(xfp);
    } catch (Exception e) {
      throw new RuntimeException("Could not parse XynaObject " + containedClass.getName(), e);
    }
  }


  private void initializeXmomFields(XmomFieldParser xfp) {
    rootWriter = createWriter(xfp, null);
    rootWriter.initStartColumnIndex(-1);
  }


  private CSVLineBoxWriter createWriter(XmomFieldParser xfp, XmomListField parent) {
    SortedMap<XmomListField, CSVLineBoxWriter> subBoxes = new TreeMap<>(COMPARATOR);
    CSVLineBoxWriter w = new CSVLineBoxWriter(xfp.getXmomFields(), subBoxes, parent);
    for (Entry<XmomListField, XmomFieldParser> e : xfp.getXmomListFields().entrySet()) {
      subBoxes.put(e.getKey(), createWriter(e.getValue(), e.getKey()));
    }
    return w;
  }


  public String write(List<GeneralXynaObject> data) {
    writeHeader();
    for (GeneralXynaObject gxo : data) {
      rootWriter.writeLineContent(csvWriter, gxo, new ArrayList<>());
    }
    csvWriter.appendEndOfFile();
    return csvWriter.toString();
  }


  private void writeHeader() {
    if (header == Header.NONE) {
      return; //nichts zu tun
    }
    if (csvWriter.doIncludeMetaColumn()) {
      csvWriter.appendString(META_HEADER);
    }
    rootWriter.writeHeader(this);
    csvWriter.appendLineSeparator();
  }


}
