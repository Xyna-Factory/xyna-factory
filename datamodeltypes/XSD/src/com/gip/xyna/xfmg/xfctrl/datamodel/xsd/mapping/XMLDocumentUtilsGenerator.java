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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.MDMClassLoader;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.Constants;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.ImportParameter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.WorkspaceXMLSupport;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.GenerationParameter;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelManagement;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.DataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ParseDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.WriteDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModelSpecific;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Datatype;
import com.gip.xyna.xprc.xfractwfe.generation.xml.ExceptionType;
import com.gip.xyna.xprc.xfractwfe.generation.xml.SnippetOperation;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Variable;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomGenerator;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;


public class XMLDocumentUtilsGenerator {
  
  private XmomType XMLDocument_BaseType;
  private XmomType Object_BaseType;
  private XmomType XML_DataModel;
  private XmomType XMLWriteOptions;
  private XmomType ParseXMLFailedException;
  private XmomType WriteXMLFailedException;
  private XmomType XMLDocument;
  private Variable XMLDocument_var;
  private Variable Data_var;
  private GenerationParameter generationParameter;
  
  public XMLDocumentUtilsGenerator(GenerationParameter generationParameter) {
    this.generationParameter = generationParameter;
  }

  public void createXMLUtils(DataModelResult dataModelResult, long revision) {
    try {
      
      createConstants(); 
      
      XmomGenerator xmomGenerator = XmomGenerator.inRevision(revision).overwrite(true).build();
      
      if( xmomGenerator.exists(XML_DataModel) ) { //TODO Versionierung? generationParameter fragen?
        //return; //XML-DataModelSupport existiert bereits
      }
      
      createAndAddUtilTypes(xmomGenerator);
      xmomGenerator.save();
      xmomGenerator.deploy();
      
      dataModelResult.info("Successfully deployed "+XML_DataModel.getFQTypeName()+" and " +XMLWriteOptions.getFQTypeName() );
    } catch ( Exception e ) {
      //XFMG_NoSuchRevisionm Ex_FileWriteException, MDMParallelDeploymentException, 
      //XPRC_DeploymentDuringUndeploymentException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, 
      //XPRC_InvalidPackageNameException
      dataModelResult.warn("Generating DocumentType XML failed "+e.getMessage() );
      Logger.getLogger(WorkspaceXMLSupport.class).warn("Generating DocumentType XML failed ", e );
    }
  }
  
  
  public void createAndAddUtilTypes(XmomGenerator xmomGenerator) {
    xmomGenerator.add(Constants.createDatatype());
    xmomGenerator.add(createParseException());
    xmomGenerator.add(createWriteException());
    xmomGenerator.add(createXMLWriteOptions());
    xmomGenerator.add(createXML());
  }

  public void createConstants() {
    String path = Constants.getUtilPath();
    XMLDocument_BaseType = new XmomType("xact.templates","XML", "XML");
    Object_BaseType = Constants.getBase_XmomType();
    XML_DataModel = new XmomType(path,"XML_DataModel", "XML (Data Model)");
    XMLWriteOptions = new XmomType(path,"XMLWriteOptions", "XML Write Options");
    ParseXMLFailedException = new XmomType(path, "ParseXMLFailedException", "Parse XML Failed");
    WriteXMLFailedException = new XmomType(path, "WriteXMLFailedException", "Write XML Failed");
    XMLDocument = new XmomType("xact.templates", "Document", "Document");
    XMLDocument_var = Variable.create("document").label("XML Document").complexType(XMLDocument).build();
    Data_var = Variable.create("data").label("XSD-generated Type").complexType(Object_BaseType).build();
  }



  private ExceptionType createParseException() {
    ExceptionType et = ExceptionType.
        create(ParseXMLFailedException).
        code("XFMG-DMM-XSD-0001").
        messageText("DE","Parsen des XML-Strings fehlgeschlagen").
        messageText("EN","Parse of xml string failed").
        build();
    return et;
  }

