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

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Globaloverwritedomain;



public class GlobaloverwritedomainImpl extends XynaCommandImplementation<Globaloverwritedomain> {

  public void execute(OutputStream statusOutputStream, Globaloverwritedomain payload) throws XynaException {
    //just set the XynaProperty
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < payload.getDomainName().length; i++) {
      sb.append(payload.getDomainName()[i]);
      if (i < payload.getDomainName().length - 1) {
        sb.append(",");
      }
    }
    String domainNameConcat = sb.toString();
    factory.getXynaMultiChannelPortalPortal().setProperty(XynaProperty.GLOBAL_DOMAIN_OVERWRITE, domainNameConcat);
  }

}
