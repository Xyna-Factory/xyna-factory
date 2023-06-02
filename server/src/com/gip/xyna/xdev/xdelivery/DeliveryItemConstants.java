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
package com.gip.xyna.xdev.xdelivery;




public class DeliveryItemConstants {

  public static final String IGNORE_DEPENDENCIES = "ignoreDependencies";
  public static final String PACKAGE_FILE_ROOT = "XynaPackage";

  public static final String XML_FILE = "delivery.xml";

  // The zip files are added to the archive relative to BASEDIR.
  // As long as the MDM path contains a .., BASEDIR has to be .. as well,
  // otherwise the relative path generation will fail.
  public static final String BASEDIR = "..";

  public static final String LOG4J_PROPERTIES = "log4j2.xml";
  public static final String SERVER_POLICY = "server.policy";

  // xml delivery item element name

  public static final String CONTAINER_ELEMENT = "DeliveryItem";

  public static final String VERSION_NUMBER_ELEMENT = "VersionNumber";

  public static final String DATA_TYPE_SECTION = "DataTypes";
  public static final String DATA_TYPE_ELEMENT = "DataType";
  
  public static final String EXCEPTION_SECTION = "Exceptions";
  public static final String EXCEPTION_ELEMENT = "Exception";
  
  public static final String WORKFLOW_SECTION = "Workflows";
  public static final String WORKFLOW_ELEMENT = "Workflow";
  public static final String WORKFLOW_DEPLOY_STATE_ATTR = "state";
  public static final String WORKFLOW_DEPLOYED = "deployed";
  public static final String WORKFLOW_SAVED = "saved";

  public static final String CRONLS_SECTION = "CronLSOrders";
  public static final String CRONLS_ELEMENT = "CronLSOrder";
  public static final String CRONLS_INTERVAL_ATTR = "interval";
  public static final String CRONLS_STARTTIME_ATTR = "starttime";
  public static final String CRONLS_ORDER_ATTR = "workflow";
  public static final String CRONLS_PAYLOAD_ELEMENT = "payload";
  public static final String CRONLS_LABEL_ATTR = "label";
  public static final String CRONLS_ENABLED_ATTR = "enabled";
  public static final String CRONLS_ONERROR_ATTR = "onError";


  public static final String PROPERTY_SECTION = "XynaProperties";
  public static final String PROPERTY_ELEMENT = "XynaProperty";
  public static final String PROPERTY_KEY = "key";
  public static final String PROPERTY_VALUE = "value";

  public static final String TRIGGER_SECTION = "Triggers";
  public static final String TRIGGER_ELEMENT = "Trigger";
  public static final String TRIGGER_NAME_ATTR = "name";
  public static final String TRIGGER_FQCLASSNAME_ATTR = "fqclasssname";
  public static final String TRIGGER_SHAREDLIB_ELEMENT = "SharedLib";
  public static final String TRIGGER_SHAREDLIB_NAME_ATTR = "name";
  public static final String TRIGGER_JAR_ELEMENT = "Jar";
  public static final String TRIGGER_JAR_NAME_ATTR = "name";
  public static final String TRIGGER_INSTANCE_ELEMENT = "TriggerInstance";
  public static final String TRIGGER_INSTANCE_NAME_ATTR = "name";
  public static final String TRIGGER_INSTANCE_INSTANCENAME_ATTR = "instanceName";
  public static final String TRIGGER_INSTANCE_STARTPARAM_ELEMENT = "StartParameter";
  public static final String TRIGGER_INSTANCE_STARTPARAM_VALUE_ATTR = "value";


