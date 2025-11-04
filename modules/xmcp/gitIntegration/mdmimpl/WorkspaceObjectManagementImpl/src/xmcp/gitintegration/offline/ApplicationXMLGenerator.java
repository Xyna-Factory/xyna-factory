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
package xmcp.gitintegration.offline;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.Document;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.utils.XMLUtils;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlCompatibilityLayer;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.FileSystemXMLSource;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.XMLSourceAbstraction;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;

import xmcp.gitintegration.ApplicationDefinition;
import xmcp.gitintegration.Capacity;
import xmcp.gitintegration.ContentEntry;
import xmcp.gitintegration.Datatype;
import xmcp.gitintegration.DispatcherDestination;
import xmcp.gitintegration.Filter;
import xmcp.gitintegration.FilterInstance;
import xmcp.gitintegration.InheritanceRule;
import xmcp.gitintegration.InputSourceSpecific;
import xmcp.gitintegration.OrderInputSource;
import xmcp.gitintegration.OrderType;
import xmcp.gitintegration.RuntimeContextDependency;
import xmcp.gitintegration.Trigger;
import xmcp.gitintegration.TriggerInstance;
import xmcp.gitintegration.WorkspaceContent;
import xmcp.gitintegration.WorkspaceContentItem;
import xmcp.gitintegration.XMOMStorable;
import xmcp.gitintegration.impl.WorkspaceContentCreator;
import xmcp.gitintegration.impl.processing.OrderTypeProcessor;

public class ApplicationXMLGenerator {

	public static void main(String[] args) throws Exception {
		if (args.length < 5 || args.length > 6) {
			// later: Capacities and XynaProperties can be added through a separate main
			// method call (in FactoryObjectManagement Coded Service)
			System.out.println("Creates application.xml from workspace-xml directory");
			System.out.println(
					"Args: <Workspace-XML directory> <XMOM directory> <Version> <Factory version> <Subtype exclusion> "
							+ "[<Output, default ./application.xml>]");
			System.out.println(
					"Workspace-XML directory: The directory, containing workspace.xml (typically called \"config\")");
			System.out.println("XMOM directory: The directory containing the XMOM files of the application");
			System.out.println("Subtype exclusion (The same as the corresponding Xyna Property): Comma separated"
					+ " list of base types whose subtypes are taken into the application only when they"
					+ " are needed directly. * to disallow all subtypes.");
			return;
		}
		File workspacexmldir = new File(args[0]);
		File xmomdir = new File(args[1]);
		String version = args[2];
		String factoryVersion = args[3];
		String subtypeExclusion = args[4];
		File outputFile;
		if (args.length == 6) {
			outputFile = new File(args[5]);
		} else {
			outputFile = new File("application.xml");
		}
		validateWorkspaceXMLDir(workspacexmldir);

		WorkspaceContent content = getWorkspaceContent(workspacexmldir);
		buildAppXml(content, xmomdir, version, factoryVersion, subtypeExclusion, outputFile);
	}

	private static WorkspaceContent getWorkspaceContent(File workspacexmldir) {
		WorkspaceContentCreator creator = new WorkspaceContentCreator();
		return creator.createWorkspaceContentFromFile(workspacexmldir);
	}

	private static class SubTypeCache {

		private final Map<DomOrExceptionGenerationBase, Set<DomOrExceptionGenerationBase>> subtypes = new HashMap<>();

		/**
		 * returns recursively subtypes and membervars. i.e. including something like
		 * the subtype of a membervar of a membervar of a subtype of a membervar of X
		 * 
		 * only returns existing objects
		 */
		public Set<DomOrExceptionGenerationBase> getSubTypesRecursively(XMOMContext ctx,
				DomOrExceptionGenerationBase obj) {
			if (ctx.subtypeExclusion.exclude.contains(obj.getOriginalFqName())) {
				return Collections.emptySet();
			}
			Set<DomOrExceptionGenerationBase> coll = new HashSet<>();
			collectSubTypesRecursively(ctx, coll, obj);
			return coll;
		}