  private ExceptionType createWriteException() {
    ExceptionType et = ExceptionType.
        create(WriteXMLFailedException).
        code("XFMG-DMM-XSD-0002").
        messageText("DE","Schreiben des XML-Strings fehlgeschlagen").
        messageText("EN","Write of xml string failed").
        build();
    return et;
  }

  private Datatype createXMLWriteOptions() {
    Datatype datatype = Datatype.
        create(XMLWriteOptions).
        variable(Variable.create("rootElementName").label("Root Element Name").simpleType(PrimitiveType.STRING)).
        variable(Variable.create("omitNullTags").label("Omit Null Tags").simpleType(PrimitiveType.BOOLEAN_OBJ)).
        variable(Variable.create("omitSingleNamespacePrefix").label("Omit Single Namespace Prefix").simpleType(PrimitiveType.BOOLEAN_OBJ)).
        variable(Variable.create("booleanAsInteger").label("Boolean As Integer").simpleType(PrimitiveType.BOOLEAN_OBJ)).
        variable(Variable.create("includePIElement").label("Include PI Element").simpleType(PrimitiveType.BOOLEAN_OBJ)).
        build();
    return datatype;
  }

  protected Datatype createXML() {
    Datatype datatype = Datatype.
        create(XML_DataModel).
        basetype(XMLDocument_BaseType).
        variable(Variable.create("dataModelName").label("Data Model Name").simpleType(PrimitiveType.STRING)).
        operation(SnippetOperation.create("parseXML").
                  requiresXynaOrder().
                  label("Parse XML").
                  input(XMLDocument_var).
                  output(Data_var).
                  exception(Variable.createException("parseXMLFailedException").type(ParseXMLFailedException)).
                  sourceCode("<![CDATA[" + createParseXML() + "]]>").
                  build()).
        operation(SnippetOperation.create("validateXML").
                  requiresXynaOrder().
                  label("Validate XML").
                  input(XMLDocument_var).
                  exception(Variable.createException("parseXMLFailedException").type(ParseXMLFailedException)).
                  sourceCode("<![CDATA[" + createValidateXML() + "]]>").
                  build()).
        operation(SnippetOperation.create("writeXML").
                  label("Write XML").
                  input(Data_var).
                  output(XMLDocument_var).
                  exception(Variable.createException("writeXMLFailedException").type(WriteXMLFailedException)).
                  sourceCode("<![CDATA[" + createWriteXML(false) + "]]>").
                  build()).
        operation(SnippetOperation.create("writeXML_withOptions").
                  label("Write XML (with Options)").
                  input(Data_var).
                  input( Variable.create("options").label("XML Write Options").complexType(XMLWriteOptions) ).
                  output(XMLDocument_var).
                  exception(Variable.createException("writeXMLFailedException").type(WriteXMLFailedException)).
                  sourceCode("<![CDATA[" + createWriteXML(true) + "]]>").
                  build()).
        build();

    return datatype;
  }

