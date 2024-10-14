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
package com.gip.xyna.xprc.xbatchmgmt.storables;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey = BatchProcessCustomizationStorable.COL_BATCH_PROCESS_ID, tableName = BatchProcessCustomizationStorable.TABLE_NAME)
public class BatchProcessCustomizationStorable extends Storable<BatchProcessCustomizationStorable> {
  private static final long serialVersionUID = 1L;

  public static final String TABLE_NAME = "bpcustomization";
  
  public static ResultSetReader<? extends BatchProcessCustomizationStorable> reader = new BatchProcessCustomizationStorableReader();
  public static ResultSetReader<Long> idReader = new BatchProcessCustomizationStorableIdReader();
  
  public static final String COL_BATCH_PROCESS_ID = "batchProcessId"; //ID des Batch Processes
  public static final String COL_COUNTER0 = "counter0"; //Erstes Custom-Feld, für die freie Verwendung durch Komponenten.
  public static final String COL_COUNTER1 = "counter1";
  public static final String COL_COUNTER2 = "counter2";
  public static final String COL_COUNTER3 = "counter3";
  public static final String COL_COUNTER4 = "counter4";
  public static final String COL_COUNTER5 = "counter5";
  public static final String COL_COUNTER6 = "counter6";
  public static final String COL_COUNTER7 = "counter7";
  public static final String COL_COUNTER8 = "counter8";
  public static final String COL_COUNTER9 = "counter9";

  public static final int NUM_COUNTER = 10;

  
  @Column(name = COL_BATCH_PROCESS_ID, index = IndexType.PRIMARY)
  private long batchProcessId;//ID des Batch Processes

  @Column(name = COL_COUNTER0)
  private Double counter0; //Erstes Counter-Feld, für die freie Verwendung durch Komponenten.
  @Column(name = COL_COUNTER1)
  private Double counter1;
  @Column(name = COL_COUNTER2)
  private Double counter2; 
  @Column(name = COL_COUNTER3)
  private Double counter3; 
  @Column(name = COL_COUNTER4)
  private Double counter4; 
  @Column(name = COL_COUNTER5)
  private Double counter5; 
  @Column(name = COL_COUNTER6)
  private Double counter6; 
  @Column(name = COL_COUNTER7)
  private Double counter7;
  @Column(name = COL_COUNTER8)
  private Double counter8; 
  @Column(name = COL_COUNTER9)
  private Double counter9; 

  private ReentrantLock lock = new ReentrantLock();
  
  public BatchProcessCustomizationStorable(){
  }

  public BatchProcessCustomizationStorable(long batchProcessId){
    this.batchProcessId = batchProcessId;
  }

  public BatchProcessCustomizationStorable(BatchProcessCustomizationStorable data) {
    setAllFieldsFromData(data);
  }

  @Override
  public ResultSetReader<? extends BatchProcessCustomizationStorable> getReader() {
    return reader;
  }

  @Override
  public Long getPrimaryKey() {
    return Long.valueOf(batchProcessId);
  }

  @Override
  public <U extends BatchProcessCustomizationStorable> void setAllFieldsFromData(U data) {
    BatchProcessCustomizationStorable cast = data;
    this.batchProcessId = cast.batchProcessId;
    this.counter0 = cast.counter0;
    this.counter1 = cast.counter1;
    this.counter2 = cast.counter2;
    this.counter3 = cast.counter3;
    this.counter4 = cast.counter4;
    this.counter5 = cast.counter5;
    this.counter6 = cast.counter6;
    this.counter7 = cast.counter7;
    this.counter8 = cast.counter8;
    this.counter9 = cast.counter9;
  }
  
  private static class BatchProcessCustomizationStorableIdReader implements ResultSetReader<Long> {
    public Long read(ResultSet rs) throws SQLException {
      return rs.getLong(COL_BATCH_PROCESS_ID);
    }
  }
  
  private static class BatchProcessCustomizationStorableReader implements ResultSetReader<BatchProcessCustomizationStorable> {
    public BatchProcessCustomizationStorable read(ResultSet rs) throws SQLException {
      BatchProcessCustomizationStorable result = new BatchProcessCustomizationStorable();
      fillByResultset(result, rs);
      return result;
    }
  }
  
  private static void fillByResultset(BatchProcessCustomizationStorable bpcs, ResultSet rs) throws SQLException {
    bpcs.batchProcessId = rs.getLong(COL_BATCH_PROCESS_ID);
    bpcs.counter0 = getDoubleOrNull(rs, COL_COUNTER0);
    bpcs.counter1 = getDoubleOrNull(rs, COL_COUNTER1);
    bpcs.counter2 = getDoubleOrNull(rs, COL_COUNTER2);
    bpcs.counter3 = getDoubleOrNull(rs, COL_COUNTER3);
    bpcs.counter4 = getDoubleOrNull(rs, COL_COUNTER4);
    bpcs.counter5 = getDoubleOrNull(rs, COL_COUNTER5);
    bpcs.counter6 = getDoubleOrNull(rs, COL_COUNTER6);
    bpcs.counter7 = getDoubleOrNull(rs, COL_COUNTER7);
    bpcs.counter8 = getDoubleOrNull(rs, COL_COUNTER8);
    bpcs.counter9 = getDoubleOrNull(rs, COL_COUNTER9);
  }
  
