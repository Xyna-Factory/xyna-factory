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
package com.gip.xyna.xfmg.xfctrl.datamodel.tr069;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xfctrl.datamodel.types.TRModel;
import com.gip.xyna.xfmg.xfctrl.datamodel.types.TRObject;
import com.gip.xyna.xfmg.xfctrl.datamodel.types.TRObjectContainer;
import com.gip.xyna.xfmg.xfctrl.datamodel.types.TRObjectReference;
import com.gip.xyna.xfmg.xfctrl.datamodel.types.TRParameter;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel.Builder;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.xml.DataModel;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Datatype;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Meta;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Variable;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;


/**
 *
 */
public class TR069ModelImport {

  private TRModel model;
  private ImportParameter parameter;
  private String xmomPath;
  private String modelName;
  private XmomType baseType;
  private String dataModelName;
  private com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel dataModel;
  private List<Datatype> dataTypes;
  private String label;

  public TR069ModelImport(TRModel model, ImportParameter parameter) {
    this.model = model;
    this.parameter = parameter;
    if (model.getLabel() == null) {
      this.label = createLabel();
    } else {
      this.label = model.getLabel();
    }
    initialize();
    dataTypes = new ArrayList<Datatype>();
  }
  
  public TR069ModelImport(TRModel model, ImportParameter parameter, String label) {
    this.model = model;
    this.parameter = parameter;
    this.label = label;
    initialize();
    dataTypes = new ArrayList<Datatype>();
  }

  /**
   * 
   */
  private void initialize() {
    StringBuilder xmomPath = new StringBuilder();
    xmomPath.append(parameter.getBasePath());
    xmomPath.append('.');
    xmomPath.append(model.getName());
    
    String version = "v"+model.getVersion().replaceAll("\\.","_");
    xmomPath.append(".").append(version);
    this.xmomPath = xmomPath.toString();
    
    String name = model.getName();
    String Name = name.substring(0,1).toUpperCase()+name.substring(1);
    
    this.modelName = xmomPath.toString()+"."+Name;
    this.baseType = parameter.getBaseType();
       
    XmomType type = new XmomType( xmomPath.toString(), Name, label );
    
    
    dataModelName = type.getFQTypeName();
    /*
    moduleData.setType(baseType, type);
    String documentation = mi.getDescription();
    moduleData.setMeta( Meta.dataModel_documentation(     
                                                     DataModel.baseModel(mi.getNode().getOidStr(), baseType),
                                                     documentation ) );
    
    dataModelSpecificsBuilder = new DataModelSpecificsBuilder(module.getId(), 
                                                              mi.getNode().getOidStr(), importOnlyOids);
    */
    Builder dataModelBuilder = new Builder();
    
    dataModelBuilder.baseType(baseType.toXynaObject());
    dataModelBuilder.type(type.toXynaObject() );
    dataModelBuilder.dataModelType(parameter.getDataModelTypeName());
    dataModelBuilder.version( "v"+model.getVersion() );
    //dataModelBuilder.documentation(documentation);
    
    dataModel = dataModelBuilder.instance();
    
  }

  private String createLabel() {
    StringBuilder label = new StringBuilder();
    
    String filename = model.getDocument().getFileName();
    label.append(filename.substring(0,6).toUpperCase());
    
    label.append(": ");
    label.append(model.getName() );
    //label.append(" ").append( model.getVersion() );
    
    return label.toString();
  }
  
  /**
   * @return
   */
  public com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel getDataModel() {
    return dataModel;
  }


  /**
   * 
   */
  public void createDataTypes() {
    for( TRObject object : model.getRootObjects() ) {
      createRootDataType(object); //TODO nur eines erwartet!
    }
    for( TRObject object : model.getChildObjects() ) {
      createDataType(object);
    }
  }
  

  private void createRootDataType(TRObject object) {
    List<Variable> variables = createVariables(object);
    DataModel dataModel = DataModel.baseModel(baseType,null);
    XmomType type = createXmomType(object);
    String label = this.dataModel.getType().getLabel();
    type = new XmomType(type.getPath(), type.getName(), label);
    Meta meta = Meta.dataModel_documentation(dataModel, object.getDescription() );
    dataTypes.add( Datatype.meta(type, null, meta, variables) );
  }
  

  private void createDataType(TRObject object) {
    List<Variable> variables = createVariables(object);
    DataModel dataModel = DataModel.modelName(modelName,null);
    XmomType type = createXmomType(object);    
    Meta meta = Meta.dataModel_documentation(dataModel, object.getDescription() );
    dataTypes.add( Datatype.meta(type, null, meta, variables) );
  }
  
  private List<Variable> createVariables(TRObject object) {
    List<Variable> variables = new ArrayList<Variable>();
    for( TRParameter param : object.getParameters() ) {
      variables.add( createVariable(param) );
    }
    for( TRObjectReference child : object.getChildren() ) {
      variables.add( createVariable( child, object.getContainer() ) );
    }
    return variables;
  }

  private XmomType createXmomType(TRObject object) {
    String name = object.getName();
    String Name = name.substring(0,1).toUpperCase()+name.substring(1);
    Name = Name.replaceAll("-", "_");
    
    String parent = object.getParentObjectFqName();
    if( parent == null ) {
      return new XmomType( xmomPath, Name, name );
    } else {
      try {
        return new XmomType( createXmomPath(parent), GenerationBase.transformNameForJava(Name), name); //parent+name );
      } catch (XPRC_InvalidPackageNameException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private String createXmomPath(String parent) {
    StringBuilder path = new StringBuilder();
    path.append(xmomPath).append(".");
    path.append(parent.replaceAll("\\{i\\}\\.", ""));
    String p = path.toString();
    while (p.endsWith(".")) {
      p = p.substring(0, p.length() - 1);
    }
    p = p.replaceAll("-", "_");
    try {
      return GenerationBase.transformNameForJava(p);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
  }

  private Variable createVariable(TRParameter param) {
    String name = param.getName();
    try {
      name = name.replaceAll("-", "_");
      name = GenerationBase.transformNameForJava(name);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
    DataModel childDM = new DataModel(); //TODO
    return Variable.create(name).label(param.getName()).
        simpleType(param.getSimpleType()).
        meta(Meta.dataModel_documentation(childDM, param.getDescription() )).
        isList(param.isList()).
        build();
  }
  
  private Variable createVariable(TRObjectReference reference, TRObjectContainer resolver) {
    if (resolver == null) {
      resolver = model;
    }
    Pair<TRObjectContainer,TRObject> pair = reference.getObjectFrom(resolver);
    TRObject object = pair.getSecond();
    String name = object.getName();
    name = name.replaceAll("-", "_");
    try {
      name = GenerationBase.transformNameForJava(name);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
    DataModel childDM = new DataModel(); //TODO
    return Variable.create(name).label(object.getName()).
        complexType(createXmomType(object)).
        meta(Meta.dataModel_documentation(childDM, object.getDescription() )).
        isList(object.isListInParent()).
        build();
  }

  /**
   * @return
   */
  public List<Datatype> getDataTypes() {
    return dataTypes;
  }

  /**
   * @return
   */
  public String getModelName() {
    return dataModelName;
  }

  public TRModel getModel() {
    return model;
  }

  public String getLabel() {
    return label;
  }

}
