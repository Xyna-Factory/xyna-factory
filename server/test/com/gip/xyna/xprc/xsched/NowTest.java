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
package com.gip.xyna.xprc.xsched;

import com.gip.xyna.xprc.xsched.xynaobjects.DateFormat;
import com.gip.xyna.xprc.xsched.xynaobjects.Now;

import junit.framework.TestCase;

public class NowTest extends TestCase {

  private static class YyyyMMDdTHHMmSs extends DateFormat {
    private static final long serialVersionUID = 1L;

    @Override
    public DateFormat clone() {
      return new YyyyMMDdTHHMmSs();
    }

    @Override
    public DateFormat clone(boolean deep) { 
      return new YyyyMMDdTHHMmSs();
    }

    @Override
    public String getFormat() {
      return "yyyy-MM-dd'T'HH:mm:ss";
    }
    
  }
  
  
  public void testNow2ArgConstructor() {
    DateFormat format = new YyyyMMDdTHHMmSs();
    Now now = new Now("2024-02-05T11:51:33", format);
    assertEquals("2024-02-05T11:51:33", now.getDate());
    assertEquals(format, now.getFormat());
  }
  
  public void testNowDateBeforeFormat() {
    Now now = new Now();
    now.setDate("2024-02-05T11:51:33");
    YyyyMMDdTHHMmSs format = new YyyyMMDdTHHMmSs();
    now.setFormat(format);
    assertEquals("2024-02-05T11:51:33", now.getDate());
    assertEquals(format, now.getFormat());
  }
  

  public void testNowFormatBeforeDate() {
    Now now = new Now();
    YyyyMMDdTHHMmSs format = new YyyyMMDdTHHMmSs();
    now.setFormat(format);
    now.setDate("2024-02-05T11:51:33");
    assertEquals("2024-02-05T11:51:33", now.getDate());
    assertEquals(format, now.getFormat());
  }
  
  public void testNowBuilder() {
    Now.Builder builder = new Now.Builder();
    YyyyMMDdTHHMmSs format = new YyyyMMDdTHHMmSs();
    builder.date("2024-02-05T11:51:33");
    builder.format(format);
    Now now = builder.instance();
    assertEquals(format, now.getFormat());
  }
  
  public void testNowNulledDate() {
    Now now = new Now();
    YyyyMMDdTHHMmSs format = new YyyyMMDdTHHMmSs();
    now.setFormat(null);
    now.setDate(null);
    now.setFormat(format);
    assertEquals(null, now.getDate());
    assertEquals(format, now.getFormat());
  }
}
