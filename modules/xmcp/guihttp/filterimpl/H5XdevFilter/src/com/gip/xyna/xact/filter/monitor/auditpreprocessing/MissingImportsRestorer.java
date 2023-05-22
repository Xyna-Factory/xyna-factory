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

package com.gip.xyna.xact.filter.monitor.auditpreprocessing;



import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.gip.xyna.xact.filter.monitor.auditFilterComponents.ComponentBasedAuditFilter;
import com.gip.xyna.xact.filter.monitor.auditFilterComponents.DetermineMissingImportsFilterComponent;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;




public class MissingImportsRestorer {


  private static final String DataTagForInAndOutput =
      "<Data Label=\"MISSING\" ReferenceName=\"AnyType\" ReferencePath=\"base\" VariableName=\"anyType-100\"></Data>";


  public MissingImportRestorationResult restoreMissingImports(String filteredXml) {
    MissingImportRestorationResult result = new MissingImportRestorationResult();

    //Step 1: Determine missing Imports
    List<MissingImport> missingImports = determineMissingImports(filteredXml);
    result.setRestoredImports(new ArrayList<MissingImport>(missingImports));

    //Step 2: Add missing Imports back into the XML
    filteredXml = addMissingImportsToXml(filteredXml, missingImports);

    result.setFilteredXml(filteredXml);
    
    return result;
  }


  private List<MissingImport> determineMissingImports(String filteredXml) {
    ComponentBasedAuditFilter filter = new ComponentBasedAuditFilter();
    DetermineMissingImportsFilterComponent determineMissingImportsCompoenent = new DetermineMissingImportsFilterComponent();
    filter.addAuditFilterComponent(determineMissingImportsCompoenent);

    try {
      filter.parse(new InputSource(new StringReader(filteredXml)));
    } catch (SAXException | IOException e) {
      throw new RuntimeException(e);
    }

    return determineMissingImportsCompoenent.retrieveMissingImports();
  }


  private String addMissingImportsToXml(String filteredXml, List<MissingImport> missingImports) {

    //group missingImports by typePath + typeName -> services in same serviceGroup/DataType -- TODO: TypeName
    missingImports.sort((x, y) -> x.getTypePath().compareTo(y.getTypePath()));
    for (int i = missingImports.size() - 1; i >= 0; i--) {
      MissingImport currentImport = missingImports.get(i);
      List<MissingImport> missingImportsThisGroup = new ArrayList<MissingImport>();
      missingImportsThisGroup.add(currentImport);

      for (int j = i - 1; j >= 0; j--) {
        MissingImport compare = missingImports.get(j);
        if (compare.getTypePath().equals(currentImport.getTypePath())) {
          missingImportsThisGroup.add(compare);
          missingImports.remove(j);
          i--;
        } //else break
      }
      missingImports.remove(i);

      filteredXml = addMissingImportGroupToXml(filteredXml, missingImportsThisGroup);
    }

    return filteredXml;
  }


  private String addMissingImportGroupToXml(String filteredXml, List<MissingImport> missingImportsThisGroup) {
    XmlBuilder builder = new XmlBuilder();

    MissingImport firstImport = missingImportsThisGroup.get(0);
    builder.startElement(GenerationBase.EL.IMPORT);
    {
      builder.startElement(GenerationBase.EL.WORKSPACE);
      {
        builder.append("MISSING");
      }
      builder.endElement(GenerationBase.EL.WORKSPACE);

      builder.startElement(GenerationBase.EL.DOCUMENT);
      {
        if (firstImport.isDataypeOperation()) {
          writeDatatypeXml(builder, firstImport, missingImportsThisGroup);
        } else {
          writeServiceXml(builder, firstImport, missingImportsThisGroup, true);
        }
      }
      builder.endElement(GenerationBase.EL.DOCUMENT);
    }
    builder.endElement(GenerationBase.EL.IMPORT);

    return filteredXml.replace("</" + GenerationBase.EL.ORDER_ITEM, builder.toString() + "\n</" + GenerationBase.EL.ORDER_ITEM);
  }


