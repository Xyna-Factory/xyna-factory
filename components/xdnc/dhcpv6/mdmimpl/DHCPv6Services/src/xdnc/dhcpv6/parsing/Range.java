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
package xdnc.dhcpv6.parsing;

import java.util.ArrayList;
import java.util.List;


public class Range {
  
  private String ipStart;
  private String ipEnd;
  private int prefixLength;
  private String pooltype;
  private long guiId;
  private List<Option> options;

  public Range(String ipStart, String ipEnd, int prefixLength, String pooltype) {
    this.ipEnd = ipEnd;
    this.ipStart = ipStart;
    this.prefixLength = prefixLength;
    this.pooltype = pooltype;
    options = new ArrayList<Option>();
  }

  public void setGUIId(long guiId) {
    this.guiId = guiId;
  }

  public void addOption(Option option) {
    options.add(option);
  }
  
  public String getIpStart() {
    return ipStart;
  }
  
  public String getIpEnd() {
    return ipEnd;
  }
  
  public int getPrefixlength() {
    return prefixLength;
  }
  
  public long getGUIId() {
    return guiId;
  }
  
  public String getPooltype() {
    return pooltype;
  }
  
  public List<Option> getOptions() {
    return options;
  }

}
