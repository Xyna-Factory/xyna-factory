/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xmcp.oas.fman.tools;


public class XmomType {

  private final FqName fqName;
  private final RtcData rtc;

  
  public XmomType(String fqn, RtcData rtc) {
    this(new FqName(fqn), rtc);
  }
  
  public XmomType(FqName fqn, RtcData rtc) {
    if (fqn == null) { throw new IllegalArgumentException("Fqname is null."); }
    if (rtc == null) { throw new IllegalArgumentException("RTC is null."); }
    this.fqName = fqn;
    this.rtc = rtc;
  }
  
  public String getFqName() {
    return fqName.getFqName();
  }
  
  public FqName getFqNameInstance() {
    return fqName;
  }
  
  public RtcData getRtc() {
    return rtc;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof XmomType)) { return false; }
    XmomType input = (XmomType) obj;
    if (input.rtc.getRevision() != rtc.getRevision()) {
      return false;
    }
    return getFqName().equals(input.getFqName());
  }
  
  @Override
  public int hashCode() {
    return getFqName().hashCode();
  }

  /*
  @Override
  public int compareTo(XmomType xmom) {
    if (xmom == null) { return 1; }
    int val = Long.compare(rtc.getRevision(), xmom.rtc.getRevision());
    if (val != 0) { return val; }
    return getFqName().compareTo(xmom.getFqName());
  }
  */
  
}
