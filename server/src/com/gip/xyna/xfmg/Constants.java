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

package com.gip.xyna.xfmg;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xnwh.persistence.Persistable;


public class Constants {

  public static final String SERVER_SHELLNAME = "xynafactory.sh";
  public static final String FACTORY_NAME = "Xyna Factory";

  /**
   * Constant '/'
   */
  public static final String fileSeparator = "/"; //FIXME: aus dem namen sollte klar werden: was unterscheidet den von System.getProperty("path.separator")?
  /**
   * equals the System property file.separator
   */
  public static final String FILE_SEPARATOR = System.getProperty("file.separator");
  public static final String PERSISTENCE_GEN_CLASSES_CLASSDIR = "xnwhclasses";
  public static final String PERSISTENCE_GEN_CLASSES_PACKAGE = "com.gip.xyna.xnwh.persistence.memory.gen";
  public static final String CLUSTER_PROVIDER_BASE_PACKAGE = "com.gip.xyna.xfmg.xclusteringservices.clusterprovider";
  
  //TODO schöner für tests wäre, wenn man wie im memory-PL nicht den lib ordner hardcoded, sondern statt dessen aus dem appclassloader die jars ermittelt.
  public static String LIB_DIR = "lib"; //nicht final, weil tests das verbiegen
  public static final String USERLIB_DIR = "userlib";
  public static String SERVER_CLASS_DIR = "bin"; //nicht final, weil tests das verbiegen
  /**
   * basisverzeichnis des servers - enthält zb MDM verzeichnis(se), server-verzeichnis, usw
   */
  public static final String BASEDIR = "..";
  public static final String ROOT_DIR_FOR_REPOSITORY = BASEDIR + fileSeparator + "xmomrepository";

  
  public static final String PERSISTENCE_LAYERS_BASEDIR = "." + fileSeparator + "persistencelayers";
  public static final String REPOSITORYACCESS_BASEDIR = "." + fileSeparator + "repositoryaccess";
  public static final String CLUSTERPROVIDERS_BASEDIR = "." + fileSeparator + "clusterproviders";
  public static final String REMOTEACCESS_BASEDIR = "." + fileSeparator + "remoteaccess";
  public static final String DATAMODELTYPE_BASEDIR = "." + fileSeparator + "datamodeltypes";
  public static final String REMOTEDESTINATIONTYPE_BASEDIR = "." + fileSeparator + "remotedestinationtypes";
  public static final String ORDERINPUTSOURCETYPE_BASEDIR = "." + fileSeparator + "orderinputsourcetypes";
  
  

  public static final String REVISION_PATH = "revisions";
  public static final String PREFIX_REVISION = "rev_";
  public static final String PREFIX_SAVED = "saved";
  public static final String SUFFIX_REVISION_WORKINGSET = "workingset";
  public static final String SUFFIX_REVISION_DATAMODEL = "datamodel";

  
  
  public static final String SUBDIR_XMOM = "XMOM";
  public static final String SUBDIR_SERVICES = "services";
  public static final String SUBDIR_XMOMCLASSES = "xmomclasses";
  public static final String SUBDIR_SHAREDLIBS = "sharedLibs";
  public static final String SUBDIR_TRIGGER = "trigger";
  public static final String SUBDIR_FILTER = "filter";
  public static final String SUBDIR_THIRD_PARTIES = "third_parties";
  
  
  @Deprecated
  public static final String DEPLOYED_MDM_DIR = RevisionManagement.getPathForRevision(PathType.XMOM, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  @Deprecated
  public static final String DEPLOYED_SERVICES_DIR = RevisionManagement.getPathForRevision(PathType.SERVICE, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  @Deprecated
  public static final String MDM_CLASSDIR = RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  @Deprecated
  public static String SHAREDLIB_BASEDIR = RevisionManagement.getPathForRevision(PathType.SHAREDLIB, RevisionManagement.REVISION_DEFAULT_WORKSPACE); // not final, because the tests may change it
  @Deprecated
  public static final String TRIGGER_DEPLOYMENT_DIR = RevisionManagement.getPathForRevision(PathType.TRIGGER, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  @Deprecated
  public static final String FILTER_DEPLOYMENT_DIR = RevisionManagement.getPathForRevision(PathType.FILTER, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  
  public static final String PERSISTENCE_CONFIGURATION_DIR_WITHIN_STORAGE = "persistence";
  
  public static final String GENERATION_DIR = "." + fileSeparator + "gen";
  public static final boolean GENERATE_CLASSLOADER_PHANTOM_REFERENCES = false; //nur für entwicklungszwecke auf true zu stellen!!
  
  //public static boolean REMOVE_GENERATED_FILES = false; //nur für entwicklungszwecke auf false zu stellen!!
  //ist nun XynaProperty.REMOVE_GENERATED_FILES
  /**
   * @see Persistable.StorableProperty#PROTECTED
   */
  public static final boolean PROTECTED_STORABLE_ENABLE = true; //nur für entwicklungszwecke auf false stellen!!

  public static boolean SHOW_SESSION_ID_IN_DEBUG = false;

  public static String PATH_TO_XSD_FILE = "." + fileSeparator + "resources" + fileSeparator + "XMDM.xsd"; //nicht final, weil tests das verbiegen
  
  public static boolean NORMAL_SECURITY_MANAGER = true;
  
  public static final String FRACTAL_MODELLING_NAMESPACE = "http://www.gip.com/xyna/xdev/xfractmod";

  public static final String DEFAULT_ENCODING = "UTF-8";
  public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

  public static final String DEFAULT_TIMEZONE = "UTC";

  private static SimpleDateFormat createDateFormat(String format) {
    // nicht statische variable weil nicht threadsafe
    //https://en.wikipedia.org/wiki/ISO_8601
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    sdf.setTimeZone(TimeZone.getTimeZone(DEFAULT_TIMEZONE));
    sdf.setLenient(false);
    return sdf;
  }

  public static SimpleDateFormat defaultUTCSimpleDateFormat() {
    // https://en.wikipedia.org/wiki/ISO_8601
    return createDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
  }
  public static SimpleDateFormat defaultUTCSimpleDateFormatWithMS() {
    // https://en.wikipedia.org/wiki/ISO_8601
    return createDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
  }

  public static final int DEFAULT_THREAD_PRIORITY = 5;
  
  public static final long DEFAULT_CANCEL_TIMEOUT = (long) 5000;
  public static final String STORAGE_PATH = "storage";
  public static final int DELETE_FILE_RETRIES = 20;
  public static final String SERVER_POLICY = "server.policy";

  
  public static final String LOG4J_CONFIGURATION_KEY = "log4j.configurationFile";
  /**
   *  in sekunden. 
   */
  public static final int RMI_DEFAULT_TIMEOUT = 15;
  
  public static final int DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__CRITICAL = Integer.MAX_VALUE;
  public static final int DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__MINOR = 100;
  public static final int DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__USERINTERACTION = 5;
  
  public static final int DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES = 3;
  
  public static final boolean RECORD_THREAD_INFO_CONNECTION_CACHE = false; // ist teuer, nur für Entwicklungszwecke auf true?
  
  /**
   * Path separator character used in java.class.path
   */
  public static final String PATH_SEPARATOR = System.getProperty("path.separator");
  
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");
  
  public static boolean PAUSE_SCHEDULER_AT_STARTUP = false;
  
  // TODO rename
  // currently used to add server/bin to compile-classpathes in cases where a xynafactory.jar is not contained in libs 
  public static boolean RUNS_FROM_SOURCE = true;
}
