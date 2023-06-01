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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.File;
import java.io.OutputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Cleanupxmomrepository;



public class CleanupxmomrepositoryImpl extends XynaCommandImplementation<Cleanupxmomrepository> {

  private static final XynaPropertyString tmpdir = new XynaPropertyString("xdev.xlibdev.repmgmt.backup.dir", "/tmp/xmomrepositorybackup").setDefaultDocumentation(DocumentationLanguage.EN, "Directory where backups are created by cli command cleanupxmomrepository.");
  
  public void execute(OutputStream statusOutputStream, Cleanupxmomrepository payload) throws XynaException {
    long age;
    try {
    age = Long.valueOf(payload.getAgeInDays());
    } catch (NumberFormatException e) {
      writeLineToCommandLine(statusOutputStream, "Invalid age: " + e.getMessage());
      return;
    }
    if (age < 0 || age >= 1000000) {
      writeLineToCommandLine(statusOutputStream, "Invalid age. must be positive and smaller than 1e6");
      return;
    }
    age *= 86400000;//tage    
    if (!payload.getNoBackup()) {
      writeLineToCommandLine(statusOutputStream, "Creating Backup of xmomrepository @ " + tmpdir.get());
      XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getRepositoryManagement().backup(new File(tmpdir.get()));
    }
    writeLineToCommandLine(statusOutputStream, "Searching for Files to delete ...");
    XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getRepositoryManagement().cleanupRepositories(System.currentTimeMillis() - age);
    writeLineToCommandLine(statusOutputStream, "Finished.");
  }

}