		private void collectSubTypesRecursively(XMOMContext ctx, Set<DomOrExceptionGenerationBase> coll,
				DomOrExceptionGenerationBase obj) {
			if (!obj.exists()) {
				return;
			}
			if (ctx.subtypeExclusion.exclude.contains(obj.getOriginalFqName())) {
				return;
			}
			if (!coll.add(obj)) {
				return;
			}
			Set<DomOrExceptionGenerationBase> subs = subtypes.get(obj);
			if (subs != null) {
				for (DomOrExceptionGenerationBase gb : subs) {
					collectSubTypesRecursively(ctx, coll, gb);
				}
			}
			for (AVariable var : obj.getAllMemberVarsIncludingInherited()) {
				if (var.getDomOrExceptionObject() != null) {
					collectSubTypesRecursively(ctx, coll, var.getDomOrExceptionObject());
				}
			}
		}

		private void fillCache(XMOMContext ctx) throws Exception {
			List<File> list = new ArrayList<>();
			FileUtils.findFilesRecursively(ctx.xmomDir, list,
					(dir, name) -> new File(dir, name).isDirectory() || name.endsWith(".xml"));
			for (File f : list) {
				Document doc = XMLUtils.parse(f);
				String fqXmlName = GenerationBase.getFqXMLName(doc);
				GenerationBase gb = GenerationBase.getOrCreateInstance(fqXmlName, ctx.cache, ctx.revision, ctx.source);
				if (gb instanceof DomOrExceptionGenerationBase) {
					if (!gb.parsingFinished() && !gb.hasError()) {
						gb.parseGeneration(true, false, false);
					}

					DomOrExceptionGenerationBase dex = (DomOrExceptionGenerationBase) gb;
					subtypes.computeIfAbsent(dex, a -> new HashSet<>());
					if (dex.getSuperClassGenerationObject() != null) {
						subtypes.computeIfAbsent(dex.getSuperClassGenerationObject(), a -> new HashSet<>()).add(dex);
					}
				} else {
					// ntbd
				}
			}
		}

	}

	private static class XMOMContext {

		public final SubtypeExclusion subtypeExclusion;
		public final File xmomDir;
		public final XMLSourceAbstraction source;
		public final GenerationBaseCache cache;
		public final long revision;
		public final SubTypeCache subtypeCache;

		public XMOMContext(SubtypeExclusion subtypeExclusion, File xmomDir, XMLSourceAbstraction source,
				GenerationBaseCache cache, long revision) throws Exception {
			this.subtypeExclusion = subtypeExclusion;
			this.xmomDir = xmomDir;
			this.source = source;
			this.cache = cache;
			this.revision = revision;
			subtypeCache = new SubTypeCache();
			subtypeCache.fillCache(this);
		}

	}

	private static Set<String> nonWFDestinations = new HashSet<>();
	static {
		nonWFDestinations.add(XynaDispatcher.DESTINATION_DEFAULT_PLANNING.getFQName());
		nonWFDestinations.add(XynaDispatcher.DESTINATION_EMPTY_PLANNING.getFQName());
		nonWFDestinations.add(XynaDispatcher.DESTINATION_EMPTY_WORKFLOW.getFQName());
	}

