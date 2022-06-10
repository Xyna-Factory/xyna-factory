/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

package xdev.xtestfactory.infrastructure.services.impl.complexerfunctions;



import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.CsvUtils;
import com.gip.xyna.utils.misc.CsvUtils.CSVDocument;
import com.gip.xyna.utils.misc.CsvUtils.CSVIterator;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import xdev.xtestfactory.infrastructure.datatypes.ManagedFileID;
import xdev.xtestfactory.infrastructure.exceptions.InvalidCSV;
import xdev.xtestfactory.infrastructure.services.impl.TestFactoryIntegrationServiceOperationImpl;
import xdev.xtestfactory.infrastructure.storables.TestDataMetaData;
import xdev.xtestfactory.infrastructure.storables.TestReport;
import xdev.xtestfactory.infrastructure.storables.TestReportEntryFeature;
import xdev.xtestfactory.infrastructure.storables.TestReportEntryTestCase;
import xnwh.persistence.Storable;



public class OtherExportImportAndUtils {

  private static Logger logger = CentralFactoryLogging.getLogger(OtherExportImportAndUtils.class);

  public static enum ContentType {

    XML("xml") {
      public String getDefaultExportFileNameSuffix() {
        return ".xml";
      }
    },
    CSV("csv") {
      @Override
      public String getDefaultExportFileNameSuffix() {
        return ".csv";
      }
    },
    XLS("xls") {
      @Override
      public String getDefaultExportFileNameSuffix() {
        return ".xls";
      }
    };
  
    private final static String NAMED_EXPORT_FILE_PREFIX = "XTF_TestData_";
    private final String contentIdentifier;

    public abstract String getDefaultExportFileNameSuffix();


    private ContentType(String contentIdentifier) {
      this.contentIdentifier = contentIdentifier;
    }


    public String getContentIdentifier() {
      return contentIdentifier;
    }


    public final String getDefaultExportFileName() {
      return "TestFactoryExport" + getDefaultExportFileNameSuffix();
    }


    public String getExportFileName(String name) {
      return NAMED_EXPORT_FILE_PREFIX + name + getDefaultExportFileNameSuffix();
    }

  }


  public static ManagedFileID createExcelFromTestReport(TestReport testreport,
                                                        List<? extends TestReportEntryFeature> features,
                                                        List<? extends TestReportEntryTestCase> testcases) {

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    sdf.setLenient(false);
    String testReportFileName = "TestReport_" + sdf.format(new Date());

    ByteArrayOutputStream os = createExcelFromTestReportImpl(testreport, features, testcases);

    ManagedFileID resultid = new ManagedFileID();
    resultid.setID(uncompressedToFileManagement(os.toByteArray(), ContentType.XLS, testReportFileName));
    if (resultid.getID() == null) {
      throw new RuntimeException("Filemanagementresultid is null!");
    }
    return resultid;

  }


