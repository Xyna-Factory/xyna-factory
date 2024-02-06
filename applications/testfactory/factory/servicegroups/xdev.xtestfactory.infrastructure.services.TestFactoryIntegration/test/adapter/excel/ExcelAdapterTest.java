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
package adapter.excel;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import junit.framework.TestCase;
import xdev.xtestfactory.infrastructure.services.impl.adapter.excel.ExcelAdapter;
import xdev.xtestfactory.infrastructure.services.impl.adapter.excel.ExcelAdapter.Sheet;
import xdev.xtestfactory.infrastructure.services.impl.adapter.excel.ExcelAdapter.Workbook;



public class ExcelAdapterTest extends TestCase {


  public void testWriteToFile() throws Exception {
    ExcelAdapter<?> adapter = ExcelAdapter.createAdapter();
    File f = new File("test.xls");
    try (FileOutputStream os = new FileOutputStream(f)) {
      Workbook<?> workbook = adapter.createWorkbook(os);
      Sheet<?> sheet = workbook.createSheet("First Page", 0);
      xdev.xtestfactory.infrastructure.services.impl.adapter.excel.ExcelAdapter.CellData label = adapter.createCellData(0, 0, "Test");
      sheet.addCell(label);
      workbook.write();
      workbook.close();


      FileInputStream file = new FileInputStream(f);
      try (HSSFWorkbook readWorkbook = new HSSFWorkbook(file)) {
        HSSFSheet readSheet = readWorkbook.getSheetAt(0);
        assertEquals("Test", readSheet.getRow(0).getCell(0).getStringCellValue());
      }
    } finally {
      Files.delete(f.toPath());
    }
  }
}
