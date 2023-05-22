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


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ErrorConditions {
    
    private ArrayList<String> regexePlus = new ArrayList<String>();
    private ArrayList<String> regexeMinus = new ArrayList<String>();
    
    private ErrorConditions() {
        // not supported!
    }

    public ErrorConditions(String errorCheckFilePath) {
        try {
            // Read file
            // String path = XynaFactory.getInstance().getXynaMultiChannelPortal().getProperty(errorCheckFilePathProperty);

            if ((errorCheckFilePath != null) && (!errorCheckFilePath.trim().equals(""))) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(errorCheckFilePath)));
                String line = reader.readLine();
                while (line != null) {
                    if ((!line.startsWith("#")) && (!line.trim().equals("")) && (line.length() > 2) && (line.startsWith("+ "))) {
                        regexePlus.add(line.substring(2));
                    } else if ((!line.startsWith("#")) && (!line.trim().equals("")) && (line.length() > 2) && (line.startsWith("- "))) {
                        regexeMinus.add(line.substring(2));
                    }
                    line = reader.readLine();
                }
            }

        } catch (FileNotFoundException e) {
            // File not found => Exception
            // logger.warn("A regex file for checking router outputs was defined, but not found on hdd! Using now empty condition lists!", e);
            regexePlus = new ArrayList<String>();
            regexeMinus = new ArrayList<String>();
        } catch (IOException e) {
            // logger.warn("Error while loading regex file. Check file permissions! Using now empty condition lists!", e);
            regexePlus = new ArrayList<String>();
            regexeMinus = new ArrayList<String>();
        }
    }
    

    public ArrayList<String> getRegexePlus() {
        return regexePlus;
    }

    public ArrayList<String> getRegexeMinus() {
        return regexeMinus;
    }
}