	private static void buildAppXml(WorkspaceContent content, File xmomDir, String version, String factoryVersion,
			String subtypeExclusion, File outputFile) throws Exception {
		ApplicationXmlCompatibilityLayer xml = new ApplicationXmlCompatibilityLayer();
		xml.setAppVersion(version);
		xml.setFactoryVersion(factoryVersion);
		Set<GenerationBase> explicitContent = new HashSet<>();
		Set<GenerationBase> implicitContent = new HashSet<>();
		String appName = extractAppName(content);
		xml.setAppName(appName);
		XMOMContext ctx = createContext(content, version, xmomDir, subtypeExclusion, appName);

		Set<String> orderTypesInContent = new HashSet<>();
		Set<String> explicitOrderTypes = new HashSet<>();
		Map<String, OrderType> allOrderTypes = new HashMap<>();
		Set<String> orderInputSourcesInContent = new HashSet<>();
		Set<String> filtersInContent = new HashSet<>();
		Set<String> filterInstancesInContent = new HashSet<>();
		Set<String> triggersInContent = new HashSet<>();
		Set<String> triggerInstancesInContent = new HashSet<>();
		Set<String> sharedLibsInContent = new HashSet<>();
		Map<String, Filter> allFilters = new HashMap<>();
		Map<String, FilterInstance> allFilterInstances = new HashMap<>();
		Map<String, Trigger> allTriggers = new HashMap<>();
		Map<String, TriggerInstance> allTriggerInstances = new HashMap<>();
		System.out.println("Collecting Workspace-XML entries ...");
		for (WorkspaceContentItem item : content.getWorkspaceContentItems()) {
			if (item instanceof OrderType) {
				OrderType ot = (OrderType) item;
				allOrderTypes.put(ot.getName(), ot);
			} else if (item instanceof ApplicationDefinition) {
				ApplicationDefinition ad = (ApplicationDefinition) item;
				xml.setRuntimeContextDependencies(ad.getRuntimeContextDependencies());
				if (ad.getContentEntries() != null) {
					for (ContentEntry ce : ad.getContentEntries()) {
						ApplicationEntryType type = ApplicationEntryType.valueOf(ce.getType());
						switch (type) {
						case DATATYPE:
							explicitContent
									.add(DOM.getOrCreateInstance(ce.getFQName(), ctx.cache, ctx.revision, ctx.source));
							break;
						case EXCEPTION:
							explicitContent.add(ExceptionGeneration.getOrCreateInstance(ce.getFQName(), ctx.cache,
									ctx.revision, ctx.source));
							break;
						case WORKFLOW:
							orderTypesInContent.add(ce.getFQName());
							explicitContent
									.add(WF.getOrCreateInstance(ce.getFQName(), ctx.cache, ctx.revision, ctx.source));
							break;
						case ORDERTYPE:
							explicitOrderTypes.add(ce.getFQName());
							orderTypesInContent.add(ce.getFQName());
							break;
						case ORDERINPUTSOURCE:
							orderInputSourcesInContent.add(ce.getFQName());
							break;
						case FILTER:
							filtersInContent.add(ce.getFQName());
							break;
						case FILTERINSTANCE:
							filterInstancesInContent.add(ce.getFQName());
							break;
						case TRIGGER:
							triggersInContent.add(ce.getFQName());
							break;
						case TRIGGERINSTANCE:
							triggerInstancesInContent.add(ce.getFQName());
							break;
						case SHAREDLIB:
							sharedLibsInContent.add(ce.getFQName());
							break;
						case FORMDEFINITION:
						case CAPACITY:
						case XYNAPROPERTY:
							// ntbd
							break;
						default:
							throw new RuntimeException("unexpected type : " + type);
						}
					}
				}
			} else if (item instanceof Filter) {
				Filter f = (Filter) item;
				allFilters.put(f.getFilterName(), f);
			} else if (item instanceof FilterInstance) {
				FilterInstance fi = (FilterInstance) item;
				allFilterInstances.put(fi.getFilterInstanceName(), fi);
			} else if (item instanceof Trigger) {
				Trigger t = (Trigger) item;
				allTriggers.put(t.getTriggerName(), t);
			} else if (item instanceof TriggerInstance) {
				TriggerInstance ti = (TriggerInstance) item;
				allTriggerInstances.put(ti.getTriggerInstanceName(), ti);
			}
		}

		// add xmom entries for ordertype destinations
		for (String ots : orderTypesInContent) {
			OrderType ot = allOrderTypes.get(ots);
			if (ot == null) {
				// assume default destination of workflow with same name
				implicitContent.add(WF.getOrCreateInstance(ots, ctx.cache, ctx.revision, ctx.source));
				ot = new OrderType();
				ot.setName(ots);
				allOrderTypes.put(ots, ot);
			} else if (ot.getDispatcherDestinations() != null) {
				for (DispatcherDestination dd : ot.getDispatcherDestinations()) {
					if (dd.getDestinationType().equals(ExecutionType.XYNA_FRACTAL_WORKFLOW.getTypeAsString())) {
						if (!nonWFDestinations.contains(dd.getDestinationValue())) {
							implicitContent.add(WF.getOrCreateInstance(dd.getDestinationValue(), ctx.cache,
									ctx.revision, ctx.source));
						}
					}
					// FIXME service destination?
				}
			}
		}

		System.out.println("Calculating implicit XMOM entries ...");
		calculateImplicitXMOMEntries(explicitContent, implicitContent, ctx);
		System.out.println("finishing up application.xml ...");
		setOrderTypeConfig(xml, explicitContent, implicitContent, allOrderTypes, orderTypesInContent,
				explicitOrderTypes);

		for (GenerationBase gb : explicitContent) {
			xml.addXMOMEntry(false, gb.getOriginalFqName(), XMOMType.getXMOMTypeByGenerationInstance(gb));
		}
		for (GenerationBase gb : implicitContent) {
			xml.addXMOMEntry(true, gb.getOriginalFqName(), XMOMType.getXMOMTypeByGenerationInstance(gb));
		}

		orderInputSourcesInContent.addAll(Stream.concat(explicitContent.stream(), implicitContent.stream())
				.filter(gb -> gb instanceof WF).map(gb -> ((WF) gb).getAllReferencedOrderInputSources())
				.flatMap(Set::stream).collect(Collectors.toSet()));

		// we could also filter by dom.isInheritedFromStorable, but that doesn't work
		// properly because we don't parse all dependent XMOMs
		Set<String> storablesInContent = new HashSet<>(Stream.concat(explicitContent.stream(), implicitContent.stream())
				.filter(gb -> gb instanceof DOM).map(gb -> gb.getOriginalFqName()).collect(Collectors.toList()));
		for (WorkspaceContentItem item : content.getWorkspaceContentItems()) {
			if (item instanceof OrderType) {
				// already handled
			} else if (item instanceof ApplicationDefinition) {
				// already handled
			} else if (item instanceof Datatype) {
				// ignore, only needed for gitintegration
			} else if (item instanceof Filter) {
				Filter f = (Filter) item;
				if (filtersInContent.contains(f.getFilterName()) || filterUsedByFilterInstances(
						filterInstancesInContent, allFilterInstances, f.getFilterName())) {
					xml.addFilter(!filtersInContent.contains(f.getFilterName()), f.getFilterName(), f.getJarfiles(),
							f.getFQFilterClassName(), f.getTriggerName(), f.getSharedlibs());
				}
			} else if (item instanceof FilterInstance) {
				FilterInstance fi = (FilterInstance) item;
				if (filterInstancesInContent.contains(fi.getFilterInstanceName())) {
					List<String> configParas = new ArrayList<>(); // TODO missing in workspace-xml
					xml.addFilterInstance(false, fi.getFilterInstanceName(), fi.getFilterName(),
							fi.getTriggerInstanceName(), configParas, null);
				}
			} else if (item instanceof Trigger) {
				Trigger t = (Trigger) item;
				if (triggersInContent.contains(t.getTriggerName())
						|| triggerIsUsed(t.getTriggerName(), allFilterInstances, filterInstancesInContent, allFilters,
								filtersInContent, allTriggerInstances, triggerInstancesInContent)) {
					xml.addTrigger(!triggersInContent.contains(t.getTriggerName()), t.getTriggerName(), t.getJarfiles(),
							t.getFQTriggerClassName(), t.getSharedlibs());
				}
			} else if (item instanceof TriggerInstance) {
				TriggerInstance ti = (TriggerInstance) item;
				if (triggerInstancesInContent.contains(ti.getTriggerInstanceName()) || triggerInstanceIsUsed(
						ti.getTriggerInstanceName(), allFilterInstances, filterInstancesInContent)) {
					xml.addTriggerInstance(!triggerInstancesInContent.contains(ti.getTriggerInstanceName()),
							ti.getTriggerInstanceName(), ti.getTriggerName(), ti.getStartParameter(),
							ti.getMaxReceives(), ti.getRejectAfterMaxReceives());
				}
			} else if (item instanceof OrderInputSource) {
				OrderInputSource ois = (OrderInputSource) item;
				if (orderInputSourcesInContent.contains(ois.getName())) {
					Map<String, String> map = new HashMap<>();
					if (ois.getInputSourceSpecifics() != null) {
						for (InputSourceSpecific iss : ois.getInputSourceSpecifics()) {
							map.put(iss.getKey(), iss.getValue());
						}
					}
					xml.addOrderInputSource(true, ois.getName(), ois.getType(), ois.getOrderType(), map,
							ois.getDocumentation());
				}
			} else if (item instanceof XMOMStorable) {
				XMOMStorable xs = (XMOMStorable) item;
				if (storablesInContent.contains(xs.getXMLName())) {
					xml.addXMOMStorable(xs.getXMLName(), xs.getPath(), xs.getODSName(), xs.getFQPath(),
							xs.getColumnName());
				}
			} else if (item instanceof RuntimeContextDependency) {
				// ignore, only needed for workspace, not for app def
			} else {
				throw new RuntimeException("unhandled type of workflow content: " + item.getClass().getName());
			}
		}

		System.out.println("Writing " + outputFile + " ...");
		xml.createXml(outputFile);
	}

