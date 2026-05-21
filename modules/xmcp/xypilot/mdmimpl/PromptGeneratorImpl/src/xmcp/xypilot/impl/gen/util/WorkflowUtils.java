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
package xmcp.xypilot.impl.gen.util;

import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.WF;

public class WorkflowUtils {

    private static Logger logger = Logger.getLogger("XyPilot");


    /**
     * Recursively tries to find the step with the given ID.
     *
     * @param step
     * @param targetId
     * @return
     */
    public static Step findStep(Step step, String targetId) {
        try {
            logger.debug("Test step " + step.getStepId());
            if (step.getStepId() != null && targetId.equals("step" + step.getStepId())) {
                return step;
            }

            List<Step> childSteps = step.getChildSteps();
            if (childSteps == null) {
                return null;
            }
            logger.debug("Try find target step in one of the " + childSteps.size() + " childsteps of step "
                    + step.getStepId());
            for (Step childStep : childSteps) {
                Step res = findStep(childStep, targetId);
                if (res != null) {
                    return res;
                }
            }
        } catch (Throwable e) {
            logger.debug(e);
        }
        return null;
    }


    /**
     * Recursively tries to find the mapping with the given ID.
     *
     * @param wf
     * @param stepId
     * @return
     */
    public static StepMapping findStepMapping(WF wf, String stepId) {
        ScopeStep asStep = wf.getWfAsStep();
        if (asStep == null) {
            logger.warn("WF.asStep was null");
            return null;
        }

        Step target = findStep(asStep, stepId);
        if (target == null) {
            logger.warn("coudln't find target step");
            return null;
        }
        if (!(target instanceof StepMapping)) {
            logger.warn("Target is not mapping. Got " + target.getClass().getName());
            return null;
        }
        return (StepMapping) target;
    }

}
