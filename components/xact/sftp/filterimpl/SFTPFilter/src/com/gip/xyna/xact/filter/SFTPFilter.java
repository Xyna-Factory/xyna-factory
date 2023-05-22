/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xact.filter;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xact.trigger.SFTPTriggerConnection;
import com.gip.xyna.xact.trigger.filesystem.FileSystemCacheParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xsched.xynaobjects.RelativeDate;

import xact.sftp.CacheParameter;
import xact.sftp.None;
import xact.sftp.SessionIsolated;
import xact.sftp.Timed;

public class SFTPFilter extends ConnectionFilter<SFTPTriggerConnection> {

  private final static long serialVersionUID = 8784549483133615178L;
  private final static Logger logger = CentralFactoryLogging.getLogger(SFTPFilter.class);
  
    
  public final static XynaPropertyString filesystemPathIdentifier = new XynaPropertyString("xact.sftp.filesystemPathIdentifier", null)
                          .setDefaultDocumentation(DocumentationLanguage.EN, "Path identifier for file system access")
                          .setDefaultDocumentation(DocumentationLanguage.DE, "Pfad-Kennzeichnung f�r Dateisystem-Zugriff");
  public final static XynaPropertyString databasePathIdentifier = new XynaPropertyString("xact.sftp.databasePathIdentifier", null)
                          .setDefaultDocumentation(DocumentationLanguage.EN, "Path identifier for database access")
                          .setDefaultDocumentation(DocumentationLanguage.DE, "Pfad-Kennzeichnung f�r Datenbank-Zugriff");
  public final static XynaPropertyString localSftpRoot = new XynaPropertyString("xact.sftp.localSftpRoot", null)
                          .setDefaultDocumentation(DocumentationLanguage.EN, "Local root for file system access")
                          .setDefaultDocumentation(DocumentationLanguage.DE, "Lokales Wurzelverzeichnis f�r Dateisystem-Zugriff");
  

  public final static String PROCESS_SFTP_ORDERTYPE = "xact.sftp.ProcessSFTPRequest";

  private PathType requestType;
  
