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
package xmcp.xypilot.impl.gen.model;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;

import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.impl.factory.XynaFactoryFacade;

import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;


/**
 * A data model providing a DOM object and a target method.
 * This model can be used to generate prompts targeting a specific method, e.g. for documentation or implementation.
 *
 * It also provides a list of DomOrExceptionGenerationBase objects, that are used by the target method as input, output or thrown exceptions.
 * This is different from the dependencies of the DomOrExceptionModel, because it only contains the dependencies of the target method.
 *
 * @see DomModel
 */
public class DomMethodModel extends DomModel {

    private Operation targetMethod;

    private List<DomOrExceptionGenerationBase> targetMethodDependencies = new ArrayList<>();

    private String latestImplementation = null;

    public DomMethodModel(DOM dom, Operation targetMethod) throws XynaException {
        super(dom);
        this.targetMethod = targetMethod;

        // dependencies---------------------------------------------------
        targetMethod.getInputVars().stream().forEach(this::addDependencyForVariable);
        targetMethod.getOutputVars().stream().forEach(this::addDependencyForVariable);
        targetMethod.getThrownExceptions().stream().forEach(this::addDependencyForVariable);
    }

    private void addDependencyForVariable(AVariable var) {
        boolean isAlreadyAdded = this.targetMethodDependencies.stream().anyMatch(
            exisitingDependency -> exisitingDependency.getFqClassName().equals(var.getFQClassName())
        );
        if (!isAlreadyAdded) {
            XynaFactoryFacade factory = XynaFactory.getInstance();
            DomOrExceptionGenerationBase dependency = var.getDomOrExceptionObject();
            try {
                dependency = factory.parseGeneration(dependency);
                this.targetMethodDependencies.add(dependency);
            } catch (XynaException e) {
            }
        }
    }

    public void setLatestImplementation(String latestImplementation) {
        this.latestImplementation = latestImplementation;
    }

    public String getLatestImplementation() {
        return latestImplementation;
    }

    public Operation getTargetMethod() {
        return targetMethod;
    }

    public List<DomOrExceptionGenerationBase> getTargetMethodDependencies() {
        return targetMethodDependencies;
    }

}
