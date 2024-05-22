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
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.WF;

import xmcp.xypilot.MemberReference;
import xmcp.xypilot.XMOMItemReference;
import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.impl.gen.util.FilterCallbackInteractionUtils;
import xmcp.xypilot.impl.gen.util.WorkflowUtils;

public class GenerationBaseObjectLocator {

    public static StepMapping getMapping(MemberReference memberReference) throws XynaException {
        // get WF instance
        WF wf = XynaFactory.getInstance().getWorkflow(
            memberReference.getItem().getFqName(),
            FilterCallbackInteractionUtils.getRevision(memberReference.getItem()),
            false
        );

        // find target mapping from stepId
        String stepId = memberReference.getMember();
        return WorkflowUtils.findStepMapping(wf, stepId);
    }


    public static DomOrExceptionGenerationBase getDomOrException(XMOMItemReference xmomItemReference) throws XynaException {
        return XynaFactory.getInstance().getDomOrExceptionGenerationBase(
            xmomItemReference.getFqName(),
            FilterCallbackInteractionUtils.getRevision(xmomItemReference),
            false
        );
    }

    public static ExceptionGeneration getException(XMOMItemReference xmomItemReference) throws XynaException {
        return XynaFactory.getInstance().getException(
            xmomItemReference.getFqName(),
            FilterCallbackInteractionUtils.getRevision(xmomItemReference),
            false
        );
    }
}