	private static boolean triggerInstanceIsUsed(String triggerInstanceName,
			Map<String, FilterInstance> allFilterInstances, Set<String> filterInstancesInContent) {
		for (String fi : filterInstancesInContent) {
			if (allFilterInstances.get(fi).getTriggerInstanceName().equals(triggerInstanceName)) {
				return true;
			}
		}
		return false;
	}

	private static boolean triggerIsUsed(String triggerName, Map<String, FilterInstance> allFilterInstances,
			Set<String> filterInstancesInContent, Map<String, Filter> allFilters, Set<String> filtersInContent,
			Map<String, TriggerInstance> allTriggerInstances, Set<String> triggerInstancesInContent) {
		for (String fi : filterInstancesInContent) {
			String filterName = allFilterInstances.get(fi).getFilterName();
			Filter f = allFilters.get(filterName);
			if (f != null && f.getTriggerName().equals(triggerName)) {
				return true;
			}
		}
		for (String f : filtersInContent) {
			Filter filter = allFilters.get(f);
			if (filter != null && filter.getTriggerName().equals(triggerName)) {
				return true;
			}
		}
		for (String ti : triggerInstancesInContent) {
			TriggerInstance triggerInstance = allTriggerInstances.get(ti);
			if (triggerInstance != null && triggerInstance.getTriggerName().equals(triggerName)) {
				return true;
			}
		}
		return false;
	}

