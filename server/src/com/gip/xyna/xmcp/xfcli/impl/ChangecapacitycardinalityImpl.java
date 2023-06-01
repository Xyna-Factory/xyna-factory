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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.OutputStream;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Changecapacitycardinality;



public class ChangecapacitycardinalityImpl extends XynaCommandImplementation<Changecapacitycardinality> {

  public void execute(OutputStream statusOutputStream, Changecapacitycardinality payload) throws XynaException {
    String name = payload.getName();
    Integer newCardinality = null;
    try {
      newCardinality = Integer.valueOf(payload.getNewCardinality());
    } catch (NumberFormatException e) {
      writeLineToCommandLine(statusOutputStream, "Could not change cardinality to '" + payload.getNewCardinality()
          + "' for capacity '" + name + "', could not parse value.");
      return;
    }

    boolean result = factory.getProcessingPortal().changeCapacityCardinality(name, newCardinality);

    if (result) {
      writeLineToCommandLine(statusOutputStream, "Successfully changed cardinality for capacity '" + name + "' to '"
          + newCardinality + "'.");
    } else {
      writeLineToCommandLine(statusOutputStream, "Could not change cardinality for capacity '" + name + "' to '"
          + newCardinality + "'.");
    }
  }

}
