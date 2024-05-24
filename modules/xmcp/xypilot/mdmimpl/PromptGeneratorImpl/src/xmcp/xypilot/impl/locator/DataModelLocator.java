/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xmcp.xypilot.impl.locator;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;

import xmcp.xypilot.MemberReference;
import xmcp.xypilot.XMOMItemReference;
import xmcp.xypilot.impl.gen.model.DomModel;
import xmcp.xypilot.impl.gen.model.ExceptionModel;
import xmcp.xypilot.impl.gen.model.ExceptionVariableModel;
import xmcp.xypilot.impl.gen.model.MappingModel;
import xmcp.xypilot.impl.gen.model.DomVariableModel;
import xmcp.xypilot.impl.gen.model.DomMethodModel;
import xmcp.xypilot.impl.gen.util.DomUtils;
import xmcp.xypilot.impl.gen.util.FilterCallbackInteractionUtils;

public class DataModelLocator {

    public static MappingModel getMappingModel(MemberReference memberReference) throws XynaException {
        StepMapping targetMapping = GenerationBaseObjectLocator.getMapping(memberReference);
        return new MappingModel(targetMapping);
    }

    public static DomModel getDomModel(XMOMItemReference xmomItemReference, XynaOrderServerExtension order) throws XynaException {
      DOM dom = FilterCallbackInteractionUtils.getDatatypeDom(xmomItemReference, order);
      return new DomModel(dom);
    }

    public static ExceptionModel getExceptionModel(XMOMItemReference xmomItemReference, XynaOrderServerExtension order) throws XynaException {
      ExceptionGeneration exception = FilterCallbackInteractionUtils.getException(xmomItemReference, order);
      return new ExceptionModel(exception);
    }

    public static DomMethodModel getDomMethodModel(MemberReference memberReference, XynaOrderServerExtension order) throws XynaException {
      DOM dom = FilterCallbackInteractionUtils.getDatatypeDom(memberReference.getItem(), order);
      Operation targetMethod = dom.getOperationByName(memberReference.getMember());
      return new DomMethodModel(dom, targetMethod);
    }

    public static DomVariableModel getDomVariableModel(MemberReference memberReference, XynaOrderServerExtension order) throws XynaException {
      DOM dom = FilterCallbackInteractionUtils.getDatatypeDom(memberReference.getItem(), order);
      AVariable targetVariable = DomUtils.getVariableByName(dom, memberReference.getMember());
      return new DomVariableModel(dom, targetVariable);
    }

    public static ExceptionVariableModel getExceptionVariableModel(MemberReference memberReference, XynaOrderServerExtension order) throws XynaException {
        ExceptionGeneration exception = FilterCallbackInteractionUtils.getException(memberReference.getItem(), order);
        AVariable targetVariable = DomUtils.getVariableByName(exception, memberReference.getMember());
        return new ExceptionVariableModel(exception, targetVariable);
    }
}
