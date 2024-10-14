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
package com.gip.xyna.utils.install.db.oracle;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;



/**
 * Loads a file as blob into a given database.
 * 
 */
public class StoreBlob extends Task {

  /*
   * Driver for database, e.g. oracle.jdbc.driver.OracleDriver
   */
  private String driver;
  /*
   * Database url, e.g. jdbc:oracle:thin:@localhost:1521:javaDemo
   */
  private String url;
  /*
   * Existing database user
   */
  private String user;
  /*
   * Password for database user
   */
  private String password;
  /*
   * Statement to use for inserting blob into database. Needs ? for filename.
   */
  private String statement;
  /*
   * Path to file that should be stored in database
   */
  private String filename;


  public void execute() throws BuildException {
    try {
      Class.forName(driver);
    }
    catch (ClassNotFoundException e) {
      throw new BuildException("Unknown driver '" + driver + "'.");
    }
    Connection conn = null;
    FileInputStream fis = null;
    try {
      conn = DriverManager.getConnection(url, user, password);
    }
    catch (SQLException e) {
      BuildException be = new BuildException(
                                             "Unable to connect to database '" + url + "' with user '" + user + "': " + e
                                                             .getMessage());
      be.setStackTrace(e.getStackTrace());
      throw be;
    }
    try {
      conn.setAutoCommit(false);
      PreparedStatement stmt = conn.prepareStatement(statement);

      File image = new File(filename);
      fis = new FileInputStream(image);

      stmt.setBinaryStream(1, fis, (int) image.length());
      stmt.execute();
      log("File '" + filename + "' successfully loaded into database '" + url + "'", Project.MSG_INFO);
    }
    catch (SQLException e) {
      BuildException be = new BuildException("Unable to store file '" + filename + "' in database '" + url + "': " + e
                      .getMessage());
      be.setStackTrace(e.getStackTrace());
      throw be;
    }
    catch (FileNotFoundException e) {
      throw new BuildException("Unable to load file '" + filename + "'.");
    }
    finally {
      if (fis != null) {
        try {
          fis.close();
        }
        catch (IOException e) {
          log("WARNING: unable to close file input stream.", Project.MSG_WARN);
        }
      }
      if (conn != null) {
        try {
          conn.commit();
          conn.close();
        }
        catch (SQLException e) {
          log("WARNING: unable to close database connection.", Project.MSG_WARN);
        }

      }

    }
  }


  /**
   * Driver for database, e.g. oracle.jdbc.driver.OracleDriver
   * 
   * @param driver
   */
  public void setDriver(String driver) {
    this.driver = driver;
  }


  /**
   * Database url, e.g. jdbc:oracle:thin:@localhost:1521:javaDemo
   * 
   * @param url
   */
  public void setURL(String url) {
    this.url = url;
  }


  /**
   * Existing database user
   * 
   * @param user
   */
  public void setUser(String user) {
    this.user = user;
  }


  /**
   * Password for database user
   * 
   * @param password
   */
  public void setPassword(String password) {
    this.password = password;
  }


  /**
   * Statement to use for inserting blob into database. Needs ? for filename.
   * 
   * @param statement
   */
  public void setStatement(String statement) {
    this.statement = statement;
  }


  /**
   * Column of table in witch the blob should be stored
   * 
   * @param filename
   */
  public void setFile(String filename) {
    this.filename = filename;
  }


}