  /**
   * Analyzes TriggerConnection and creates XynaOrder if it accepts the connection.
   * The method return a FilterResponse object, which can include the XynaOrder if the filter is responsibleb for the request.
   * # If this filter is not responsible the returned object must be: FilterResponse.notResponsible()
   * # If this filter is responsible the returned object must be: FilterResponse.responsible(XynaOrder order)
   * # If this filter is responsible but it handle the request without creating a XynaOrder the 
   *   returned object must be: FilterResponse.responsibleWithoutXynaorder()
   * # If this filter is responsible but the version of this filter is too new the returned
   *    object must be: FilterResponse.responsibleButTooNew(). The trigger will try an older version of the filter.
   * @param tc
   * @return FilterResponse object
   * @throws XynaException caused by errors reading data from triggerconnection or having an internal error.
   *         results in onError() being called by Xyna Processing.
   */
  public FilterResponse createXynaOrder(SFTPTriggerConnection tc) throws XynaException {
    try {
      if (tc.getPath() == null || 
          tc.getPath().isEmpty()) {
        if (logger.isDebugEnabled()) {
          logger.debug("Requested path is empty, returning fileNotFound");
        }
        tc.fileNotFound();
        return FilterResponse.responsibleWithoutXynaorder();
      } else {
        String processedPath = preprocessPath(tc.getPath());
        if (isRoot(processedPath)) {
          if (logger.isDebugEnabled()) {
            logger.debug("RequestProtocol.SFTP, requesting root as '" + processedPath + "', returning native file: '" + getSftpRoot().getAbsolutePath() + "'");
          }
          tc.setCacheParameter(FileSystemCacheParameter.noCaching());
          tc.replyWithNativeFile(getSftpRoot());
          return FilterResponse.responsibleWithoutXynaorder();
        } else {
          requestType = PathType.determineRequestType(processedPath);
          if (logger.isDebugEnabled()) {
            logger.debug("Delegating request of path '" + processedPath + "' to Filesystem.");
          }
          if (requestType == PathType.FILESYSTEM) {
            File root = new File(localSftpRoot.get());
            if (!root.exists()) {
              root.mkdirs();
            }
            File adjustedFile = new File(root, tc.getPath().substring(filesystemPathIdentifier.get().length()));
            tc.setCacheParameter(FileSystemCacheParameter.noCaching());
            tc.replyWithNativeFile(adjustedFile);
            return FilterResponse.responsibleWithoutXynaorder();
          } else {
            XynaOrder xo = new XynaOrder(generateOrder(processedPath, tc));
            return FilterResponse.responsible(xo);
          }
        }
      }
    } catch (RuntimeException e) {
      if (logger.isDebugEnabled()) {
        logger.debug("RuntimeException during request on '" + tc.getPath() + "'", e);
      }
      tc.fileNotFound();
      throw e;
    } catch (Error e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Error during request on '" + tc.getPath() + "'", e);
      }
      tc.fileNotFound();
      throw e;
    }
  }
  
  
  private String preprocessPath(String path) {
    return path.replaceAll("//", "/");
  }


  private boolean isRoot(String path) {
    try {
      return path.equals(".") || getSftpRoot().getCanonicalFile().equals(new File(path).getCanonicalFile());
    } catch (IOException e) {
      logger.debug("Exception during root check.",e);
      return false;
    }
  }
  
  
  private XynaOrderCreationParameter generateOrder(String processedPath, SFTPTriggerConnection tc) {
    DestinationKey dk = new DestinationKey(PROCESS_SFTP_ORDERTYPE);
    xact.sftp.Path path = new xact.sftp.Path(processedPath);
    xact.sftp.Username username = new xact.sftp.Username(tc.getUsername());
    xact.sftp.SourceIP sourceip = new xact.sftp.SourceIP(tc.getSourceIp());
    
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(dk, path, username, sourceip);
    return xocp;
  }


  private static enum PathType {
    FILESYSTEM(),
    WORKFLOW();
    
    
    private PathType() {
    }
    
    static PathType determineRequestType(String path) {
      if (path.startsWith(filesystemPathIdentifier.get())) {
        return PathType.FILESYSTEM;
      } else {
        return PathType.WORKFLOW;
      }
    } 
    
  }

  /**
   * called when above XynaOrder returns successfully.
   * @param response by XynaOrder returned XynaObject
   * @param tc corresponding triggerconnection
   */
  public void onResponse(XynaObject response, SFTPTriggerConnection tc) {
    xact.sftp.Content content = null;
    if (response instanceof xact.sftp.Content) {
      content = (xact.sftp.Content) response;
      handleCacheParameter(content.getCacheParameter(), tc);
      logger.debug("Received content: " + content.getContent());
    } else {
      logger.debug("Received content is no xact.sftp.Content: " + String.valueOf(response));
      if (response != null) {
        logger.debug("loaded by: " + response.getClass().getClassLoader());
      }
    }
    if (content == null) {
      logger.debug("SFTPFilter: No valid response given!");
      tc.fileNotFound();
    } else if (content.getDoesExist() != null && !content.getDoesExist()) {
      tc.fileNotFound();
    } else {
      byte[] rawContent = (byte[]) ((List) content.getRawContent()).get(0);
      tc.reply(rawContent);

      logger.debug("Successful reply.");
    }
  }
  
  
  private void handleCacheParameter(CacheParameter cacheParameter, SFTPTriggerConnection tc) {
    if (cacheParameter != null) {
      if (cacheParameter instanceof None) {
        tc.setCacheParameter(FileSystemCacheParameter.noCaching());
      } else if (cacheParameter instanceof SessionIsolated) {
        tc.setCacheParameter(FileSystemCacheParameter.sessionIsolated());
      } else if (cacheParameter instanceof Timed) {
        RelativeDate date = ((Timed)cacheParameter).getTimeout();
        tc.setCacheParameter(FileSystemCacheParameter.timed(new Duration(date.toMillis())));
      }
    } else {
      // default
      tc.setCacheParameter(FileSystemCacheParameter.sessionIsolated());
    }
  }


  private File getSftpRoot() {
    File root = new File(localSftpRoot.get());
    if (!root.exists()) {
      root.mkdirs();
    }
    return root;
  }
  

  /**
   * called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   * @param e
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] e, SFTPTriggerConnection tc) {
    for (XynaException xynaException : e) {
      logger.debug("",xynaException);      
    }
    tc.fileNotFound();
  }

  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    return "SFTPFilter: Supports adhoc generated content from orders and file system based access";
  }

}
