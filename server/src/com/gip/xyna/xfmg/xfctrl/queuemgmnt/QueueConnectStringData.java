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

import com.gip.xyna.utils.collections.CSVStringList;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.utils.misc.StringParameter.Unmatched;
import com.gip.xyna.utils.misc.Documentation;

public class QueueConnectStringData {

    public static final StringParameter<String> QTYPE = StringParameter.typeString("connectDataType")
            .label("connectDataType")
            .documentation(
                    Documentation.en("type of queue connect data").de("Art der Verbindungsdaten der Queue").build())
            .mandatory().build();

    private static final List<StringParameter<?>> allParams = StringParameter.asList(QTYPE);

    boolean externalNameIsEnv = false;

    public QueueConnectData fromStringParameters(String paramString) {
        List<String> params = CSVStringList.valueOf(paramString);

        Map<String, Object> paramValues;
        try {
            paramValues = StringParameter.parse(params).unmatchedKey(Unmatched.Ignore).with(allParams);
            String qType = QTYPE.getFromMap(paramValues);

            switch (Enum.<QueueType>valueOf(QueueType.class, qType)) {
                case ACTIVE_MQ:
                    return ActiveMQConnecStringtData.fromStringParameters(params);
                case WEBSPHERE_MQ:
                    return WebSphereMQConnectStringData.fromStringParameters(params);
                case ORACLE_AQ:
                    return OracleAQConnectStringData.fromStringParameters(params);
                default:
                    throw new RuntimeException("Unknown queue type: " + qType);
            }
        } catch (StringParameterParsingException e) {
            // logger.error(e);
        }

        return null;
    }

    public String fromConnectData(QueueConnectData qcd) {
        throw new UnsupportedOperationException(
                "Unknonw QueueConnectData type " + qcd == null ? "null" : qcd.getClass().getCanonicalName());
    }

    public String fromConnectData(ActiveMQConnectData qcd) {
        List<String> params = new ArrayList<String>();
        params.add(QTYPE.toNamedParameterObject(QueueType.ACTIVE_MQ.name()));
        params.addAll(ActiveMQConnecStringtData.fromConnectData(qcd).toParameters());

        return new CSVStringList(params).serializeToString();
    }

    public String fromConnectData(OracleAQConnectData qcd) {
        List<String> params = new ArrayList<String>();
        params.add(QTYPE.toNamedParameterObject(QueueType.ORACLE_AQ.name()));
        params.addAll(OracleAQConnectStringData.fromConnectData(qcd).toParameters());

        return new CSVStringList(params).serializeToString();
    }

    public String fromConnectData(WebSphereMQConnectData qcd) {
        List<String> params = new ArrayList<String>();
        params.add(QTYPE.toNamedParameterObject(QueueType.WEBSPHERE_MQ.name()));
        params.addAll(WebSphereMQConnectStringData.fromConnectData(qcd).toParameters());

        return new CSVStringList(params).serializeToString();
    }

}
