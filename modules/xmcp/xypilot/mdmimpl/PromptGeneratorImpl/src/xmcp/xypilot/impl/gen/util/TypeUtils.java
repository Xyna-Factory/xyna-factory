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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;

import xmcp.xypilot.impl.factory.XynaFactory;

public class TypeUtils {

    public final static List<String> JAVA_PRIMITIVES = new ArrayList<>();
    static {
        JAVA_PRIMITIVES.add("String");
        JAVA_PRIMITIVES.add("int");
        JAVA_PRIMITIVES.add("Integer");
        JAVA_PRIMITIVES.add("long");
        JAVA_PRIMITIVES.add("Long");
        JAVA_PRIMITIVES.add("double");
        JAVA_PRIMITIVES.add("Double");
        JAVA_PRIMITIVES.add("boolean");
        JAVA_PRIMITIVES.add("Boolean");
    }

    private final static Map<String, String> JAVA_PRIMITIVE_TO_REFERENCE_TYPE_MAP = new HashMap<>();
    static {
        JAVA_PRIMITIVE_TO_REFERENCE_TYPE_MAP.put("String", "base.Text");
        JAVA_PRIMITIVE_TO_REFERENCE_TYPE_MAP.put("int", "base.math.IntegerNumber");
        JAVA_PRIMITIVE_TO_REFERENCE_TYPE_MAP.put("Integer", "base.math.IntegerNumber");
        JAVA_PRIMITIVE_TO_REFERENCE_TYPE_MAP.put("long", "base.math.IntegerNumber");
        JAVA_PRIMITIVE_TO_REFERENCE_TYPE_MAP.put("Long", "base.math.IntegerNumber");
        JAVA_PRIMITIVE_TO_REFERENCE_TYPE_MAP.put("double", "base.math.DoubleNumber");
        JAVA_PRIMITIVE_TO_REFERENCE_TYPE_MAP.put("Double", "base.math.DoubleNumber");
        JAVA_PRIMITIVE_TO_REFERENCE_TYPE_MAP.put("boolean", "base.Boolean");
        JAVA_PRIMITIVE_TO_REFERENCE_TYPE_MAP.put("Boolean", "base.Boolean");
    }

    private final static Map<String, String> JAVA_REFERENCE_TO_PRIMITIVE_TYPE_MAP = new HashMap<>();
    static {
        JAVA_REFERENCE_TO_PRIMITIVE_TYPE_MAP.put("base.Text", "String");
        JAVA_REFERENCE_TO_PRIMITIVE_TYPE_MAP.put("base.math.IntegerNumber", "int");
        JAVA_REFERENCE_TO_PRIMITIVE_TYPE_MAP.put("base.math.DoubleNumber", "double");
        JAVA_REFERENCE_TO_PRIMITIVE_TYPE_MAP.put("base.Boolean", "boolean");
    }

    private static final String EXCEPTION_BASE_TYPE = "core.exception.XynaException";
    private static final String EXCEPTION_BASE_TYPE_GUI = "core.exception.XynaExceptionBase";

    private static boolean isExcludedType(String fqn) {
        return GenerationBase.CORE_EXCEPTION.equals(fqn) ||
                EXCEPTION_BASE_TYPE.equals(fqn) ||
                EXCEPTION_BASE_TYPE_GUI.equals(fqn);
    }

    public static String getFqn(String value, Map<String, String> toFqn) {
        if (value == null) {
            return null;
        }
        // xypilot tends to mess up the fqn by writing stuff like int[] or List<int> ->
        // clean up
        String fqn = value.replace("[", "")
                .replace("]", "")
                .replaceAll(".*?<(.*?)>.*?", "$1")
                .replace("java.lang.", "");

        if (toFqn.containsKey(fqn)) {
            fqn = toFqn.get(fqn);
        }
        return fqn;
    }

    public static Map<String, String> simpleToFqnMap(Set<String> availableTypes) {
        Map<String, String> result = new HashMap<>();
        availableTypes.forEach((subType) -> {
            String[] parts = subType.split("\\.");
            if (parts.length <= 0) {
                return;
            }
            String simpleName = parts[parts.length - 1];
            result.put(simpleName, subType);
        });
        return result;
    }

    /**
     * Returns the reference type to use instead of the given primitive type, if
     * primitive types are not allowed, e.g. for method parameters.
     *
     * @param primitiveType
     * @return
     */
    public static String getMatchingReferenceType(String primitiveType) {
        return JAVA_PRIMITIVE_TO_REFERENCE_TYPE_MAP.get(primitiveType);
    }

    /**
     * Returns the primitive type to use instead of the given reference type, if
     * reference types are not desired, e.g. for member variables.
     * @param referenceType
     * @return
     */
    public static String getMatchingPrimitiveType(String referenceType) {
        return JAVA_REFERENCE_TO_PRIMITIVE_TYPE_MAP.get(referenceType);
    }

    /**
     * Returns true if the given type is a primitive type.
     *
     * @param type
     */
    public static boolean isPrimitiveType(String type) {
        return JAVA_PRIMITIVES.contains(type);
    }

