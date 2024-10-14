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
package com.gip.xyna.xfmg.xfctrl.appmgmt.events;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.gip.xyna.update.Version;
import com.gip.xyna.utils.misc.EventHandler;
import com.gip.xyna.utils.streams.StreamUtils;

public abstract class AppMgmtEventHandler implements EventHandler<AppMgmtEvent> {
  
  private final static String ADDITIONAL_FILE_PREFIX = "AppMgmtAdd_";
  
  
  public void handleEvent(AppMgmtEvent event) {
    if (event instanceof ApplicationExportEvent) {
      handleExport((ApplicationExportEvent) event);
    } else if (event instanceof ApplicationImportEvent) {
      handleImport((ApplicationImportEvent) event);
    } 
    
  }
  
  
  private void handleImport(ApplicationImportEvent event) {
    receiveAdditionalFiles(event.getName(), event.getVersion(), event.getAdditionalFiles());
  }


  private void handleExport(ApplicationExportEvent event) {
    List<AdditionalData> exports = getAdditionalExports(event.getName(), event.getVersion());
    if (exports != null &&
        exports.size() > 0) {
      for (AdditionalData export : exports) {
        ZipOutputStream zos = event.getZos();
        ZipEntry entry = new ZipEntry(ADDITIONAL_FILE_PREFIX + export.name);
        try {
          zos.putNextEntry(entry);
          StreamUtils.copy(export.getContentStream(), zos);
        } catch (IOException e) {
          handleFileException(e);
        }
      }
    }
  }
  
  public Version getVersion() {
    return new Version(getVersionName());
  }
  
  protected abstract String getVersionName();

  public abstract void receiveAdditionalFiles(String appName, String version, List<AdditionalFile> additionalFiles);

  public abstract List<AdditionalData> getAdditionalExports(String appName, String version);
  
  public abstract void handleFileException(IOException e);
  
  
  public static List<AdditionalFile> filterToAdditionalData(File[] allFiles) {
    List<AdditionalFile> additionalFiles = new ArrayList<>();
    for (File file : allFiles) {
      if (file.getName().startsWith(ADDITIONAL_FILE_PREFIX)) {
        String originalName = file.getName().substring(ADDITIONAL_FILE_PREFIX.length());
        additionalFiles.add(new AdditionalFile(originalName, file));
      }
    }
    return additionalFiles;
  }
  
  
  public static class AdditionalData {
    
    private final String name;
    private final InputStream content;
    
    public AdditionalData(String name, InputStream content) {
      this.name = name;
      this.content = content;
    }
    
    
    public String getName() {
      return name;
    } 
    
    public InputStream getContentStream() {
      return content;
    } 
    
  }
  
  
  public static class AdditionalFile {
    
    private final String name;
    private final File file;
    
    public AdditionalFile(String name, File file) {
      this.name = name;
      this.file = file;
    }
    
    
    public String getName() {
      return name;
    } 
    
    public File getFile() {
      return file;
    } 
    
  }

}
