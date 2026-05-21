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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides functions to help with imports
 */
public class ImportHandler {
    // set of simple names that are imported
    Set<String> importedSimpleNames = new HashSet<>();

    // Maps the FQN of a type to what would be used in the code with all the imports
    private Map<String, String> toResolvedType = new HashMap<>();

    /**
     * Creates new ImportHandler without any imports.
     */
    public ImportHandler() {
    }

    /**
     * Creates new ImportHandler by setting up the FQN-Map with the given imports
     * @param imports Set of FQNs that are imported by the context.
     */
    public ImportHandler(Set<String> imports) {
        addImports(imports);
    }

    /**
     * Adds the given FQN to the imports if there is no type with the same simple name already imported.
     * @param fqn FQN of type to add to imports
     * @return this
     */
    public ImportHandler addImport(String fqn) {
        String simpleName = getSimpleName(fqn);
        if (!importedSimpleNames.contains(simpleName)) {
            toResolvedType.put(fqn, simpleName);
            importedSimpleNames.add(simpleName);
        }
        return this;
    }

    /**
     * Adds the given FQNs to the imports if there is a type with the same simple name already imported it is skipped.
     * @param imports Set of FQNs to add to imports
     * @return this
     */
    public ImportHandler addImports(Set<String> imports) {
        for (String fqn : imports) {
            addImport(fqn);
        }
        return this;
    }

    /**
     * @param fqn FQN of some type
     * @return Simple name of that type. Example: java.util.HashMap -> HashMap
     */
    private String getSimpleName(String fqn) {
        int lastDot = fqn.lastIndexOf(".");
        return fqn.substring(lastDot + 1);
    }

    /**
     * Resolves the given FQN with respect to the imports. If the Type is imported, the simple name is used. Else the FQN
     * @param fqn
     * @return resolved type specifier
     */
    public String resolve(String fqn) {
        String resolved = this.toResolvedType.get(fqn);
        if (resolved == null) {
            return fqn;
        }
        return resolved;
    }
}