    /**
     * Will get all datatypes that are subtypes of base.AnyType for the given revision
     *
     * @param revision
     * @return Stream of pairs where first is the fqn and second the revision
     * @throws XPRC_InvalidPackageNameException
     * @throws Ex_FileAccessException
     * @throws XPRC_XmlParsingException
     */
    public static Set<ReferenceType> getSubDataTypes(Long revision) {
        // filter out the ones that are visible from the given RTC
        Set<Long> visibleRevs = new HashSet<>();
        visibleRevs.add(revision);
        visibleRevs.add(-1L);
        XynaFactory.getInstance().getDependenciesRecursivly(revision, visibleRevs);

        Set<ReferenceType> subTypes = new HashSet<>();
        Map<Long, List<String>> dtsByRev = XynaFactory.getInstance().getDeployedDatatypes();
        for (long rev : dtsByRev.keySet()) {
            if (visibleRevs.contains(rev)) {
                for (String visibleFqn : dtsByRev.get(rev)) {
                    if (!isExcludedType(visibleFqn)) {
                        subTypes.add(new ReferenceType(visibleFqn, rev));
                    }
                }
            }
        }

        return subTypes;
    }


    /**
     * Will get all exceptions that are subtypes of base.AnyType for the given
     * revision
     *
     * @param revision
     * @param targetFqn
     * @return Stream of pairs where first is the fqn and second the revision
     * @throws XPRC_InvalidPackageNameException
     * @throws Ex_FileAccessException
     * @throws XPRC_XmlParsingException
     */
    public static Set<ReferenceType> getSubExceptionTypes(Long revision) {
        // filter out the ones that are visible from the given RTC
        Set<Long> visibleRevs = new HashSet<>();
        visibleRevs.add(revision);
        visibleRevs.add(-1L);
        XynaFactory.getInstance().getDependenciesRecursivly(revision, visibleRevs);

        Set<ReferenceType> exceptions = new HashSet<>();
        Map<Long, List<String>> exceptionsByRev = XynaFactory.getInstance().getDeployedExceptions();
        for (long rev : exceptionsByRev.keySet()) {
            if (visibleRevs.contains(rev)) {
                for (String visibleFqn : exceptionsByRev.get(rev)) {
                    if (!isExcludedType(visibleFqn)) {
                        exceptions.add(new ReferenceType(visibleFqn, rev));
                    }
                }
            }
        }

        return exceptions;
    }


    /**
     * Will get all subtypes from base.AnyType for the given revision
     *
     * @param revision
     * @param targetFqn
     * @return Stream of pairs where first is the fqn and second the revision
     * @throws XPRC_InvalidPackageNameException
     * @throws Ex_FileAccessException
     * @throws XPRC_XmlParsingException
     */
    public static Set<ReferenceType> getSubTypes(Long revision) {
        Set<ReferenceType> subDataTypes = getSubDataTypes(revision);
        subDataTypes.addAll(getSubExceptionTypes(revision));
        return subDataTypes;
    }


    /**
     * Gets the {@link ReferenceType ReferenceTypes} used for the members of
     * {@code dom}
     *
     * @param dom
     * @return Set of all reference types
     */
    public static Set<ReferenceType> getUsedMemberTypes(DomOrExceptionGenerationBase dom) {
        return dom.getMemberVars()
                .stream()
                .filter((member) -> !member.isJavaBaseType())
                .map((member) -> new ReferenceType(member))
                .collect(Collectors.toSet());
    }


    /**
     * Gets the {@link ReferenceType ReferenceTypes} used for the methods of
     * {@code dom}
     *
     * @param dom
     * @return Set of all reference types
     */
    public static Set<ReferenceType> getUsedMethodTypes(DOM dom) {
        return dom.getOperations()
                .stream()
                .map((op) -> getUsedOperationTypes(op))
                .reduce(new HashSet<>(), (curr, next) -> {
                    curr.addAll(next);
                    return curr;
                });
    }


    /**
     * Gets the {@link ReferenceType ReferenceTypes} used for the given
     * {@link Operation}
     *
     * @param dom
     * @return Set of all reference types
     */
    public static Set<ReferenceType> getUsedOperationTypes(Operation op) {
        Set<ReferenceType> ret = new HashSet<>();
        for (AVariable input : op.getInputVars()) {
            if (!input.isJavaBaseType()) {
                ret.add(new ReferenceType(input));
            }
        }
        for (AVariable input : op.getOutputVars()) {
            if (!input.isJavaBaseType()) {
                ret.add(new ReferenceType(input));
            }
        }
        return ret;
    }


    /**
     * Gets all the reference Types uses by the given {@code dom} by combining the
     * types used for the members with the types used for the operations.
     *
     * @param dom
     * @return
     */
    public static Set<ReferenceType> getUsedTypes(DomOrExceptionGenerationBase dom) {
        Set<ReferenceType> ret = getUsedMemberTypes(dom);
        if (dom instanceof DOM) {
            ret.addAll(getUsedMethodTypes((DOM) dom));
        }
        return ret;
    }


    /**
     * Represents a xyna reference type by its name and revision
     */
    public static class ReferenceType {
        String fqn;
        long revision;

        public ReferenceType(String fqn, long revision) {
            this.fqn = fqn;
            this.revision = revision;
        }

        public ReferenceType(AVariable var) {
            this(var.getDomOrExceptionObject().getOriginalFqName(), var.getRevision());
        }

        public String getFqn() {
            return fqn;
        }

        public long getRevision() {
            return revision;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ReferenceType other = (ReferenceType) obj;
            return this.revision == other.revision && this.fqn.equals(other.fqn);
        }

        @Override
        public String toString() {
            return "(" + fqn + ", " + revision + ")";
        }
    }
}
