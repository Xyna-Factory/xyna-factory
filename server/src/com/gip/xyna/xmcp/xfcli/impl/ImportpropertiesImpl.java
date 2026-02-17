package com.gip.xyna.xmcp.xfcli.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_IllegalPropertyValueException;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.DocumentationMap;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Importproperties;
import com.gip.xyna.xmcp.xfcli.impl.ExportpropertiesImpl.OutputFormat;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

public class ImportpropertiesImpl extends XynaCommandImplementation<Importproperties> {

  public static void importProperties(String data, OutputFormat format, boolean overwriteExisting)
      throws IOException, PersistenceLayerException, XFMG_IllegalPropertyValueException {

    switch (format) {
      case YAML:
        importYAML(data, overwriteExisting);
        break;

      case CSV:
        importCSV(data, overwriteExisting);
        break;
    }
  }

  @Override
  public void execute(OutputStream statusOutputStream, Importproperties payload)
      throws XynaException {

    File file = new File(payload.getFilename());
    final String data = FileUtils.readFileAsString(file);

    OutputFormat format;
    try {
      format = OutputFormat.valueOf(payload.getFormat().toUpperCase());
    } catch (Exception e) {
      format = OutputFormat.CSV;
    }

    try {
      importProperties(data, format, payload.getOverwriteExisting());
    } catch (IOException ioe) {
      throw new XynaException("Import failed: " + ioe.getMessage(), ioe);
    }
  }

  /* ===========================================================================================
   * CSV Import
     =========================================================================================== */

  /**
   * Import CSV content that may contain multi-line values and semicolons inside quoted fields.
   * Quotes inside values are backslash-escaped (\").
   */
  private static void importCSV(String csv, boolean overwriteExisting)
      throws IOException, PersistenceLayerException, XFMG_IllegalPropertyValueException {

    try (BufferedReader br = new BufferedReader(new StringReader(csv))) {
      // parse header
      String headerRecord = readCSVRecord(br);
      String[] header = parseCSVRecord(headerRecord);
      Map<String, Integer> colIndex = new HashMap<>();
      for (int i = 0; i < header.length; i++) {
        colIndex.put(header[i], i);
      }

      // resolve indices of known columns
      Integer idxName = colIndex.get(ExportpropertiesImpl.COL_NAME);
      Integer idxValue = colIndex.get(ExportpropertiesImpl.COL_VALUE);

      // map documentation columns by language (only present if included in export)
      Map<DocumentationLanguage, Integer> docuColumns = new EnumMap<>(DocumentationLanguage.class);
      for (DocumentationLanguage lang : DocumentationLanguage.values()) {
        String key = ExportpropertiesImpl.COL_DOCU_PREFIX + lang.name();
        if (colIndex.containsKey(key)) {
          docuColumns.put(lang, colIndex.get(key));
        }
      }

      String record;
      while ((record = readCSVRecord(br)) != null) {
        String[] cols = parseCSVRecord(record);

        String name = unescapeCSV(cols[idxName]);
        String value = unescapeCSV(cols[idxValue]);

        Map<DocumentationLanguage, String> docus = new EnumMap<>(DocumentationLanguage.class);
        docuColumns.forEach((lang, index) -> 
          Optional.ofNullable(unescapeCSV(cols[index]))
                  .ifPresent(v -> docus.put(lang, v))
        );

        storeProperty(name, value, docus, overwriteExisting);
      }
    }
  }

  /**
   * Reads one complete CSV record which may span multiple lines.
   * A record is complete when all non-escaped quotes are balanced (i.e., we are not inside quotes).
   */
  private static String readCSVRecord(BufferedReader br) throws IOException {
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      if (sb.length() > 0) {
        // Preserve the original newline INSIDE a CSV field
        sb.append("\n");
      }

      sb.append(line);
      if (quotesBalanced(sb)) {
        return sb.toString();
      }
    }

