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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;


public class InterFactoryLink {
  
  private final EnumMap<InterFactoryLinkProfileIdentifier, InterFactoryLinkProfile> profiles = new EnumMap<InterFactoryLinkProfileIdentifier, InterFactoryLinkProfile>(InterFactoryLinkProfileIdentifier.class);
  private final EnumMap<InterFactoryChannelIdentifier, InterFactoryChannel> channels = new EnumMap<InterFactoryChannelIdentifier, InterFactoryChannel>(InterFactoryChannelIdentifier.class);
  
  public InterFactoryLink(String nodeName, Set<InterFactoryChannel> channels, Set<InterFactoryLinkProfile> profiles) {
    for (InterFactoryChannel channel : channels) {
      this.channels.put(channel.getIdentifier(), channel);
    }
    for (InterFactoryLinkProfile profile : profiles) {
      this.profiles.put(profile.getIdentifier(), profile);
    }
    // TODO verify / retrieve supported channel 
    if (this.channels.size() > 0 &&
        this.profiles.size() > 0) {
      for (InterFactoryLinkProfileIdentifier pi : InterFactoryLinkProfileIdentifier.values()) {
        this.profiles.get(pi).init(nodeName, this.channels.get(InterFactoryChannelIdentifier.RMI), pi.getTimeout());
      }
    }
  }
  
  public Set<InterFactoryLinkProfileIdentifier> getSupportedProfiles() {
    return Collections.unmodifiableSet(profiles.keySet());
  }
  
  public <R extends InterFactoryLinkProfile> R getProfile(Class<R> clazz) {
    for (InterFactoryLinkProfile profile : profiles.values()) {
      if (clazz.isInstance(profile)) {
        return clazz.cast(profile);
      }
    }
    return null;
  }
  
  public <R extends InterFactoryLinkProfile> R getProfile(InterFactoryLinkProfileIdentifier profile) {
    return (R) profiles.get(profile);
  }
  
  public void addChannel(InterFactoryChannelIdentifier channelIdentifier, Map<String, String> parameter) {
    // TODO
  }


  public static enum InterFactoryLinkProfileIdentifier {
    Infrastructure(), 
    OrderExecution(),
    RuntimeContextManagement(XynaProperty.RMI_IL_SOCKET_TIMEOUT_RTC_MGMT),
    FileManagement(XynaProperty.RMI_IL_SOCKET_TIMEOUT_FILE_MGMT),
    Monitoring(XynaProperty.RMI_IL_SOCKET_TIMEOUT_MONITORING);

    private final XynaPropertyDuration timeout;
    
    private InterFactoryLinkProfileIdentifier() {
      this.timeout = XynaProperty.RMI_IL_SOCKET_TIMEOUT;
    }
    
    private InterFactoryLinkProfileIdentifier(XynaPropertyDuration timeout) {
      this.timeout = timeout;
    }
    
    public Duration getTimeout() {
      return timeout.get();
    }
  }
  
  public static enum InterFactoryChannelIdentifier {
    RMI;
  }

}