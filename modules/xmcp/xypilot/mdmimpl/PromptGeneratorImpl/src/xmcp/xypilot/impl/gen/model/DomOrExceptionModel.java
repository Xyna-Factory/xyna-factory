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
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;

import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.impl.factory.XynaFactoryFacade;
import xmcp.xypilot.impl.gen.util.TypeUtils;
import xmcp.xypilot.impl.gen.util.TypeUtils.ReferenceType;

/**
 * A data model providing a DomOrExceptionGenerationBase object.
 * It also provides a list of available types for variables, parameters and exceptions,
 * as well as a list of direct dependencies.
 * The dependencies can be used to include api references in the templates to give the ai model context beyond the current object.
 * There are also more specific models for DOMs and Exceptions.
 *
 * @see DomModel
 * @see ExceptionModel
 */
public class DomOrExceptionModel extends BaseModel {

    private static Logger logger = Logger.getLogger("XyPilot");

    private Class<DOM> DOM = DOM.class;
    private DomOrExceptionGenerationBase domOrException;
    private List<String> availableVariableTypes = new ArrayList<String>();
    private List<String> availableReferenceTypes = new ArrayList<String>();
    private List<String> availableParameterTypes = new ArrayList<String>();
    private List<String> availableExceptionTypes = new ArrayList<String>();

    private List<DomOrExceptionGenerationBase> dependencies = new ArrayList<DomOrExceptionGenerationBase>();

    // the request can pass the latest documentation which may not be in the saved state of the domOrException
    private String latestDocumentation = null;

    public DomOrExceptionModel(DomOrExceptionGenerationBase domOrException) throws XynaException {
        super();
        getUtils().getImportHandler().addImport(domOrException.getFqClassName());

        this.domOrException = domOrException;

        // available types------------------------------------------------

        // base path is everything except the last part of the fqClassName
        String basePath = domOrException.getFqClassName().substring(0, domOrException.getFqClassName().lastIndexOf("."));
        logger.debug("Only accept subTypes that start with " + basePath + "\n");

        Set<String> referenceTypesInPath = fqnsInPath(
            basePath,
            TypeUtils.getSubTypes(domOrException.getRevision())
        );

        Set<String> datatypesInPath = fqnsInPath(
            basePath,
            TypeUtils.getSubDataTypes(domOrException.getRevision())
        );

        Set<String> exceptionTypesInPath = fqnsInPath(
            basePath,
            TypeUtils.getSubExceptionTypes(domOrException.getRevision())
        );

        this.availableVariableTypes.addAll(datatypesInPath);
        this.availableVariableTypes.addAll(TypeUtils.JAVA_PRIMITIVES);

        this.availableParameterTypes.addAll(datatypesInPath);
        this.availableExceptionTypes.addAll(exceptionTypesInPath);

        this.availableReferenceTypes.addAll(referenceTypesInPath);

        // dependencies---------------------------------------------------
        domOrException.getDirectlyDependentObjects().stream()
            .filter(dependency -> !dependency.getFqClassName().equals(domOrException.getFqClassName()))
            .filter(dependency -> dependency instanceof DomOrExceptionGenerationBase)
            .map(dependency -> (DomOrExceptionGenerationBase) dependency)
            .forEach(this::addDependency);

    }

    private void addDependency(DomOrExceptionGenerationBase dependency) {
        XynaFactoryFacade factory = XynaFactory.getInstance();
        try {
            dependency = factory.parseGeneration(dependency);
            this.dependencies.add(dependency);
        } catch (XynaException e) {
            // ignore
        }
    }

    private Set<String> fqnsInPath(String targetPath, Set<ReferenceType> types) {
        return types.stream()
                .map(ReferenceType::getFqn)
                .filter(fqn -> fqn.startsWith(targetPath))
                .collect(Collectors.toSet());
    }

    public void setLatestDocumentation(String latestDocumentation) {
        this.latestDocumentation = latestDocumentation;
    }

    public String getLatestDocumentation() {
        return latestDocumentation;
    }

    public Class<DOM> getDOM() {
        return DOM;
    }

    public DomOrExceptionGenerationBase getDomOrException() {
        return domOrException;
    }

    public List<String> getAvailableVariableTypes() {
        return availableVariableTypes;
    }

    public List<String> getAvailableParameterTypes() {
        return availableParameterTypes;
    }

    public List<String> getAvailableExceptionTypes() {
        return availableExceptionTypes;
    }

    public List<String> getAvailableReferenceTypes() {
        return availableReferenceTypes;
    }

    public List<DomOrExceptionGenerationBase> getDependencies() {
        return dependencies;
    }

}
