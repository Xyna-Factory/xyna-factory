/*----------------------------------------------------
* Xyna 6.1 (Black Edition)
* Activation
*----------------------------------------------------
* Copyright GIP AG 2015
* (http://www.gip.com)
* Hechtsheimer Str. 35-37
* 55131 Mainz
*----------------------------------------------------
* $Revision: 221112 $
* $Date: 2018-04-04 15:22:31 +0200 (Mi, 04 Apr 2018) $
*----------------------------------------------------
*/
package com.gip.xyna.xact.filter;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xact.trigger.SFTPTriggerConnection;
import xact.ssh.sftp.filesystem.FileSystemCacheParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xsched.xynaobjects.RelativeDate;
import com.gip.xyna.xdev.xfractmod.xmdm.FilterConfigurationParameter;

import xact.sftp.CacheParameter;
import xact.sftp.None;
import xact.sftp.SessionIsolated;
import xact.sftp.Timed;

public class SFTPFilter extends ConnectionFilter<SFTPTriggerConnection> {

  private final static long serialVersionUID = 8784549483133615178L;
  private final static Logger logger = CentralFactoryLogging.getLogger(SFTPFilter.class);

  public FilterConfigurationParameter createFilterConfigurationTemplate() {
    return new SFTPFilterConfigurationParameter();
  }

  /**
   * Wird vom Processing in einem eigenen Thread aufgerufen, sobald etwas
   * empfangen wurde und ein Workerthread zur
   * Verarbeitung aufgerufen wurde.
   * Wenn der Filter nicht zuständig ist, wird ein FilterResponse-Objekt mit
   * leerer XynaOrder
   * und NOT_RESPONSIBLE oder RESPONSIBLE_BUT_TO_NEW als FilterResponsibility
   * zurückgeliefert.
   * 
   */
  public FilterResponse createXynaOrder(SFTPTriggerConnection tc, FilterConfigurationParameter config)
      throws XynaException {
    try {
      logger.debug("createXynaOrder");
      logger.debug("Path:" + tc.getPath());
      logger.debug("Username:" + tc.getUsername());
      logger.debug("sourceIp:" + tc.getSourceIp());

      if (tc.getPath() == null || tc.getPath().isEmpty()) {
        if (logger.isDebugEnabled()) {
          logger.debug("Requested path is empty, not responsible.");
        }
        return FilterResponse.notResponsible();
      }

      if (!(config instanceof SFTPFilterConfigurationParameter)) {
        if (logger.isDebugEnabled()) {
          logger.debug("Wrong type of config parameters: " + config.getClass().getName());
        }
        return FilterResponse.notResponsible();
      }
      var fc = (SFTPFilterConfigurationParameter) config;

      if (fc.getFilterPrefix().isPresent() && fc.getFilterPrefix().filter(x -> tc.getPath().startsWith(x)).isEmpty()) {
        if (logger.isDebugEnabled()) {
          logger.debug("Filter does not match prefix: " + fc.getFilterPrefix());
        }
        return FilterResponse.notResponsible();
      }

      DestinationKey dk = new DestinationKey(fc.getOrdertype());
      xact.sftp.Path path = new xact.sftp.Path(tc.getPath());
      xact.sftp.Username username = new xact.sftp.Username(tc.getUsername());
      xact.sftp.SourceIP sourceip = new xact.sftp.SourceIP(tc.getSourceIp());

      XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(dk, path, username, sourceip);

      return FilterResponse.responsible(new XynaOrder(xocp));
    } catch (RuntimeException | Error e) {
      if (null != tc) {
        logger.debug("RuntimeException during request on '" + tc.getPath() + "'", e);
        tc.fileNotFound();
      } else {
        logger.debug("RuntimeException during request");
      }
      throw e;
    } catch (Throwable t) {
      logger.error("Unexpected Error", t);
      throw t;
    }
  }

  /**
   * called when above XynaOrder returns successfully.
   * 
   * @param response by XynaOrder returned XynaObject
   * @param tc       corresponding triggerconnection
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

  /**
   * called when above XynaOrder returns with error or if an XynaException occurs
   * in generateXynaOrder().
   * 
   * @param e
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] e, SFTPTriggerConnection tc) {
    for (XynaException xynaException : e) {
      logger.debug("", xynaException);
    }
    tc.fileNotFound();
  }

  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    return "SFTPFilter: Supports adhoc generated content from orders.";
  }

  private void handleCacheParameter(CacheParameter cacheParameter, SFTPTriggerConnection tc) {
    if (cacheParameter != null) {
      if (cacheParameter instanceof None) {
        tc.setCacheParameter(FileSystemCacheParameter.noCaching());
      } else if (cacheParameter instanceof SessionIsolated) {
        tc.setCacheParameter(FileSystemCacheParameter.sessionIsolated());
      } else if (cacheParameter instanceof Timed) {
        RelativeDate date = ((Timed) cacheParameter).getTimeout();
        tc.setCacheParameter(FileSystemCacheParameter.timed(new Duration(date.toMillis())));
      }
    } else {
      // default
      tc.setCacheParameter(FileSystemCacheParameter.noCaching());
    }
  }

}