  private void writeServiceXml(XmlBuilder builder, MissingImport firstImport, List<MissingImport> missingImportsThisGroup,
                               boolean immediateChildOfDocument) {
    builder.startElementWithAttributes(GenerationBase.EL.SERVICE);
    {
      builder.addAttribute(GenerationBase.ATT.LABEL, firstImport.getLabel());
      builder.addAttribute(GenerationBase.ATT.TYPENAME, firstImport.getTypeName());
      if (immediateChildOfDocument) {
        addVersionAndNameSpace(builder);
        builder.addAttribute(GenerationBase.ATT.TYPEPATH, firstImport.getTypePath());
      }
      builder.endAttributes();

      for (MissingImport minport : missingImportsThisGroup) {
        builder.startElementWithAttributes(GenerationBase.EL.OPERATION);
        builder.addAttribute(GenerationBase.ATT.ISSTATIC, "" + minport.isDataypeOperation());
        builder.addAttribute(GenerationBase.ATT.LABEL, minport.getLabel());
        builder.addAttribute(GenerationBase.ATT.OPERATION_NAME, minport.getOperationName());
        builder.endAttributes();

        builder.startElement(GenerationBase.EL.INPUT);
        {
          appendInOrOutput(builder, minport.getNumberOfInputs());
        }
        builder.endElement(GenerationBase.EL.INPUT);

        builder.startElement(GenerationBase.EL.OUTPUT);
        {
          appendInOrOutput(builder, minport.getNumberOfOutputs());
        }
        builder.endElement(GenerationBase.EL.OUTPUT);

        builder.endElement(GenerationBase.EL.OPERATION);
      }
    }
    builder.endElement(GenerationBase.EL.SERVICE);
  }


  private void writeDatatypeXml(XmlBuilder builder, MissingImport firstImport, List<MissingImport> missingImportsThisGroup) {
    builder.startElementWithAttributes(GenerationBase.EL.DATATYPE);
    builder.addAttribute(GenerationBase.ATT.LABEL, firstImport.getLabel());
    builder.addAttribute(GenerationBase.ATT.TYPENAME, firstImport.getTypeName());
    builder.addAttribute(GenerationBase.ATT.TYPEPATH, firstImport.getTypePath());
    addVersionAndNameSpace(builder);
    builder.endAttributes();
    {
      builder.startElement(GenerationBase.EL.META);
      {
        builder.startElement(GenerationBase.EL.IS_SERVICE_GROUP_ONLY);
        {
          builder.append("" + firstImport.isDataypeOperation());
        }
        builder.endElement(GenerationBase.EL.IS_SERVICE_GROUP_ONLY);
      }
      builder.endElement(GenerationBase.EL.META);
      writeServiceXml(builder, firstImport, missingImportsThisGroup, false);
    }
    builder.endElement(GenerationBase.EL.DATATYPE);
  }


  private void addVersionAndNameSpace(XmlBuilder builder) {
    builder.addAttribute(GenerationBase.ATT.AUDIT_VERSION, "1.8");
    builder.addAttribute(GenerationBase.ATT.XMLNS, "http://www.gip.com/xyna/xdev/xfractmod");
  }


  private void appendInOrOutput(XmlBuilder builder, int numberOfElements) {
    for (int i = 0; i < numberOfElements; i++) {
      builder.append(DataTagForInAndOutput);
      builder.append("\n");
    }
  }

  //only missing services for now

  
  public static class MissingImportRestorationResult {
    private String filteredXml;
    private List<MissingImport> restoredImports;
    
    public String getFilteredXml() {
      return filteredXml;
    }
    
    public void setFilteredXml(String filteredXml) {
      this.filteredXml = filteredXml;
    }
    
    public List<MissingImport> getRestoredImports() {
      return restoredImports;
    }
    
    public void setRestoredImports(List<MissingImport> restoredImports) {
      this.restoredImports = restoredImports;
    }
  }

  public static class MissingImport {

    //DataType tag
    private String typeName;
    private String typePath;
    private String label;

    //Service tag - reuse label and typeName

    //inside operation tag
    private String operationName;
    private boolean isDatatypeOperation;
    private int numberOfInputs;
    private int numberOfOutputs;


    public MissingImport() {

    }


    public String getTypeName() {
      return typeName;
    }


    public void setTypeName(String typeName) {
      this.typeName = typeName;
    }


    public String getTypePath() {
      return typePath;
    }


    public void setTypePath(String typePath) {
      this.typePath = typePath;
    }


    public String getLabel() {
      return label;
    }


    public void setLabel(String label) {
      this.label = label;
    }


    public int getNumberOfInputs() {
      return numberOfInputs;
    }


    public void setNumberOfInputs(int numberOfInputs) {
      this.numberOfInputs = numberOfInputs;
    }


    public int getNumberOfOutputs() {
      return numberOfOutputs;
    }


    public void setNumberOfOutputs(int numberOfOutputs) {
      this.numberOfOutputs = numberOfOutputs;
    }


    public String getOperationName() {
      return operationName;
    }


    public void setOperationName(String operationName) {
      this.operationName = operationName;
    }


    public boolean isDataypeOperation() {
      return isDatatypeOperation;
    }


    public void setIsDatatypeOperation(boolean isDatatypeOperation) {
      this.isDatatypeOperation = isDatatypeOperation;
    }
  }
}
