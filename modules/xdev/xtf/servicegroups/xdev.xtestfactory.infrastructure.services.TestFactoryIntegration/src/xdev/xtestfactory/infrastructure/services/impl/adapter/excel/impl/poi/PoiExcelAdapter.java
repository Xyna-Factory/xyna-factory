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
package xdev.xtestfactory.infrastructure.services.impl.adapter.excel.impl.poi;



import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import xdev.xtestfactory.infrastructure.services.impl.adapter.excel.ExcelAdapter;



public class PoiExcelAdapter extends ExcelAdapter<PoiExcelAdapter> {

  @Override
  public Workbook<PoiExcelAdapter> createWorkbook(OutputStream bos) throws IOException {
    return new PoiWorkbook(bos);
  }


  public static class PoiWorkbook implements Workbook<PoiExcelAdapter> {

    private org.apache.poi.ss.usermodel.Workbook workbook;
    private OutputStream stream;


    public PoiWorkbook(OutputStream stream) {
      this.workbook = new HSSFWorkbook();
      this.stream = stream;
    }


    @Override
    public Sheet<PoiExcelAdapter> createSheet(String name, int index) {
      return new PoiSheet(workbook.createSheet());
    }


    @Override
    public void write() throws IOException {
      workbook.write(stream);

    }


    @Override
    public void close() throws IOException {
      workbook.close();
    }

  }

  public static class PoiSheet implements Sheet<PoiExcelAdapter> {

    private org.apache.poi.ss.usermodel.Sheet sheet;


    public PoiSheet(org.apache.poi.ss.usermodel.Sheet sheet) {
      this.sheet = sheet;
    }


    public void addCell(CellData cellData) {
      Row row = sheet.getRow(cellData.getRow());
      if (row == null) {
        row = sheet.createRow(cellData.getRow());
      }
      Cell cell = row.getCell(cellData.getColumn());
      if (cell == null) {
        cell = row.createCell(cellData.getColumn());
      }
      cell.setCellValue(cellData.getContent());
    }

  }


}
