/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

package xact.ssh;


import java.util.ArrayList;

public class SSHTools {
    
    /**
     * Checks if the router output contains an error
     * @param output The router output
     * @param conditions The error conditions for this device type
     * @param indicatorForErrors The default regular expression if no regex file could be found. 
     * @return true, if the output contains an error. Otherwise false.
     */
    public static boolean checkForError(String output, ErrorConditions conditions, String indicatorForErrors) {
        boolean result = false;
        
        ArrayList<String> regexePlus = conditions.getRegexePlus();
        ArrayList<String> regexeMinus = conditions.getRegexeMinus();
        
        // 1. Line: Input, 2. Line: Output, 3. Line: Prompt
        String[] trimmedOutputArray = output.split("\\n");
        String trimmedOutput = "";

        if ((trimmedOutputArray != null) && (trimmedOutputArray.length > 2)) {
            
            // Check all output rows - except first row (input) and last row (prompt) 
            for (int i = 1; i < trimmedOutputArray.length; i++) {
                trimmedOutput = trimmedOutputArray[i];
                
                if ((trimmedOutput == null) || (trimmedOutput.trim().equals(""))) {
                    // Leere Zeile? Dann direkt weiter machen.
                    continue;
                }
                
                trimmedOutput = trimmedOutput.trim();
                
                // Default: NO Error found
                result = false;
                
                // Checking black list items
                for (String regex: regexeMinus) { 
                    // (?i) for "case insensitive"
                    if (trimmedOutput.matches("(?i).*" + regex + ".*")) {
                        
                        // Output matches on regex => error!
                        result = true;
                    }
                }
                
                if (result) {
                    // Error found => return!
                    return true;
                }
                
                // No error found from black list => Checking indicators and white list (only, if indicator is specified)
                if ((indicatorForErrors != null) && (!indicatorForErrors.trim().equals("")) && (trimmedOutput.startsWith(indicatorForErrors))) {
                   
                    // Output seems to be an error atm => Check if it's only a warning via regex
                    result = true;
                    
                    for (String regex: regexePlus) {
                        // (?i) for "case insensitive"
                        if (trimmedOutput.matches("(?i).*" + regex + ".*")) {
                            // Output matches on regex => warning => no error!
                            result = false;
                        }
                    }
                    
                    // Found a row with an error indicator where no regex matches => Error!
                    if (result) {
                        return true;
                    }
                }
            }
        }
        else {
            // No output found (only input and prompt) => everything seems to be ok...
            return false;
        }
        
        return result;
    }
}
