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
package com.gip.xyna.xprc.xsched;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.PredecessorFinder;
import com.gip.xyna.xprc.SuccessorFinder;
import com.gip.xyna.xprc.XynaOrder;



@Persistable(primaryKey = OrderSeriesManagementStorable.COL_ID, tableName = OrderSeriesManagementStorable.TABLE_NAME)
public final class OrderSeriesManagementStorable extends Storable<OrderSeriesManagementStorable> {


  private static final long serialVersionUID = -6694918288510582615L;


  public static final String TABLE_NAME = "orderseriesmanagement";
  static final String COL_ID = "id";
  static final String COL_PRE_FINDERS = "preFinders";
  static final String COL_SUC_FINDERS = "sucFinders";

  private static final ResultSetReader<OrderSeriesManagementStorable> reader =
      new OrderSeriesManagementStorableReader();


  @Column(name = COL_ID)
  private int id;
  @Column(name = COL_PRE_FINDERS, type = ColumnType.BLOBBED_JAVAOBJECT)
  private transient HashMap<XynaOrder, ArrayList<PredecessorFinder>> preFinders;
  @Column(name = COL_SUC_FINDERS, type = ColumnType.BLOBBED_JAVAOBJECT)
  private transient HashMap<XynaOrder, ArrayList<SuccessorFinder>> sucFinders;


  public OrderSeriesManagementStorable(HashMap<XynaOrder, ArrayList<PredecessorFinder>> preFindersWaiting,
                                       HashMap<XynaOrder, ArrayList<SuccessorFinder>> sucFindersWaiting) {
    this.preFinders = preFindersWaiting;
    this.sucFinders = sucFindersWaiting;
    id = 1;
  }


  public OrderSeriesManagementStorable() {
    id = 1;
  }


  public HashMap<XynaOrder, ArrayList<SuccessorFinder>> getSucFinders() {
    return sucFinders;
  }


  public HashMap<XynaOrder, ArrayList<PredecessorFinder>> getPreFinders() {
    return preFinders;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }

  
  public int getId() {
    return id;
  }


  @Override
  public ResultSetReader<? extends OrderSeriesManagementStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends OrderSeriesManagementStorable> void setAllFieldsFromData(U data) {
    OrderSeriesManagementStorable cast = data;
    this.id = cast.id;
    this.preFinders = cast.preFinders;
    this.sucFinders = cast.sucFinders;
  }


  //prefinder und sucfinder k�nnen mit filterclassloader gespeichert worden sein und m�ssen deshalb manuell
  //behandelt werden
  private void writeObject(java.io.ObjectOutputStream s) throws IOException {
    s.defaultWriteObject();
    s.writeInt(preFinders.size());
    for (XynaOrder xo : preFinders.keySet()) {
      s.writeObject(xo);
      s.writeInt(preFinders.get(xo).size());
      for (PredecessorFinder pf : preFinders.get(xo)) {
        s.writeObject(new SerializableClassloadedObject(pf));
      }
    }
    //sucFinders
    s.writeInt(sucFinders.size());
    for (XynaOrder xo : sucFinders.keySet()) {
      s.writeObject(xo);
      s.writeInt(sucFinders.get(xo).size());
      for (SuccessorFinder sf : sucFinders.get(xo)) {
        s.writeObject(new SerializableClassloadedObject(sf));
      }
    }
  }


  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    int hashMapSize = s.readInt();
    preFinders = new HashMap<XynaOrder, ArrayList<PredecessorFinder>>();
    for (int i = 0; i < hashMapSize; i++) {
      XynaOrder xo = (XynaOrder) s.readObject();
      int preFinderListSize = s.readInt();
      ArrayList<PredecessorFinder> list = new ArrayList<PredecessorFinder>();
      preFinders.put(xo, list);
      for (int j = 0; j < preFinderListSize; j++) {
        list.add((PredecessorFinder) ((SerializableClassloadedObject) s.readObject()).getObject());
      }
    }
    //sucs
    hashMapSize = s.readInt();
    sucFinders = new HashMap<XynaOrder, ArrayList<SuccessorFinder>>();
    for (int i = 0; i < hashMapSize; i++) {
      XynaOrder xo = (XynaOrder) s.readObject();
      int sucFinderListSize = s.readInt();
      ArrayList<SuccessorFinder> list = new ArrayList<SuccessorFinder>();
      sucFinders.put(xo, list);
      for (int j = 0; j < sucFinderListSize; j++) {
        list.add((SuccessorFinder) ((SerializableClassloadedObject) s.readObject()).getObject());
      }
    }
  }


  public static class OrderSeriesManagementStorableReader implements ResultSetReader<OrderSeriesManagementStorable> {

    public OrderSeriesManagementStorable read(ResultSet rs) throws SQLException {
      OrderSeriesManagementStorable result = new OrderSeriesManagementStorable();
      fillFromData(result, rs);
      return result;
    }

  }


  private static void fillFromData(OrderSeriesManagementStorable osms, ResultSet rs) throws SQLException {
    osms.id = rs.getInt(COL_ID);
    osms.preFinders =
        (HashMap<XynaOrder, ArrayList<PredecessorFinder>>) osms.readBlobbedJavaObjectFromResultSet(rs, COL_PRE_FINDERS);
    osms.sucFinders =
        (HashMap<XynaOrder, ArrayList<SuccessorFinder>>) osms.readBlobbedJavaObjectFromResultSet(rs, COL_SUC_FINDERS);
  }

}