  private static Double getDoubleOrNull(ResultSet rs, String col) throws SQLException {
    Double d = rs.getDouble(col);
    if(rs.wasNull() ) {
      return null;
    }
    return d;
  }

  public ReentrantLock getLock() {
    return lock;
  }
  
  public static enum Operation {
    Set() {
      public Double exec(Double old, Double value) {
        return value;
      }
    },
    Add() {
      public Double exec(Double old, Double value) {
        if( old == null ) {
          return value; //Initalisierung: (null + 1 => 1)
        } else {
          if( value == null ) {
            return old; //keine Änderung: (1 + null => 1);
          } else {
            return new Double(old.doubleValue()+value.doubleValue()); //Änderung: (1 + 2 => 3)
          }
        }
      }
    };
    
    public abstract Double exec(Double old, Double value);
    
  }
  
  public void modifyCounter(int i, Operation operation, Double value) {
    switch( i ) {
      case 0: counter0 = operation.exec( counter0, value ); break;
      case 1: counter1 = operation.exec( counter1, value ); break;
      case 2: counter2 = operation.exec( counter2, value ); break;
      case 3: counter3 = operation.exec( counter3, value ); break;
      case 4: counter4 = operation.exec( counter4, value ); break;
      case 5: counter5 = operation.exec( counter5, value ); break;
      case 6: counter6 = operation.exec( counter6, value ); break;
      case 7: counter7 = operation.exec( counter7, value ); break;
      case 8: counter8 = operation.exec( counter8, value ); break;
      case 9: counter9 = operation.exec( counter9, value ); break;
      default:
        throw new IllegalArgumentException("invalid index "+i+" out of range [0,"+NUM_COUNTER+"[");
    }
  }

  public List<Double> getCountersAsList() {
    List<Double> counters = new ArrayList<Double>(NUM_COUNTER);
    counters.add(counter0);
    counters.add(counter1);
    counters.add(counter2);
    counters.add(counter3);
    counters.add(counter4);
    counters.add(counter5);
    counters.add(counter6);
    counters.add(counter7);
    counters.add(counter8);
    counters.add(counter9);
    return counters;
  }

  public String getCounterAsString(int i) {
    Double d = getCounter(i);
    if( d == null ) {
      return null;
    } else {
      return String.valueOf(d);
    }
  }
  
  public Double getCounter(int i) {
    switch( i ) {
      case 0: return counter0;
      case 1: return counter1;
      case 2: return counter2;
      case 3: return counter3;
      case 4: return counter4;
      case 5: return counter5;
      case 6: return counter6;
      case 7: return counter7;
      case 8: return counter8;
      case 9: return counter9;
      default:
        throw new IllegalArgumentException("invalid index "+i+" out of range [0,"+NUM_COUNTER+"[");
    }
  }
  
  public void setCounter(int i, Double value) {
    switch( i ) {
      case 0: counter0 = value; break;
      case 1: counter1 = value; break;
      case 2: counter2 = value; break;
      case 3: counter3 = value; break;
      case 4: counter4 = value; break;
      case 5: counter5 = value; break;
      case 6: counter6 = value; break;
      case 7: counter7 = value; break;
      case 8: counter8 = value; break;
      case 9: counter9 = value; break;
      default:
        throw new IllegalArgumentException("invalid index "+i+" out of range [0,"+NUM_COUNTER+"[");
    }
  }
  
  
  public long getBatchProcessId() {
    return batchProcessId;
  }

  
  public void setBatchProcessId(long batchProcessId) {
    this.batchProcessId = batchProcessId;
  }

  
  public Double getCounter0() {
    return counter0;
  }

  
  public void setCounter0(Double counter0) {
    this.counter0 = counter0;
  }

  
  public Double getCounter1() {
    return counter1;
  }

  
  public void setCounter1(Double counter1) {
    this.counter1 = counter1;
  }

  
  public Double getCounter2() {
    return counter2;
  }

  
  public void setCounter2(Double counter2) {
    this.counter2 = counter2;
  }

  
  public Double getCounter3() {
    return counter3;
  }

  
  public void setCounter3(Double counter3) {
    this.counter3 = counter3;
  }

  
  public Double getCounter4() {
    return counter4;
  }

  
  public void setCounter4(Double counter4) {
    this.counter4 = counter4;
  }

  
  public Double getCounter5() {
    return counter5;
  }

  
  public void setCounter5(Double counter5) {
    this.counter5 = counter5;
  }

  
  public Double getCounter6() {
    return counter6;
  }

  
  public void setCounter6(Double counter6) {
    this.counter6 = counter6;
  }

  
  public Double getCounter7() {
    return counter7;
  }

  
  public void setCounter7(Double counter7) {
    this.counter7 = counter7;
  }

  
  public Double getCounter8() {
    return counter8;
  }

  
  public void setCounter8(Double counter8) {
    this.counter8 = counter8;
  }

  
  public Double getCounter9() {
    return counter9;
  }

  
  public void setCounter9(Double counter9) {
    this.counter9 = counter9;
  }
  
}