  private String createParseXML() {
    StringBuilder sb = createBase(ParseDataModelParameters.class);
    sb.append("try {\n");
    sb.append("  String xml = document.getCompleteBufferContent();\n");
    sb.append("  if( xml == null || xml.length() == 0 ) {\n").
       append("     xml = document.getText();\n").
       append("  }\n");

    createRevisionDetermination(sb);
    
    sb.append("  ").append(Object_BaseType.getFQTypeName() ).append( " data = \n");
    sb.append("    (").append(Object_BaseType.getFQTypeName()).
       append(") dmm.parseDataModel(parameters,xml,revision);\n\n");
    sb.append("  return data;\n");
    sb.append("} catch ( XynaException e ) {\n");
    sb.append("  throw new ").append(ParseXMLFailedException.getFQTypeName()).append("(e);\n");
    sb.append("}\n");
    return sb.toString();
  }
  
  
  private void createRevisionDetermination(StringBuilder sb) {
    sb.append("long rootRevision = correlatedXynaOrder.getRootOrder().getRevision();\n")     
    .append("revision = rootRevision;\n")
    .append(RevisionManagement.class.getName() + " revMgmt = " + XynaFactory.class.getName() + ".getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();\n")
    .append(RuntimeDependencyContext.class.getName() +  " rc = " + RuntimeContextDependencyManagement.class.getName() + ".asRuntimeDependencyContext(revMgmt.getRuntimeContext(rootRevision));\n")
    .append(RuntimeContextDependencyManagement.class.getName() + " rcdMgmt = " + XynaFactory.class.getName() + ".getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();\n")
    .append("Set<" + RuntimeDependencyContext.class.getName() + "> requirements = new HashSet<" + RuntimeDependencyContext.class.getName() + ">();\n")
    .append("rcdMgmt.getRequirementsRecursivly(rc, requirements);\n")
    .append("requirements.add(rc);\n")
    .append(com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel.class.getName()  + " foundDm = null;\n")
    .append("for (" + com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel.class.getName() + " dm : dmm.listDataModels(parameters.getDataModelType())) {\n")
    .append("  if (dm.getType().getLabel().equals(parameters.getDataModelName())) {\n")
    .append("    boolean distributesToWorkspaces = false;\n")
    .append("    for (" + DataModelSpecific.class.getName() + " dms : dm.getDataModelSpecifics()) {\n")
    .append("      if (dms.getKey().endsWith(\"" + ImportParameter.DISTRIBUTE_TO_WORKSPACES.getName() + "\")) {\n")
    .append("        distributesToWorkspaces = " + Boolean.class.getName() + ".parseBoolean(dms.getValue());\n")
    .append("        break;\n")
    .append("      }\n")
    .append("    }\n")
    .append("    if (distributesToWorkspaces) {\n")
    .append("      Set<" + RuntimeDependencyContext.class.getName() + "> distributedTo = new HashSet<" + RuntimeDependencyContext.class.getName() + ">();\n")
    .append("      for (" + DataModelSpecific.class.getName() + " dms : dm.getDataModelSpecifics()) {\n")
    .append("        if (dms.getKey().contains(\".workspaces[\") && requirements.contains(new " + Workspace.class.getName() + "(dms.getValue()))) {\n")
    .append("          try {\n")
    .append("            foundDm = dm;\n")
    .append("            revision = revMgmt.getRevision(null, null, dms.getValue());\n")
    .append("            break;\n")
    .append("          } catch (" + XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY.class.getName() + " e) { /* try next */ }\n")
    .append("        }\n")
    .append("      }\n")
    .append("    } else {\n")
    .append("      if (requirements.contains(new " + Application.class.getName() + "(parameters.getDataModelName(), dm.getVersion()))) {\n")
    .append("        try {\n")
    .append("          foundDm = dm;\n")
    .append("          parameters.setDataModelVersion(dm.getVersion());\n")
    .append("          revision = revMgmt.getRevision(parameters.getDataModelName(), dm.getVersion(), null);\n")
    .append("          break;\n")
    .append("        } catch (" + XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY.class.getName() + " e) { /* try next */ }\n")
    .append("      }\n")
    .append("    }\n")
    .append("  }\n")
    .append("}\n")
    .append("if (foundDm == null) {\n")
    .append("  throw new " + RuntimeException.class.getName() + "(\"Datamodel not found\");\n")
    .append("}\n");
  }


