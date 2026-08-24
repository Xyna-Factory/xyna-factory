/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.utils.misc.StringParameter.Unmatched;
import com.gip.xyna.utils.misc.EnvironmentVariable.StringEnvironmentVariable;
import com.gip.xyna.utils.misc.EnvironmentVariable.IntegerEnvironmentVariable;
import com.gip.xyna.utils.misc.Documentation;



public class WebSphereMQConnectStringData extends WebSphereMQConnectData {

    private StringEnvironmentVariable hostnameEnv;
    private IntegerEnvironmentVariable portEnv;
    private StringEnvironmentVariable queueManagerEnv;
    private StringEnvironmentVariable channelEnv;


    public StringEnvironmentVariable getHostnameEnv() {
        return hostnameEnv;
    }


    public void setHostnameEnv(StringEnvironmentVariable hostnameEnv) {
        this.hostnameEnv = hostnameEnv;
    }


    public IntegerEnvironmentVariable getPortEnv() {
        return portEnv;
    }


    public void setPortEnv(IntegerEnvironmentVariable portEnv) {
        this.portEnv = portEnv;
    }


    public StringEnvironmentVariable getQueueManagerEnv() {
        return queueManagerEnv;
    }


    public void setQueueManagerEnv(StringEnvironmentVariable queueManagerEnv) {
        this.queueManagerEnv = queueManagerEnv;
    }


    public StringEnvironmentVariable getChannelEnv() {
        return channelEnv;
    }


    public void setChannelEnv(StringEnvironmentVariable channelEnv) {
        this.channelEnv = channelEnv;
    }


    @Override
    public String getHostname() {
        if (hostnameEnv != null) {
            return hostnameEnv.getValue().orElse(getConfiguredHostname());
        }

        return getConfiguredHostname();
    }


    @Override
    public int getPort() {
        if (portEnv != null) {
            return portEnv.getValue().orElse(getConfiguredPort());
        }

        return getConfiguredPort();
    }


    @Override
    public String getQueueManager() {
        if (queueManagerEnv != null) {
            return queueManagerEnv.getValue().orElse(getConfiguredQueueManager());
        }

        return getConfiguredQueueManager();
    }


    @Override
    public String getChannel() {
        if (channelEnv != null) {
            return channelEnv.getValue().orElse(getConfiguredChannel());
        }

        return getConfiguredChannel();
    }


    private static final StringParameter<String> HOSTNAME_PARAM =
            StringParameter.typeString("hostname").label("hostname").documentation(Documentation.en("FQDN hostname or IP of queue manager.")
                    .de("FQDN Hostname oder IP des Queue-Managers.").build()).build();

    private static final StringParameter<StringEnvironmentVariable> HOSTNAME_ENV_PARAM =
            StringParameter.typeEnvironmentVariable(StringEnvironmentVariable.class, "hostnameEnv").label("hostname env var")
                    .documentation(Documentation.en("Env var for FQDN hostname or IP of queue manager.")
                            .de("Umgebungsvariable für FQDN Hostname oder IP des Queue-Managers.").build())
                    .build();

    private static final StringParameter<Integer> PORT_PARAM = StringParameter.typeInteger("port").label("port number")
            .documentation(Documentation.en("Port of queue manager.").de("Port des Queue-Managers.").build()).build();

    private static final StringParameter<IntegerEnvironmentVariable> PORT_ENV_PARAM = StringParameter
            .typeEnvironmentVariable(IntegerEnvironmentVariable.class, "portEnv").label("port number env var").documentation(Documentation
                    .en("Env var for port of queue manager.").de("Umgebungsvariable für Port des Queue-Managers.").build())
            .build();

    private static final StringParameter<String> QMGR_PARAM = StringParameter.typeString("queueManager").label("queueManager")
            .documentation(Documentation.en("Name of the queue manager.").de("Name des Queue-Managers.").build()).build();

    private static final StringParameter<StringEnvironmentVariable> QMGR_ENV_PARAM =
            StringParameter.typeEnvironmentVariable(StringEnvironmentVariable.class, "queueManagerEnv").label("queueManager env var")
                    .documentation(Documentation.en("Env var for name of the queue manager.")
                            .de("Umgebungsvariable für Name des Queue-Managers.").build())
                    .build();

    private static final StringParameter<String> CHANNEL_PARAM = StringParameter.typeString("channel").label("channel")
            .documentation(Documentation.en("Name of the used channel.").de("Name des zu verwendenden Kanals.").build()).build();

