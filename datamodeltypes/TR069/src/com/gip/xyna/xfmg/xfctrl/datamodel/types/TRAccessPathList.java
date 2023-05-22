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
package com.gip.xyna.xfmg.xfctrl.datamodel.types;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.xfmg.xfctrl.datamodel.tr069.DataModelParseContext;
import com.gip.xyna.xfmg.xfctrl.datamodel.tr069.TR069Tools;


public class TRAccessPathList extends TRDataModelDefinition {
  
  private TRModel model;
  private String fileContent;
  
  public TRAccessPathList(File file) throws Ex_FileAccessException {
    super(file);
    fileContent = FileUtils.readFileAsString(file);
  }
  

  @Override
  public String toString() {
    return "TRDocument("+name+")";
  }

  
  

  public String getName() {
    return name;
  }
  
  public String getFileName() {
    return fileName;
  }


  public Collection<TRModel> getModels() {
    return Collections.singleton(model);
  }
  public TRModel getModel(String fqName) {
    // TODO check
    return model;
  }
  


  @Override
  public Set<String> getImportReferences() {
    return Collections.emptySet();
  }


  @Override
  public void parseImports() {
    // ntbd
  }


  @Override
  public void parse(DataModelParseContext context) {
    String lines[] = fileContent.split("\n");
    String version = "1";
    String label = null;
    int index = 0;
    if (lines[index].startsWith("Model=")) {
      label = lines[index].substring("Model=".length());
      index++;
    }
    if (lines[index].startsWith("Version=")) {
      version = lines[index].substring("Version=".length());
      index++;
    }
    for (int i=index; i < lines.length; i++) {
      String line = lines[i];
      String[] keyValueSplit = line.split("=");
      if (keyValueSplit.length > 1) {
        String[] accessPath = keyValueSplit[0].split("\\.");
        if (model == null) {
          model = new TRModel(this, accessPath[0] + ":" + version);
          model.setLabel(label);
        }
        parseAccessPath(model, accessPath);
      }
    }
  }
  
  
  private void parseAccessPath(TRModel model, String[] accessParts) {
    parseAccessPathRecursivly(model, accessParts, 0);
  }
  
  
  private void parseAccessPathRecursivly(TRObjectContainer container, String[] accessParts, int accessIndex) {
    String fqName = joinUpTo(accessParts, accessIndex) + ".";
    String[] prunedParts = accessParts;
    boolean isList;
    try {
      Integer.parseInt(accessParts[accessIndex+1]);
      isList = true;
      prunedParts=prune(accessParts, accessIndex+1);
    } catch (NumberFormatException e) {
      isList = false;
    }
    TRObject obj = container.getObject(fqName);
    if (obj == null) {
      obj = new TRObject(container, fqName);
      if (!container.getName().equals(obj.getFqName())) {
        container.addObject(obj);
      }
      if (isList) {
        obj.markAsListInParent();
      }
    }
    
    if (accessIndex+1 >= prunedParts.length-1) {
      parseAccessPathLeaf(obj, prunedParts, accessIndex+1);
    } else {
      parseAccessPathRecursivly(container, prunedParts, accessIndex+1);
    }
  }
  
  
  private void parseAccessPathLeaf(TRObject parent, String[] accessParts, int accessIndex) {
    TRParameter param = new TRParameter(parent, accessParts[accessIndex]);
    if (parent.getParameter(param.getName()) == null) {
      param.setType("string");
      parent.addParameter(param);
    }
  }
  
  private String joinUpTo(String[] parts, int index) {
    if (index >= parts.length - 1) {
      return StringUtils.joinStringArray(parts, ".");
    } else {
      String[] subArray = new String[index+1];
      System.arraycopy(parts, 0, subArray, 0, index+1);
      return StringUtils.joinStringArray(subArray, ".");
    }
  }
  
  private String[] prune(String[] parts, int index) {
    String[] subArray = new String[parts.length-1];
    System.arraycopy(parts, 0, subArray, 0, index);
    System.arraycopy(parts, index+1, subArray, index, parts.length-(index+1));
    return subArray;
  }


  public TRComponent getComponent(String name) {
    return null;
  }


  public Collection<TRComponent> getComponents() {
    return Collections.emptySet();
  }


  public TRModel getModelByNameOrSynonym(String fqName) {
    return null;
  }


  
}
