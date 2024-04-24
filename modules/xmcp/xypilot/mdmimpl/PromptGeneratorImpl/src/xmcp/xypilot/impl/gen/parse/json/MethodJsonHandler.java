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
package xmcp.xypilot.impl.gen.parse.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;

import xmcp.xypilot.MethodDefinition;
import xmcp.xypilot.Parameter;
import xmcp.xypilot.impl.gen.util.ParserUtils;
import xmcp.xypilot.impl.gen.util.StringUtils;
import xmcp.xypilot.impl.gen.util.TypeUtils;


public class MethodJsonHandler implements JsonLineHandler {
    // the parameter type to use if no other type matches
    private static final String DEFAULT_TYPE = "base.Text";
    // the exception type to use if no other type matches
    private static final String DEFAULT_EXCEPTION = "base.core.Exception";

    private static final List<String> OBJECT_METHODS = Arrays.asList(
        "equals",
        "hashCode",
        "toString",
        "getClass",
        "notify",
        "notifyAll",
        "wait"
    );

    private static enum ObjectType {
        METHOD,
        PARAMETERS
    };

    private static enum ParameterType {
        INPUT,
        OUTPUT,
        EXCEPTION
    }

    private Parameter parameter;
    private ParameterType parameterType = ParameterType.INPUT;
    private MethodDefinition method;
    private ObjectType objectType = ObjectType.METHOD;
    private ArrayList<MethodDefinition> methods = new ArrayList<>();

    private Set<String> availableParameterTypes;
    private Set<String> availableExceptionTypes;
    private DOM dom;

    private Map<String, String> parameterTypeToFqn;
    private Map<String, String> exceptionTypeToFqn;


    public MethodJsonHandler(DOM dom, List<String> availableParameterTypes, List<String> availableExceptionTypes) {
        this.dom = dom;
        this.availableParameterTypes = new HashSet<>(availableParameterTypes);
        this.availableExceptionTypes = new HashSet<>(availableExceptionTypes);
        this.parameterTypeToFqn = TypeUtils.simpleToFqnMap(this.availableParameterTypes);
        this.exceptionTypeToFqn = TypeUtils.simpleToFqnMap(this.availableExceptionTypes);
    }


    public List<MethodDefinition> getMethods() {
        return methods;
    }


    @Override
    public void objectStart(Optional<String> key) {
        addObjectIfValid(); // in case last object not closed by }
        if (!key.isPresent()) { // no key means this is an object in an array or a root object
            createNextObject();
        }
    }


    @Override
    public void objectEnd() {
        addObjectIfValid();
    }


    @Override
    public void arrayStart(Optional<String> key) {
        if (key.isPresent()) {
            try {
                objectType = ObjectType.valueOf(key.get().toUpperCase());
            } catch (IllegalArgumentException e) {
                objectType = null;
            }
        }
    }


    @Override
    public void arrayEnd() {
        addObjectIfValid(); // in case last object not closed by }

        if (objectType == null) {
            objectType = ObjectType.METHOD;
        } else {
            switch (objectType) {
                case METHOD:
                    // ignore, still assert next objects are methods
                    break;
                case PARAMETERS:
                    // parameter list ends -> back up to method
                    objectType = ObjectType.METHOD;
                    break;
            }
        }
    }


    @Override
    public void property(String key, String value) {
        if (objectType != null) {
            switch (objectType) {
                case METHOD:
                    handleMethodProperty(key, value);
                    break;
                case PARAMETERS:
                    handleParameterProperty(key, value);
                    break;
            }
        }
    }


    @Override
    public void endParsing() {
        // in case last objects not closed
        addParameterIfValid();
        addMethodIfValid();
    }


    private void handleMethodProperty(String key, String value) {
        if (method != null) {
            switch (key) {
                case "name":
                    method.setName(ParserUtils.getQuoted(value));
                    break;
                case "documentation":
                    method.setDocumentation(
                        ParserUtils.unescapeString(ParserUtils.getQuoted(value))
                    );
                    break;
            }
        }
    }


    private void handleParameterProperty(String key, String value) {
        if (parameter != null) {
            switch (key) {
                case "name":
                    parameter.setName(ParserUtils.getQuoted(value));
                    break;
                case "fqn":
                    parameter.setType(ParserUtils.getQuoted(value));
                    break;
                case "isList":
                    parameter.setIsList(Boolean.parseBoolean(value));
                    break;
                case "type":
                    try {
                        parameterType = ParameterType.valueOf(ParserUtils.getQuoted(value).toUpperCase());
                    } catch (IllegalArgumentException e) {
                        parameterType = ParameterType.INPUT;
                    }
                    break;
            }
        }
    }


    private void createNextObject() {
        if (objectType != null) {
            switch (objectType) {
                case METHOD:
                    createNextMethod();
                    break;
                case PARAMETERS:
                    createNextParameter();
                    break;
            }
        }
    }


    private void addObjectIfValid() {
        if (objectType != null) {
            switch (objectType) {
                case METHOD:
                    addMethodIfValid();
                    break;
                case PARAMETERS:
                    addParameterIfValid();
                    break;
            }
        }
    }


