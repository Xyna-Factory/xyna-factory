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
package xmcp.oas.fman.codedservice.parameter;

import java.util.List;

import com.gip.xyna.xprc.XynaOrderServerExtension;

public class CreateImplWfParameter {

  public XynaOrderServerExtension order;
  public String wfLabel;
  public String wfPath;
  public String parentFqn;
  public Long revision;
  public String serviceName;
  public List<SignatureVariable> inputs;
  public List<SignatureVariable> outputs;


  public CreateImplWfParameter(XynaOrderServerExtension order, String label, String path, String parentFqn, Long revision,
                               String serviceName, List<SignatureVariable> inputs, List<SignatureVariable> outputs) {
    this.order = order;
    this.wfLabel = label;
    this.wfPath = path;
    this.parentFqn = parentFqn;
    this.revision = revision;
    this.serviceName = serviceName;
    this.inputs = inputs;
    this.outputs = outputs;
  }
  
  public static class SignatureVariable {
    public String fqn;
    public String label;
    
    public SignatureVariable(String fqn, String label) {
      this.fqn = fqn;
      this.label = label;
    }
  }
}