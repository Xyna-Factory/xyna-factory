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
package com.gip.xyna.utils.misc;

import junit.framework.Assert;
import junit.framework.TestCase;


/**
 *
 */
public class LazyDateFormatTest extends TestCase {

  
  public void testToMillis() {
    
    String date = "2001-07-04T12:08:56";
    String format = "yyyy-MM-dd'T'HH:mm:ss";
    
    LazyDateFormat ldf = new LazyDateFormat(date,format);
    
    Assert.assertEquals("994241336000", String.valueOf( ldf.toMillis(date, format) ));
   
  }
  
  public void testFromMillis() {
    
    long millis = 994241336000L;
    String format = "yyyy-MM-dd'T'HH:mm:ss";
    
    LazyDateFormat ldf = new LazyDateFormat("",format);
   
    Assert.assertEquals("2001-07-04T12:08:56", ldf.format(millis, format));
    
    
  }
  
  public void testCache_ToMillis() {
    resetDateFormat();
    
    String date1 = "2001-07-04T12:08:56";
    String date2 = "2014-07-04T12:08:56";
    String format = "yyyy-MM-dd'T'HH:mm:ss";
    
    LazyDateFormat ldf = new LazyDateFormat(date1,format);
    
  //  Assert.assertEquals("C=0 F=0 P=0", countDateFormat() );
    
    Assert.assertEquals("994241336000", String.valueOf( ldf.toMillis(date1, format) ));
 
  //  Assert.assertEquals("C=1 F=0 P=1", countDateFormat() );
    Assert.assertEquals("994241336000", String.valueOf( ldf.toMillis(date1, format) ));
//    Assert.assertEquals("C=1 F=0 P=1", countDateFormat() );
    
    
    Assert.assertEquals("1404468536000", String.valueOf( ldf.toMillis(date2, format) ));
   // Assert.assertEquals("C=1 F=0 P=2", countDateFormat() );
    Assert.assertEquals("1404468536000", String.valueOf( ldf.toMillis(date2, format) ));
   // Assert.assertEquals("C=1 F=0 P=2", countDateFormat() );
    
  }
  
  public void testCache_SecondLDF() {
    resetDateFormat();

    String date1 = "2001-07-04T12:08:56";
    String date2 = "2014-07-04T12:08:56";
    String format = "yyyy-MM-dd'T'HH:mm:ss";

    LazyDateFormat ldf1 = new LazyDateFormat(date1,format);
    LazyDateFormat ldf2 = new LazyDateFormat(date1,format);
    
//    Assert.assertEquals("C=0 F=0 P=0", countDateFormat() );
    
    Assert.assertEquals("994241336000", String.valueOf( ldf1.toMillis(date1, format) ));
    
    Assert.assertEquals("1404468536000", String.valueOf( ldf2.toMillis(date2, format) ));
    
  //  Assert.assertEquals("C=1 F=0 P=2", countDateFormat() );
  }
  
  public void testLocaleAndTimezone() {
    resetDateFormat();
    String date1 = "2001-12-04 12:08:56";
    String format1 = "yyyy-MM-dd HH:mm:ss|timezone=EST";
    String format2 = "yyyy-MMM-dd'T'HH:mm:ssZ|locale=DE";
    String format3 = "yyyy-MMM-dd'T'HH:mm:ss'Z'|locale=EN,timezone=UTC";
    String format4 = "yyyy-MMM-dd HH:mm:ssZ|timezone=PST locale=EN";

    LazyDateFormat ldf1 = new LazyDateFormat();
    
    assertEquals("2001-Dez-04T18:08:56+0100", ldf1.format(ldf1.toMillis(date1, format1), format2));
    assertEquals("2001-Dec-04T17:08:56Z", ldf1.format(ldf1.toMillis(date1, format1), format3));
    assertEquals("2001-Dec-04 09:08:56-0800", ldf1.format(ldf1.toMillis(date1, format1), format4));

    String date2 = "2001-Dec-04 12:08:56-0800";
    
    LazyDateFormat ldf2 = new LazyDateFormat();
    assertEquals("2001-Dez-04T21:08:56+0100", ldf2.format(ldf2.toMillis(date2, format4), format2));

  }

  private String countDateFormat() {
   // return "C="+DateFormat.createCnt+" F="+DateFormat.formatCnt+" P="+DateFormat.parseCnt;
   return "";
  }

  private void resetDateFormat() {
    LazyDateFormat.DateFormatCache.clear();
    /*
    DateFormat.createCnt = 0;
    DateFormat.formatCnt = 0;
    DateFormat.parseCnt = 0;
    */
  }

  
}
