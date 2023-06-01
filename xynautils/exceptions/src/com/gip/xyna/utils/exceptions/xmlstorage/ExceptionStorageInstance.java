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
package com.gip.xyna.utils.exceptions.xmlstorage;



import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.utils.exceptions.utils.codegen.JavaClass;




public abstract class ExceptionStorageInstance {

  private List<ExceptionEntry> entries;
  private List<ExceptionStorageInstance> includes;
  private List<String> includedFiles;
  private String xmlFile;
  private String defaultLanguage;
  protected boolean skipGettersAndSetters = false;

  public ExceptionStorageInstance() {
    entries = new ArrayList<ExceptionEntry>(1);
    includes = new ArrayList<ExceptionStorageInstance>(0);
    includedFiles = new ArrayList<String>(0);
  }
  
  public void skipGettersAndSetters() {
    skipGettersAndSetters = true;
  }
  
  public String getDefaultLanguage() {
    return defaultLanguage;
  }
  
  public void setDefaultLanguage(String defaultLanguage) {
    this.defaultLanguage = defaultLanguage;
  }
  
  public void addEntry(ExceptionEntry entry) {
    entries.add(entry);
  }
  
  public void addIncludedFile(String fileName) {
    includedFiles.add(fileName);
  }
  
  public void addInclude(ExceptionStorageInstance include) {
    includes.add(include);
  }

  public List<ExceptionStorageInstance> getIncludes() {
    return includes;
  }
  
  public List<String> getIncludedFileNames() {
    return includedFiles;
  }

  public List<ExceptionEntry> getEntries() {
    return entries;
  }
  
  /**
   * erstellt javaklassen für alle exceptioninformationen dieser instanz (ohne includes)
   * @param loadFromResource 
   * @return
   * @throws Exception 
   */
  public abstract JavaClass[] generateJavaClasses(boolean loadFromResource, ExceptionEntryProvider provider, String xmlFile) throws InvalidValuesInXMLException;
  
  /**
   * die exceptioninformationen aller includes werden rekursiv zu dieser instanz hinzugefügt.
   * @return
   */
  public void mergeWithIncludes() {
    List<ExceptionStorageInstance> collection = new ArrayList<ExceptionStorageInstance>();
    //this nicht adden
    for (ExceptionStorageInstance esi : includes) {
      esi.addIncludesToListRecursively(collection);
    }
    for (ExceptionStorageInstance esi : collection) {
      entries.addAll(esi.getEntries());
    }
  }
  
  private void addIncludesToListRecursively(List<ExceptionStorageInstance> collection) {
    collection.add(this);
    for (ExceptionStorageInstance esi : includes) {
      esi.addIncludesToListRecursively(collection);
    }
  }
  
  public void setXmlFile(String xmlFile) {
    this.xmlFile = xmlFile;
  }

  public String getXmlFile() {
    return xmlFile;
  }
  
}
