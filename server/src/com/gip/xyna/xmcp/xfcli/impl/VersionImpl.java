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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;

import com.gip.xyna.update.Updater;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Version;



public class VersionImpl extends XynaCommandImplementation<Version> {

  public void execute(OutputStream statusOutputStream, Version payload) throws XynaException {

    com.gip.xyna.update.Version v = Updater.getInstance().getFactoryVersion();
    String factoryVersion = "Xyna Factory Server version: " + v.getString();
    writeLineToCommandLine(statusOutputStream, factoryVersion);
    com.gip.xyna.update.Version u = Updater.getInstance().getVersionOfLastSuccessfulUpdate();
    if (!v.equals(u)) {
      String lastSuccessfulUpdate = "Last successful Update: " + u.getString();
      writeLineToCommandLine(statusOutputStream, lastSuccessfulUpdate);
    }

    String xmomVersion = "Xyna Factory XMOM version: " + Updater.getInstance().getXMOMVersion().getString();
    writeLineToCommandLine(statusOutputStream, xmomVersion);

  }

}