	private static boolean filterUsedByFilterInstances(Set<String> filterInstancesInContent,
			Map<String, FilterInstance> allFilterInstances, String filterName) {
		for (String fi : filterInstancesInContent) {
			if (allFilterInstances.get(fi).getFilterName().equals(filterName)) {
				return true;
			}
		}
		return false;
	}

	private static void setOrderTypeConfig(ApplicationXmlCompatibilityLayer xml, Set<GenerationBase> explicitContent,
			Set<GenerationBase> implicitContent, Map<String, OrderType> orderTypeConfig,
			Set<String> orderTypesInContent, Set<String> explicitOrderTypes) {
		Set<String> defaultOrderTypes = new HashSet<>();
		defaultOrderTypes.addAll(Stream.concat(explicitContent.stream(), implicitContent.stream())
				.filter(gb -> gb instanceof WF).map(gb -> gb.getOriginalFqName()).collect(Collectors.toList()));

		/*
		 * add ordertype config to application.xml
		 * 
		 * skip ordertypes that are not used by explicit or implicit xmom entries
		 */
		Set<String> orderTypesToBeRemoved = new HashSet<>();
		for (String k : orderTypeConfig.keySet()) {
			if (!defaultOrderTypes.contains(k) && !orderTypesInContent.contains(k)) {
				orderTypesToBeRemoved.add(k);
			}
		}
		for (String k : orderTypesToBeRemoved) {
			orderTypeConfig.remove(k);
		}

		// add default ordertypes to configs to be included in application.xml
		for (String dot : defaultOrderTypes) {
			if (!orderTypeConfig.containsKey(dot)) {
				OrderType ot = new OrderType();
				ot.setName(dot);
				orderTypeConfig.put(dot, ot);
			}
		}
		for (OrderType ot : orderTypeConfig.values()) {
			boolean implicit = !explicitOrderTypes.contains(ot.getName());
			String planningDest = null;
			String executionDest = null;
			String cleanupDest = null;
			if (ot.getDispatcherDestinations() != null) {
				for (DispatcherDestination dd : ot.getDispatcherDestinations()) {
					if (dd.getDestinationType().equals(ExecutionType.XYNA_FRACTAL_WORKFLOW.getTypeAsString())) {
						if (dd.getDispatcherName().equals(OrderTypeProcessor.DISPATCHERNAME_PLANNING)) {
							planningDest = dd.getDestinationValue();
						} else if (dd.getDispatcherName().equals(OrderTypeProcessor.DISPATCHERNAME_EXECUTION)) {
							executionDest = dd.getDestinationValue();
						} else if (dd.getDispatcherName().equals(OrderTypeProcessor.DISPATCHERNAME_CLEANUP)) {
							cleanupDest = dd.getDestinationValue();
						}
					}
				}
			}
			boolean ordercontextMapping = false; // why is this not part of workspace-xml?
			xml.addOrderTypeDispatcherAndMiscConfig(ot.getName(), implicit, planningDest, executionDest, cleanupDest,
					ordercontextMapping);
			if (ot.getCapacities() != null) {
				for (Capacity cap : ot.getCapacities()) {
					xml.addCapacityRequirement(ot.getName(), cap.getCapacityName(), cap.getCardinality());
				}
			}
			if (ot.getMonitoringLevel() != null) {
				xml.addMonitoringLevelConfig(ot.getName(), ot.getMonitoringLevel());
			}
			if (ot.getPrioritySetting() != null) {
				xml.addOrderTypePriority(ot.getName(), ot.getPrioritySetting().getPriority());
			}
			if (ot.getInheritanceRules() != null) {
				for (InheritanceRule rule : ot.getInheritanceRules()) {
					int precedence = Integer.valueOf(rule.getPrecedence());
					xml.addInheritanceRule(ot.getName(), ParameterType.valueOf(rule.getParameterType()),
							rule.getChildFilter(), rule.getValue(), precedence);
				}
			}
		}
	}