    // return the last (possibly empty) record if any content has been accumulated
    return (sb.length() == 0) ? null : sb.toString();
  }

  /**
   * Checks whether quotes are balanced in the given buffer, ignoring backslash-escaped quotes.
   */
  private static boolean quotesBalanced(CharSequence cs) {
    boolean inQuotes = false;
    for (int i = 0; i < cs.length(); i++) {
      char c = cs.charAt(i);
      if (c == '"') {
        if (!isEscaped(cs, i)) {
          inQuotes = !inQuotes;
        }
      }
    }

    return !inQuotes;
  }

  /**
   * Returns true if the quote at position 'i' is escaped by an odd number of backslashes.
   */
  private static boolean isEscaped(CharSequence cs, int i) {
    int backslashes = 0;
    int j = i - 1;
    while (j >= 0 && cs.charAt(j) == '\\') {
      backslashes++;
      j--;
    }

    return (backslashes % 2) == 1;
  }

  /**
   * Tokenizes a single CSV record respecting quotes, escaped quotes and delimiter.
   * Delimiter is ExportpropertiesImpl.CSV_DELIMITER
   */
  private static String[] parseCSVRecord(String record) {
    List<String> result = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < record.length(); i++) {
      char c = record.charAt(i);

      if (c == '"') {
        if (!isEscaped(record, i)) {
          inQuotes = !inQuotes; // toggle only on non-escaped quotes
        }
        field.append(c);
        continue;
      }

      if (c == ExportpropertiesImpl.CSV_DELIMITER.charAt(0) && !inQuotes) {
        result.add(field.toString());
        field.setLength(0);
      } else {
        field.append(c);
      }
    }

    // last field
    result.add(field.toString());

    return result.toArray(new String[0]);
  }

  private static String unescapeCSV(String value) {
    if (value.length() == 0 || value == null) {
      return null;
    }

    // strip outer quotes if present
    if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
      value = value.substring(1, value.length() - 1);
    }

    // unescape inner quotes
    value = value.replaceAll("\\\\\"", "\"");

    return value;
  }

  /* ===========================================================================================
   * YAML Import
     =========================================================================================== */

  /**
   * Import YAML in the format produced by ExportpropertiesImpl
   */
  private static void importYAML(String yaml, boolean overwriteExisting)
      throws IOException, PersistenceLayerException, XFMG_IllegalPropertyValueException {

    try (BufferedReader br = new BufferedReader(new java.io.StringReader(yaml))) {

      String line;
      String curName = null;
      StringBuilder valueBuffer = null;
      StringBuilder docuBuffer = null;
      boolean headerRead = false;
      Integer indentDepth = null;
      int indentLevel = 1; // start one level under YAML_DATA_START

      while ((line = br.readLine()) != null) {
        if (!headerRead) {
          if (line.strip().equals(ExportpropertiesImpl.YAML_DATA_START.strip())) {
            headerRead = true;
          }

          continue;
        }

        if (indentDepth == null) {
          // detect indentation depth
          indentDepth = line.length() - line.replaceFirst("^\\s+", "").length();
        }

        // a new property starts where the level 1 indented line ends with YAML_NAME_POSTFIX
        if (line.startsWith(getIndent(indentDepth, indentLevel)) && line.endsWith(ExportpropertiesImpl.YAML_NAME_POSTFIX)) {
          // finish previous entry
          if (curName != null) {
            String docusStr = docuBuffer != null ? docuBuffer.toString() : "";
            Map<DocumentationLanguage, String> docus = DocumentationMap.valueOf(docusStr.replace("=&gt;", "=>"));
            storeProperty(curName, valueBuffer.toString(), docus, overwriteExisting);
          }

          // property name is everything before the postfix
          curName = line.substring(indentDepth * indentLevel, line.length() - ExportpropertiesImpl.YAML_NAME_POSTFIX.length()).trim();
          valueBuffer = new StringBuilder();
          docuBuffer = null;
        }
        // the documentation for the current property starts where the level 1 indented line ends with YAML_DOCU_POSTFIX
        else if (line.startsWith(getIndent(indentDepth, indentLevel)) && line.endsWith(ExportpropertiesImpl.YAML_DOCU_POSTFIX)) {
          docuBuffer = new StringBuilder();
        }
        // collect indented documentation lines (keeps original line breaks) - one indent level deeper
        else if (line.startsWith(getIndent(indentDepth, indentLevel + 1)) && docuBuffer != null) {
          String docuPart = line.substring(indentDepth * (indentLevel + 1));
          docuBuffer.append(docuBuffer.length() > 0 ? "\n" + docuPart : docuPart);
        }
        // collect indented value lines (keeps original line breaks) - one indent level deeper
        else if (line.startsWith(getIndent(indentDepth, indentLevel + 1)) && valueBuffer != null) {
          String valuePart = line.substring(indentDepth * (indentLevel + 1));
          valueBuffer.append(valueBuffer.length() > 0 ? "\n" + valuePart : valuePart);
        }
      }

      // last pending entry
      if (curName != null) {
        String docusStr = docuBuffer != null ? docuBuffer.toString() : "";
        Map<DocumentationLanguage, String> docus = DocumentationMap.valueOf(docusStr.replace("=&gt;", "=>"));
        storeProperty(curName, valueBuffer.toString(), docus, overwriteExisting);
      }
    }
  }

  private static String getIndent(int indentDepth, int level) {
    return " ".repeat(indentDepth * level);
  }

  /* ===========================================================================================
   * General Import Helpers
     =========================================================================================== */

  private static void storeProperty(String propertyName, String propertyValue, Map<DocumentationLanguage, String> docus, boolean overwrite)
      throws PersistenceLayerException, XFMG_IllegalPropertyValueException {

    Configuration cfg = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration();
    if (!overwrite) {
      XynaPropertyWithDefaultValue existing = cfg.getPropertyWithDefaultValue(propertyName);
      if (existing != null && existing.getValue() != null) {
        return;
      }
    }

    cfg.setProperty(propertyName, propertyValue);

    if (docus != null) {
      for (Entry<DocumentationLanguage, String> docuEntry : docus.entrySet()) {
        cfg.addPropertyDocumentation(propertyName, docuEntry.getKey(), docuEntry.getValue());
      }
    }
  }
}
