/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.File;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Exportproperties;



public class ExportpropertiesImpl extends XynaCommandImplementation<Exportproperties> {

  public enum OutputFormat { CSV, YAML };

  // CSV
  public static final String COL_NAME = "name";
  public static final String COL_VALUE = "value";
  public static final String COL_DOCU_PREFIX = "docu";
  public static final String CSV_DELIMITER = ";";

  // YAML
  public static final String YAML_NAME_POSTFIX = ".propertyvalue: |";
  public static final String YAML_DOCU_POSTFIX = ".propertydocumentation: |";
  public static final int YAML_INDENTATION_DEPTH = 2;
  public static final String YAML_HEADER = "kind: ConfigMap\n"
      + "metadata:\n"
      + " ".repeat(YAML_INDENTATION_DEPTH) + "date:";
  public static final String YAML_DATA_START = "data:";


  public static String export(OutputFormat format, String filter, boolean inclUnchanged, boolean inclDoc) {
    StringBuilder sb = new StringBuilder();

    switch (format) {
      case YAML:
        generateYAML(sb, filter, inclUnchanged, inclDoc);
        break;

      case CSV:
        generateCSV(sb, filter, inclUnchanged, inclDoc);
        break;
    }

    return sb.toString();
  }

  public void execute(OutputStream statusOutputStream, Exportproperties payload) throws XynaException {
    OutputFormat format;
    try {
      format = OutputFormat.valueOf(payload.getFormat().toUpperCase());
    } catch (Exception e) {
      format = OutputFormat.CSV;
    }

    String export = export(format, payload.getFilter(), payload.getIncludeUnchangedProperties(), payload.getIncludeDocumentation());

    // write to file
    File outputFile = new File(payload.getFilename());
    FileUtils.writeStringToFile(export, outputFile);
  }

  /* ===========================================================================================
   * CSV export
     =========================================================================================== */

  private static void generateCSV(StringBuilder sb, String filter, boolean inclUnchanged, boolean inclDoc) {
    // add header

    List<String> header = Stream.concat(
      List.of(COL_NAME, COL_VALUE).stream(),
      inclDoc
        ? EnumSet.allOf(DocumentationLanguage.class).stream()
                 .map(lang -> COL_DOCU_PREFIX + lang.name())
        : Stream.empty()
    ).collect(Collectors.toList());

    addCSVRow(sb, header);

    // add properties

    Collection<XynaPropertyWithDefaultValue> properties = getProperties(filter, inclUnchanged);
    for (XynaPropertyWithDefaultValue property : properties) {
      Map<DocumentationLanguage, String> docus = property.getDocuOrDefDocu();

      List<String> row = Stream.concat(
        Stream.of(property.getName(), property.getValue()),
        inclDoc
          ? EnumSet.allOf(DocumentationLanguage.class).stream()
                   .map(lang -> docus.get(lang))
          : Stream.empty()
      ).collect(Collectors.toList());

      addCSVRow(sb, row);
    }
  }

  private static void addCSVRow(StringBuilder sb, List<String> values) {
    StringJoiner sj = new StringJoiner(String.valueOf(CSV_DELIMITER));
    for (String v : values) {
      sj.add(escapeCSV(v));
    }

    sb.append(sj.toString() + "\n");
  }

  private static String escapeCSV(String value) {
    if (value == null) {
      return "";
    }

    boolean needsEscaping =
      value.indexOf(CSV_DELIMITER) >= 0 ||
      value.indexOf('"') >= 0 ||
      value.indexOf('\n') >= 0 ||
      value.indexOf('\r') >= 0 ||
      value.length() == 0;

    return (needsEscaping) ? '"' + value.replace("\"", "\\\"") + '"' : value;
  }

  /* ===========================================================================================
   * YAML export
     =========================================================================================== */

  private static void generateYAML(StringBuilder sb, String filter, boolean inclUnchanged, boolean inclDoc) {
    // add header data to make the YAML a config map
    sb.append(YAML_HEADER);
    sb.append(" " + OffsetDateTime.now().withNano(0) + "\n");
    sb.append(YAML_DATA_START + "\n");

    // add property data
    Collection<XynaPropertyWithDefaultValue> properties = getProperties(filter, inclUnchanged);
    properties.forEach(property -> {
      String docuStr = inclDoc
          ? property.getDocuOrDefDocu().entrySet().stream()
              .map(e -> e.getKey() + " =&gt; " + e.getValue())
              .collect(Collectors.joining(", "))
          : null;

      addYAMLEntry(sb, property.getName(), property.getValue(), docuStr, 1);
    });
  }

  private static void addYAMLEntry(StringBuilder sb, String propertyName, String propertyValue, String docu, int indentLevel) {
    Function<String, String> indentLines = value -> getIndent(indentLevel + 1) +
        ((value != null) ? value.replace("\n", "\n" + getIndent(indentLevel + 1)) : "");

    sb.append(getIndent(indentLevel) + propertyName + YAML_NAME_POSTFIX + "\n");
    sb.append(indentLines.apply(propertyValue) + "\n");

    if (docu != null && docu.length() > 0) {
      sb.append(getIndent(indentLevel) + propertyName + YAML_DOCU_POSTFIX + "\n");
      sb.append(indentLines.apply(docu) + "\n");
    }
  }

  private static String getIndent(int level) {
    return " ".repeat(YAML_INDENTATION_DEPTH * level);
  }

  /* ===========================================================================================
   * General export helpers
     =========================================================================================== */

  private static Collection<XynaPropertyWithDefaultValue> getProperties(String filter, boolean inclUnchanged) {
    Collection<XynaPropertyWithDefaultValue> properties = XynaFactory.getInstance().getXynaMultiChannelPortalPortal().getPropertiesWithDefaultValuesReadOnly();

    List<XynaPropertyWithDefaultValue> filteredProps = new ArrayList<XynaPropertyWithDefaultValue>(properties);
    filteredProps.removeIf(prop -> (prop.getValue() == null && !inclUnchanged) ||
                                   (filter != null && !prop.getName().matches(filter)));

    Collections.sort(filteredProps, new Comparator<XynaPropertyWithDefaultValue>() {
      public int compare(XynaPropertyWithDefaultValue o1, XynaPropertyWithDefaultValue o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });

    return filteredProps;
  }

}