  private static ByteArrayOutputStream createExcelFromTestReportImpl(TestReport testreport,
                                                      List<? extends TestReportEntryFeature> features,
                                                      List<? extends TestReportEntryTestCase> testcases) {

    SimpleDateFormat df = Constants.defaultUTCSimpleDateFormat();

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      WritableWorkbook workbook = Workbook.createWorkbook(os);
      WritableSheet featuresheet = workbook.createSheet("Features", 0);
      WritableSheet testcasessheet = workbook.createSheet("TestCases", 1);

      int x = 0;
      int y = 0;

      // Ueberschrift mit Testreportname und Datum
      Label label = new Label(x, y, testreport.getName() + " Features");
      featuresheet.addCell(label);
      x++;
      if (testreport.getCreationDate() != null && testreport.getCreationDate().getTimeInMilliseconds() != 0) {
        label = new Label(x, y, df.format(new Date(testreport.getCreationDate().getTimeInMilliseconds())));
        featuresheet.addCell(label);
      }
      y = y + 2;

      // Spaltenbeschreibungen
      x = 0;

      label = new Label(x, y, "Featurename");
      featuresheet.addCell(label);
      x++;
      label = new Label(x, y, "Executions");
      featuresheet.addCell(label);
      x++;
      label = new Label(x, y, "Successes");
      featuresheet.addCell(label);
      x++;
      label = new Label(x, y, "LastWasSuccess");
      featuresheet.addCell(label);
      y++;

      // Daten

      for (TestReportEntryFeature cur : features) {
        x = 0;
        label = new Label(x, y, (cur.getFeature().getDeleted() ? "[deleted] " : "") + cur.getFeature().getName());
        featuresheet.addCell(label);
        x++;
        label = new Label(x, y, String.valueOf(cur.getOutcomeStatistics().getExecutions()));
        featuresheet.addCell(label);
        x++;
        label = new Label(x, y, String.valueOf(cur.getOutcomeStatistics().getSuccesses()));
        featuresheet.addCell(label);
        x++;
        label = new Label(x, y, String.valueOf(cur.getOutcomeStatistics().getLastWasSuccess()));
        featuresheet.addCell(label);
        y++;
      }

      x = 0;
      y = 0;

      // Ueberschrift mit Testreportname und Datum
      label = new Label(x, y, testreport.getName() + " Test Cases");
      testcasessheet.addCell(label);
      x++;
      if (testreport.getCreationDate() != null && testreport.getCreationDate().getTimeInMilliseconds() != 0) {
        label = new Label(x, y, df.format(new Date(testreport.getCreationDate().getTimeInMilliseconds())));
        testcasessheet.addCell(label);
      }
      y = y + 2;

      // Spaltenbeschreibungen
      x = 0;

      label = new Label(x, y, "Test Case Name");
      testcasessheet.addCell(label);
      x++;
      label = new Label(x, y, "Executions");
      testcasessheet.addCell(label);
      x++;
      label = new Label(x, y, "Successes");
      testcasessheet.addCell(label);
      x++;
      label = new Label(x, y, "LastWasSuccess");
      testcasessheet.addCell(label);
      y++;


      // Daten

      for (TestReportEntryTestCase cur : testcases) {
        x = 0;
        label = new Label(x, y, (cur.getTestCase().getDeleted() ? "[deleted] " : "") + cur.getTestCase().getName());
        testcasessheet.addCell(label);
        x++;
        label = new Label(x, y, String.valueOf(cur.getOutcomeStatistics().getExecutions()));
        testcasessheet.addCell(label);
        x++;
        label = new Label(x, y, String.valueOf(cur.getOutcomeStatistics().getSuccesses()));
        testcasessheet.addCell(label);
        x++;
        label = new Label(x, y, String.valueOf(cur.getOutcomeStatistics().getLastWasSuccess()));
        testcasessheet.addCell(label);
        y++;
      }
    
      workbook.write();
      workbook.close();
      return os;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
        os.close();
      } catch (IOException e) {
        logger.debug("Failed to close ByteArrayOutputStream", e);
      }
    }
  }


  public static String compressContentToFileManagement(byte[] out, ContentType type) {
    return compressContentToFileManagement(out, type, null);
  }


  public static String compressContentToFileManagement(byte[] out, ContentType type, String name) {
    try (InputStream inputstream = new ByteArrayInputStream(compress(out, type))) {
      FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
      String fileName;
      if (name == null) {
        fileName = type.getDefaultExportFileName();
      } else {
        fileName = type.getExportFileName(name);
      }
      return fm.store("Testfactory", fileName, inputstream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  public static String uncompressedToFileManagement(byte[] out, ContentType type, String name) {

    InputStream is = new ByteArrayInputStream(out);

    FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    String fileName = type.getExportFileName(name);
    return fm.store("Testfactory", fileName, is);

  }


  private static byte[] compress(byte[] out, ContentType type) {
    try (ByteArrayInputStream fis = new ByteArrayInputStream(out)) {
      try (ByteArrayOutputStream fos = new ByteArrayOutputStream()) {
        try (ZipOutputStream gzipOS = new ZipOutputStream(fos)) {
          //gzipOS.setLevel(9);
          byte[] buffer = new byte[1024];
          int len;
          ZipEntry ze = new ZipEntry("content." + type.getContentIdentifier());
          gzipOS.putNextEntry(ze);
          while ((len = fis.read(buffer)) != -1) {
            gzipOS.write(buffer, 0, len);
          }
          gzipOS.closeEntry();
          gzipOS.flush();
          return fos.toByteArray();
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  public static byte[] getBytes(String input) {
    try {
      return input.getBytes(Constants.DEFAULT_ENCODING);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }


  private static final String USED_VAR_NAME = "used";

  
  private static void getMemberVarsRecursively(DOM dom, String path, List<AVariable> vars, Map<AVariable, String> varPaths) {
    List<AVariable> variables = dom.getMemberVars();
    
    for (AVariable av : variables) {
      vars.add(av);
      varPaths.put(av, path + av.getVarName());
      
      if (!av.isJavaBaseType()) {
        DOM childDOM = (DOM)av.getDomOrExceptionObject();
        
        if (childDOM != null) {
          getMemberVarsRecursively(childDOM, path + av.getVarName() + ".", vars, varPaths);
        }
      }
    }
  }
  

  private static List<List<String>> calculateStringTable(List<? extends Storable> testdata, String fqn, TestDataMetaData metaData)
      throws XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException,
      XPRC_MDMDeploymentException {

    DOM targetDOM = DOM.generateUncachedInstance(fqn, true, TestFactoryIntegrationServiceOperationImpl.getRev());
    List<AVariable> variables = new ArrayList<>();
    Map<AVariable, String> pathMap = new HashMap<>();
    getMemberVarsRecursively(targetDOM, "", variables, pathMap);
    List<Map<String, String>> testDataEntries = new ArrayList<Map<String, String>>();
    
    try {
      for (Storable nextTestDataObject : testdata) {
        Map<String, String> oneTestDataLine = calculateVariableNamesAndContentForTestData(nextTestDataObject, variables, pathMap);
        testDataEntries.add(oneTestDataLine);
      }
    } catch (InvalidObjectPathException e) {
      throw new RuntimeException(e);
    }

    List<List<String>> stringTable = new ArrayList<List<String>>();

    List<String> headerLine = new ArrayList<>();
    if (metaData.getOneTimeTestData()) {
      headerLine.add(USED_VAR_NAME);
    }
    for (AVariable nextVariableObject : variables) {
      headerLine.add(pathMap.get(nextVariableObject));
    }
    stringTable.add(headerLine);

    // Nun Werte in gleicher Reihenfolge
    for (Map<String, String> entry : testDataEntries) {
      List<String> nextLine = new ArrayList<>();
      if (metaData.getOneTimeTestData()) {
        nextLine.add(entry.get(USED_VAR_NAME));
      }
      for (AVariable nextVariableObject : variables) {
        String path = pathMap.get(nextVariableObject);
        nextLine.add(entry.get(path));
      }
      stringTable.add(nextLine);
    }

    return stringTable;

  }


  private static Map<String, String> calculateVariableNamesAndContentForTestData(Storable nextTestDataObject, List<AVariable> variables, Map<AVariable, String> varPaths)
      throws InvalidObjectPathException {
    Map<String, String> oneTestDataLine = new HashMap<String, String>();
    Object usedObject = nextTestDataObject.get(USED_VAR_NAME);
    
    if (usedObject != null) {
      oneTestDataLine.put(USED_VAR_NAME, usedObject.toString());
    } else {
      oneTestDataLine.put(USED_VAR_NAME, "false");
    }
    
    for (AVariable current : variables) {
      String path = varPaths.get(current);
      String pathSegments[] = path.split("\\.");
      Object nextVariableContent = null;
      int depth = pathSegments.length;
      int index = 0;
      
      XynaObject node = nextTestDataObject;
      
      while ((depth > 1) && (node != null)) {
        node = (XynaObject) node.get(pathSegments[index]);
        index++;
        depth--;
      }

      if (node != null) {
        nextVariableContent = node.get(pathSegments[index]);
      }
      
      if (nextVariableContent != null) {
        if (!current.isJavaBaseType()) {
          nextVariableContent = current.getFQClassName();
        }
        
        oneTestDataLine.put(path, nextVariableContent.toString());
      } else {
        oneTestDataLine.put(path, "");
      }
    }
    
    return oneTestDataLine;
  }


  private static String createCSVContentFromTestdata(List<? extends Storable> testdata, String fqn, TestDataMetaData metaData) {

    try {
      List<List<String>> stringTable = calculateStringTable(testdata, fqn, metaData);
      StringBuilder sb = new StringBuilder();
      sb.append(fqn + "\n");
      sb.append(CsvUtils.toCSVMultiLine(stringTable, ";", "\"", "\n"));
      return sb.toString();
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }

  }


  private static ByteArrayOutputStream createExcelContentFromTestdata(List<? extends Storable> testdata, String fqn,
                                                                      TestDataMetaData metaData) {

    List<List<String>> stringTable;
    try {
      stringTable = calculateStringTable(testdata, fqn, metaData);
    } catch (XPRC_InvalidPackageNameException | XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException
        | XPRC_MDMDeploymentException e1) {
      throw new RuntimeException("Failed to gather information", e1);
    }

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      WritableWorkbook workbook = Workbook.createWorkbook(os);
      WritableSheet sheet = workbook.createSheet("Test data: " + fqn, 0);

      Label label = new Label(0, 0, fqn);
      sheet.addCell(label);

      int columnIndex = 0;
      int rowIndex = 1;
      for (List<String> nextLine : stringTable) {
        for (String cv : nextLine) {
          label = new Label(columnIndex, rowIndex, cv);
          sheet.addCell(label);
          columnIndex++;
        }
        rowIndex++;
        columnIndex = 0;
      }

      workbook.write();
      workbook.close();
      os.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return os;

  }


  public static ManagedFileID createCSVFromTestdata(List<? extends Storable> testdata, TestDataMetaData metaData) {
    ManagedFileID resultid = new ManagedFileID();
    String resultcsv = createCSVContentFromTestdata(testdata, metaData.getTestDataFullQualifiedStorableName(), metaData);
    String testdataName = metaData.getName().replace(" ", "");
    resultid.setID(uncompressedToFileManagement(OtherExportImportAndUtils.getBytes(resultcsv), ContentType.CSV, testdataName));
    return resultid;
  }


  public static ManagedFileID createExcelFromTestdata(List<? extends Storable> testdata, TestDataMetaData metaData) {
    ManagedFileID resultid = new ManagedFileID();
    ByteArrayOutputStream out = createExcelContentFromTestdata(testdata, metaData.getTestDataFullQualifiedStorableName(), metaData);
    String testdataName = metaData.getName().replace(" ", "");
    resultid.setID(uncompressedToFileManagement(out.toByteArray(), ContentType.XLS, testdataName));
    return resultid;
  }


  public static List<? extends Storable> getTestdataStorablesFromCSV(ManagedFileID mfid) {
    try {
      List<Storable> resultlist = new ArrayList<Storable>();
      String content = getUncompressedFromFileManagement(mfid.getID(), Constants.DEFAULT_ENCODING);
      
      if (!content.contains("ä") && !content.contains("ö") && !content.contains("ü")) {
        String content2 = getUncompressedFromFileManagement(mfid.getID(), "ISO-8859-15");
        
        if (content2.contains("ä") || content2.contains("ö") || content2.contains("ü")) {
          content = content2;
        } else {
          String content3 = getUncompressedFromFileManagement(mfid.getID(), "windows-1252");
          
          if (content3.contains("ä") || content3.contains("ö") || content3.contains("ü")) {
            content = content3;
          }
        }
      }

      String fqn = content.substring(0, content.indexOf("\n")).replaceAll(";", "").trim();
      content = content.substring(content.indexOf("\n") + 1);

      long rev = TestFactoryIntegrationServiceOperationImpl.getRev();
      
      if (logger.isTraceEnabled()) {
        logger.trace("Import Testdata (" + fqn + ") in revision " + rev);
      }

      CSVDocument csvDoc = new CSVDocument(content, ";", "\"", "");
      Iterator<CSVIterator> linesIterator = csvDoc.getLines().iterator();
      
      if (!linesIterator.hasNext()) {
        throw new InvalidCSV();
      }

      boolean hasUsedColumn = false;

      // Get the object containing all member variable names in the correct order; put them into a String list
      CSVIterator membervariableNamesIterator = linesIterator.next();
      List<String> memberVariableNames = new ArrayList<>();
      
      if (membervariableNamesIterator.hasNext()) {
        String firstColumnName = membervariableNamesIterator.next();
        if (USED_VAR_NAME.equals(firstColumnName)) {
          hasUsedColumn = true;
        } else {
          memberVariableNames.add(firstColumnName);
        }
      }
      
      while (membervariableNamesIterator.hasNext()) {
        memberVariableNames.add(membervariableNamesIterator.next());
      }
      
      if (logger.isDebugEnabled()) {
        int numberOfRows = csvDoc.getLines().size() - 1;
        logger.debug("Import Testdata: " + numberOfRows + " row" + (numberOfRows != 1 ? "s" : "") + ", Variablenames: "
            + memberVariableNames);
      }

      // Build an object that can be cloned during the creation of the overall list;
      // Get all variables from the original object in the XMOM to be able to tell which type each column is
      Map<String, List<AVariable>> fqnVarsMap = new HashMap<>();
      String xml = buildXynaObjectXML(fqn, memberVariableNames, "", fqnVarsMap);
      if (logger.isTraceEnabled()) {
        logger.trace("XML: " + xml);
      }
      Storable objectToBeCloned = (Storable) Storable.fromXml(xml, rev);

      // finally fill the objects and put them into a list
      while (linesIterator.hasNext()) {
        Storable currentEntry = processRow(objectToBeCloned, fqn, rev, linesIterator.next(), fqnVarsMap, memberVariableNames, hasUsedColumn);
        resultlist.add(currentEntry);
      }
      
      return resultlist;
    } catch (Exception e) { // TODO Better exceptions
      throw new RuntimeException(e);
    }
  }


  private static XynaObject processEntry(XynaObject currentEntry, String fqn, long rev, String path, String value, Map<String, List<AVariable>> fqnVarsMap)
      throws XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, NumberFormatException, XDEV_PARAMETER_NAME_NOT_FOUND, InvalidObjectPathException {
    List<AVariable> variables = fqnVarsMap.get(fqn);
    String pathSegments[] = path.split("\\.");
    String varName = pathSegments[0];
    AVariable var = null;
    String type = "";
    
    for (AVariable a : variables) {
      if (a.getVarName().equals(varName)) {
        var = a;
        break;
      }
    }

    if (var.isJavaBaseType()) {
      type = var.getJavaTypeEnum().getClassOfType();
      
      if (type.equalsIgnoreCase("long")) {
        if (value.length() > 0) {
          currentEntry.set(varName, Long.parseLong(value));
        }
      } else if (type.equalsIgnoreCase("integer") || type.equalsIgnoreCase("int")) {
        if (value.length() > 0) {
          currentEntry.set(varName, Integer.parseInt(value));
        }
      } else if (type.equalsIgnoreCase("boolean") || type.equalsIgnoreCase("bool")) {
        if (value.length() > 0) {
          currentEntry.set(varName, Boolean.parseBoolean(value));
        }
      } else if (type.equalsIgnoreCase("double")) {
        if (value.length() > 0) {
          currentEntry.set(varName, Double.parseDouble(value));
        }
      } else {
        currentEntry.set(varName, value);
      }
    } else {
      if (path.equals(varName)) {
        if (value.length() == 0) {
          currentEntry.set(varName, null);
        }
      } else {
        String subPath = path.replaceFirst(varName + "\\.", "");
        XynaObject nextEntry = (XynaObject) currentEntry.get(varName);
        
        if (nextEntry != null) {
          processEntry(nextEntry, var.getFQClassName(), rev, subPath, value, fqnVarsMap);
        }
      }
    }

    return currentEntry;
  }


  private static Storable processRow(Storable objectToBeCloned, String fqn, long rev, CSVIterator nextRow, Map<String, List<AVariable>> fqnVarsMap,
                              List<String> membervariableNames, boolean hasUsedColumn) throws NumberFormatException,
      XDEV_PARAMETER_NAME_NOT_FOUND, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, InvalidObjectPathException {

    Storable currentEntry = objectToBeCloned.clone();

    if (hasUsedColumn) {
      if (!nextRow.hasNext()) {
        // should not happen
        return currentEntry;
      }
      String usedValue = nextRow.next().trim().toLowerCase();
      if (usedValue.equals("true")) {
        currentEntry.set(USED_VAR_NAME, true);
      } else {
        currentEntry.set(USED_VAR_NAME, false);
      }
    }

    int index = 0;
    
    while (nextRow.hasNext()) {
      String val = nextRow.next();

      if (index >= membervariableNames.size()) {
        if (index == membervariableNames.size() && "".equals(val.trim())) {
          continue; // happens with old versions of excel: it appends ";" to the end of a line
        } else {
          throw new IllegalArgumentException("Value <" + val
              + "> cannot be inserted into test data object - too many columns");
        }
      }
      
      String varPath = membervariableNames.get(index);
      processEntry(currentEntry, fqn, rev, varPath, val, fqnVarsMap);
      index++;
    }

    return currentEntry;
  }


  private static String getUncompressedFromFileManagement(String id, String encoding) {

    FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    TransientFile file = fm.retrieve(id);

    InputStream in = file.openInputStream();
    try {
      InputStreamReader is = new InputStreamReader(in, encoding);
      StringBuilder sb = new StringBuilder();
      BufferedReader br = new BufferedReader(is);
      String read = br.readLine();

      while (read != null) {
        //System.out.println(read);
        sb.append(read);
        sb.append("\n");
        read = br.readLine();
      }

      return sb.toString();
    } catch (IOException e) {
      throw new RuntimeException("Failed to read file", e);
    } finally {
      try {
        in.close();
      } catch (IOException e) {
        logger.warn("Failed to close input stream", e);
      }
    }

  }


  private static String buildXynaObjectXML(String fqn, List<String> vars, String path, Map<String, List<AVariable>> fqnVarsMap) {
    List<String> membervariables = new ArrayList<>();
    List<AVariable> variables;
    
    for (String s : vars) {
      if (s.startsWith(path) && !s.equals(path)) {
        String varName = (path.length() > 0) ? s.replaceFirst(path + "\\.", "") : s;
        int dotIndex = varName.indexOf(".");
        
        if (dotIndex == -1) {
          membervariables.add(varName);
        } else {
          String prefix = varName.substring(0, dotIndex);
          
          if (!membervariables.contains(prefix)) {
            membervariables.add(prefix);
          }
        }
      }
    }
    
    variables = fqnVarsMap.get(fqn);

    if (variables == null) {
      try {
        variables =
            DOM.generateUncachedInstance(fqn, true, TestFactoryIntegrationServiceOperationImpl.getRev()).getMemberVars();
      } catch (XPRC_InvalidPackageNameException | XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException
          | XPRC_MDMDeploymentException e1) {
        throw new RuntimeException("Failed to create XML", e1);
      }

      fqnVarsMap.put(fqn, variables);
    }
    
    int lastDotIndex = fqn.lastIndexOf(".");
    String typename = fqn.substring(lastDotIndex + 1);
    String typepath = fqn.substring(0, lastDotIndex);
    StringBuilder sb = new StringBuilder();
    
    if (path.length() == 0) {
      sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
      sb.append("<Data ReferenceName=\"" + typename + "\" ReferencePath=\"" + typepath + "\">\n");
    } else {
      String pathSplit[] = path.split("\\.");
      String varName = pathSplit[pathSplit.length - 1];
      sb.append("<Data Label=\"ABC123\" ReferenceName=\"" + typename + "\" ReferencePath=\"" + typepath + "\" VariableName=\"" + varName + "\">\n");
    }
    
    for (String s : membervariables) {
      AVariable var = null;
      
      for (AVariable a : variables) {
        if (a.getVarName().equals(s)) {
          var = a;
          break;
        }
      }
      
      if (var == null) {
        throw new RuntimeException("Failed to determine type of variable <" + s + ">");
      }
      
      if (var.isJavaBaseType()) {
        String type = var.getJavaTypeEnum().getClassOfType();
        sb.append("<Data Label=\"" + s + "\" VariableName=\"" + s + "\">\n");
        sb.append("<Meta>\n");
        sb.append("<Type>" + type + "</Type>\n");
        sb.append("</Meta>\n");
        sb.append("</Data>\n");
      } else {
        String varPath = (path.length() > 0) ? (path + "." + var.getVarName()) : var.getVarName(); 
        String cmplx = buildXynaObjectXML(var.getFQClassName(), vars, varPath, fqnVarsMap);
        sb.append(cmplx);
      }
    }
    
    sb.append("</Data>");
    return sb.toString();
  }
}
