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
package com.gip.xyna.xnwh.sharedresources.synchronizers.sqlsharedresourcesynchronizer;



import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.EnvironmentVariable;
import com.gip.xyna.utils.misc.EnvironmentVariable.IntegerEnvironmentVariable;
import com.gip.xyna.utils.misc.EnvironmentVariable.StringEnvironmentVariable;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xmcp.PluginDescription.PluginType;
import com.gip.xyna.xnwh.sharedresources.SharedResourceSynchronizer;
import com.gip.xyna.xnwh.sharedresources.SharedResourceSynchronizerFactory;



public class SQLSharedResourceSynchronizerFactory implements SharedResourceSynchronizerFactory {

  private static final StringParameter<String> TABLENAME = StringParameter
      .typeString("tablename").label("Tablename").defaultValue("sharedresources").documentation(Documentation
          .en("Table to store shared resource instances in").de("Tabelle in der shared resource Instanzen gespeichert werden").build())
      .build();

  private static final StringParameter<String> URL = StringParameter.typeString("url").label("Url")
      .documentation(Documentation.en("JDBC connect string").de("JDBC Verbindungsdaten").build()).build();

  private static final StringParameter<String> USER = StringParameter.typeString("username").label("User")
      .documentation(Documentation.en("Database user").de("Datenbankbenutzer").build()).build();

  private static final StringParameter<String> PASSWORD = StringParameter.typeString("password").label("Password")
      .documentation(Documentation.en("Database user password").de("Passwort des Datenbankbenutzers").build()).build();

  private static final StringParameter<Integer> CONNECTIONS = StringParameter
      .typeInteger("connections").label("Connections").defaultValue(10).documentation(Documentation
          .en("Number of database connections to use").de("Anzahl an Datenbankverbindungen, die gleichzeitig verwendet werden").build())
      .build();

  private static final StringParameter<Duration> CONNECTION_TIMEOUT =
      StringParameter.typeDuration("connectionTimeout").label("Connection Timeout").defaultValue(Duration.valueOf("10 s"))
          .documentation(Documentation.en("Database connection timeout").de("Datenbank-Verbindungstimeout").build()).build();

  private static final StringParameter<Duration> SOCKET_TIMEOUT =
      StringParameter.typeDuration("socketTimeout").label("Socket Timeout").defaultValue(Duration.valueOf("10 s"))
          .documentation(Documentation.en("Database socket timeout").de("Datenbank-Sockettimeout").build()).build();

  private static final StringParameter<StringEnvironmentVariable> URL_ENV = StringParameter
      .typeEnvironmentVariable(StringEnvironmentVariable.class, "connectStringEnv").label("Connectstring environment variable.")
      .documentation(Documentation.en("Name of the environment variable containing the JDBC connect string.")
          .de("Name der Umgebungsvariable mit den JDBC Verbindungsdaten.").build())
      .optional().build();

  private static final StringParameter<StringEnvironmentVariable> USER_ENV =
      StringParameter.typeEnvironmentVariable(StringEnvironmentVariable.class, "usernameEnv").label("User environment variable.")
          .documentation(Documentation.en("Name of the environment variable containing the db username.")
              .de("Name der Umgebungsvariable, die den DB Nutzernamen enth�lt.").build())
          .optional().build();

  private static final StringParameter<StringEnvironmentVariable> PASSWORD_ENV =
      StringParameter.typeEnvironmentVariable(StringEnvironmentVariable.class, "passwordEnv").label("Connectstring environment variable.")
          .documentation(Documentation.en("Name of the environment variable containing the db password.")
              .de("Name der Umgebungsvariable, die das DB Passwort enth�lt.").build())
          .optional().build();

  private static final StringParameter<IntegerEnvironmentVariable> CONNECTIONS_ENV =
      StringParameter.typeEnvironmentVariable(IntegerEnvironmentVariable.class, "connectionsEnv").label("Connections environment variable.")
          .documentation(Documentation.en("Name of the environment variable containing how many connections to use.")
              .de("Name der Umgebungsvariable mit der Anzahl an Datenbankverbindungen, die gleichzeitig verwendet werden.").build())
          .optional().build();

