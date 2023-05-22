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
import java.io.FileFilter;
import java.util.Collection;
import java.util.Set;


import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xfmg.xfctrl.datamodel.tr069.DataModelParseContext;
import com.gip.xyna.xfmg.xfctrl.datamodel.tr069.TR069Tools;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;


public abstract class TRDataModelDefinition {
  
  private final static String ACCESS_PATH_LIST_FILE_SUFFIX = ".apl";
  
  private static final FileFilter DATA_MODEL_DEFINITION_FILTER = new FileFilter() {
    
    public boolean accept(File pathname) {
      if( pathname.isDirectory() ) {
        return true;
      }
      String name = pathname.getName().toLowerCase();
      if( name.endsWith(".xml") || name.endsWith(ACCESS_PATH_LIST_FILE_SUFFIX)) {
        return true;
      }
      return false;
    }
    
  };

  protected String name;
  protected String fileName;
  
  protected TRDataModelDefinition(File file) {
    this.fileName = file.getName();
    this.name = TR069Tools.createReference(fileName);
  }
  
  public String getName() {
    return name;
  }
  
  public String getFileName() {
    return fileName;
  }
  
  
  public static FileFilter getDataDefinitionAcceptor() {
    return DATA_MODEL_DEFINITION_FILTER;
  }
  

  public static TRDataModelDefinition instantiateModelDefinition(File file) throws XPRC_XmlParsingException, Ex_FileAccessException {
    if (file.getName().endsWith(ACCESS_PATH_LIST_FILE_SUFFIX)) {
      return new TRAccessPathList(file);
    } else {
      return new TRDocument(file);
    }
  }

  public abstract Set<String> getImportReferences();

  public abstract void parseImports();

  public abstract void parse(DataModelParseContext context);

  public abstract Collection<TRModel> getModels();

  public abstract TRComponent getComponent(String name);

  public abstract Collection<TRComponent> getComponents();

  public abstract TRModel getModelByNameOrSynonym(String fqName);

  
}
