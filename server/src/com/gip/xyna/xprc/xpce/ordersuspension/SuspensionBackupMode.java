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
package com.gip.xyna.xprc.xpce.ordersuspension;

import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils;

public enum SuspensionBackupMode {
  BACKUP(true),
  NO_BACKUP(false);
  
  public final static XynaPropertyBuilds<SuspensionBackupMode> DEFAULT_ORDERBACKUP_MODE =
                  new XynaPropertyBuilds<SuspensionBackupMode>("xprc.xpce.ordersuspension.defaultbackupmode", new XynaPropertyUtils.XynaPropertyBuilds.Builder<SuspensionBackupMode>() {

                    public SuspensionBackupMode fromString(String string)
                                    throws com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds.Builder.ParsingException {
                      return SuspensionBackupMode.valueOf(string);
                    }

                    public String toString(SuspensionBackupMode value) {
                      return value.toString();
                    }
                    
                  }, SuspensionBackupMode.BACKUP).setDefaultDocumentation(DocumentationLanguage.EN, "Possible values are \"BACKUP\" and \"NO_BACKUP\".");
  
  private final boolean doBackup;
  
  private SuspensionBackupMode(boolean doBackup) {
    this.doBackup = doBackup;
  }
  
  public boolean doBackup() {
    return doBackup;
  }

  public static SuspensionBackupMode combine(SuspensionBackupMode orderBackupMode, SuspensionBackupMode otherOrderBackupMode) {
    if (orderBackupMode == BACKUP || otherOrderBackupMode == BACKUP) {
      return BACKUP;
    } else {
      return NO_BACKUP;
    }
  }
  
}
