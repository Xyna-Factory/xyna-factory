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

package com.gip.juno.ws.tools.ssh;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.tools.FailoverTools;
import com.gip.juno.ws.tools.LocationTools;
import com.gip.juno.ws.tools.PropertiesHandler;
import com.gip.xyna.utils.ssh.Ssh;


public class MySqlDumpTableCopy {

  public enum MySqlUser {
    devicetemplates("mysql.devicetemplates.user", "mysql.devicetemplates.password"),
    dhcpv6conf("mysql.dhcpv6conf.user", "mysql.dhcpv6conf.password"),
    dhcptriggerv6("xyna.dhcptriggerv6.user","xyna.dhcptriggerv6.password"),
    dhcptrigger("xyna.dhcptrigger.user","xyna.dhcptrigger.password");

    private String propertyNameUser = null;
    private String propertyNamePassword = null;

    private MySqlUser(String propertyNameUser, String propertyNamePassword) {
      this.propertyNameUser = propertyNameUser;
      this.propertyNamePassword = propertyNamePassword;
    }
    public String getPropertyNameUser() {
      return propertyNameUser;
    }
    public String getPropertyNamePassword() {
      return propertyNamePassword;
    }
  }


  public static class Constant {
    public static class PropertyName {
      //public static final String CONFIG_DEPLOY = "dump.table.copy.shell.command.config.deploy";
      //public static final String STATUS = "dump.table.copy.shell.command.status";
      //public static final String COPY_TABLE_SLEEP_MILLIS = "dump.table.copy.table.sleep.millis";
      public static final String KEY_SQL_DUMP_DEST_DIR = "dump.table.copy.shell.sql.dump.destination.dir";
      private static final String KEY_SQL_DUMP_BASE = "dump.table.copy.shell.sql.dump.base";

    }
    /*
    public static class DPPEnvironmentPropertyNames {
      public static final String MYSQL_DHCPCONF_USER = "mysql.dhcpv6conf.user";
      public static final String MYSQL_DHCPCONF_PASSWORD = "mysql.dhcpv6conf.password";
    }
    */
    public static class DumpScript {
      public static final String MULTIPLE_TABLES = "MultipleTables";
      public static final String EXPORT = "export";
    }
  }

  private Logger logger = null;
  private Properties wsProperties = null;
  private String _database = null;
  private List<String> _tables = new ArrayList<String>();
  private List<TargetSshConnection> _targets = null;
  private MySqlUser _mySqlUser = null;


  public void execute() throws RemoteException {
    check();
    copyTables();
  }


  /**
   * copies tables from management instance to a pair of DPP instances;
   * first a mysqldump file (for all tables at once) is generated on the management instance;
   * then the mysql user and password for the DPP instance are read by ssh from a property file
   * on the DPP instance;
   * then a mysql command is executed on the management instance, that logs in to mysql on the DPP
   * instance and executes the sql commands of the dump file there.
   *
   */
  private void copyTables() throws RemoteException {
    logger.info("Copying tables ");
    long startTime = System.currentTimeMillis();

    Ssh management = getSshForManagement();
    try {
      dumpTables(management);
      logger.debug("copyTables() - time diff after dumping table: " + (System.currentTimeMillis() - startTime));

      String dumpFile = buildDumpFilePath();

      for (TargetSshConnection targetCon : _targets) {
        Ssh targetSsh = targetCon.getSsh();
        String importUser = getTargetMySqlUser(targetSsh);
        String targetPwd = getTargetMySqlPassword(targetSsh);

        logger.debug("copyTables() - time diff after getting password: " +
                    (System.currentTimeMillis() - startTime));

        String hostip = targetCon.getIp();
        String command = "mysql -u '" + importUser + "' -p'" + targetPwd + "' -h " + hostip
                          + " -D " + this._database + " < '" + dumpFile + "'";
        SshTools.exec(management, command, logger);

        logger.debug("copyAllTables() - time diff after copying: " + (System.currentTimeMillis() - startTime));
      }
    }
    finally {
      //close ssh connection to management
      SshTools.closeConnection(management, logger);
    }
  }


