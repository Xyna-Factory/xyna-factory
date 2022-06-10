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

package com.gip.xyna.xact.filter.monitor.auditFilterComponents;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.gip.xyna.xact.filter.monitor.auditpreprocessing.MissingImportsRestorer.MissingImport;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;




public class DetermineMissingImportsFilterComponent implements AuditFilterComponent {

  private Map<String, ServiceReferenceTag> serviceReferences; //key is id
  private List<ImportTag> importTags;
  private List<ImportRequest> openImportRequests;

  private boolean insideImport;
  private boolean insideService;
  private boolean insideDatatype;


  private boolean insideFunction;
  private boolean insideInvoke;
  private boolean insideReceive;


  private String dataTypeTypeName;
  private String dataTypeTypePath;

  private String operationName;
  private String serviceReferenceId;

  private int numberOfInputs;
  private int numberOfOutputs;


  public DetermineMissingImportsFilterComponent() {
    serviceReferences = new HashMap<String, ServiceReferenceTag>();
    importTags = new ArrayList<ImportTag>();
    openImportRequests = new ArrayList<ImportRequest>();
  }


  public List<MissingImport> retrieveMissingImports() {
    List<MissingImport> result = new ArrayList<MissingImport>();

    for (ImportRequest request : openImportRequests) {
      ServiceReferenceTag reference = serviceReferences.get(request.getServiceReferenceId());
      if (reference == null) {
        continue; //beyond repair.
      }

      if (!hasImportTag(request, reference)) {
        MissingImport missingImport = new MissingImport();
        missingImport.setLabel(reference.getLabel());
        String typeName = reference.getReferenceName();
        typeName = typeName.contains(".") ? typeName.substring(typeName.lastIndexOf(".") + 1) : typeName;
        missingImport.setTypeName(typeName);
        missingImport.setTypePath(reference.getReferencePath());
        missingImport.setOperationName(request.getOperationName());
        missingImport.setIsDatatypeOperation(reference.isService());
        missingImport.setNumberOfInputs(request.getNumberOfInputs());
        missingImport.setNumberOfOutputs(request.getNumberOfOutputs());
        result.add(missingImport);
      }

    }


    return result;
  }


  private boolean hasImportTag(ImportRequest request, ServiceReferenceTag reference) {
    for (ImportTag importTag : importTags) {
      boolean pathMatch = importTag.getPath().equals(reference.getReferencePath());
      boolean nameMatch = importTag.getName().equals(request.getOperationName());
      if (pathMatch && nameMatch) {
        return true;
      }
    }
    return false;
  }


  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    if (qName.equals(GenerationBase.EL.IMPORT)) {
      insideImport = true;
      return;
    }

    if (insideImport && qName.equals(GenerationBase.EL.SERVICE)) {
      insideService = true;
      if (atts.getValue(GenerationBase.ATT.TYPENAME) != null) {
        dataTypeTypeName = atts.getValue(GenerationBase.ATT.TYPENAME);
      }
      if (atts.getValue(GenerationBase.ATT.TYPEPATH) != null) {
        dataTypeTypePath = atts.getValue(GenerationBase.ATT.TYPEPATH); //typePath may be here (for WorkflowsCalls)
      }
      return;
    }

    if (insideImport && qName.equals(GenerationBase.EL.DATATYPE)) {
      insideDatatype = true;
      if (atts.getValue(GenerationBase.ATT.TYPENAME) != null) {
        dataTypeTypeName = atts.getValue(GenerationBase.ATT.TYPENAME);
      }
      if (atts.getValue(GenerationBase.ATT.TYPEPATH) != null) {
        dataTypeTypePath = atts.getValue(GenerationBase.ATT.TYPEPATH); //typePath may be here (for static/instance services)
      }
      return;
    }

    if (!insideImport && qName.equals(GenerationBase.EL.FUNCTION)) {
      insideFunction = true;
      return;
    }

    if (insideService && qName.equals(GenerationBase.EL.OPERATION)) {
      ImportTag importTag = new ImportTag();
      importTag.setName(atts.getValue(GenerationBase.ATT.OPERATION_NAME));
      importTag.setPath(dataTypeTypePath);
      importTags.add(importTag);
      return;
    }

