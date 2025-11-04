/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.appmgmt;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.CapacityRequirementXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.FilterInstanceXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.FilterXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.InheritanceRuleXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.MonitoringLevelXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.OrderInputSourceXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.OrdertypeXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.PriorityXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.RuntimeContextRequirementXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.TriggerInstanceXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.TriggerXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.XMOMStorableXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.XMOMXmlEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;

import xmcp.gitintegration.RuntimeContextDependency;

/* 
 * ApplicationXmlEntry has several protected inner classes and fields
 * This classes package allows to access them.
 * 
 * Plan:
 * Phase 1: This class allows making modifications to the protected fields as needed
 * Phase 2: Refactor ApplicationXmlEntry to make inner classes/fields public
 * Phase 3: Refactor this class: move it into a proper package and rename it potentially 
 */
public class ApplicationXmlCompatibilityLayer {

	private ApplicationXmlEntry xml = new ApplicationXmlEntry();

	public void setAppName(String appName) {
		xml.applicationName = appName;
	}

	public void setAppVersion(String version) {
		xml.versionName = version;
	}

	public void createXml(File outputFile) throws ParserConfigurationException, Ex_FileAccessException {
		xml.xmlVersion = ApplicationXmlHandler.XMLVERSION;
		Document doc = xml.buildXmlDocument();
		XMLUtils.saveDom(outputFile, doc);
	}

	public void addFilter(boolean implicit, String filterName, String jarFiles, String fqFilterClassName,
			String triggerName, String sharedLibs) {
		xml.filters.add(new FilterXmlEntry(implicit, filterName, jarFiles, fqFilterClassName, triggerName, sharedLibs));
	}

	public void addFilterInstance(boolean implicit, String filterInstanceName, String filterName,
			String triggerInstanceName, List<String> configurationParameter, String description) {
		xml.filterInstances.add(new FilterInstanceXmlEntry(implicit, filterInstanceName, filterName,
				triggerInstanceName, configurationParameter, description));
	}

	public void addTrigger(boolean implicit, String triggerName, String jarFiles, String fqTriggerClassName,
			String sharedLibs) {
		xml.triggers.add(new TriggerXmlEntry(implicit, triggerName, jarFiles, fqTriggerClassName, sharedLibs));
	}

	public void addTriggerInstance(boolean implicit, String triggerInstanceName, String triggerName,
			String startParameter, Long maxEvents, Boolean rejectRequestsAfterMaxReceives) {
		xml.triggerInstances.add(new TriggerInstanceXmlEntry(implicit, triggerInstanceName, triggerName, startParameter,
				maxEvents, rejectRequestsAfterMaxReceives));
	}

	public void addOrderInputSource(boolean implicit, String name, String type, String orderType,
			Map<String, String> parameterMap, String documentation) {
		xml.orderInputSources
				.add(new OrderInputSourceXmlEntry(implicit, name, type, orderType, parameterMap, documentation));
	}

	public void addInheritanceRule(String ot, ParameterType inheritanceRuleType, String childFilter, String value,
			int precedence) {
		xml.parameterInheritanceRules
				.add(new InheritanceRuleXmlEntry(ot, inheritanceRuleType, childFilter, value, precedence));
	}

	public void addOrderTypePriority(String ot, int priority) {
		xml.priorities.add(new PriorityXmlEntry(ot, priority));
	}

	public void addOrderTypeDispatcherAndMiscConfig(String ot, boolean implicit, String planningDest,
			String executionDest, String cleanupDest, boolean ordercontextMapping) {
		if (implicit && planningDest == null && executionDest == null && cleanupDest == null
				&& ordercontextMapping == false) {
			// could theoretically be skipped, but for now include them to match the
			// behavior of xyna creating the application.xml
		}
		xml.ordertypes.add(
				new OrdertypeXmlEntry(implicit, planningDest, executionDest, cleanupDest, ot, ordercontextMapping));
	}

	public void addMonitoringLevelConfig(String ot, int monitoringlevel) {
		xml.monitoringLevels.add(new MonitoringLevelXmlEntry(ot, monitoringlevel));
	}

	public void addCapacityRequirement(String ot, String cap, int cardinality) {
		xml.capacityRequirements.add(new CapacityRequirementXmlEntry(ot, cap, cardinality));
	}

	public void addXMOMEntry(boolean implicit, String fqName, XMOMType type) {
		xml.xmomEntries.add(new XMOMXmlEntry(implicit, fqName, type.name()));
	}

	public void setRuntimeContextDependencies(List<? extends RuntimeContextDependency> list) {
		for (RuntimeContextDependency dep : list) {
			if ("Workspace".equals(dep.getDepType())) {
				xml.applicationInfo.getRuntimeContextRequirements()
						.add(new RuntimeContextRequirementXmlEntry(null, null, dep.getDepName()));
			} else if ("Application".equals(dep.getDepType())) {
				xml.applicationInfo.getRuntimeContextRequirements()
						.add(new RuntimeContextRequirementXmlEntry(dep.getDepName(), dep.getDepAddition(), null));
			} else {
				throw new RuntimeException("unexpected dep type: " + dep.getDepType());
			}
		}
	}

	public void setFactoryVersion(String factoryVersion) {
		xml.factoryVersion = factoryVersion;
	}

	public void addXMOMStorable(String xmlName, String path, String odsName, String fqPath, String colName) {
		xml.xmomStorables.add(new XMOMStorableXmlEntry(xmlName, path, odsName, fqPath, colName));
	}
}