  private String buildDumpFilePath() throws RemoteException {
    StringBuilder s = new StringBuilder();
    String dumpDir = PropertiesHandler.getProperty(wsProperties, Constant.PropertyName.KEY_SQL_DUMP_DEST_DIR,
                                                   logger);
    s.append(dumpDir);
    if (!dumpDir.endsWith("/")) {
      s.append("/");
    }
    if (_tables.size() > 1) {
      s.append(Constant.DumpScript.MULTIPLE_TABLES);
      //ret += "MultipleTables_dhcpv6.class.sql";
    }
    s.append("_").append(this._database).append(".").append(_tables.get(0)).append(".sql");
    return s.toString();
  }


  private Ssh getSshForManagement() throws RemoteException {
    FailoverFlag flag = FailoverTools.getCurrentFailover(DBSchema.dhcpv6, logger);
    LocationTools.LocationsRow row = LocationTools.getManagementRow(flag, logger);
    Ssh ssh = SshTools.openSshConnection(row, logger);
    return ssh;
  }


  private void dumpTables(Ssh ssh) throws RemoteException {
    String commandBase = PropertiesHandler.getProperty(wsProperties, Constant.PropertyName.KEY_SQL_DUMP_BASE,
                                                       logger);
    String command = commandBase + " " + Constant.DumpScript.EXPORT + " " + this._database + " " +
                     getTableNames();
    SshTools.exec(ssh, command, logger);
  }


  private String getTableNames() {
    String ret = "";
    for (String s : _tables) {
      ret += s;
    }
    return ret;
  }


  private String getTargetMySqlPassword(Ssh ssh) throws RemoteException {
    String command = "cat /etc/xyna/environment/${HOSTNAME}.properties | grep " +
                     this._mySqlUser.getPropertyNamePassword();
    String out = SshTools.execForOutput(ssh, command, logger);
    String[] parts = out.split("=");
    if (parts.length != 2) {
      throw new DPPWebserviceException("Wrong format of password entry in property file on DPP instance." +
                "Property key = " + this._mySqlUser.getPropertyNamePassword());
    }
    return parts[1];
  }


  private String getTargetMySqlUser(Ssh ssh) throws RemoteException {
    String command = "cat /etc/xyna/environment/${HOSTNAME}.properties | grep " +
                     this._mySqlUser.getPropertyNameUser();
    String out = SshTools.execForOutput(ssh, command, logger);
    String[] parts = out.split("=");
    if (parts.length != 2) {
      throw new DPPWebserviceException("Wrong format of username entry in property file on DPP instance." +
                "Property key = " + this._mySqlUser.getPropertyNameUser());
    }
    return parts[1];
  }



  public void setLogger(Logger logger) {
    this.logger = logger;
  }

  public void setWebserviceProperties(Properties wsProperties) {
    this.wsProperties = wsProperties;
  }

  public void setDatabaseName(String database) {
    this._database = database;
  }

  public void addTable(String table) throws DPPWebserviceException {
    if ((table == null) || (table.trim().length() < 1)) {
      throw new DPPWebserviceException("MySqlDumpTableCopy: table name to add is empty.");
    }
    this._tables.add(table);
  }

  public void setTargetConnections(List<TargetSshConnection> targets) {
    this._targets = targets;
  }

  public void setMySqlUser(MySqlUser mySqlUser) {
    this._mySqlUser = mySqlUser;
  }

  public void check() throws DPPWebserviceException {
    if (logger == null) {
      throw new DPPWebserviceException("MySqlDumpTableCopy: logger not set.");
    }
    if (wsProperties == null) {
      throw new DPPWebserviceException("MySqlDumpTableCopy: webservice properties not set.");
    }
    if (_database == null) {
      throw new DPPWebserviceException("MySqlDumpTableCopy: database name not set.");
    }
    if (_database == null) {
      throw new DPPWebserviceException("MySqlDumpTableCopy: database name not set.");
    }
    if (_targets == null) {
      throw new DPPWebserviceException("MySqlDumpTableCopy: target connections not set.");
    }
    if (_tables.size() < 1) {
      throw new DPPWebserviceException("MySqlDumpTableCopy: No table names set.");
    }
    if (_mySqlUser == null) {
      throw new DPPWebserviceException("MySqlDumpTableCopy: MySQL user not set.");
    }
  }

}
