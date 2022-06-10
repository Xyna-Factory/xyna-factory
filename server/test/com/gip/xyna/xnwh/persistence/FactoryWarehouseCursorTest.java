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
package com.gip.xyna.xnwh.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.junit.Assert;

import com.gip.xyna.xnwh.persistence.mocks.ODSConnectionMockBase;
import com.gip.xyna.xprc.xsched.XynaThreadFactory;

import junit.framework.TestCase;


/**
 *
 */
public class FactoryWarehouseCursorTest extends TestCase {

 
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    MockStorable.failAt = Integer.MAX_VALUE;
    FactoryWarehouseCursor.threadPool  =
        new ThreadPoolExecutor(15,
                               15,
                               10, TimeUnit.SECONDS,
                               new LinkedBlockingQueue<Runnable>(), new XynaThreadFactory(3));
    FactoryWarehouseCursor.threadPool.allowCoreThreadTimeOut(true);
  }
  
  
  
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    FactoryWarehouseCursor.threadPool.shutdown();
  }




  private static class MockStorable extends Storable<MockStorable> {

    private static final long serialVersionUID = 1L;
    private int id;
    
    public static int failAt = Integer.MAX_VALUE;
    
    public MockStorable(int id) {
      this.id = id;
    }

    @Override
    public ResultSetReader<? extends MockStorable> getReader() {
      return reader;
    }

    @Override
    public Object getPrimaryKey() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public <U extends MockStorable> void setAllFieldsFromData(U data) {
      // TODO Auto-generated method stub
      
    }
    
   @Override
    public String toString() {
      return String.valueOf(id);
    }
    
    public static ResultSetReader<MockStorable> reader = new ResultSetReader<MockStorable>() {

      public MockStorable read(ResultSet rs) throws SQLException {
        int rsCount = rs.getInt(1);
        if( rsCount == failAt ) {
          throw new SQLException("Failure for testing");
        }
        //System.err.println( "rsCount= "+rsCount );
        return new MockStorable(rsCount);
      }
      
    };
    
    
  }
  
  private static class MockPersistenceLayerException extends PersistenceLayerException {

    public MockPersistenceLayerException(SQLException e) {
      super(new String[]{"XYNA-04100", "23"},e);
    }
    
  }
  
  private static class MockODSConnection extends ODSConnectionMockBase {

    public static int maxData = 7;
    
    @Override
    public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows, ResultSetReader<? extends E> reader)
        throws PersistenceLayerException {
          List<E> data = new ArrayList<E>();
      
      try {
        ResultSet rs = EasyMock.createMock(ResultSet.class);
        final AtomicInteger counter = new AtomicInteger();
        
        EasyMock.expect(rs.getInt(1)).andAnswer( new IAnswer<Integer>() {
          public Integer answer() throws Throwable {
            return counter.getAndIncrement();
          }
        }).anyTimes();

        EasyMock.replay(rs);

        for( int i=0; i<maxData; ++i ) {
          E read = reader.read( rs );
          data.add( read );
        }
        return data;
      }
      catch (SQLException e) {
        //e.printStackTrace();
        throw new MockPersistenceLayerException( e);
      }  
    }
    
  }
  
  
  
  public void testNormal() throws PersistenceLayerException {
    
    MockODSConnection con = new MockODSConnection();
    
    FactoryWarehouseCursor<MockStorable> fwc = new FactoryWarehouseCursor<MockStorable>(
                    con, "Select * from dual", new Parameter(), MockStorable.reader, 5   );
    
    
    MockODSConnection.maxData = 7;
    
    List<MockStorable> data = fwc.getRemainingCacheOrNextIfEmpty(); 
    
   
    Assert.assertEquals( "[0, 1, 2, 3, 4]", data.toString() );
    data = fwc.getRemainingCacheOrNextIfEmpty(); 
    Assert.assertEquals( "[5, 6]", data.toString() );
  }
  
  public void testThrowsException() throws PersistenceLayerException {
    
    MockODSConnection con = new MockODSConnection();
    
    FactoryWarehouseCursor<MockStorable> fwc = new FactoryWarehouseCursor<MockStorable>(
                    con, "Select * from dual", new Parameter(), MockStorable.reader, 5   );
    
    
    MockODSConnection.maxData = 7;
    MockStorable.failAt = 3;
        
    List<MockStorable> data = fwc.getRemainingCacheOrNextIfEmpty(); 
    Assert.assertEquals( "[0, 1, 2]", data.toString() );
    try {
      data = fwc.getRemainingCacheOrNextIfEmpty();
      Assert.fail( "Exception expected ");
    } catch( PersistenceLayerException e ) {
      e.printStackTrace();
      Assert.assertEquals( "Failure for testing",  e.getCause().getCause().getMessage() );
    }
  }
  
  public void testThrowsExceptionOnFirstRead() throws PersistenceLayerException {
    
    MockODSConnection con = new MockODSConnection();
    
    FactoryWarehouseCursor<MockStorable> fwc = new FactoryWarehouseCursor<MockStorable>(
                    con, "Select * from dual", new Parameter(), MockStorable.reader, 5   );
    
    
    MockODSConnection.maxData = 7;
    MockStorable.failAt = 0;
    
    try {
      List<MockStorable> data = fwc.getRemainingCacheOrNextIfEmpty();
      Assert.fail( "Exception expected ");
    } catch( PersistenceLayerException e ) {
      e.printStackTrace();
      Assert.assertEquals( "Failure for testing",  e.getCause().getCause().getMessage() );
    }
  }

  public void testSingleIterator() throws PersistenceLayerException {
    
    MockODSConnection con = new MockODSConnection();
    
    FactoryWarehouseCursor<MockStorable> fwc = new FactoryWarehouseCursor<MockStorable>(
                    con, "Select * from dual", new Parameter(), MockStorable.reader );
    
    
    MockODSConnection.maxData = 7;
    
    List<MockStorable> read = new ArrayList<MockStorable>();
    int r =0;
    try {
      for( MockStorable ms : fwc.separated() ) {
        read.add(ms);
        r++;
      }
      fwc.checkForExceptions();
    } catch( PersistenceLayerException e ) {
      Assert.fail( "No exception expected "+ e.getMessage() );
    }
    Assert.assertEquals( MockODSConnection.maxData,  r);
    Assert.assertEquals( "[0, 1, 2, 3, 4, 5, 6]",  read.toString() );
  }

  public void testBatchIterator() throws PersistenceLayerException {
    
    MockODSConnection con = new MockODSConnection();
    
    FactoryWarehouseCursor<MockStorable> fwc = new FactoryWarehouseCursor<MockStorable>(
                    con, "Select * from dual", new Parameter(), MockStorable.reader );
    
    
    MockODSConnection.maxData = 7;
    
    List<List<MockStorable>> read = new ArrayList<List<MockStorable>>();
    int r =0;
    try {
      for( List<MockStorable> ms : fwc.batched(5) ) {
        read.add(ms);
        r++;
      }
      fwc.checkForExceptions();
    } catch( PersistenceLayerException e ) {
      Assert.fail( "No exception expected "+ e.getMessage() );
    }
    Assert.assertEquals( 2,  r);
    Assert.assertEquals( "[[0, 1, 2, 3, 4], [5, 6]]",  read.toString() );
  }

  public void testSingleIteratorException() throws PersistenceLayerException {
    
    MockODSConnection con = new MockODSConnection();
    
    FactoryWarehouseCursor<MockStorable> fwc = new FactoryWarehouseCursor<MockStorable>(
                    con, "Select * from dual", new Parameter(), MockStorable.reader );
    
    
    MockODSConnection.maxData = 7;
    MockStorable.failAt = 0;
    
    List<MockStorable> read = new ArrayList<MockStorable>();
    int r =0;
    try {
      for( MockStorable ms : fwc.separated() ) {
        read.add(ms);
        r++;
      }
      fwc.checkForExceptions();
    } catch( PersistenceLayerException e ) {
      e.printStackTrace();
      Assert.assertEquals( "Failure for testing",  e.getCause().getCause().getMessage() );
    }
    
    Assert.assertEquals( 0,  r);
    Assert.assertEquals( "[]",  read.toString() );
  }
  
  
}
