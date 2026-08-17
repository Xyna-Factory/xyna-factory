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

import com.gip.xyna.utils.misc.EnvironmentVariable.IntegerEnvironmentVariable;
import com.gip.xyna.utils.misc.EnvironmentVariable.StringEnvironmentVariable;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.utils.misc.StringParameter.Unmatched;



public class ActiveMQConnecStringtData extends ActiveMQConnectData {

    private StringEnvironmentVariable hostnameEnv;
    private IntegerEnvironmentVariable portEnv;


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

    public static final List<StringParameter<?>> allParams =
            Collections.unmodifiableList(StringParameter.asList(HOSTNAME_PARAM, PORT_PARAM, HOSTNAME_ENV_PARAM, PORT_ENV_PARAM));


    private ActiveMQConnecStringtData(ActiveMQConnectData qcd) {
        Objects.requireNonNull(qcd, "ActiveMQConnectData must not be null");

        if (qcd instanceof ActiveMQConnecStringtData) {
            ActiveMQConnecStringtData qcsd = (ActiveMQConnecStringtData) qcd;
            this.setHostname(qcsd.getConfiguredHostname());
            this.setPort(qcsd.getConfiguredPort());

            this.setHostnameEnv(qcsd.getHostnameEnv());
            this.setPortEnv(qcsd.getPortEnv());
        } else {
            this.setHostname(qcd.getHostname());
            this.setPort(qcd.getPort());
        }
    }


    private String getConfiguredHostname() {
        return super.getHostname();
    }


    private int getConfiguredPort() {
        return super.getPort();
    }


    @Override
    public String toString() {
        return "ActiveMQConnecStringtData {hostnameEnv:" + hostnameEnv + ", portEnv:" + portEnv + ", " + super.toString() + "}";
    }


    public static ActiveMQConnecStringtData fromStringParameters(List<String> parameters) {
        Map<String, Object> paramValues;
        try {
            paramValues = StringParameter.parse(parameters).unmatchedKey(Unmatched.Ignore).with(allParams);

            ActiveMQConnecStringtData qcd = new ActiveMQConnecStringtData(new ActiveMQConnectData());
            qcd.setHostname(HOSTNAME_PARAM.getFromMap(paramValues));
            Integer port = PORT_PARAM.getFromMap(paramValues);
            if (port != null) {
                qcd.setPort(port);
            }

            qcd.setHostnameEnv(HOSTNAME_ENV_PARAM.getFromMap(paramValues));
            qcd.setPortEnv(PORT_ENV_PARAM.getFromMap(paramValues));

            validateMandatoryValueOrEnv(qcd);

            return qcd;
        } catch (StringParameterParsingException e) {
            throw new IllegalArgumentException("Unable to parse ActiveMQ connect data parameters", e);
        }
    }


    public static QueueConnectData fromRegisterQueueParameters(String[] connectParams) {
        if (connectParams == null || connectParams.length == 0) {
            throw new IllegalArgumentException("Error: Connect parameter missing.");
        }

        if (isNamedParameterSyntax(connectParams)) {
            return fromStringParameters(Arrays.asList(connectParams));
        }

        if (connectParams.length != 2) {
            throw new IllegalArgumentException("Error: Wrong number of connect parameters.");
        }

        ActiveMQConnectData connectData = new ActiveMQConnectData();
        connectData.setHostname(QueueManagement.checkParameter("hostname", connectParams[0]));
        String portVal = QueueManagement.checkParameter("port", connectParams[1]);
        try {
            connectData.setPort(Integer.parseInt(portVal));
        } catch (Exception e) {
            throw new IllegalArgumentException("Error: Cannot parse int: " + portVal);
        }
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


    private static void validateMandatoryValueOrEnv(ActiveMQConnecStringtData qcd) {
        requireTextValueOrEnvVar("hostname", qcd.getHostname(), qcd.getHostnameEnv());
        requirePositiveIntValueOrEnvVar("port", qcd.getPort(), qcd.getPortEnv());
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


    public static ActiveMQConnecStringtData fromConnectData(ActiveMQConnectData qcd) {
        Objects.requireNonNull(qcd, "ActiveMQConnectData must not be null");
        return new ActiveMQConnecStringtData(qcd);
    }


    public List<String> toParameters() {
        List<String> params = new ArrayList<String>();
        if (getConfiguredHostname() != null) {
            params.add(HOSTNAME_PARAM.toNamedParameterObject(getConfiguredHostname()));
        }
        if (getConfiguredPort() > 0) {
            params.add(PORT_PARAM.toNamedParameterObject(getConfiguredPort()));
        }

        if (getHostnameEnv() != null) {
            params.add(HOSTNAME_ENV_PARAM.toNamedParameterObject(getHostnameEnv()));
        }
        if (getPortEnv() != null) {
            params.add(PORT_ENV_PARAM.toNamedParameterObject(getPortEnv()));
        }

        return params;
    }

}