    if (!insideImport && qName.equals(GenerationBase.EL.SERVICEREFERENCE)) {
      ServiceReferenceTag sr = new ServiceReferenceTag();
      String id = atts.getValue(GenerationBase.ATT.ID);
      sr.setLabel(atts.getValue(GenerationBase.ATT.LABEL));
      sr.setReferenceName(atts.getValue(GenerationBase.ATT.REFERENCENAME));
      sr.setReferencePath(atts.getValue(GenerationBase.ATT.REFERENCEPATH));
      sr.setService(sr.getReferenceName().contains("."));
      serviceReferences.put(id, sr);
      return;
    }

    if (!insideImport && insideFunction && qName.equals(GenerationBase.EL.INVOKE)) {
      insideInvoke = true;
      operationName = atts.getValue(GenerationBase.ATT.INVOKE_OPERATION);
      serviceReferenceId = atts.getValue(GenerationBase.ATT.SERVICEID);
      return;
    }

    if (insideInvoke && qName.equals(GenerationBase.EL.SOURCE)) {
      numberOfInputs++;
      return;
    }

    if (!insideImport && insideFunction && qName.equals(GenerationBase.EL.RECEIVE)) {
      insideReceive = true;
      return;
    }

    if (insideReceive && qName.equals(GenerationBase.EL.TARGET)) {
      numberOfOutputs++;
    }
  }


  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {

    if (qName.equals(GenerationBase.EL.IMPORT)) {
      insideImport = false;
    }

    if (insideDatatype && qName.equals(GenerationBase.EL.DATATYPE)) {
      insideDatatype = false;
    }

    if (insideImport && qName.equals(GenerationBase.EL.SERVICE)) {
      insideService = false;
    }

    if (insideInvoke && qName.equals(GenerationBase.EL.INVOKE)) {
      insideInvoke = false;
    }

    if (insideReceive && qName.equals(GenerationBase.EL.RECEIVE)) {
      insideReceive = false;
    }

    if (insideFunction && qName.equals(GenerationBase.EL.FUNCTION)) {
      insideFunction = false;
      ImportRequest request = new ImportRequest();
      request.setNumberOfInputs(numberOfInputs);
      request.setNumberOfOutputs(numberOfOutputs);
      request.setOperationName(operationName);
      request.setServiceReferenceId(serviceReferenceId);
      openImportRequests.add(request);

      numberOfInputs = 0;
      numberOfOutputs = 0;
      operationName = null;
      serviceReferenceId = null;
    }

  }


  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    // nothing to be done

  }


  private class ServiceReferenceTag {

    private String label;
    private String referenceName;
    private String referencePath;
    private boolean isService;


    public String getLabel() {
      return label;
    }


    public void setLabel(String label) {
      this.label = label;
    }


    public String getReferenceName() {
      return referenceName;
    }


    public void setReferenceName(String referenceName) {
      this.referenceName = referenceName;
    }


    public String getReferencePath() {
      return referencePath;
    }


    public void setReferencePath(String referencePath) {
      this.referencePath = referencePath;
    }


    public boolean isService() {
      return isService;
    }


    public void setService(boolean isService) {
      this.isService = isService;
    }
  }

  private static class ImportTag {

    private String path;
    private String name;


    public String getPath() {
      return path;
    }


    public void setPath(String path) {
      this.path = path;
    }


    public String getName() {
      return name;
    }


    public void setName(String name) {
      this.name = name;
    }
  }

  private static class ImportRequest {

    private String operationName;
    private String serviceReferenceId;
    private int numberOfInputs;
    private int numberOfOutputs;


    public String getOperationName() {
      return operationName;
    }


    public void setOperationName(String operationName) {
      this.operationName = operationName;
    }


    public String getServiceReferenceId() {
      return serviceReferenceId;
    }


    public void setServiceReferenceId(String serviceReferenceId) {
      this.serviceReferenceId = serviceReferenceId;
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
  }
}
