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

package com.gip.xyna.xfint.xnumdav;



import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;


// TODO this is not really storable at the moment
@Persistable(tableName = StorableAggregatableDataEntry.TABLE_NAME, primaryKey = StorableAggregatableDataEntry.COL_ID)
public class StorableAggregatableDataEntry
//extends Storable<StorableAggregatableDataEntry<T>>
                implements
                  IAggregatableDataEntry, Serializable {


  private static final long serialVersionUID = 6991949988722764897L;

  private static AggregatableDataEntryResultSetReader reader = new AggregatableDataEntryResultSetReader();


  public static final String TABLE_NAME = "aggregationstore";
  public static final String COL_ID = "id";

  public static final String COL_X_VALUE = "xvalueraw";
  public static final String COL_Y_VALUE = "yvalueraw";

  public static final String COL_NUMBER_OF_DATAPOINTS = "numberOfDatapoints";
  public static final String COL_MINIMUM = "minimum";
  public static final String OCL_MAXIMUM = "maximum";

  private static enum NumberType {
    INT, LONG, DOUBLE;
  }


  @Column(name = COL_ID)
  private int id;

  @Column(name = COL_X_VALUE, type = ColumnType.BLOBBED_JAVAOBJECT)
  private BigDecimal xValueRaw;

  @Column(name = COL_Y_VALUE, type = ColumnType.BLOBBED_JAVAOBJECT)
  private BigDecimal yValueRaw;

  private long numberOfDatapoints;
  private Number minimumValue;
  private Number maximumValue;

  private NumberType xValueClass;
  private NumberType yValueClass;


  public StorableAggregatableDataEntry(Number xvalue, Number yvalue) {

    if (xvalue instanceof Long) {
      this.xValueRaw = new BigDecimal((Long) xvalue);
      xValueClass = NumberType.LONG;
    } else if (xvalue instanceof Integer) {
      this.xValueRaw = new BigDecimal((Integer) xvalue);
      xValueClass = NumberType.INT;
    } else if (xvalue instanceof Double) {
      this.xValueRaw = new BigDecimal((Double) xvalue);
      xValueClass = NumberType.DOUBLE;
    } else {
      throw new RuntimeException("Unsupported xvalue type: "
                      + (xvalue != null ? xvalue.getClass().getSimpleName() : "<null>"));
    }

    if (yvalue instanceof Long) {
      this.yValueRaw = new BigDecimal((Long) yvalue);
      yValueClass = NumberType.LONG;
    } else if (yvalue instanceof Integer) {
      this.yValueRaw = new BigDecimal((Integer) yvalue);
      yValueClass = NumberType.INT;
    } else if (yvalue instanceof Double) {
      this.yValueRaw = new BigDecimal((Double) yvalue);
      yValueClass = NumberType.DOUBLE;
    } else {
      throw new RuntimeException("Unsupported yvalue type: "
                      + (yvalue != null ? yvalue.getClass().getSimpleName() : "<null>"));
    }

    this.minimumValue = yvalue;
    this.maximumValue = yvalue;
    this.numberOfDatapoints = 1;

  }


  StorableAggregatableDataEntry() {
    numberOfDatapoints = 1;
  }


//  @Override
  public Object getPrimaryKey() {
    return id;
  }


  public int getId() {
    return this.id;
  }


//  @Override
  public ResultSetReader<? extends StorableAggregatableDataEntry> getReader() {
    return (ResultSetReader<? extends StorableAggregatableDataEntry>) reader;
  }


//  @Override
  public <U extends StorableAggregatableDataEntry> void setAllFieldsFromData(U data) {
    StorableAggregatableDataEntry cast = data;
    this.id = cast.id;
    this.xValueClass = cast.xValueClass;
    this.xValueRaw = cast.xValueRaw;
    this.yValueClass = cast.yValueClass;
    this.yValueRaw = cast.yValueRaw;
    this.minimumValue = cast.minimumValue;
    this.maximumValue = cast.maximumValue;
    this.numberOfDatapoints = cast.numberOfDatapoints;
  }


  static void fillByResultSet(ResultSet rs, StorableAggregatableDataEntry storable) throws SQLException {
    // FIXME this is incomplete...
//    storable.xValueRaw = (BigDecimal) Storable.readBlobbedJavaObjectFromResultSet(rs, COL_X_VALUE);
//    storable.yValueRaw = (BigDecimal) Storable.readBlobbedJavaObjectFromResultSet(rs, COL_Y_VALUE);
  }


  //TODO ergebnisse cachen
  public Number getValue() {
    if (NumberType.LONG == yValueClass) {
      return Long.valueOf(yValueRaw.divide(new BigDecimal(numberOfDatapoints), RoundingMode.HALF_UP).longValue());
    } else if (NumberType.INT == yValueClass) {
      return Integer.valueOf(yValueRaw.divide(new BigDecimal(numberOfDatapoints), RoundingMode.HALF_UP).intValue());
    } else if (NumberType.DOUBLE == yValueClass) {
      return Double.valueOf(yValueRaw.divide(new BigDecimal(numberOfDatapoints), yValueRaw.scale() + new BigDecimal(1.0/numberOfDatapoints).scale(), RoundingMode.HALF_UP).doubleValue());
    } else {
      throw new RuntimeException("illegal class setting for y value: "
                      + yValueClass.toString());
    }
  }


  public Number getValueX() {
    if (xValueClass == null) {
      return null;
    }
    if (NumberType.LONG == xValueClass) {
      return Long.valueOf(xValueRaw.divide(new BigDecimal(numberOfDatapoints), RoundingMode.HALF_UP).longValue());
    } else if (NumberType.INT == xValueClass) {
      return Integer.valueOf(xValueRaw.divide(new BigDecimal(numberOfDatapoints), RoundingMode.HALF_UP).intValue());
    } else if (NumberType.DOUBLE == xValueClass) {
      return Double.valueOf(xValueRaw.divide(new BigDecimal(numberOfDatapoints), yValueRaw.scale() + new BigDecimal(1.0/numberOfDatapoints).scale(), RoundingMode.HALF_UP).doubleValue());
    } else {
      throw new RuntimeException("illegal class setting for x value: "
                                 + yValueClass.toString());
    }
  }


  public Number getMaximumValue() {
    return maximumValue;
  }


  public Number getMinimumValue() {
    return minimumValue;
  }


  public long getNumberOfDatapoints() {
    return numberOfDatapoints;
  }


  void setMinimum(Number minimum) {
    this.minimumValue = minimum;
  }


  void setMaximum(Number maximum) {
    this.maximumValue = maximum;
  }


  void setTotalNumberOfDatapoints(int totalNumberOfDatapoints) {
    this.numberOfDatapoints = totalNumberOfDatapoints;
  }


  private BigDecimal getValueXRaw() {
    return xValueRaw;
  }


  private BigDecimal getValueRaw() {
    return yValueRaw;
  }


  public void mergeEntryIntoThis(StorableAggregatableDataEntry newEntry) {

    xValueRaw = xValueRaw.add(newEntry.getValueXRaw());
    yValueRaw = yValueRaw.add(newEntry.getValueRaw());

    minimumValue = NumberHelper.getMinimum(minimumValue, newEntry.getMinimumValue());
    maximumValue = NumberHelper.getMaximum(maximumValue, newEntry.getMaximumValue());

    numberOfDatapoints += newEntry.getNumberOfDatapoints();

  }

}
