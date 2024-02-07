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
package xdev.xtestfactory.infrastructure.services.impl.adapter.excel;



import java.io.IOException;
import java.io.OutputStream;

import xdev.xtestfactory.infrastructure.services.impl.adapter.excel.impl.poi.PoiExcelAdapter;



public abstract class ExcelAdapter<T> {

  public abstract Workbook<T> createWorkbook(OutputStream bos) throws IOException;


  public CellData createCellData(int c, int r, String cont) {
    return new CellData(c, r, cont);
  }


  public static class CellData {

    private int c;
    private int r;
    private String cont;


    public CellData(int c, int r, String cont) {
      this.c = c;
      this.r = r;
      this.cont = cont;
    }


    public int getColumn() {
      return c;
    }


    public int getRow() {
      return r;
    }


    public String getContent() {
      return cont;
    }

  }

  public static ExcelAdapter<?> createAdapter() {
    return new PoiExcelAdapter();
  }


  public static interface Sheet<T> {


    public void addCell(CellData cellData);
  }

  public static interface Workbook<T> {


    public Sheet<T> createSheet(String name, int index);


    public void write() throws IOException;


    public void close() throws IOException;
  }
}