    private void createNextMethod() {
        method = new MethodDefinition();
        method.setInputParams(new ArrayList<>());
        method.setOutputParams(new ArrayList<>());
        method.setThrowParams(new ArrayList<>());
    }


    private void createNextParameter() {
        parameter = new Parameter();
        parameterType = ParameterType.INPUT;
        parameter.setType(DEFAULT_TYPE);
    }


    private void addMethodIfValid() {
        // requires at least a valid name to be included in the list of methods
        if (method != null && method.getName() != null && !method.getName().isEmpty()
                && !isGetterOrSetter(method) && !isObjectMethod(method) && !hasEquivalentMethod(method)) {
            methods.add(method);
        }
        method = null;
    }


    private void addParameterIfValid() {
        // requires at least a fqn to be included in the list of parameters
        if (method != null && parameter != null && parameter.getType() != null && !parameter.getType().isEmpty()) {
            switch (parameterType) {
                case INPUT:
                    parameter.setType(toDataTypeFQN(parameter.getType()));
                    method.addToInputParams(parameter);
                    break;
                case OUTPUT:
                    parameter.setType(toDataTypeFQN(parameter.getType()));
                    parameter.setName(null);
                    method.addToOutputParams(parameter);
                    break;
                case EXCEPTION:
                    parameter.setType(toExceptionTypeFQN(parameter.getType()));
                    parameter.setIsList(false);
                    parameter.setName(null);
                    method.addToThrowParams(parameter);
                    break;
                default:
                    break;
            }
        }
        parameter = null;
        parameterType = ParameterType.INPUT;
    }


    private AVariable findVarByName(String name) {
        for (AVariable var : dom.getMemberVars()) {
            if (var.getVarName().equals(name)) {
                return var;
            }
        }
        return null;
    }


    private Parameter parameterOf(AVariable variable) {
        Parameter param = new Parameter();
        param.setName(variable.getVarName());
        param.setType(variable.getFQClassName());
        param.setIsList(variable.isList());
        return param;
    }


    private MethodDefinition definitionOf(Operation operation) {
        MethodDefinition definition = new MethodDefinition();
        definition.setName(operation.getName());
        definition.setDocumentation(operation.getDocumentation());
        definition.setInputParams(new ArrayList<>());
        definition.setOutputParams(new ArrayList<>());
        definition.setThrowParams(new ArrayList<>());
        for (AVariable var : operation.getInputVars()) {
            definition.addToInputParams(parameterOf(var));
        }
        for (AVariable var : operation.getOutputVars()) {
            definition.addToOutputParams(parameterOf(var));
        }
        for (AVariable var : operation.getThrownExceptions()) {
            definition.addToThrowParams(parameterOf(var));
        }
        return definition;
    }


    private boolean methodsAreEquivalent(MethodDefinition method1, MethodDefinition method2) {
        // if (method1.getName().equals(method2.getName())) {
        //     if (method1.getInputParams().size() == method2.getInputParams().size()) {
        //         for (int i = 0; i < method1.getInputParams().size(); i++) {
        //             Parameter param1 = method1.getInputParams().get(i);
        //             Parameter param2 = method2.getInputParams().get(i);
        //             if (!param1.getType().equals(param2.getType())) {
        //                 return false;
        //             }
        //         }
        //         return true;
        //     }
        // }
        // return false;

        // usually the above would be correct to determine if two methods are equivalent,
        // but we don't want to generate methods with the same name, as it is not supported by the factory
        return method1.getName().equals(method2.getName());
    }


    private boolean hasEquivalentMethod(MethodDefinition method) {
        return methods.stream().anyMatch(m -> methodsAreEquivalent(m, method))
            || dom.getOperations().stream().anyMatch(op -> methodsAreEquivalent(definitionOf(op), method));
    }


    private boolean isGetterOrSetter(MethodDefinition method) {
        for (String prefix : new String[]{"get", "set", "is"}) {
            if (method.getName().startsWith(prefix)) {
                String varName = StringUtils.decapitalize(method.getName().substring(prefix.length()));
                AVariable var = findVarByName(varName);
                if (var != null) {
                    return true;
                }
            }
        }
        return false;
    }


    private boolean isObjectMethod(MethodDefinition method) {
        String name = method.getName();
        return OBJECT_METHODS.contains(name);
    }


    private String toDataTypeFQN(String fqn) {
        fqn = TypeUtils.getFqn(fqn, parameterTypeToFqn);
        if (TypeUtils.isPrimitiveType(fqn)) {
            // primitive types are not allowed as types for parameters -> use matching reference type
            return TypeUtils.getMatchingReferenceType(fqn);
        }
        return availableParameterTypes.contains(fqn) ? fqn : DEFAULT_TYPE;
    }


    private String toExceptionTypeFQN(String fqn) {
        fqn = TypeUtils.getFqn(fqn, exceptionTypeToFqn);
        return availableExceptionTypes.contains(fqn) ? fqn : DEFAULT_EXCEPTION;
    }
}