  private static final List<StringParameter<?>> importParameters =
      StringParameter.asList(TABLENAME, URL, USER, PASSWORD, CONNECTIONS, CONNECTION_TIMEOUT, SOCKET_TIMEOUT, URL_ENV, USER_ENV,
                             PASSWORD_ENV, CONNECTIONS_ENV);


  @Override
  public SharedResourceSynchronizer createSynchronizer(List<String> params) {
    List<String> errors = new ArrayList<>();
    Map<String, Object> param = null;
    try {
      param = StringParameter.parse(params).with(importParameters);
    } catch (StringParameterParsingException e) {
      throw new IllegalArgumentException(e);
    }
    String tableName = TABLENAME.getFromMap(param);
    String url = readPropertyWithEnv(URL, URL_ENV, param, errors);
    String username = readPropertyWithEnv(USER, USER_ENV, param, errors);
    String password = readPropertyWithEnv(PASSWORD, PASSWORD_ENV, param, errors);
    int numConnections = readNumConnections(param, errors);
    Duration connectTimeout = CONNECTION_TIMEOUT.getFromMap(param);
    Duration socketTimeout = SOCKET_TIMEOUT.getFromMap(param);

    if (!errors.isEmpty()) {
      String msg = String.format("Could not create Synchronizer. %d errors: \n\t%s", errors.size(), 
                                 String.join("\n\t", errors));
      throw new IllegalArgumentException(msg);
    }

    return new SQLSharedResourceSynchronizer(tableName, url, username, password, numConnections, connectTimeout, socketTimeout);
  }


  private int readNumConnections(Map<String, Object> map, List<String> errors) {
    int result = -1;
    Optional<EnvironmentVariable<Integer>> envValue = Optional.ofNullable(CONNECTIONS_ENV.getFromMap(map));
    if (envValue.isEmpty()) {
      result = CONNECTIONS.getFromMap(map);
    } else {
      Optional<Integer> val = envValue.get().getValue();
      if (val.isEmpty()) {
        errors.add("Environment variable not set: " + CONNECTIONS_ENV.getName());
        return -1;
      }
    }
    if (result < 1) {
      errors.add("Invalid amount of connections: " + result);
      return -1;
    }
    return result;
  }


  private String readPropertyWithEnv(StringParameter<String> direct, StringParameter<StringEnvironmentVariable> env,
                                     Map<String, Object> map, List<String> errors) {
    String directValue = direct.getFromMap(map);
    Optional<EnvironmentVariable<String>> envValue = Optional.ofNullable(env.getFromMap(map));
    if (directValue == null && envValue.isEmpty()) {
      errors.add("Missing variable. Set either " + direct.getName() + " or " + env.getName());
      return null;
    }

    if (directValue != null && envValue.isPresent()) {
      errors.add("Duplicate variable. Both " + direct.getName() + " and " + env.getName() + " are set.");
      return null;
    }

    if (directValue != null) {
      return directValue;
    }

    Optional<String> valueFromEnv = envValue.get().getValue();
    if (valueFromEnv.isEmpty()) {
      errors.add("Environment variable not set: " + env.getName());
      return null;
    }

    return valueFromEnv.get();
  }


  @Override
  public PluginDescription getDescription() {
    PluginDescription.Builder result = PluginDescription.create(PluginType.sharedResourceSynchronizer);
    result.name("SqlSharedResourceSynchronizer");
    result.label("Shared Resource Synchronizer: SqlSharedResourceSynchronizer");
    result.description("Connects to an SQL Database to synchronize shared resources.");
    result.parameters(ParameterUsage.Create, importParameters);
    return result.build();
  }


  @Override
  public String getSynchronizerName() {
    return "SQLSharedResourceSynchronizer";
  }


}