    private static final StringParameter<StringEnvironmentVariable> CHANNEL_ENV_PARAM = StringParameter
            .typeEnvironmentVariable(StringEnvironmentVariable.class, "channelEnv").label("channel env var").documentation(Documentation
                    .en("Env var for name of the used channel.").de("Umgebungsvariable für Name des zu verwendenden Kanals.").build())
            .build();

    public static final List<StringParameter<?>> allParams =
            Collections.unmodifiableList(StringParameter.asList(HOSTNAME_PARAM, PORT_PARAM, QMGR_PARAM, CHANNEL_PARAM, HOSTNAME_ENV_PARAM,
                                                                PORT_ENV_PARAM, QMGR_ENV_PARAM, CHANNEL_ENV_PARAM));


    private WebSphereMQConnectStringData(WebSphereMQConnectData qcd) {
        Objects.requireNonNull(qcd, "WebSphereMQConnectData must not be null");

        if (qcd instanceof WebSphereMQConnectStringData) {
            var qcsd = (WebSphereMQConnectStringData) qcd;
            this.setHostname(qcsd.getConfiguredHostname());
            this.setPort(qcsd.getConfiguredPort());
            this.setQueueManager(qcsd.getConfiguredQueueManager());
            this.setChannel(qcsd.getConfiguredChannel());

            this.setHostnameEnv(qcsd.getHostnameEnv());
            this.setPortEnv(qcsd.getPortEnv());
            this.setQueueManagerEnv(qcsd.getQueueManagerEnv());
            this.setChannelEnv(qcsd.getChannelEnv());
        } else {
            this.setHostname(qcd.getHostname());
            this.setPort(qcd.getPort());
            this.setQueueManager(qcd.getQueueManager());
            this.setChannel(qcd.getChannel());
        }
    }


    private String getConfiguredHostname() {
        return super.getHostname();
    }


    private int getConfiguredPort() {
        return super.getPort();
    }


    private String getConfiguredQueueManager() {
        return super.getQueueManager();
    }


    private String getConfiguredChannel() {
        return super.getChannel();
    }


    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("WebSphereMQConnectStringData {hostnameEnv: ");
        s.append(hostnameEnv);
        s.append(", portEnv: ").append(portEnv);
        s.append(", queueManagerEnv: ").append(queueManagerEnv);
        s.append(", channelEnv: ").append(channelEnv).append(", ");
        s.append(" WebSphereMQConnectData { ");
        s.append("queueManager: ").append(this.getConfiguredQueueManager());
        s.append(", hostname: ").append(this.getConfiguredHostname());
        s.append(", port: ").append(this.getConfiguredPort());
        s.append(", channel: ").append(this.getConfiguredChannel());
        s.append(" }");
        s.append(" } ");

