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
package com.gip.xyna.xdev.map.typegen;

import java.util.EnumMap;

import com.gip.xyna.xdev.map.typegen.XmomDataCreator.LabelCustomization;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;


/**
 *
 */
public class TypeGenerationOptions implements GenerationParameter {

  public enum HostInXmomPath {
    useHost,
    omitHost,
    reverseHost;
  }
  
  private HostInXmomPath hostInXmomPath;
  private boolean useNamespaceForXmomPath;
  
  protected String basePath;
  protected XmomType baseType;
  protected XmomType baseTypeRoot;
  protected EnumMap<LabelCustomization,String> labelCustomization;
  protected String dataModelName;
  protected String pathCustomization;
  protected boolean generationOptions_expandChoice;
  protected boolean overwrite;

  
  public TypeGenerationOptions() {
    this.labelCustomization = new EnumMap<LabelCustomization,String>(LabelCustomization.class);
    this.basePath = "xdnc.model.xsd";
    this.setHostInXmomPath(HostInXmomPath.useHost);
  }
  
  public TypeGenerationOptions(String basePathForGeneration, boolean useNamespaceForXmomPath,
                              boolean changeLabelForAttribute, XmomType baseXmomTypeRoot, XmomType baseXmomType) {
    this();
    this.basePath = basePathForGeneration;
    this.setChangeLabelForAttribute(changeLabelForAttribute);
    this.setHostInXmomPath(HostInXmomPath.useHost);
    
  }

  public TypeGenerationOptions(TypeGenerationOptions tgo) {
    this.basePath = tgo.getBasePath();
    this.labelCustomization = new EnumMap<LabelCustomization,String>(tgo.getLabelCustomization());
    this.pathCustomization = tgo.getPathCustomization();
    this.dataModelName = tgo.getDataModelName();
  }

  public void setBasePathForGeneration(String basePathForGeneration) {
    this.basePath = basePathForGeneration;
  }
  
  public void setChangeLabelForAttribute(boolean changeLabelForAttribute) {
    if( changeLabelForAttribute ) {
      this.labelCustomization.put(LabelCustomization.suffixForAttribute, " (attribute)");
    } else {
      this.labelCustomization.remove(LabelCustomization.suffixForAttribute);
    }
  }

  public void setUseNamespaceForXmomPath(boolean useNamespaceForXmomPath) {
    this.useNamespaceForXmomPath = useNamespaceForXmomPath;
    if( ! useNamespaceForXmomPath ) {
      this.pathCustomization = "/#basepath";
    } else {
      setHostInXmomPath(hostInXmomPath);
    }
  }
  
  public void setHostInXmomPath(HostInXmomPath hostInXmomPath) {
    this.hostInXmomPath = hostInXmomPath;
    if( useNamespaceForXmomPath ) {
      switch( hostInXmomPath) {
        case omitHost:
          this.pathCustomization = "/#basepath/#nspath";
          break;
        case reverseHost:
          this.pathCustomization = "/#basepath/#nsrevhost/#nspath";
          break;
        case useHost:
          this.pathCustomization = "/#basepath/#nshost/#nspath";
          break;
      }
    } else {
      this.pathCustomization = "/#basepath/";
    }
  }

  public void setBaseXmomType(XmomType xmomType) {
    this.baseType = xmomType;
  }
  
  public void setBaseXmomTypeRoot(XmomType xmomType) {
    this.baseTypeRoot = xmomType;
  }

  
  
  
  public XmomType getBaseTypeRoot() {
    return baseTypeRoot;
  }
  
  public XmomType getBaseType() {
    return baseType;
  }

  public String getBasePath() {
    return basePath;
  }
   
  public EnumMap<LabelCustomization, String> getLabelCustomization() {
    return labelCustomization;
  }

  public String getPathCustomization() {
    return pathCustomization;
  }
  
  public String getDataModelName() {
    return dataModelName;
  }

  public boolean isGenerationOptions_expandChoice() {
    return generationOptions_expandChoice;
  }

  public boolean isOverwrite() {
    return overwrite;
  }

}
