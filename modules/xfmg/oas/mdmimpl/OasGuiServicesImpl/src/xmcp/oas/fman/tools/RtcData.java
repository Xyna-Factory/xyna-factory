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

import java.util.Optional;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;


public class RtcData implements Comparable<RtcData> {

  private final long _revision;
  private final String _name;
  private final String _version;
  private final RuntimeContext _rtc;
  
  
  public RtcData(String nameIn) {
    this(nameIn, null);
  }
  
  
  public RtcData(String nameIn, String versionIn) {
    if (nameIn == null) {
      throw new IllegalArgumentException("Rtc name is empty");
    }
    this._name = nameIn.trim();
    if (this._name.isEmpty()) {
      throw new IllegalArgumentException("Rtc name is empty");
    }
    String version = versionIn;
    if (version != null) {
      version = version.trim();
      if (version.isEmpty()) {
        version = null;
      }
    }
    this._version = version;
    if (version == null) {
      this._rtc = new Workspace(this._name);
    } else {
      this._rtc = new Application(this._name, this._version);
    }
    this._revision = getRevisionOfRtc(this._rtc);
  }
  
  
  public RtcData(long revision) {
    this(revision, getRtcOfRevision(revision));
  }
  
  
  public RtcData(RuntimeContext rtc) {
    this(getRevisionOfRtc(rtc), rtc);
  }
  
  
  public RtcData(long revision, RuntimeContext rtc) {
    this._revision = revision;
    this._rtc = rtc;
    if (rtc instanceof Workspace) {
      this._name = rtc.getName();
      this._version = null;
    } else if (rtc instanceof Application) {
      Application app = (Application) rtc;
      this._name = app.getName();
      this._version = app.getVersionName();
    } else {
      throw new IllegalArgumentException("Unexpected run time context type: " + rtc.getType());
    }
  }
  
  
  private static RuntimeContext getRtcOfRevision(long revision) {
    try {
      return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                        .getRuntimeContext(revision);
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to determine run time context of revision " + revision, e);
    }
  }
  
  
  private static long getRevisionOfRtc(RuntimeContext rtc) {
    try {
      return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().
                         getRevision(rtc);
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to determine revision of run time context " + rtc.getGUIRepresentation(), e);
    }
  }
  
  
  public boolean isApplication() {
    return _version == null;
  }
  
  public boolean isWorkspace() {
    return _version != null;
  }

  
  public long getRevision() {
    return _revision;
  }

  
  public String getName() {
    return _name;
  }

  
  public Optional<String> getVersion() {
    return Optional.ofNullable(_version);
  }

  
  public RuntimeContext getRuntimeContext() {
    return _rtc;
  }
  
  
  public Optional<Application> getRtcAsApplication() {
    if (isApplication()) {
      return Optional.ofNullable((Application) getRuntimeContext());
    }
    return Optional.empty();
  }
  
  
  public Optional<Workspace> getRtcAsWorkspace() {
    if (isWorkspace()) {
      return Optional.ofNullable((Workspace) getRuntimeContext());
    }
    return Optional.empty();
  }
  
  @Override
  public String toString() {
    return _rtc.getGUIRepresentation();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RtcData)) { return false; }
    RtcData input = (RtcData) obj;
    return _revision == input._revision;
  }
  
  @Override
  public int hashCode() {
    return _name.hashCode();
  }


  @Override
  public int compareTo(RtcData rtc) {
    if (rtc == null) { return 1; }
    return Long.valueOf(_revision).compareTo(Long.valueOf(rtc._revision));
  }
  
}
