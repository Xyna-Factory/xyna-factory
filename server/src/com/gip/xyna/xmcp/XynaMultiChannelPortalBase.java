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

package com.gip.xyna.xmcp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.gip.xyna.Department;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PackageDefinitionFileInvalidRootException;
import com.gip.xyna.xdev.exceptions.XDEV_PackageDefinitionFileNotFoundException;
import com.gip.xyna.xdev.exceptions.XDEV_PackageDefinitionItemNotFoundException;
import com.gip.xyna.xdev.exceptions.XDEV_ZipFileCouldNotBeCreatedException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;


public abstract class XynaMultiChannelPortalBase extends Department implements Channel {

  public XynaMultiChannelPortalBase() throws XynaException {
    super();
  }


  public abstract GeneralXynaObject waitForMI(XynaOrderServerExtension xo, String reason, String type,
                                              String userGroup, String todo, GeneralXynaObject payload)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


  /**
   * Install a delivery item into this Xyna Factory.
   * @deprecated use installDeliveryItem(InputStream deliveryItem, OutputStream statusOutputStream, boolean
   *             forceOverwrite, boolean dontUpdateXMOM, boolean verboseOutput)
   */
  @Deprecated
  public abstract void installDeliveryItem(File deliveryItem, OutputStream out, boolean forceOverwrite,
                                           boolean dontUpdateMdm, boolean verboseOutput) throws IOException,
      Ex_FileAccessException, XynaException;


  /**
   * Install a delivery item into this Xyna Factory.
   * @param deliveryItem a stream containing the delivery item information
   * @param statusOutputStream an output stream to which status information is written subsequently
   * @param forceOverwrite specifies whether existing files should be overwritten
   * @param dontUpdateXMOM specifies whether no XMOM files should be installed at all
   * @param verboseOutput specifies whether extra debug output should be written to the status output stream
   */
  public abstract void installDeliveryItem(InputStream deliveryItem, OutputStream statusOutputStream,
                                           boolean forceOverwrite, boolean dontUpdateXMOM, boolean verboseOutput)
      throws IOException, Ex_FileAccessException, XynaException;


  /**
   * Create a delivery.
   * @throws XPRC_DESTINATION_NOT_FOUND 
   * @deprecated use installDeliveryItem(InputStream deliveryItem, OutputStream statusOutputStream, boolean
   *             forceOverwrite, boolean dontUpdateXMOM, boolean verboseOutput)
   */
  @Deprecated
  public abstract void createDeliveryItem(File packageDefininition, File deliveryItem, OutputStream out,
                                          boolean verboseOutput, boolean includeXynaComponents)
      throws XDEV_PackageDefinitionFileNotFoundException, XDEV_PackageDefinitionFileInvalidRootException,
      XDEV_ZipFileCouldNotBeCreatedException, Ex_FileAccessException, XDEV_PackageDefinitionItemNotFoundException,
      PersistenceLayerException, XPRC_VERSION_DETECTION_PROBLEM, XPRC_XmlParsingException, IOException, XPRC_DESTINATION_NOT_FOUND;


  /**
   * Create a delivery item
   * @param packageDefinition an InputStream containing the package definition information.
   * @param deliveryItem an OutputStream to which the package content is written
   * @param statusOutputStream an output stream to which status information is written subsequently
   * @param verboseOutput specifies whether extra debug output should be written to the status output stream
   * @param includeXynaComponents specifies whether Xyna components should be exported as well
   * @throws XPRC_DESTINATION_NOT_FOUND 
   */
  public abstract void createDeliveryItem(InputStream packageDefinition, OutputStream deliveryItem,
                                          OutputStream statusOutputStream, boolean verboseOutput,
                                          boolean includeXynaComponents)
      throws XDEV_PackageDefinitionFileNotFoundException, XDEV_PackageDefinitionFileInvalidRootException,
      XDEV_ZipFileCouldNotBeCreatedException, Ex_FileAccessException, XDEV_PackageDefinitionItemNotFoundException,
      PersistenceLayerException, XPRC_VERSION_DETECTION_PROBLEM, XPRC_XmlParsingException, IOException, XPRC_DESTINATION_NOT_FOUND;


  //public abstract XMOMGuiReply processXmomGuiRequest(Session session, XMOMGuiRequest request);
}