	private static XMOMContext createContext(WorkspaceContent content, String version, File xmomDir,
			String subtypeExclusion, String appName) throws Exception {
		Map<RuntimeContext, Set<RuntimeContext>> rtcDeps = new HashMap<>();
		Map<RuntimeContext, File> dirs = new HashMap<>();
		Application app = new Application(appName, version);
		dirs.put(app, xmomDir);
		rtcDeps.put(app, new HashSet<>());

		FileSystemXMLSource source = new FileSystemXMLSource(rtcDeps, dirs, null) {

			@Override
			public Document getOrParseXML(GenerationBase generator, boolean fileFromDeploymentLocation)
					throws Ex_FileAccessException, XPRC_XmlParsingException {
				try {
					return super.getOrParseXML(generator, fileFromDeploymentLocation);
				} catch (Ex_FileAccessException e) {
					generator.setDoesntExist();
					return null;
				}
			}

		};
		return new XMOMContext(new SubtypeExclusion(subtypeExclusion), xmomDir, source, new GenerationBaseCache(),
				source.getRevision(app));
	}

	private static String extractAppName(WorkspaceContent content) {
		for (WorkspaceContentItem item : content.getWorkspaceContentItems()) {
			if (item instanceof ApplicationDefinition) {
				ApplicationDefinition ad = (ApplicationDefinition) item;
				return ad.getName();
			}
		}
		throw new RuntimeException();
	}