        return s.toString();
    }


    public static WebSphereMQConnectStringData fromStringParameters(List<String> parameters) {
        Map<String, Object> paramValues;
        try {
            paramValues = StringParameter.parse(parameters).unmatchedKey(Unmatched.Ignore).with(allParams);
            WebSphereMQConnectStringData qcsd = new WebSphereMQConnectStringData(new WebSphereMQConnectData());
            qcsd.setHostname(HOSTNAME_PARAM.getFromMap(paramValues));
            Integer port = PORT_PARAM.getFromMap(paramValues);
            if (port != null) {
                qcsd.setPort(port);
            }
            qcsd.setQueueManager(QMGR_PARAM.getFromMap(paramValues));
            qcsd.setChannel(CHANNEL_PARAM.getFromMap(paramValues));

            qcsd.setHostnameEnv(HOSTNAME_ENV_PARAM.getFromMap(paramValues));
            qcsd.setPortEnv(PORT_ENV_PARAM.getFromMap(paramValues));
            qcsd.setQueueManagerEnv(QMGR_ENV_PARAM.getFromMap(paramValues));
            qcsd.setChannelEnv(CHANNEL_ENV_PARAM.getFromMap(paramValues));

            validateMandatoryValueOrEnv(qcsd);

            return qcsd;
        } catch (StringParameterParsingException e) {
            throw new IllegalArgumentException("Unable to parse WebSphereMQ connect data parameters", e);
        }
    }


    public static QueueConnectData fromRegisterQueueParameters(String[] connectParams) {
        if (connectParams == null || connectParams.length == 0) {
            throw new IllegalArgumentException("Error: Connect parameter missing.");
        }

        if (isNamedParameterSyntax(connectParams)) {
            return fromStringParameters(Arrays.asList(connectParams));
        }

        if (connectParams.length != 4) {
            throw new IllegalArgumentException("Error: Wrong number of connect parameters.");
        }

        WebSphereMQConnectData connectData = new WebSphereMQConnectData();
        connectData.setQueueManager(QueueManagement.checkParameter("queueManager", connectParams[0]));
        connectData.setHostname(QueueManagement.checkParameter("hostname", connectParams[1]));
        connectData.setPort(QueueManagement.checkParameter("port", connectParams[2]));
        connectData.setChannel(QueueManagement.checkParameter("channel", connectParams[3]));
        return connectData;
    }


    private static boolean isNamedParameterSyntax(String[] connectParams) {
        int namedParameters = 0;
        for (String param : connectParams) {
            if (isNamedParameter(param, allParams)) {
                namedParameters++;
            }
        }

        if (namedParameters > 0 && namedParameters < connectParams.length) {
            throw new IllegalArgumentException("Error: Mixed named and unnamed connect parameters are not supported.");
        }

        return namedParameters == connectParams.length;
    }


    private static boolean isNamedParameter(String param, List<StringParameter<?>> validParameters) {
        String parameterName = extractParameterName(param);
        if (parameterName == null) {
            return false;
        }

        for (StringParameter<?> validParameter : validParameters) {
            if (validParameter.getName().equals(parameterName)) {
                return true;
            }
        }

        return false;
    }


    private static String extractParameterName(String param) {
        if (param == null) {
            return null;
        }

        int separatorIndex = param.indexOf('=');
        if (separatorIndex <= 0) {
            return null;
        }

        return param.substring(0, separatorIndex);
    }


    private static void validateMandatoryValueOrEnv(WebSphereMQConnectStringData qcsd) {
        requireTextValueOrEnvVar("hostname", qcsd.getHostname(), qcsd.getHostnameEnv());
        requirePositiveIntValueOrEnvVar("port", qcsd.getPort(), qcsd.getPortEnv());
        requireTextValueOrEnvVar("queueManager", qcsd.getQueueManager(), qcsd.getQueueManagerEnv());
        requireTextValueOrEnvVar("channel", qcsd.getChannel(), qcsd.getChannelEnv());
    }


    private static void requireTextValueOrEnvVar(String parameterName, String value, StringEnvironmentVariable envVar) {
        if (!hasText(value) && envVar == null) {
            throw new IllegalArgumentException("Missing mandatory parameter '" + parameterName + "': provide either '" + parameterName
                    + "' or '" + parameterName + "Env'.");
        }
    }


    private static void requirePositiveIntValueOrEnvVar(String parameterName, int value, IntegerEnvironmentVariable envVar) {
        if (value <= 0 && envVar == null) {
            throw new IllegalArgumentException("Missing mandatory parameter '" + parameterName + "': provide either '" + parameterName
                    + "' or '" + parameterName + "Env'.");
        }
    }


    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }


    public static WebSphereMQConnectStringData fromConnectData(WebSphereMQConnectData qcd) {
        Objects.requireNonNull(qcd, "WebSphereMQConnectData must not be null");
        return new WebSphereMQConnectStringData(qcd);
    }


    public List<String> toParameters() {
        List<String> params = new ArrayList<>();
        // Serialize explicitly configured base values and keep env-var references separate.
        if (getConfiguredHostname() != null) {
            params.add(HOSTNAME_PARAM.toNamedParameterObject(getConfiguredHostname()));
        }
        if (getConfiguredPort() > 0) {
            params.add(PORT_PARAM.toNamedParameterObject(getConfiguredPort()));
        }
        if (getConfiguredQueueManager() != null) {
            params.add(QMGR_PARAM.toNamedParameterObject(getConfiguredQueueManager()));
        }
        if (getConfiguredChannel() != null) {
            params.add(CHANNEL_PARAM.toNamedParameterObject(getConfiguredChannel()));
        }

        if (getHostnameEnv() != null) {
            params.add(HOSTNAME_ENV_PARAM.toNamedParameterObject(getHostnameEnv()));
        }
        if (getPortEnv() != null) {
            params.add(PORT_ENV_PARAM.toNamedParameterObject(getPortEnv()));
        }
        if (getQueueManagerEnv() != null) {
            params.add(QMGR_ENV_PARAM.toNamedParameterObject(getQueueManagerEnv()));
        }
        if (getChannelEnv() != null) {
            params.add(CHANNEL_ENV_PARAM.toNamedParameterObject(getChannelEnv()));
        }

        return params;
    }

}