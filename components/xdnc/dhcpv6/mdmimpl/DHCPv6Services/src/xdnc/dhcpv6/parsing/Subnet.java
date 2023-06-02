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
package xdnc.dhcpv6.parsing;

import java.util.ArrayList;
import java.util.List;

import xdnc.dhcpv6.parsing.Option;


public class Subnet {

  private String address;
  private int prefixlength;
  private long guiId;
  private List<Range> ranges;
  private List<Option> options;
  
  public Subnet(String address, int prefixlength) {
    this.address = address;
    this.prefixlength = prefixlength;
    ranges = new ArrayList<Range>();
    options = new ArrayList<Option>();

  }

  public void setGUIId(long guiId) {
    this.guiId = guiId;
  }

  public void addRange(Range range) {
    ranges.add(range);
  }
  
  public void addOption(Option option) {
    options.add(option);
  }
  
  public String getAddress() {
    return address;
  }
  
  public int getPrefixlength() {
    return prefixlength;
  }
  
  public long getGUIId() {
    return guiId;
  }
  public List<Range> getRanges() {
    return ranges;
  }
  
  public List<Option> getOptions() {
    return options;
  }


}