  public static final String FILTER_SECTION = "Filters";
  public static final String FILTER_ELEMENT = "Filter";
  public static final String FILTER_NAME_ATTR = "name";
  public static final String FILTER_TRIGGERNAME_ATTR = "triggerName";
  public static final String FILTER_FQCLASSNAME_ATTR = "fqclasssname";
  public static final String FILTER_SHAREDLIB_ELEMENT = "SharedLib";
  public static final String FILTER_SHAREDLIB_NAME_ATTR = "name";
  public static final String FILTER_JAR_ELEMENT = "Jar";
  public static final String FILTER_JAR_NAME_ATTR = "name";
  public static final String FILTER_INSTANCE_ELEMENT = "FilterInstance";
  public static final String FILTER_INSTANCE_NAME_ATTR = "name";
  public static final String FILTER_INSTANCE_INSTANCENAME_ATTR = "instanceName";
  public static final String FILTER_INSTANCE_TRIGGERINSTANCENAME_ATTR = "triggerInstanceName";

  public static final String CAPACITY_SECTION = "Capacities";
  public static final String CAPACITY_ELEMENT = "Capacity";
  public static final String CAPACITY_NAME_ATTR = "name";
  public static final String CAPACITY_CARDINALITY_ATTR = "cardinality";
  public static final String CAPACITY_STATE_ATTR = "state";

  public static final String CAPACITYMAPPING_SECTION = "CapacityMappings";
  public static final String CAPACITYMAPPING_ELEMENT = "CapacityMapping";
  public static final String CAPACITYMAPPING_WORKFLOW_ATTR = "workflow";
  public static final String CAPACITYMAPPING_ORDERTYPE_ATTR = "orderType";
  public static final String CAPACITYMAPPING_CAPACITYNAME_ATTR = "capacityName";
  public static final String CAPACITYMAPPING_CARDINALITY_ATTR = "cardinality";

  public static final String RIGHTS_SECTION = "Rights";
  public static final String RIGHTS_ELEMENT = "Right";
  public static final String RIGHTS_NAME_ATTR = "name";
  public static final String RIGHTS_DESCRIPTION_ATTR = "description";
  
  public static final String DOMAINS_SECTION = "Domains";
  public static final String DOMAINS_ELEMENT = "Domain";
  public static final String DOMAINS_NAME_ATTR = "name";
  public static final String DOMAINS_DESCRIPTION_ATTR = "description";
  public static final String DOMAINS_TYPE_ATTR = "type";
  public static final String DOMAINS_MAXRETRIES_ATTR = "maxretries";
  public static final String DOMAINS_CONNECTIONTIMEOUT_ATTR = "connectiontimeout";
  public static final String DOMAINS_RADIUSDATA_ASSOCIATEDORDERTYPE_ATTR = "associatedordertype";
  public static final String DOMAINS_RADIUSSERVER_ELEMENT = "RADIUSServer";
  public static final String DOMAINS_RADIUSSERVER_IP_ATTR = "ip";
  public static final String DOMAINS_RADIUSSERVER_PORT_ATTR = "port";
  public static final String DOMAINS_RADIUSSERVER_PRESHAREDKEY_ATTR = "presharedkey";

  public static final String ROLE_SECTION = "Roles";
  public static final String ROLE_ELEMENT = "Role";
  public static final String ROLE_NAME_ATTR = "name";
  public static final String ROLE_RIGHTNAME_ATTR = "name";
  public final static String ROLE_DESCRIPTION_ATTR = "description";
  public final static String ROLE_ALIAS_ATTR = "alias";
  public final static String ROLE_DOMAIN_ATTR = "domain";
  

  public static final String USER_SECTION = "Users";
  public static final String USER_ELEMENT = "User";
  public static final String USER_USERID_ATTR = "usedId";
  public static final String USER_ROLENAME_ATTR = "role";
  public static final String USER_PASSWORD_ATTR = "password";
  public static final String USER_DOMAIN_ATTR = "domainname";
  public static final String USER_LOCKED_ATTR = "locked";
  
  public static final String DOMAIN_ELEMENT = "Domain"; //currently only used inside user user-sections
  
  public static final String MONITORING_SECTION = "MonitoringProperties";
  public static final String MONITORING_ELEMENT = "MonitoringProperty";
  public static final String MONITORING_WF_ATTR = "workflow";
  public static final String MONITORING_LEVEL_ATTR = "level";
  
  public static final String FILE_ELEMENT = "File";

}
