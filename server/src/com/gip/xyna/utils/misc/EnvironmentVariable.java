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
package com.gip.xyna.utils.misc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.log4j.Logger;
import com.gip.xyna.CentralFactoryLogging;

public abstract class EnvironmentVariable<T> {

    protected final static Logger logger = CentralFactoryLogging.getLogger(EnvironmentVariable.class);

    private final String varName;
    private boolean isFile;

    public EnvironmentVariable(String varName) {
        this.varName = varName;
        this.isFile = false;
    }

    public EnvironmentVariable(String varName, boolean isFilePath) {
        this.varName = varName;
        this.isFile = isFilePath;
    }

    public abstract Optional<T> getValue();

    public void setIsFile() {
        this.isFile = true;
    }

    protected Optional<String> readValue() {
        try {
            if (isFilePath()) {
                return Optional.of(readValueFromFile(readValueFromEnv()));
            } else {
                return Optional.ofNullable(readValueFromEnv());
            }
        } catch (IOException e) {
            logger.error("Error reading from file in environment variable '" + varName + "'': " + e.getMessage());
            return Optional.empty();
        }
    }

    protected String readValueFromFile(String filePath) throws IOException {
        if (filePath == null)
            throw new IOException("no filePath available.");

        return Files.readString(Paths.get(filePath));
    }

    protected String readValueFromEnv() {
        return System.getenv(varName);
    }

    protected boolean isFilePath() {
        return isFile || varName.toLowerCase().endsWith("_file");
    }

    public String toString() {
        return "EnvironmentVariale '" + varName + "' isFile: " + isFilePath();
    }

    public String getVarName() {
        return varName;
    }

    public static class StringEnvironmentVariable extends EnvironmentVariable<String> {

        StringEnvironmentVariable(String varName) {
            super(varName);
        }

        StringEnvironmentVariable(String varName, boolean isFilePath) {
            super(varName, isFilePath);
        }

        @Override
        public Optional<String> getValue() {
            return readValue();
        }
    }

    public static class IntegerEnvironmentVariable extends EnvironmentVariable<Integer> {
        IntegerEnvironmentVariable(String varName) {
            super(varName);
        }

        IntegerEnvironmentVariable(String varName, boolean isFilePath) {
            super(varName, isFilePath);
        }

        @Override
        public Optional<Integer> getValue() {
            return readValue().flatMap(value -> {
                try {
                    return Optional.of(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    if (isFilePath())
                        logger.error(
                                "Error parsing integer value from file from variable: '" + getVarName() + "'"
                                        + e.getMessage());
                    else
                        logger.error(
                                "Error parsing integer value from variable: '" + getVarName() + "'" + e.getMessage());
                    return Optional.empty();
                }
            });
        }
    }

    public static class DoubleEnvironmentVariable extends EnvironmentVariable<Double> {
        DoubleEnvironmentVariable(String varName) {
            super(varName);
        }

        DoubleEnvironmentVariable(String varName, boolean isFilePath) {
            super(varName, isFilePath);
        }

        @Override
        public Optional<Double> getValue() {
            return readValue().flatMap(value -> {
                try {
                    return Optional.of(Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    if (isFilePath())
                        logger.error(
                                "Error parsing double value from file from variable: '" + getVarName() + "'"
                                        + e.getMessage());
                    else
                        logger.error(
                                "Error parsing double value from variable: '" + getVarName() + "'" + e.getMessage());
                    return Optional.empty();
                }
            });
        }
    }

    public static class BooleanEnvironmentVariable extends EnvironmentVariable<Boolean> {
        BooleanEnvironmentVariable(String varName) {
            super(varName);
        }

        BooleanEnvironmentVariable(String varName, boolean isFilePath) {
            super(varName, isFilePath);
        }

        @Override
        public Optional<Boolean> getValue() {
            return readValue().flatMap(value -> {
                return Optional.of(Boolean.parseBoolean(value));
            });
        }
    }

}