	private static class SubtypeExclusion {

		public final boolean excludeAll;
		public final Set<String> exclude;

		private SubtypeExclusion(String conf) {
			excludeAll = conf.equals("*");
			if (!excludeAll) {
				exclude = new HashSet<>(Arrays.asList(conf.trim().split("\\s*,\\s*")));
			} else {
				exclude = null;
			}
		}
	}

	private static void calculateImplicitXMOMEntries(Set<GenerationBase> explicitContent,
			Set<GenerationBase> implicitContent, XMOMContext ctx) throws Exception {
		Set<String> subtypeCheckDone = new HashSet<>();
		System.out.println("Checking explicit entries for dependencies ...");
		for (GenerationBase gb : explicitContent) {
			if (!gb.parsingFinished() && !gb.hasError()) {
				gb.parseGeneration(true, false, false);
			}
			addSubtypesOfOutputsOfOperations(gb, implicitContent, subtypeCheckDone, ctx);
			implicitContent.addAll(gb.getDependenciesRecursively().getDependencies(false).stream()
					.filter(a -> a.exists()).collect(Collectors.toList()));
		}
		int lastCount = -1;
		int newCount = implicitContent.size();
		System.out.println("Checking dependencies of implicit entries ...");
		while (lastCount != newCount) {
			lastCount = newCount;
			Set<GenerationBase> toAdd = new HashSet<>();
			for (GenerationBase gb : implicitContent) {
				if (!gb.parsingFinished() && !gb.hasError()) {
					gb.parseGeneration(true, false, false);
				}
				addSubtypesOfOutputsOfOperations(gb, toAdd, subtypeCheckDone, ctx);
				toAdd.addAll(gb.getDependenciesRecursively().getDependencies(false).stream().filter(a -> a.exists())
						.collect(Collectors.toList()));

			}
			implicitContent.addAll(toAdd);
			newCount = implicitContent.size();
		}
		implicitContent.removeAll(explicitContent);
	}

	private static void addSubtypesOfOutputsOfOperations(GenerationBase gb, Set<GenerationBase> implicitContent,
			Set<String> subtypeCheckDone, XMOMContext ctx) {
		if (ctx.subtypeExclusion.excludeAll) {
			return;
		}
		if (!(gb instanceof DOM)) {
			return;
		}
		if (subtypeCheckDone.contains(gb.getOriginalFqName())) {
			return;
		}
		subtypeCheckDone.add(gb.getOriginalFqName());
		/*
		 * for all outputs of coded services check whether the outputs have subtypes
		 * that have to be included. also look at member vars of them
		 * 
		 * we cannot use the default "getsubtypes" method of DOM, because it relies on a
		 * running factory
		 */
		DOM dom = (DOM) gb;
		List<Operation> operations = dom.getOperations();
		for (Operation op : operations) {
			List<AVariable> outputVars = op.getOutputVars();
			for (AVariable outputVar : outputVars) {
				if (outputVar.getDomOrExceptionObject() != null) {
					implicitContent
							.addAll(ctx.subtypeCache.getSubTypesRecursively(ctx, outputVar.getDomOrExceptionObject()));
				}
			}
		}
	}

	private static void validateWorkspaceXMLDir(File workspacexmldir) {
		if (!workspacexmldir.exists() || !workspacexmldir.isDirectory()) {
			System.err.println(
					"Workspace-XML directory does not exist or is not a directory: " + workspacexmldir.getPath());
			System.exit(1);
		}
		if (!new File(workspacexmldir, "workspace.xml").exists()) {
			System.err.println("Did not find workspace.xml inside of workspace-XML directory.");
			System.exit(2);
		}
	}

}
