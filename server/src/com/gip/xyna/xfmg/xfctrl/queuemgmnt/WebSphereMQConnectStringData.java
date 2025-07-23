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

package com.gip.xyna.xfmg.xfctrl.queuemgmnt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.utils.misc.StringParameter.Unmatched;
import com.gip.xyna.utils.misc.Documentation;

public class WebSphereMQConnectStringData extends WebSphereMQConnectData {

      private static final StringParameter<String> HOSTNAME_PARAM 
      = StringParameter.typeString("hostname")
        .label("hostname")
        .documentation(Documentation
            .en("FQDN hostname or IP of queue manager.")
            .de("FQDN Hostname oder IP des Queue-Managers.")
            .build())
        .mandatory()
        .build();

      private static final StringParameter<Integer> PORT_PARAM 
      = StringParameter.typeInteger("port")
        .label("port number")
        .documentation(Documentation
            .en("Port of queue manager.")
            .de("Port des Queue-Managers.")
            .build())
        .mandatory()
        .build();

      private static final StringParameter<String> QMGR_PARAM 
      = StringParameter.typeString("queueManager")
        .label("queueManager")
        .documentation(Documentation
            .en("Name of the queue manager.")
            .de("Name des Queue-Managers.")
            .build())
        .mandatory()
        .build();

      private static final StringParameter<String> CHANNEL_PARAM 
      = StringParameter.typeString("channel")
        .label("channel")
        .documentation(Documentation
            .en("Name of the used channel.")
            .de("Name des zu verwendenden Kanals.")
            .build())
        .mandatory()
        .build();

    public static final List<StringParameter<?>> allParams = StringParameter.asList(HOSTNAME_PARAM, PORT_PARAM, QMGR_PARAM, CHANNEL_PARAM);

    private WebSphereMQConnectStringData(WebSphereMQConnectData qcd) {
        this.setHostname(qcd.getHostname());
        this.setPort(qcd.getPort());
        this.setQueueManager(qcd.getQueueManager());
        this.setChannel(qcd.getChannel());
    };

    public static WebSphereMQConnectStringData fromStringParameters(List<String> parameters) {
        Map<String, Object> paramValues;
        try {
            paramValues = StringParameter.parse(parameters).unmatchedKey(Unmatched.Ignore).with(allParams);
            WebSphereMQConnectData qcd = new WebSphereMQConnectData();
            qcd.setHostname(HOSTNAME_PARAM.getFromMap(paramValues));
            qcd.setPort(PORT_PARAM.getFromMap(paramValues));
            qcd.setQueueManager(QMGR_PARAM.getFromMap(paramValues));
            qcd.setChannel(CHANNEL_PARAM.getFromMap(paramValues));

            return new WebSphereMQConnectStringData(qcd);
        } catch (StringParameterParsingException e) {

        }
        return null;
    }

    public static WebSphereMQConnectStringData fromConnectData(WebSphereMQConnectData qcd) {
        return new WebSphereMQConnectStringData(qcd);
    }

    public List<String> toParameters() {
        List<String> params = new ArrayList<String>();
        params.add(HOSTNAME_PARAM.toNamedParameterObject(getHostname()));
        params.add(PORT_PARAM.toNamedParameterObject(getPort()));
        params.add(QMGR_PARAM.toNamedParameterObject(getQueueManager()));
        params.add(CHANNEL_PARAM.toNamedParameterObject(getChannel()));

        return params;
    }

}