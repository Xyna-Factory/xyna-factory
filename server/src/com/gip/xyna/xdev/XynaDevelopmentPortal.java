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

package com.gip.xyna.xdev;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PackageDefinitionFileInvalidRootException;
import com.gip.xyna.xdev.exceptions.XDEV_PackageDefinitionFileNotFoundException;
import com.gip.xyna.xdev.exceptions.XDEV_PackageDefinitionItemNotFoundException;
import com.gip.xyna.xdev.exceptions.XDEV_RefactoringConflict;
import com.gip.xyna.xdev.exceptions.XDEV_RefactoringFault;
import com.gip.xyna.xdev.exceptions.XDEV_ZipFileCouldNotBeCreatedException;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringActionParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringResult;
import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagementPortal;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;



public interface XynaDevelopmentPortal extends LockManagementPortal {

  public void installDeliveryItem(InputStream deliveryItem, OutputStream statusOutputStream, boolean forceOverwrite,
                                  boolean dontUpdateXMOM, boolean verboseOutput) throws IOException,
      Ex_FileAccessException, XynaException;
  
  public void installDeliveryItem(File deliveryItem, OutputStream statusOutputStream, boolean forceOverwrite,
                                  boolean dontUpdateXMOM, boolean verboseOutput) throws IOException,
      Ex_FileAccessException, XynaException;


  public void createDeliveryItem(InputStream packageDefininition, OutputStream deliveryItem,
                                 OutputStream statusOutputStream, boolean verboseOutput, boolean includeXynaComponents)
      throws XDEV_PackageDefinitionFileNotFoundException, XDEV_PackageDefinitionFileInvalidRootException,
      XDEV_ZipFileCouldNotBeCreatedException, Ex_FileAccessException, XDEV_PackageDefinitionItemNotFoundException,
      PersistenceLayerException, XPRC_VERSION_DETECTION_PROBLEM, XPRC_XmlParsingException, IOException, XPRC_DESTINATION_NOT_FOUND;


  public void createDeliveryItem(File packageDefininition, File deliveryItem, OutputStream statusOutputStream,
                                 boolean verboseOutput, boolean includeXynaComponents)
      throws XDEV_PackageDefinitionFileNotFoundException, XDEV_PackageDefinitionFileInvalidRootException,
      XDEV_ZipFileCouldNotBeCreatedException, Ex_FileAccessException, XDEV_PackageDefinitionItemNotFoundException,
      PersistenceLayerException, XPRC_VERSION_DETECTION_PROBLEM, XPRC_XmlParsingException, IOException,
      XPRC_DESTINATION_NOT_FOUND;


  public RefactoringResult refactorXMOM(RefactoringActionParameter action) throws XDEV_RefactoringConflict, XDEV_RefactoringFault;

}
