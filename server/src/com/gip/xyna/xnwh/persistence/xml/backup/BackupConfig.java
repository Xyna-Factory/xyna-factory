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

package com.gip.xyna.xnwh.persistence.xml.backup;

import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;


public class BackupConfig {

  public static final String BACKUP_SUFFIX = ".bkup";
  public static final String SYSTEM_PROPERTY_NAME_FOR_ENABLED = "xnwh.persistence.xml.backup.enabled";

  public static class XynaProperty {
       
    public static final XynaPropertyInt BACKUP_FILE_MIN_LIFETIME =
                        new XynaPropertyInt("xnwh.persistence.xml.backup.file.min.lifetime.sec", 30)
                        .setDefaultDocumentation(DocumentationLanguage.EN,
                        "Minimal time in seconds before new backup files can be deleted again.");
  }
  
}