  private String createWriteXML(boolean withOptions) {
    StringBuilder sb = createBase(WriteDataModelParameters.class);
    if( withOptions ) {
      sb.append("parameters.addParameter(\"rootElementName\",options.getRootElementName());\n");
      sb.append("parameters.addParameter(\"omitNullTags\",options.getOmitNullTags());\n");
      sb.append("parameters.addParameter(\"omitSingleNamespacePrefix\",options.getOmitSingleNamespacePrefix());\n");
      sb.append("parameters.addParameter(\"booleanAsInteger\",options.getBooleanAsInteger());\n");
      sb.append("parameters.addParameter(\"includePIElement\",options.getIncludePIElement());\n");
      sb.append("\n");
    }
    
    sb.append("try {\n");
    sb.append("  if (data.getClass().getClassLoader() instanceof " + ClassLoaderBase.class.getName() + ") {\n");
    sb.append("    revision = ((" + ClassLoaderBase.class.getName() + ")data.getClass().getClassLoader()).getRevision();\n")
      .append("    " + RevisionManagement.class.getName() + " revMgmt = " + XynaFactory.class.getName() + ".getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();\n")
      .append("    " + RuntimeContext.class.getName() +  " rc = revMgmt.getRuntimeContext(revision);\n")
      .append("    if (rc instanceof " + Application.class.getName() + " && ((" + Application.class.getName() + ")rc).getName().equals(getDataModelName())) {\n")
      .append("      parameters.setDataModelVersion(((" + Application.class.getName() + ")rc).getVersionName());\n")
      .append("    }\n")
      .append("  }\n");
    
    sb.append("  String xml = dmm.writeDataModel(parameters,data,revision);\n\n");
    sb.append("  Document doc = new Document(this, xml);\n");
    sb.append("  doc.addToBuffer(xml);\n");
    sb.append("  return doc;\n");
    sb.append("} catch ( XynaException e ) {\n");
    sb.append("  throw new ").append(WriteXMLFailedException.getFQTypeName()).append("(e);\n");
    sb.append("}\n");
    return sb.toString();
  }
  
  private String createValidateXML() {
    StringBuilder sb = createBase(ParseDataModelParameters.class);
    
    sb.append("try {\n");
    sb.append("  String xml = document.getCompleteBufferContent();\n");
    sb.append("  if( xml == null || xml.length() == 0 ) {\n").
       append("     xml = document.getText();\n").
       append("  }\n");
    
    createRevisionDetermination(sb);
    
    sb.append("  " + Method.class.getName() + " getDataModelType = " + DataModelManagement.class.getName() + ".class.getDeclaredMethod(\"getDataModelType\", " + DataModelParameters.class.getName() + ".class);\n")
      .append("  getDataModelType.setAccessible(true);\n")
      .append("  Object dataModelType = getDataModelType.invoke(dmm, parameters);\n");
    
    sb.append("  " + Method.class.getName() + " validateAgainstDataModel = dataModelType.getClass().getDeclaredMethod(\"validateAgainstDataModel\", String.class, " + DataModel.class.getName() + ".class);\n")
      .append("  validateAgainstDataModel.invoke(dataModelType, xml, foundDm);\n");

    
    sb.append("} catch ( XynaException e ) {\n");
    sb.append("  throw new ").append(ParseXMLFailedException.getFQTypeName()).append("(e);\n");
    sb.append("} catch ( ").append(InvocationTargetException.class.getName()).append(" e ) {\n");
    sb.append("  throw new ").append(ParseXMLFailedException.getFQTypeName()).append("(e.getTargetException());\n");
    sb.append("} catch ( Exception e ) {\n");
    sb.append("  throw new ").append(RuntimeException.class.getName()).append("(e);\n");
    sb.append("}\n");
    return sb.toString();
  }

  

  private StringBuilder createBase(Class<?> parameterClass) {
    StringBuilder sb = new StringBuilder();
    sb.append("long revision = ((").append(MDMClassLoader.class.getName()).append(")getClass().getClassLoader()).getRevision();\n\n");
    sb.append(DataModelManagement.class.getName()).append(" dmm =\n").
    append("  ").append(XynaFactory.class.getName()).append(".getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();\n\n");
    sb.append(parameterClass.getName()).append(" parameters = \n").
    append("  new ").append(parameterClass.getName()).append("(").
    append("\"").append(generationParameter.getDataModelTypeName()).append("\",").
    append("getDataModelName(), \"0\");\n\n");
    return sb;
  }
  
  
}
