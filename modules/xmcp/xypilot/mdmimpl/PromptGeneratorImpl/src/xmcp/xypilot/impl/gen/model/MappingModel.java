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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;

import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.impl.gen.util.StringUtils;

/**
 * A data model providing a StepMapping object.
 * It also provides a list of dependencies,
 * i.e. the DomOrExceptionGenerationBase objects that are used by the mapping as input or output.
 * The dependencies can be used to include api references in the templates to give the ai model context about the mapped objects.
 *
 * The mapping model alters the names of the input and output variables to make them more readable, and thus more easily to use by the ai model.
 */
public class MappingModel extends BaseModel {

    private static Logger logger = Logger.getLogger("XyPilot");

    private StepMapping mapping;
    private List<DomOrExceptionGenerationBase> dependencies;


    public MappingModel(StepMapping mapping) throws XynaException {
        // ------------------ mapping ------------------
        this.mapping = mapping;
        makeVariableNamesReadable(this.mapping.getInputVars());
        makeVariableNamesReadable(this.mapping.getOutputVars());

        // ------------------ dependencies ------------------

        Map<String, DomOrExceptionGenerationBase> dependencyMap = createDependencyMap();

        dependencies = dependencyMap.values().stream().collect(Collectors.toList());
        for (int i = 0; i < dependencies.size(); i++) {
            dependencies.set(i, XynaFactory.getInstance().parseGeneration(dependencies.get(i)));
        }

        getUtils().getImportHandler().addImports(dependencyMap.keySet());
    }


    public StepMapping getMapping() {
        return mapping;
    }


    public List<DomOrExceptionGenerationBase> getDependencies() {
        return dependencies;
    }


    /**
     * Changes the names of the given variables to more readable names.
     * The initial variable names are obtained from the labels of the variables.
     * The names are then made unique by appending "_2", "_3", ... to the names if necessary.
     * @param vars
     * @return
     */
    private void makeVariableNamesReadable(List<AVariable> vars) {
        List<String> varNames = vars.stream()
            .map(var -> StringUtils.toIdentifier(var.getLabel()))
            .collect(Collectors.toList());

        for (int i = 0; i < varNames.size(); i++) {
            // count the number of occurrences of the current variable name
            long count = varNames.subList(0, i).stream().filter(varNames.get(i)::equals).count();

            // if there is more than one occurrence, append _2, _3, ...
            if (count > 0) {
                vars.get(i).setVarName(varNames.get(i) + "_" + (count + 1));
            } else {
                vars.get(i).setVarName(varNames.get(i));
            }
        }
    }


    /**
     * Collects the FQNs of types from the Input and Output.
     *
     * @param mapping
     * @return Map where FQNs are the keys and the values are the corresponding
     *         doms. Note that the doms are not parsed yet
     */
    private Map<String, DomOrExceptionGenerationBase> createDependencyMap() {
        Map<String, DomOrExceptionGenerationBase> dependencies = new HashMap<>();

        for (AVariable var : mapping.getInputVars()) {
            logger.debug("fqn: " + var.getFQClassName() + "\nrev: " + var.getRevision());
            if (!dependencies.containsKey(var.getFQClassName())) {
                dependencies.put(var.getFQClassName(), var.getDomOrExceptionObject());
            }
        }
        for (AVariable var : mapping.getOutputVars()) {
            if (!dependencies.containsKey(var.getFQClassName())) {
                dependencies.put(var.getFQClassName(), var.getDomOrExceptionObject());
            }
        }

        return dependencies;
    }

}
