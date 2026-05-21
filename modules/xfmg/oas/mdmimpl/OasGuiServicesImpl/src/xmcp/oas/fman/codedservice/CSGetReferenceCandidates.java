/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xmcp.oas.fman.codedservice;

import java.util.List;

import com.gip.xyna.xprc.XynaOrderServerExtension;

import base.KeyValue;
import xmcp.oas.fman.datatypes.EndpointImplementationCreationData;

public class CSGetReferenceCandidates {

  public List<? extends KeyValue> execute(XynaOrderServerExtension order, EndpointImplementationCreationData data) {
    List<KeyValue> result = null;
    String dtFqn = data.getImplementationDatatypeFqn();
    String opName = data.getServiceName();
    Long revision = data.getImplementationRtcRevision();
    try {
      result = FilterCallbackInteractionUtils.getReferenceCanddates(order, dtFqn, opName, revision);
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
    
    return result;
  }
}
