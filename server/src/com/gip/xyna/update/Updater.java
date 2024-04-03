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

package com.gip.xyna.update;

import java.io.File;
import java.io.FileFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.update.Update.ExecutionTime;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelStorage;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_GENERAL_UPDATE_ERROR;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_VALIDATION_ERROR;
import com.gip.xyna.xprc.xfractwfe.SerialVersionIgnoringObjectInputStream;
import com.gip.xyna.xprc.xfractwfe.XynaFractalWorkflowEngine;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderCount;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;

//updatekonzept vergleiche auch entwicklerdoku
public class Updater implements UpdaterInterface {

  private static Logger logger = CentralFactoryLogging.getLogger(Updater.class);
  protected static final String START_VERSION = "3.0.0.0";
  protected static final String START_MDM_VERSION = "1.0";
  
  public static Version VERSION_APPLICATION_CREATIONDATE;
  
  private ArrayList<Update> updates = new ArrayList<Update>();
  private ArrayList<MDMUpdate> mdmupdates = new ArrayList<MDMUpdate>();
  
  private Version updateStartVersion; //FactoryVersion zu Beginn des Updates

  private volatile boolean doMDMUpdates = true;
  private volatile boolean doGeneralUpdates = true;

  private static volatile UpdaterInterface instance;

  private Updater() {
  }

  private void init() {
    try {
      Update ud;
      
      updateStartVersion = getVersionOfLastSuccessfulUpdate();
      doMDMUpdates = true;
      doGeneralUpdates = true;
      mdmupdates.add(new UpdateMDM1_1());
      mdmupdates.add(new UpdateMDM1_2());
      mdmupdates.add(new UpdateMDMThrows1_3());
      mdmupdates.add(new UpdateMDMExceptionElements1_4());
      mdmupdates.add(new UpdateMDMDependencies1_5());
      mdmupdates.add(new UpdateCatchAIds1_6());
      mdmupdates.add(new UpdateRefactorLibraryTags1_7());
      mdmupdates.add(new UpdateMDMClone1_8());
      // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
      //ACHTUNG!!!! BEI MDM UPDATES BITTE DARAN DENKEN, DASS DAS XML EVTL AUCH IN DER KLASSE CallServiceHelper ANGEPASST WERDEN MUSS !!!!!!!!!!!!!!!!!!!
      // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    
      Version v0 = new Version(START_VERSION);
      ud = new UpdateInitialize(v0, v0);
      updates.add(ud);
      
      //5.0.0.0
      Version v163 = new Version("5.0.0.0");
      ud = new UpdateJustVersion(v0, v163, false);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //4.2.8.x darf hier drauf updaten
      updates.add(ud);

      //5.0.0.1
      Version v164 = new Version(v163).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v163, v164, false);
      updates.add(ud);
      
      //5.0.0.2 (5.0.0.1 lieferung erstellt)
      Version v165 = new Version(v164).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v164, v165, false);
      updates.add(ud);
      
      //5.0.0.3
      Version v166 = new Version(v165).increaseToMajorVersion(4, 1);
      ud = new UpdateSynchronizationEntries(v165, v166, false);
      updates.add(ud);
      
      //5.0.0.4
      Version v167 = new Version(v166).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v166, v167, false, true, true, false);
      updates.add(ud);
      
      //5.0.1.0 (5.0.0.4 branch erstellt)
      Version v168 = new Version(v167).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v167, v168, false); 
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //5.0.0.x darf hier drauf updaten
      updates.add(ud);
      
      //5.0.1.1
      Version v169 = new Version(v168).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v168, v169, false);
      updates.add(ud);

      //5.0.1.2
      Version v170 = new Version(v169).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v169, v170, false);
      updates.add(ud);
      
      //5.0.1.3 regenerate 
      Version v171 = new Version(v170).increaseToMajorVersion(4, 1);
      ud = new UpdateDeployMDMs(v170, v171,
                                new String[] {},
                                new String[] {"xact.ssh.SSHConnection"},
                                new String[] {},
                                true, true,
                                false);
      updates.add(ud);

      //5.0.1.4 Generierte Workflows haben sich wegen getNeededInputVarsCount() geändert
      Version v172 = new Version(v171).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v171, v172, false, true, false, false );
      updates.add(ud);
      
      
      //5.1.0.0 
      Version v173 = new Version(v172).increaseToMajorVersion(2, 1);
      ud = new UpdateJustVersion(v172, v173, false);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //5.0.1.x darf hier drauf updaten
      updates.add(ud);
      
      // 5.1.1.0 deploy PersistenceServices for new extended Operations
      Version v174 = new Version(v173).increaseToMajorVersion(3, 1);
      ud = new UpdateDeployMDMs(v173, v174,
                                new String[] {},
                                new String[] {"xnwh.persistence.PersistenceServices"},
                                new String[] {},
                                true, false, false);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //5.1.0.x darf hier drauf updaten
      updates.add(ud);
      
      // 5.1.1.1 Bug 15861: generierte XynaObjekte haben nun Builder; generierte Workflows verwenden diese
      Version v175 = new Version(v174).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v174, v175, false, true, true, true );
      updates.add(ud);
      
      // 5.1.1.2 deploy PersistenceServices for passing correlatedOrders & ScopedRight checking
      Version v176 = new Version(v175).increaseToMajorVersion(4, 1);
      ud = new UpdateDeployMDMs(v175, v176,
                                new String[] {},
                                new String[] {"xnwh.persistence.PersistenceServices"},
                                new String[] {},
                                true, false, false); 
      updates.add(ud);
   
      // 5.1.1.3 deploy SNMPService, weil neue typen SNMPCounter64 und SNMPNull verwendet werden
      Version v177 = new Version(v176).increaseToMajorVersion(4, 1);
      ud = new UpdateDeployMDMs(v176, v177,
                                new String[] {},
                                new String[] {"xact.snmp.types.SNMPNull", "xact.snmp.types.SNMPCounter64", "xact.snmp.types.SNMPVariableTypes", "xact.snmp.commands.SNMPService"},
                                new String[] {},
                                true, false, false);
      updates.add(ud);
   
      // 5.1.1.4
      Version v178 = new Version(v177).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v177, v178, false);
      updates.add(ud);
      
      
      // 5.1.1.5
      Version v179 = new Version(v178).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v178, v179, false, false, true, false);
      updates.add(ud);
      
      
      // 5.1.1.6 Update Query-Mappings to appropriate escaping (BUG 16445)
      Version v180 = new Version(v179).increaseToMajorVersion(4, 1);
      ud = new UpdateQueryMappingEscaping(v179, v180, false);
      updates.add(ud);
      
      
      // 5.1.1.7
      Version v181 = new Version(v180).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v180, v181, false); 
      updates.add(ud);
      
      
      // 5.1.1.8 deploy PersistenceServices mit neuer update-methode
      Version v182 = new Version(v181).increaseToMajorVersion(4, 1);
      ud = new UpdateDeployMDMs(v181, v182,
                                new String[] {},
                                new String[] {"xnwh.persistence.UpdateParameter", "xnwh.persistence.PersistenceServices"},
                                new String[] {},
                                true, false, false);
      updates.add(ud);
      
      
      // 5.1.2.0
      Version v183 = new Version(v182).increaseToMajorVersion(3, 1);      
      ud = new UpdateJustVersion(v182, v183, false);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4);
      updates.add(ud);
      
      
      // 5.1.2.1 XMOM-Objekte für Applications in Workflow-Database Storables eintragen (Bug 15026)
      Version v184 = new Version(v183).increaseToMajorVersion(4, 1);
      ud = new UpdateWorkflowDatabaseForApplications(v183, v184, false); 
      updates.add(ud);
      
      
      // 5.1.2.2 bug16588 => generierter wf code ist nicht abwärtskompatibel mit laufenden aufträgen
      Version v185 = new Version(v184).increaseToMajorVersion(4, 1);
      ud = new UpdateDontAllowOrdersInOrderBackup(v184, v185, true); //true ist beabsichtigt
      updates.add(ud);
      
      
      // 5.1.3.0 branch 5.1.2.2 erstellt etc
      Version v186 = new Version(v185).increaseToMajorVersion(3, 1);      
      ud = new UpdateJustVersion(v185, v186, false); 
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4);
      updates.add(ud);
      
      
      // 5.1.3.1 deploy workflows die von gui benötigt werden
      Version v187 = new Version(v186).increaseToMajorVersion(4, 1);
      ud = new UpdateDeployMDMs(v186, v187,
                                new String[] {"xnwh.persistence.Query", 
                                              "xnwh.persistence.Delete", "xnwh.persistence.Store"},
                                new String[] {},
                                new String[] {},
                                true, false, false);
      ((UpdateDeployMDMs) ud).setDeployNew();
      updates.add(ud);
      
      // 5.1.3.2 deploy changed and new activation components
      Version v188 = new Version(v187).increaseToMajorVersion(4, 1);
      ud = new UpdateDeployMDMs(v187, v188,
                                new String[] {},
                                new String[] {"xact.templates.CommandLineInterface", "xact.ssh.SSHConnection", "xact.ssh.SSHShellConnection", // changed
                                              "xact.ssh.SSHShellResponse", "xact.ssh.SSHShellPromptExtractor"}, // new
                                new String[] {},
                                true, false, false);
      updates.add(ud);
      
      
      //  5.1.3.3 cleanup invalid xmomodsconfigs
      Version v189 = new Version(v188).increaseToMajorVersion(4, 1);
      ud = new CleanXMOMOdsConfig(v188, v189, false);
      updates.add(ud);
      
      
      // 5.1.4.0 Branch 5.1.3 erstellung + Bug 15109: generierte XynaObjekte haben neue clone-Methode; generierte Workflows verwenden diese
      Version v190 = new Version(v189).increaseToMajorVersion(3, 1);      
      ud = new UpdateJustVersion(v189, v190, false, true, true, true);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4);
      updates.add(ud);
      
      // 5.1.4.1: triggerconfiguration auf XML konfigurieren
      Version v191 = new Version(v190).increaseToMajorVersion(4, 1);
      ud = new UpdateConfigureTriggerConfigurationPersistenceLayerToXml(v190, v191, false);
      updates.add(ud);
      
      // 5.1.4.2
      Version v192 = new Version(v191).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v191, v192, false);
      updates.add(ud);

      // 5.1.4.3: Umzug des saved-Ordners nach revisions/rev_workingset
      Version v193 = new Version(v192).increaseToMajorVersion(4, 1);
      ud = new UpdateMoveSavedFolder(v192, v193, false);
      updates.add(ud);

      // 5.1.4.4: in Storables applicationName/versionName durch revision ersetzen
      Version v194 = new Version(v193).increaseToMajorVersion(4, 1);
      ud = new UpdateStorablesToRevisions(v193, v194, false);
      updates.add(ud);

      // 5.1.4.5: SharedLibs vom deployed- ins saved-Verzeichnis kopieren (falls nicht bereits vorhanden)
      Version v195 = new Version(v194).increaseToMajorVersion(4, 1);
      ud = new UpdateCopySharedLibsToSaved(v194, v195, false);
      updates.add(ud);
      
      // 5.1.4.6: XMOMDatabase-Storables haben neue Spalte 'revision' und neuen primaryKey 'id'
      Version v196 = new Version(v195).increaseToMajorVersion(4, 1);
      ud = new UpdateXMOMDatabaseToRevision(v195, v196, false);
      updates.add(ud);

      // 5.1.4.7: Application- und ApplicationEntry-Storable haben neue Spalte 'parentRevision'
      Version v197 = new Version(v196).increaseToMajorVersion(4, 1);
      ud = new UpdateApplicationToRevisions(v196, v197, false);
      updates.add(ud);
      
      // 5.1.4.8: Applications in XMOM-Database aufnehmen
      Version v198 = new Version(v197).increaseToMajorVersion(4, 1);
      ud = new UpdateDiscoverXMOMDatabase(v197, v198, false, true);
      updates.add(ud);

      //5.1.4.9: DefaultEmptyMasterWorkflow deployen (Bug 17770)
      Version v199 = new Version(v198).increaseToMajorVersion(4, 1);
      ud = new UpdateDeployMDMs(v198, v199,
                                new String[] {"xprc.xbatchmgmt.DefaultEmptyMasterWorkflow"},
                                new String[] {},
                                new String[] {},
                                true, false, false);
      ((UpdateDeployMDMs) ud).setDeployNew();
      updates.add(ud);

      // 5.1.4.10 deploy changed and new activation components
      Version v200 = new Version(v199).increaseToMajorVersion(4, 1);
      ud = new UpdateDeployMDMs(v199, v200,
                                new String[] {},
                                new String[] {"xact.ssh.SSHConnectionParameter", "xact.ssh.SSHConnection", "xact.ssh.SSHShellConnection",
                                              "xact.ssh.SSHNETCONFConnection", "xact.ssh.SSHConnectionManagement", // changed
                                              "xact.ssh.ProxyParameter", "xact.ssh.SSHProxyParameter"}, // new
                                new String[] {},
                                true, false, false);
      updates.add(ud);
      
      // 5.1.4.11
      Version v201 = new Version(v200).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v200, v201, false); 
      updates.add(ud);
      
      // 5.1.4.12
      Version v202 = new Version(v201).increaseToMajorVersion(4, 1);
      ud = new UpdateDiscoverXMOMDatabase(v201, v202, false, true);
      updates.add(ud);
      
      // 5.1.4.13
      Version v203 = new Version(v202).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v202, v203, false); 
      updates.add(ud);
      
      // 5.1.5.0
      //update war bei 5.1.4.x vergessen gegangen, und gleichzeitig gab es hier die brancherstellung
      Version v204 = new Version(v203).increaseToMajorVersion(3, 1);
      ud = new UpdateOrderArchiveSetWorkspace(v203, v204, false);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4);
      updates.add(ud);
      
      // 5.1.5.1: Trigger- und FilterInstance-Storables haben neue Spalten 'state' und 'errorCause' statt 'enabled' und 'disabledautomatically'
      Version v205 = new Version(v204).increaseToMajorVersion(4, 1);
      ud = new UpdateTriggerAndFilterInstanceState(v204, v205, false);
      updates.add(ud);
            
      // 5.1.5.2: weil am 17.6. noch etwas in XMOMCommonValueParser gefixt wurde
      Version v206 = new Version(v205).increaseToMajorVersion(4, 1);
      ud = new UpdateDiscoverXMOMDatabase(v205, v206, false, true); 
      updates.add(ud);
      
      // 5.2.0.0
      Version v207 = new Version(v206).increaseToMajorVersion(2, 1);
      ud = new UpdateJustVersion(v206, v207, false);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //v5.1.5.x darf hier drauf updaten
      updates.add(ud);
      
      // 5.2.0.1: neue Spalte fqnamelowercase in XMOMDatabase füllen
      Version v208 = new Version(v207).increaseToMajorVersion(4, 1);
      ud = new UpdateDiscoverXMOMDatabase(v207, v208, false, true);
      updates.add(ud);
      
      // 5.2.0.2
      Version v209 = new Version(v208).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v208, v209, false); 
      updates.add(ud);      
      
      // 5.2.0.3
      Version v210 = new Version(v209).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v209, v210, false); 
      updates.add(ud);
      
      // 5.2.0.4
      Version v211 = new Version(v210).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v210, v211, false); 
      updates.add(ud);
      
      // 5.2.0.5
      Version v212 = new Version(v211).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v211, v212, false); 
      updates.add(ud);
      
      // 5.2.0.6
      Version v213 = new Version(v212).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v212, v213, false); 
      updates.add(ud);
      
      // 5.2.0.7
      Version v214 = new Version(v213).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v213, v214, false); 
      updates.add(ud);
      
      // 5.2.0.8
      Version v215 = new Version(v214).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v214, v215, false); 
      updates.add(ud);
      
      // 5.2.0.9
      Version v216 = new Version(v215).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v215, v216, false);
      updates.add(ud);
      
      // 6.0.0.9
      Version v217 = new Version(6, 0, 0, 9);
      ud = new UpdateJustVersion(v216, v217, false);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(3);
      updates.add(ud);
      
      // 6.0.1.0 merge point for v6.0.0.x 
      Version v218 = new Version(v217).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v217, v218, false); 
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4);
      updates.add(ud);
      
      // 6.0.1.1
      Version v219 = new Version(v218).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v218, v219, false);
      updates.add(ud);    
      
      // 6.0.1.2
      Version v220 = new Version(v219).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v219, v220, false); 
      updates.add(ud);
      
      // 6.0.1.3 
      Version v221 = new Version(v220).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v220, v221, false);
      updates.add(ud);
      
      // 6.0.1.4
      Version v222 = new Version(v221).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v221, v222, false);
      updates.add(ud);

      // 6.0.1.5: Default-Workspace in FrequencyControlledTaskInformations eintragen
      Version v223 = new Version(v222).increaseToMajorVersion(4, 1);
      ud = new UpdateFrequencyControlledTaskInformationSetWorkspace(v222, v223, false);
      updates.add(ud);
      
      // 6.0.1.6 auslassen, weil es im 6.0.1.6 branch das gleiche update enthält)
      Version v_6016 = new Version(v223).increaseToMajorVersion(4, 1);
      
      // 6.0.1.7
      Version v224 = new Version(v223).increaseToMajorVersion(4, 2);
      ud = new UpdateJustVersion(v223, v224, false);
      // 6.0.1.6 auch erlauben
      ud.addAllowedVersionRangeForUpdate(v_6016, v_6016);
      updates.add(ud);
      
      // 6.0.1.9
      Version v225 = new Version(v224).increaseToMajorVersion(4, 2);
      ud = new UpdateDeployMDMs(v224, v225,
                                new String[] {},
                                new String[] {"xprc.synchronization.Synchronization"}, //anytype support
                                new String[] {},
                                true, false, true);
      updates.add(ud);

      // 6.1.0.0: 6.0.1.6 branch erstellt, der auch
      Version v226 = new Version(v225).increaseToMajorVersion(2, 1);
      ud = new UpdateJustVersion(v225, v226, false);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //alles was mit 6.0.1 anfängt, und >= 6.0.1.9 ist.
      updates.add(ud);

      // 6.1.1.0: 6.1.0 branch erstellt
      Version v227 = new Version(v226).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v226, v227, false);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //alles was mit 6.1.0 anfängt, und >= 6.1.0.0 ist.
      updates.add(ud);
      
      // 6.1.1.1
      Version v228 = new Version(v227).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v227, v228, false);
      updates.add(ud);
      
      // 6.1.1.2
      Version v229 = new Version(v228).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v228, v229, false);
      updates.add(ud);      
      
      // 6.1.1.3
      Version v230 = new Version(v229).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v229, v230, false);
      updates.add(ud);    
      
      // 6.1.1.4
      Version v231 = new Version(v230).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v230, v231, false);
      updates.add(ud);
      
      // 6.1.1.5
      Version v232 = new Version(v231).increaseToMajorVersion(4, 1);
      ud = new UpdateDeployMDMs(v231, v232,
                                new String[] {},
                                new String[] {"xact.ssh.SSHConnection", "xact.ssh.SSHShellConnection",
                                              "xact.ssh.SSHNETCONFConnection", "xact.telnet.TelnetConnection", // changed
                                              "xfmg.xfmon.protocolmsg.ProtocolMessageStore"}, // new
                                new String[] {},
                                true, false, false);
      updates.add(ud);
      
      // 6.1.2.0
      Version v233 = new Version(v232).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v232, v233, true); //true wegen bug 19364
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //alles was mit 6.1.1 anfängt, und größer gleich 6.1.1.5 ist
      updates.add(ud);
      
      // 6.1.2.1
      Version v234 = new Version(v233).increaseToMajorVersion(4, 1);
      ud = new UpdatePasswordChangeDate(v233, v234, false);
      updates.add(ud);
      
      // 6.1.2.2
      Version v235 = new Version(v234).increaseToMajorVersion(4, 1);
      ud = new UpdateIDGenerationStorableWithRealmColumn(v234, v235, false); 
      updates.add(ud);
      
      // 6.1.2.3
      //codegen Änderungen für objekt-versionierung, xml referenzen in toXml-darstellung, und deep-equals Möglichkeiten. xynaobjectlist ist nicht serialisierungskompatibel
      Version v236 = new Version(v235).increaseToMajorVersion(4, 1);
      ud = new UpdateOrderBackupNewXynaObjectList(v235, v236); 
      updates.add(ud);
      
      // 6.1.2.4 
      //wegen bug 19488 wird nun code anders generiert
      Version v237 = new Version(v236).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v236, v237, true);
      updates.add(ud);
      
      // 6.1.2.5
      Version v238 = new Version(v237).increaseToMajorVersion(4, 1);
      ud = new UpdateDeployMDMs(v237, v238,
                                new String[] {},
                                new String[] {"xprc.synchronization.Synchronization"}, //NPE gefixt
                                new String[] {},
                                true, false, false);
      updates.add(ud);
      
      // 6.1.2.6
      Version v239 = new Version(v238).increaseToMajorVersion(4, 1);
      ud = new UpdateDeployMDMs(v238, v239,
                                new String[] {},
                                new String[] {"xact.snmp.commands.SNMPService"}, //dieses update fehlte bisher. das sollte eigtl in 6.1.2.0. passieren. im service wurde die impl angepasst
                                new String[] {},
                                true, false, false);
      updates.add(ud);
      
      // 6.1.2.7
      //Änderung der Code-Generierung (Bug 19072)
      Version v240 = new Version(v239).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v239, v240, true);
      updates.add(ud);
      
      // 6.1.2.8
      //Änderung der Code-Generierung (Performance/Memory-Bug bei Listenwertigen Membervariablen)
      Version v241 = new Version(v240).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v240, v241, true);
      updates.add(ud);
      
      // 6.1.2.9
      Version v242 = new Version(v241).increaseToMajorVersion(4, 1);
      ud = new UpdatePoolDefinition(v241, v242, false);
      updates.add(ud);
      
      // 6.1.3.0: NPE verhindern, wenn query-schritt keine filtercondition hat -> fix in persistenceservice
      Version v243 = new Version(v242).increaseToMajorVersion(3, 1);
      ud = new UpdateDeployMDMs(v242, v243,
                                new String[] {},
                                new String[] {"xnwh.persistence.PersistenceServices"},
                                new String[] {},
                                true, false, false);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //alles was mit 6.1.2 anfängt, und größer gleich 6.1.2.9 ist
      updates.add(ud);
      
      // 6.1.3.1
      Version v244 = new Version(v243).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v243, v244, true); //wegen bug 19689 neu generieren
      updates.add(ud);
      
      // 6.1.3.2
      Version v245 = new Version(v244).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v244, v245, true); //wegen bug 19696 neu generieren
      updates.add(ud);

      // 6.1.3.3
      Version v246 = new Version(v245).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v245, v246, true); //wegen toXml-Anpassungen für Modularisierung neu generieren
      updates.add(ud);
      
      // 6.1.3.4 NPE-Fix in Impl
      Version v247 = new Version(v246).increaseToMajorVersion(4, 1);
      ud = new UpdateDeployMDMs(v246, v247,
                                new String[] {},
                                new String[] {"xact.snmp.commands.SNMPService"},
                                new String[] {},
                                true, false, false);
      updates.add(ud);
      
      // 7.0.0.0
      Version v248 = new Version(v247).increaseToMajorVersion(1, 1);
      ud = new UpdateJustVersion(v247, v248, true); //neu generieren: 20439
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //6.1.3.x darf hier drauf updaten
      updates.add(ud);

      
      // 7.0.0.1: wichtig vor 7.0.0.2, damit auch alle reservierten objekte gefunden werden können
      Version v249 = new Version(v248).increaseToMajorVersion(4, 1);
      ud = new UpdateDiscoverXMOMDatabase(v248, v249, false, true);
      updates.add(ud);
      
      // 7.0.0.2
      Version v250 = new Version(v249).increaseToMajorVersion(4, 1);
      ud = new UpdateApplicationsReservedServerObjects(v249, v250);
      updates.add(ud);
      
      // 7.0.0.3
      Version v251 = new Version(v250).increaseToMajorVersion(4, 1);
      ud = new UpdateCreateXMOMRepository(v250, v251);
      updates.add(ud);
      
      // 7.0.0.4
      Version v252 = new Version(v251).increaseToMajorVersion(4, 1);
      ud = new UpdateSetCreationDateInApplications(v251, v252);
      VERSION_APPLICATION_CREATIONDATE = v252;
      updates.add(ud);
      
      // 7.0.0.5
      Version v253 = new Version(v252).increaseToMajorVersion(4, 1);
      ud = new UpdateOldXSDDatamodels(v252, v253);
      updates.add(ud);
      
      // 7.0.0.6
      Version v254 = new Version(v253).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v253, v254, true); //neu generieren wegen capacitylimitation bug
      updates.add(ud);
      
      // 7.0.0.7
      Version v255 = new Version(v254).increaseToMajorVersion(4, 1);
      ud = new UpdateRights(v254, v255);
      updates.add(ud);

      // 7.0.0.8 neue hinzugekommene Rechte den bestehenden Rollen zuweisen
      Version v256 = new Version(v255).increaseToMajorVersion(4, 1);
      ud = new UpdateGrantNewRights(v255, v256);
      updates.add(ud);
      
      // 7.0.0.9
      Version v257 = new Version(v256).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v256, v257, false);
      updates.add(ud);
      
      // 7.0.0.10
      Version v258 = new Version(v257).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v257, v258, false);
      updates.add(ud);
      
      // 7.0.0.11
      Version v259 = new Version(v258).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v258, v259, false);
      updates.add(ud);  
      
      // 7.0.0.12
      Version v260 = new Version(v259).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v259, v260, false);
      updates.add(ud);   
      
      // 7.0.0.13
      Version v261 = new Version(v260).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v260, v261, false);
      updates.add(ud);  
      
      // 7.0.0.14      
      Version v262 = new Version(v261).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v261, v262, false);
      updates.add(ud);
      
      // 7.0.0.15  
      Version v263 = new Version(v262).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v262, v263, false);
      updates.add(ud);
      
      // 7.0.0.16  
      Version v264 = new Version(v263).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v263, v264, true); //regenerieren, wegen bug 20439 in stepchoice in verbindung mit retries
      updates.add(ud);
      
      // 7.0.0.17  
      Version v265 = new Version(v264).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v264, v265, false);
      updates.add(ud);
      
      // 7.0.0.18
      Version v266 = new Version(v265).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v265, v266, false);
      updates.add(ud);  
      
      // 7.0.0.19      
      Version v267 = new Version(v266).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v266, v267, false);
      updates.add(ud);    
      
      // 7.0.0.20 
      Version v268 = new Version(v267).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v267, v268, false);
      updates.add(ud);
      
      // 7.0.0.21
      Version v269 = new Version(v268).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v268, v269, false);
      updates.add(ud);    
      
      // 7.0.0.22
      Version v270 = new Version(v269).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v269, v270, false);
      updates.add(ud);  
      
      // 7.0.0.23
      Version v271 = new Version(v270).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v270, v271, false);
      updates.add(ud);   
      
      // 7.0.0.24
      Version v272 = new Version(v271).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v271, v272, false);
      updates.add(ud);
      
      // 7.0.0.25
      Version v273 = new Version(v272).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v272, v273, false);
      updates.add(ud);
      
      // 7.0.0.26
      Version v274 = new Version(v273).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v273, v274, false);
      updates.add(ud);
      
      // 7.0.0.27
      Version v275 = new Version(v274).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v274, v275, false);
      updates.add(ud);
      
      // 7.0.0.28
      Version v276 = new Version(v275).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v275, v276, false);
      updates.add(ud);
      
      // 7.0.0.29
      Version v277 = new Version(v276).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v276, v277, false);
      updates.add(ud);
      
      // 7.0.0.30
      Version v278 = new Version(v277).increaseToMajorVersion(4, 1);
      ud = new UpdateGrantListWorkspacesRight(v277, v278, false);
      updates.add(ud);  
      
      // 7.0.0.31
      Version v279 = new Version(v278).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v278, v279, false);
      updates.add(ud);
      
      // 7.0.1.0
      Version v280 = new Version(v279).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v279, v280, false);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //7.0.0.x darf hier drauf updaten
      updates.add(ud);
      
      // 7.0.1.1
      Version v281 = new Version(v280).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v280, v281, false);
      updates.add(ud);   
      
      // 7.0.1.2
      Version v282 = new Version(v281).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v281, v282, false);
      updates.add(ud);
      
      // 7.0.1.3
      Version v283 = new Version(v282).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v282, v283, false);
      updates.add(ud);
      
      // 7.0.1.4
      Version v284 = new Version(v283).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v283, v284, false);
      updates.add(ud);     
      
      // 7.0.2.0
      Version v285 = new Version(v284).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v284, v285, false);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //7.0.1.x darf hier drauf updaten
      updates.add(ud);
      
      // 7.0.2.1
      Version v286 = new Version(v285).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v285, v286, false);
      updates.add(ud);
      
      // 7.0.2.2
      Version v287 = new Version(v286).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v286, v287, false);
      updates.add(ud);
      
      // 7.0.2.3
      Version v288 = new Version(v287).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v287, v288, false);
      updates.add(ud);   
      
      // 7.0.2.4
      Version v289 = new Version(v288).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v288, v289, false);
      updates.add(ud);  
      
      // 7.0.2.5
      Version v290 = new Version(v289).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v289, v290, false);
      updates.add(ud);     
      
      // 7.0.2.6
      Version v291 = new Version(v290).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v290, v291, false);
      updates.add(ud); 
      
      // 7.0.2.7
      Version v292 = new Version(v291).increaseToMajorVersion(4, 1);
      ud = new UpdateRightDescriptionsToLocalizedVersion(v291, v292);
      updates.add(ud);
      
      // 7.0.2.8
      Version v293 = new Version(v292).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v292, v293, false);
      updates.add(ud);   
      
      // 7.0.2.9
      Version v294 = new Version(v293).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v293, v294, false);
      updates.add(ud);     
      
      // 7.0.2.10      
      Version v295 = new Version(v294).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v294, v295, false);
      updates.add(ud);   
      
      // 7.0.2.11 
      Version v296 = new Version(v295).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v295, v296, false);
      updates.add(ud);   
      
      // 7.0.2.12
      Version v297 = new Version(v296).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v296, v297, true); //aufrufe von remotecalls: neue signatur (bug 22026)
      updates.add(ud);
      
      // 7.0.2.13
      Version v298 = new Version(v297).increaseToMajorVersion(4, 1);
      ud = new UpdateAddRootRevisionForOrderBackups(v297, v298);
      updates.add(ud); 
      
      // 7.0.2.14
      Version v299 = new Version(v298).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v298, v299, false);
      updates.add(ud);   
      
      // 7.0.2.15
      Version v300 = new Version(v299).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v299, v300, false);
      updates.add(ud);  
      
      // 7.0.2.16
      Version v301 = new Version(v300).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v300, v301, false);
      updates.add(ud);    
      
      // 7.0.2.17
      Version v302 = new Version(v301).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v301, v302, false);
      updates.add(ud);   
      
      // 7.0.2.18
      Version v303 = new Version(v302).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v302, v303, false);
      updates.add(ud);   
      
      // 7.0.2.19
      Version v304 = new Version(v303).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v303, v304, false);
      updates.add(ud);
      
      // 7.0.2.20
      Version v305 = new Version(v304).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v304, v305, false);
      updates.add(ud);   
      
      // 7.0.2.21
      Version v306 = new Version(v305).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v305, v306, false);
      updates.add(ud);         
      
      // 7.0.2.22
      Version v307 = new Version(v306).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v306, v307, false);
      updates.add(ud);    
      
      // 7.0.2.23
      Version v308 = new Version(v307).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v307, v308, false);
      updates.add(ud);
      
      // 7.0.2.24
      Version v309 = new Version(v308).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v308, v309, false);
      updates.add(ud);   
      
      // 7.0.2.25
      Version v310 = new Version(v309).increaseToMajorVersion(4, 1);
      updates.add(new UpdateDiscoverXMOMDatabase(v309, v310, false, true));

      // 7.0.2.26
      Version v311 = new Version(v310).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v310, v311, false);
      updates.add(ud);   

      // 7.0.2.27
      Version v312 = new Version(v311).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v311, v312, false);
      updates.add(ud);   

      // 7.0.3.0
      Version v313 = new Version(v312).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v312, v313, false);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //7.0.2.x darf hier drauf updaten
      updates.add(ud);
      
      // 7.0.3.1
      Version v314 = new Version(v313).increaseToMajorVersion(4, 1);
      ud = new UpdateGrantFileAccessRights(v313, v314);
      updates.add(ud);   

      // 7.0.3.2
      Version v315 = new Version(v314).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v314, v315, false);
      updates.add(ud);   
      
      // 7.0.3.3
      Version v316 = new Version(v315).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v315, v316, false);
      updates.add(ud);   
      
      // 7.0.3.4
      Version v317 = new Version(v316).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v316, v317, false);
      updates.add(ud);   
      
      // 7.0.3.5
      Version v318 = new Version(v317).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v317, v318, false);
      updates.add(ud);    
      
      // 7.0.3.6
      Version v319 = new Version(v318).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v318, v319, false);
      updates.add(ud);   
      
      // 7.0.3.7
      Version v320 = new Version(v319).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v319, v320, false);
      updates.add(ud);
      
      // 7.0.3.8
      Version v321 = new Version(v320).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v320, v321, false);
      updates.add(ud);
      
      // 7.0.3.9
      Version v322 = new Version(v321).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v321, v322, false);
      updates.add(ud);  
      
      // 7.0.3.10
      Version v323 = new Version(v322).increaseToMajorVersion(4, 1);
      ud = new UpdateCleanupUnusedDeploymentItemStateStorables(v322, v323);
      updates.add(ud);
      
      // 7.0.3.11
      Version v324 = new Version(v323).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v323, v324, false);
      updates.add(ud);  
      
      // 7.0.3.12
      Version v325 = new Version(v324).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v324, v325, false);
      updates.add(ud);
      
      // 7.0.3.13
      Version v326 = new Version(v325).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v325, v326, false);
      updates.add(ud);
      
      // 7.0.3.14
      Version v327 = new Version(v326).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v326, v327, false);
      updates.add(ud);
      
      // 7.0.3.15
      Version v328 = new Version(v327).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v327, v328, false);
      updates.add(ud);   
      
      // 7.0.3.16
      Version v329 = new Version(v328).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v328, v329, false);
      updates.add(ud);
      
      // 7.0.3.17
      Version v330 = new Version(v329).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v329, v330, false);
      updates.add(ud);
      
      // 7.0.3.18
      Version v331 = new Version(v330).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v330, v331, false);
      updates.add(ud);   
      
      // 7.0.3.19
      Version v332 = new Version(v331).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v331, v332, false);
      updates.add(ud);  
      
      // 7.0.4.0
      Version v333 = new Version(v332).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v332, v333, false);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //7.0.3.x darf hier drauf updaten
      updates.add(ud);
      
      // 7.0.4.1
      Version v334 = new Version(v333).increaseToMajorVersion(4, 1);
      ud = new UpdateToStandAloneOrderInstance(v333, v334);
      updates.add(ud);
      
      // 7.0.4.2
      Version v335 = new Version(v334).increaseToMajorVersion(4, 1);
      ud = new Fix_UpdateOldXSDDatamodels(v334, v335);
      updates.add(ud);
      
      // 7.0.4.3
      Version v336 = new Version(v335).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v335, v336, false);
      updates.add(ud);  
      
      // 7.0.4.4
      Version v337 = new Version(v336).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v336, v337, false);
      updates.add(ud);
      
      // 7.0.4.5
      Version v338 = new Version(v337).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v337, v338, false);
      updates.add(ud);
      
      // 7.0.4.6
      Version v339 = new Version(v338).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v338, v339, false);
      updates.add(ud);   
      
      // 7.0.4.7
      Version v340 = new Version(v339).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v339, v340, false);
      updates.add(ud);   
      
      // 7.0.4.8
      Version v341 = new Version(v340).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v340, v341, false);
      updates.add(ud);     
      
      // 7.0.4.9
      Version v342 = new Version(v341).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v341, v342, false);
      updates.add(ud);
      
      // 7.0.4.10
      Version v343 = new Version(v342).increaseToMajorVersion(4, 1);
      ud = new UpdateSetInterlinkRMIProperties(v342, v343);
      updates.add(ud);
      
      // 7.0.4.11
      Version v344 = new Version(v343).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v343, v344, false);
      updates.add(ud);   
      
      // 7.0.4.12
      Version v345 = new Version(v344).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v344, v345, false);
      updates.add(ud);     
      
      // 7.0.4.13
      Version v346 = new Version(v345).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v345, v346, false);
      updates.add(ud);
      
      // 7.0.4.14
      Version v347 = new Version(v346).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v346, v347, false);
      updates.add(ud);   
      
      // 7.0.4.15
      Version v348 = new Version(v347).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v347, v348, false);
      updates.add(ud);
      
      // 7.0.4.16
      Version v349 = new Version(v348).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v348, v349, false);
      updates.add(ud);
      
      // 7.0.4.17
      Version v350 = new Version(v349).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v349, v350, false);
      updates.add(ud);
      
      // 7.0.4.18
      Version v351 = new Version(v350).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v350, v351, false);
      updates.add(ud);      
      
      // 7.0.5.0
      Version v352 = new Version(v351).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v351, v352, false);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //7.0.4.x darf hier drauf updaten
      updates.add(ud);
      
      // 7.0.5.1
      Version v353 = new Version(v352).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v352, v353, false);
      updates.add(ud);     
            
      // 7.0.5.2
      Version v354 = new Version(v353).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v353, v354, false);
      updates.add(ud);
      
      // 7.0.6.0: added case sensitive label
      Version v355 = new Version(v354).increaseToMajorVersion(3, 1);
      ud = new UpdateDiscoverXMOMDatabase(v354, v355, false, true);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //7.0.5.x darf hier drauf updaten
      updates.add(ud);
      
      // 8.0.0.0
      Version v356 = new Version(v355).increaseToMajorVersion(1, 1);
      ud = new UpdateJustVersion(v355, v356, false);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //7.0.6.x darf hier drauf updaten
      updates.add(ud);

      // 8.0.0.1
      Version v357 = new Version(v356).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v356, v357, false);
      updates.add(ud);  
     
      // 8.0.0.2
      Version v358 = new Version(v357).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v357, v358, false);
      updates.add(ud);     
      
      // 8.0.0.3
      Version v359 = new Version(v358).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v358, v359, false);
      updates.add(ud);     
      
      // 8.0.0.4
      Version v360 = new Version(v359).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v359, v360, false);
      updates.add(ud);     
      
      // 8.0.0.5
      Version v361 = new Version(v360).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v360, v361, false);
      updates.add(ud);      
      
      // 8.0.0.6
      Version v362 = new Version(v361).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v361, v362, false);
      updates.add(ud);     
      
      // 8.0.0.7
      Version v363 = new Version(v362).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v362, v363, false);
      updates.add(ud); 
      
      // 8.0.0.8
      Version v364 = new Version(v363).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v363, v364, false);
      updates.add(ud);
      
      // 8.0.0.9
      Version v365 = new Version(v364).increaseToMajorVersion(4, 1);
      ud = new UpdateRemoveRelics(v364, v365);
      updates.add(ud);
      
      // 8.0.0.10
      Version v366 = new Version(v365).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v365, v366);
      updates.add(ud);   
      
      // 8.0.0.11
      Version v367 = new Version(v366).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v366, v367);
      updates.add(ud); 
      
      // 8.0.0.12
      Version v368 = new Version(v367).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v367, v368);
      updates.add(ud);
      
      // 8.0.0.13
      Version v369 = new Version(v368).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v368, v369);
      updates.add(ud);
      
      // 8.0.0.14
      Version v370 = new Version(v369).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v369, v370);
      updates.add(ud);      
      
      // 8.0.0.15
      Version v371 = new Version(v370).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v370, v371);
      updates.add(ud);    
      
      // 8.0.0.16
      Version v372 = new Version(v371).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v371, v372);
      updates.add(ud);
      
      // 8.0.0.17
      Version v373 = new Version(v372).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v372, v373);
      updates.add(ud);
      
      // 8.0.0.18
      Version v374 = new Version(v373).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v373, v374);
      updates.add(ud);   
      
      // 8.0.0.19
      Version v375 = new Version(v374).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v374, v375);
      updates.add(ud);
      
      // 8.1.0.0 Storable Inheritance
      Version v376 = new Version(v375).increaseToMajorVersion(2, 1);
      ud = new UpdateJustVersion(v375, v376, false);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(3); //8.0.x.x darf hier drauf updaten
      updates.add(ud);
      
      // 8.1.0.1
      Version v377 = new Version(v376).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v376, v377);
      updates.add(ud);
      
      // 8.1.0.2
      Version v378 = new Version(v377).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v377, v378);
      updates.add(ud);
      
      // 8.1.0.3
      Version v379 = new Version(v378).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v378, v379);
      updates.add(ud);
      
      // 8.1.0.4
      Version v380 = new Version(v379).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v379, v380);
      updates.add(ud);
      
      // 8.1.0.5
      Version v381 = new Version(v380).increaseToMajorVersion(4, 1);
      ud = new UpdateTablesForStorableInheritance(v380, v381);
      updates.add(ud);
     
      // 8.2.0.0 Java11
      Version v382 = new Version(v381).increaseToMajorVersion(2, 1);
      ud = new UpdateJustVersion(v381, v382);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(3); //8.1.x.x darf hier drauf updaten
      updates.add(ud);
      
      // 8.2.0.1
      Version v383 = new Version(v382).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v382, v383);
      updates.add(ud);   
      
      // 8.2.0.2
      Version v384 = new Version(v383).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v383, v384);
      updates.add(ud);  
      
      // 8.2.0.3
      Version v385 = new Version(v384).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v384, v385);
      updates.add(ud);
      
      // 8.2.0.4
      Version v386 = new Version(v385).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v385, v386);
      updates.add(ud);
      
      // 8.2.0.5
      Version v387 = new Version(v386).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v386, v387, true); // for restriction generation
      updates.add(ud);
      
      // 8.2.0.6
      Version v388 = new Version(v387).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v387, v388);
      updates.add(ud);
      
      // 8.2.0.7
      Version v389 = new Version(v388).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v388, v389);
      updates.add(ud);
      
      // 8.2.0.8
      Version v390 = new Version(v389).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v389, v390);
      updates.add(ud);
      
      // 8.2.0.9
      Version v391 = new Version(v390).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v390, v391);
      updates.add(ud);
      
      // 8.2.0.10
      Version v392 = new Version(v391).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v391, v392);
      updates.add(ud);
      
      // 8.2.0.11
      Version v393 = new Version(v392).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v392, v393);
      updates.add(ud);
      
      // 8.2.0.12
      Version v394 = new Version(v393).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v393, v394);
      updates.add(ud);
      
      // 8.2.0.13
      Version v395 = new Version(v394).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v394, v395);
      updates.add(ud);
      
      // 8.2.0.14
      Version v396 = new Version(v395).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v395, v396);
      updates.add(ud);
      
      // 8.2.0.15
      Version v397 = new Version(v396).increaseToMajorVersion(4, 1);
      ud = new UpdateGrantZetaMonitorRights(v396, v397);
      updates.add(ud);
      
      // 8.2.0.16
      Version v398 = new Version(v397).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v397, v398);
      updates.add(ud); 
      
      // 8.2.0.17
      Version v399 = new Version(v398).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v398, v399);
      updates.add(ud);
      
      // 8.2.0.18
      Version v400 = new Version(v399).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v399, v400);
      updates.add(ud);
      
      // 8.2.0.19
      Version v401 = new Version(v400).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v400, v401);
      updates.add(ud);
      
      // 8.2.0.20
      Version v402 = new Version(v401).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v401, v402);
      updates.add(ud);
      
      // 8.2.1.0
      Version v403 = new Version(v402).increaseToMajorVersion(3, 1);
      ud = new UpdateXMOMODSMappings(v402, v403);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //8.2.0.x darf hier drauf updaten
      updates.add(ud);
      
      // 8.2.1.1
      Version v404 = new Version(v403).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v403, v404);
      updates.add(ud);
      
      // 8.2.1.2
      Version v405 = new Version(v404).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v404, v405);
      updates.add(ud);     
      
      // 8.2.1.3
      Version v406 = new Version(v405).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v405, v406);
      updates.add(ud);
      
      // 8.2.1.4
      Version v407 = new Version(v406).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v406, v407);
      updates.add(ud);
      
      // 8.2.1.5
      Version v408 = new Version(v407).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v407, v408);
      updates.add(ud);
      
      // 8.2.1.6
      Version v409 = new Version(v408).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v408, v409);
      updates.add(ud);
      
      // 8.2.1.7
      Version v410 = new Version(v409).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v409, v410);
      updates.add(ud);
      
      // 8.2.1.8
      Version v411 = new Version(v410).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v410, v411, true); // neu generieren wegen 26405
      updates.add(ud);
      
      // 8.2.1.9
      Version v412 = new Version(v411).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v411, v412);
      updates.add(ud);

      // 8.2.1.10
      Version v413 = new Version(v412).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v412, v413);
      updates.add(ud);

      // 8.2.1.11
      Version v414 = new Version(v413).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v413, v414);
      updates.add(ud);
      
      // 8.2.1.12
      Version v415 = new Version(v414).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v414, v415);
      updates.add(ud);
      
      // 8.2.1.13
      Version v416 = new Version(v415).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v415, v416);
      updates.add(ud);
      
      // 8.2.1.14
      Version v417 = new Version(v416).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v416, v417);
      updates.add(ud);
      
      // 8.2.1.15
      Version v418 = new Version(v417).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v417, v418);
      updates.add(ud);
      
      // 8.2.1.16
      Version v419 = new Version(v418).increaseToMajorVersion(4, 1);
      ud = new UpdateKeyStoreMgmtStorable(v418, v419);
      updates.add(ud);
      
      // 8.2.1.17
      Version v420 = new Version(v419).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v419, v420);
      updates.add(ud);
      
      // 8.2.1.18
      Version v421 = new Version(v420).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v420, v421);
      updates.add(ud);
      
      // 8.2.1.19
      Version v422 = new Version(v421).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v421, v422);
      updates.add(ud);
      
      // 8.2.1.20
      Version v423 = new Version(v422).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v422, v423);
      updates.add(ud);
      
      // 8.2.2.0
      Version v424 = new Version(v422).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v422, v424);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //8.2.1.x darf hier drauf updaten
      updates.add(ud);
      
      // 8.2.2.1
      Version v425 = new Version(v424).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v424, v425);
      updates.add(ud);
      
      // 8.2.2.2
      Version v426 = new Version(v425).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v425, v426);
      updates.add(ud);
      
      // 8.2.2.3
      Version v427 = new Version(v426).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v426, v427);
      updates.add(ud);
      
      // 8.2.2.4
      Version v428 = new Version(v427).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v427, v428);
      updates.add(ud);
      
      // 8.2.2.5
      Version v429 = new Version(v428).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v428, v429);
      updates.add(ud);

      // 8.2.2.6
      Version v430 = new Version(v429).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v429, v430);
      updates.add(ud);
      
      // 8.2.2.7
      Version v431 = new Version(v430).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v430, v431);
      updates.add(ud);
      
      // 8.2.2.8
      Version v432 = new Version(v431).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v431, v432);
      updates.add(ud);

      // 8.2.2.9
      Version v433 = new Version(v432).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v432, v433);
      updates.add(ud);
      
      // 8.2.2.10
      Version v434 = new Version(v433).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v433, v434);
      updates.add(ud);
      
      // 8.2.2.11
      Version v435 = new Version(v434).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v434, v435);
      updates.add(ud);
      
      // 8.2.2.12
      Version v436 = new Version(v435).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v435, v436);
      updates.add(ud);
      
      // 8.2.2.13
      Version v437 = new Version(v436).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v436, v437);
      updates.add(ud);
      
      // 8.2.2.14
      Version v438 = new Version(v437).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v437, v438);
      updates.add(ud);
      
      // 8.2.2.15
      Version v439 = new Version(v438).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v438, v439);
      updates.add(ud);
      
      // 8.2.2.16
      Version v440 = new Version(v439).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v439, v440);
      updates.add(ud);

      // 8.2.2.17
      Version v441 = new Version(v440).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v440, v441, true); // neu generieren wegen XBE-115
      updates.add(ud);
      
      // 8.2.2.18
      Version v442 = new Version(v441).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v441, v442, true); // neu generieren wegen XBE-118
      updates.add(ud);

      // 8.2.2.19
      Version v443 = new Version(v442).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v442, v443);
      updates.add(ud);
      
      // 8.2.2.20
      Version v444 = new Version(v443).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v443, v444);
      updates.add(ud);
      
      // 8.2.2.21
      Version v445 = new Version(v444).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v444, v445);
      updates.add(ud);
      
      // 8.2.2.22
      Version v446 = new Version(v445).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v445, v446);
      updates.add(ud);
      
      // 8.2.2.23
      Version v447 = new Version(v446).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v446, v447);
      updates.add(ud);
      
      // 8.2.2.24
      Version v448 = new Version(v447).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v447, v448);
      updates.add(ud);
      
      // 8.2.2.25
      Version v449 = new Version(v448).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v448, v449);
      updates.add(ud);
      
      // 8.2.3.0
      Version v450 = new Version(v449).increaseToMajorVersion(3, 1);
      ud = new UpdatePerformanceOptimization(v449, v450);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //8.2.2.x darf hier drauf updaten
      updates.add(ud);
      
      // 8.2.3.1
      Version v451 = new Version(v450).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v450, v451);
      updates.add(ud);
      
      // 8.2.3.2
      Version v452 = new Version(v451).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v451, v452);
      updates.add(ud);
      
      // 8.2.3.3
      Version v453 = new Version(v452).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v452, v453);
      updates.add(ud);
      
      // 8.2.3.4
      Version v454 = new Version(v453).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v453, v454);
      updates.add(ud);
      
      // 8.2.4.0
      Version v455 = new Version(v454).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v454, v455);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //8.2.3.x darf hier drauf updaten
      updates.add(ud);
      
      // 8.2.5.0
      Version v456 = new Version(v455).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v455, v456);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //8.2.4.x darf hier drauf updaten
      updates.add(ud);
      
      // 8.2.5.1
      Version v457 = new Version(v456).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v456, v457);
      updates.add(ud);
      
      // 8.2.5.2
      Version v458 = new Version(v457).increaseToMajorVersion(4, 1);
      ud = new UpdateJustVersion(v457, v458);
      updates.add(ud);
      
      // 8.2.6.0
      Version v459 = new Version(v458).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v458, v459);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //8.2.5.x darf hier drauf updaten
      updates.add(ud);

      // 8.2.7.0
      Version v460 = new Version(v459).increaseToMajorVersion(3, 1);
      ud = new UpdatePoolDefinitionPasswords(v459, v460);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //8.2.6.x darf hier drauf updaten
      updates.add(ud);
      
      // 8.2.8.0
      Version v461 = new Version(v460).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v460, v461, true); // neu generieren wegen XBE-232, XBE-294
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //8.2.7.x darf hier drauf updaten
      updates.add(ud);

      // 8.2.9.0
      Version v462 = new Version(v461).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v461, v462, true);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //8.2.8.x darf hier drauf updaten
      updates.add(ud);
      
      // 8.2.10.0
      Version v463 = new Version(v462).increaseToMajorVersion(3, 1);
      ud = new UpdateXMOMODSMappingsCorrectColumnParentTables(v462, v463);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //8.2.9.x darf hier drauf updaten
      updates.add(ud);

      // 8.2.11.0
      Version v464 = new Version(v463).increaseToMajorVersion(3, 1);
      ud = new UpdateMultiSessionRights(v463, v464);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //8.2.10.x darf hier drauf updaten
      updates.add(ud);
      
      // 9.0.0.0
      Version v465 = new Version(v464).increaseToMajorVersion(1, 1);
      ud = new UpdateJustVersion(v464, v465, true);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //8.2.11.x darf hier drauf updaten
      updates.add(ud);
      
      // 9.0.1.0
      Version v466 = new Version(v465).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v465, v466, true);
      updates.add(ud);

      // 9.0.2.0
      Version v467 = new Version(v466).increaseToMajorVersion(3, 1);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //9.0.1.x darf hier drauf updaten
      ud = new UpdateJustVersion(v466, v467, true);
      updates.add(ud);

      // 9.0.3.0
      Version v468 = new Version(v467).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v467, v468);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //9.0.2.x darf hier drauf updaten  
      updates.add(ud);

      // 9.0.4.0
      Version v469 = new Version(v468).increaseToMajorVersion(3, 1);
      ud = new UpdateJustVersion(v468, v469);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(4); //9.0.3.x darf hier drauf updaten  
      updates.add(ud);

      // 9.1.0.0
      Version v470 = new Version(v469).increaseToMajorVersion(2, 1);
      ud = new UpdateJustVersion(v469, v470);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(3); //9.0.x.x darf hier drauf updaten
      updates.add(ud);

      // 10.0.0.0
      Version v471 = new Version(v470).increaseToMajorVersion(1, 1);
      ud = new UpdateJustVersion(v470, v471);
      ud.addFollowingBranchVersionsAsAllowedForUpdate(2); //9.x.x.x darf hier drauf updaten
      updates.add(ud);

      //ACHTUNG: bei updates in einem branch muss gewährleistet werden, dass alle späteren versionen (trunk, spätere branches)
      //         auf dem branch updaten können. bei updates, die in späteren versionen dann sonderbehandlungen im update-
      //         prozess benötigen, müssen die versionen, auf denen die updates passieren sollen, oben genau angegeben werden.
      //         beispiel: in einem branch-update wird eine spalte zu einer tabelle hinzugefügt. dieses update passiert
      //                   auch im trunk. dann benötigen alle branches zwischen dem betroffenen branch und der trunk-version
      //                   ein neues update (und ein neues release), welches den update-fall mit und ohne der schon vorhandenen
      //                   neuen spalte berücksichtigt. alternativ kann das update selbst natürlich so schlau sein, dass es
      //                   bewerten kann, ob es erneut ausgeführt werden muss...
      
      
      // Regeln zum Hochzählen von Versionsstellen
      // erste stelle: von chef vorgegeben
      // zweite stelle: dicke features, mit produktmanagement absprechen
      // dritte stelle: kleinere features, dickere bugfixing-pakete
      // vierte stelle: bugfixing-releases / branch-updates
      
      // weil bei tags mindestens die 3te stelle hochgezählt wird, können branches kollisionslos die vierte stelle weiterzählen.

      // hier weitere updates registrieren. reihenfolge sollte so sein, dass nicht versucht werden muss, ein update
      // mehrfach anzuwenden
      // updates.add(...)
      // nach dem update wird die aktuelle mdm-version auf die ziel-version des letzten mdm-updates gesetzt.

    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.fatal("fatal error during update", t);
      throw new RuntimeException(t);
    }
  }
  
  private PreparedQuery<UpdateHistoryStorable> selectHistoryOfVersionSorted;
  private ODS odsRegisteredStorableLast;


  public synchronized long getUpdateTime(Version v, ExecutionTime time) throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    if (ods != odsRegisteredStorableLast) {
      ods.registerStorable(UpdateHistoryStorable.class);
      odsRegisteredStorableLast = ods;
    }
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      if (selectHistoryOfVersionSorted == null) {
        selectHistoryOfVersionSorted =
            con.prepareQuery(new Query<UpdateHistoryStorable>("select * from " + UpdateHistoryStorable.TABLE_NAME + " where "
                + UpdateHistoryStorable.COL_VERSION + " = ? and " + UpdateHistoryStorable.COL_SUCCESS + " = ? and "
                + UpdateHistoryStorable.COL_UPDATE_TYPE + " = ?", UpdateHistoryStorable.reader));
      }
      UpdateHistoryStorable s = con.queryOneRow(selectHistoryOfVersionSorted, new Parameter(v.getString(), true, time.name()));
      if (s == null) {
        throw new RuntimeException("Update time for version " + v.getString() + " not found.");
      }
      return s.getUpdateTime();
    } finally {
      con.closeConnection();
    }
  }

  public synchronized void refreshPLCache() {
    selectCountUpdateHistory = null;
    selectHistoryOfVersionSorted = null;
  }

  private PreparedQuery<OrderCount> selectCountUpdateHistory;


  public synchronized void storeUpdateTime(Version v, ExecutionTime time, Throwable t) throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    if (ods != odsRegisteredStorableLast) {
      ods.registerStorable(UpdateHistoryStorable.class);
      odsRegisteredStorableLast = ods;
    }
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      if (selectCountUpdateHistory == null) {
        selectCountUpdateHistory =
            con.prepareQuery(new Query<OrderCount>("select count(*) from " + UpdateHistoryStorable.TABLE_NAME, OrderCount.getCountReader()));
      }
      long id = con.queryOneRow(selectCountUpdateHistory, new Parameter()).getCount();
      UpdateHistoryStorable s =
          new UpdateHistoryStorable(id, v.getString(), System.currentTimeMillis(), t == null, getString(t), time.name());
      con.persistObject(s);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  private String getString(Throwable t) {
    if (t == null) {
      return null;
    }
    StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }


  public static UpdaterInterface getInstance() {
    if (instance == null) {
      synchronized (Updater.class) {
        if (instance == null) {
          instance = new Updater();
          ((Updater) instance).init();
        }
      }
    }
    return instance;
  }


  public static void setInstance(UpdaterInterface u) {
    instance = u;
  }


  public void checkUpdate() throws XPRC_GENERAL_UPDATE_ERROR, XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException {
    //updates im mdm wirken sich auf updates der generierten klassen aus, andersrum sollte es keine abhängigkeiten geben
    checkUpdateMdm();
    checkUpdateGeneral();
  }


  private void checkUpdateGeneral() throws XPRC_GENERAL_UPDATE_ERROR, XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException {
    // get old version
    Version currentVersion = getVersionOfLastSuccessfulUpdate();
    if (logger.isInfoEnabled()) {
      logger.info("found current server version = " + currentVersion.getString());
    }
    Version targetVersion = null;
    try {

      // if the property storage is available and the doMDMupdates property is set, use that one.
      Configuration configuration = Configuration.getConfigurationPreInit();
      String s = configuration.getProperty(XynaProperty.XYNA_PERFORM_GENERAL_UPDATES);
      if (s != null) {
        doGeneralUpdates = Boolean.valueOf(s);
      }

      if (doGeneralUpdates) {
        // server updates
        // TODO departments haben eigene versionierung? use-cases?
        ExecutionTime[] updateTimes = new ExecutionTime[] {ExecutionTime.initialUpdate, ExecutionTime.endOfUpdate};
        Version[] currentVersions = new Version[] {currentVersion, getVersion(ExecutionTime.endOfUpdate)};
        
        VersionStorable vs = getCurrentVersionStorable();        
        boolean mustUpdateGeneratedClasses = vs.isMustupdategeneratedclasses();
        boolean mustRewriteWorkflows = vs.isMustrewriteworkflows();
        boolean mustRewriteDatatypes = vs.isMustrewritedatatypes();
        boolean mustRewriteExceptions = vs.isMustrewriteexceptions();

        for (int i = 0; i < updateTimes.length; i++) {
          ExecutionTime executionTime = updateTimes[i];
          Version version = currentVersions[i];

          for (Update u : updates) {
            targetVersion = u.getVersionAfterUpdate();
            Version updatedVersion = tryUpdate(u, version, executionTime);
            if (!updatedVersion.equals(version)) {
              // set new version, weil update durchgeführt wurde
              setVersion(version, updatedVersion, executionTime);
              version = updatedVersion;

              if (u.mustUpdateGeneratedClasses()) {
                mustUpdateGeneratedClasses = true;
              }
              if (u.mustRewriteWorkflows()) {
                mustRewriteWorkflows = true;
              }
              if (u.mustRewriteDatatypes()) {
                mustRewriteDatatypes = true;
              }
              if (u.mustRewriteExceptions()) {
                mustRewriteExceptions = true;
              }
            }
          }
        }


        //FutureExecution für Updates, die erst später ausgeführt werden sollen, einstellen
        FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
        
        final boolean finalMustUpdateGeneratedClasses = mustUpdateGeneratedClasses;
        final boolean finalMustRewriteWorkflows = mustRewriteWorkflows;
        final boolean finalMustRewriteDatatypes = mustRewriteDatatypes;
        final boolean finalMustRewriteExceptions = mustRewriteExceptions;
        fExec.addTask(Updater.class, "UpdateGeneratedClasses")
             .after(XynaFractalWorkflowEngine.FUTUREEXECUTION_ID)
             .after(XynaProcessCtrlExecution.class)
             .after(DependencyRegister.ID_FUTURE_EXECUTION)
             .after(XynaProcessingODS.FUTUREEXECUTION_ID)
             .after(XynaClusteringServicesManagement.class)
             .after(RevisionManagement.class)
             .after(DataModelStorage.class)
             .after(RuntimeContextDependencyManagement.class)
             .before(WorkflowDatabase.FUTURE_EXECUTION_ID)
             .execAsync( new Runnable() { public void run() {
          //nun die einmal durchzuführenden xmom objekt bezogenen updates falls notwendig
          boolean localMustUpdateGeneratedClasses = finalMustUpdateGeneratedClasses;
          boolean localMustRewriteWorkflows = finalMustRewriteWorkflows;
          boolean localMustRewriteDatatypes = finalMustRewriteDatatypes;
          boolean localMustRewriteExceptions = finalMustRewriteExceptions;
          Version previousVersion = null;
          boolean success = false;
          try {
            GenerationBase.clearGlobalCache();
            try {
              if (localMustUpdateGeneratedClasses) {
                new UpdateGeneratedClasses().update();
                localMustUpdateGeneratedClasses = false;
              }
              try {
                Version currentVersion = getVersion(ExecutionTime.afterUpdateGeneratedClassesBeforeRewriteOrderBackup);
                previousVersion = currentVersion;
                for (Update u : updates) {
                  Version updatedVersion = u.update(currentVersion, ExecutionTime.afterUpdateGeneratedClassesBeforeRewriteOrderBackup);
                  if (!updatedVersion.equals(currentVersion)) {
                    // set new update version, weil update durchgeführt wurde
                    setVersion(ExecutionTime.afterUpdateGeneratedClassesBeforeRewriteOrderBackup, updatedVersion);
                    currentVersion = updatedVersion;
                    
                    if (u.mustRewriteWorkflows()) {
                      localMustRewriteWorkflows = true;
                    }
                    if (u.mustRewriteDatatypes()) {
                      localMustRewriteDatatypes = true;
                    }
                    if (u.mustRewriteExceptions()) {
                      localMustRewriteExceptions = true;
                    }
                  }
                }
                } catch (XynaException e) {
                  throw new RuntimeException(e);
                }
                success = true;
            } finally {
              if (!success) {
                //falls generation nicht stattgefunden hat, beim nächsten mal erneut versuchen
                saveMustUpdateGeneratedClasses(localMustUpdateGeneratedClasses);
              } else {
                saveMustUpdateGeneratedClasses(false);
              }
            }
          } catch (XynaException e) {
            throw new RuntimeException("Error during Regeneration- & Rewrite-Updates", e);
          } finally {
            GenerationBase.clearGlobalCache();
            //hier werden das erstemal nach dem regenerate auch die mdmclassen geclassloaded.
            if (localMustRewriteWorkflows || localMustRewriteDatatypes || localMustRewriteExceptions) {
              try {
                if (success) {
                  success = false;
                  try {
                    try {
                      new UpdateRewriteOrderBackupAndCronLikeOrders(localMustRewriteWorkflows, localMustRewriteDatatypes, localMustRewriteExceptions).update();
                    } catch (XynaException e) {
                      throw new RuntimeException("Error during Regeneration- & Rewrite-Updates", e);
                    }
                    success = true;
                  } finally {
                    if (!success && previousVersion != null) {
                      //rollback, damit die parametrisierung für das rewrite beim nächsten update erneut verwendet wird
                      try {
                        setVersion(ExecutionTime.afterUpdateGeneratedClassesBeforeRewriteOrderBackup, previousVersion);
                      } catch (XynaException e) {
                        throw new RuntimeException("Error during Regeneration- & Rewrite-Updates", e);
                      }
                    }
                  }
                }
              } finally {
                SerialVersionIgnoringObjectInputStream.clearClassDescriptorMappings();
                try {
                  if (!success) {
                    //falls rewrite nicht stattgefunden hat
                    saveMustRewriteOrderBackupAndCronLikeOrders(localMustRewriteWorkflows, localMustRewriteDatatypes, localMustRewriteExceptions);
                  } else {
                    saveMustRewriteOrderBackupAndCronLikeOrders(false, false, false);
                  }
                } catch (XynaException e) {
                  throw new RuntimeException("Error during Regeneration- & Rewrite-Updates", e);
                }
              }
            }
          }
        }});
        
        fExec.addTask(Updater.class, "Updater").
              after(XMOMDatabase.class). //damit bei UpdateDeployMDMs die DeploymentHandler vorhanden sind
              before(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION). // necessary?
              execAsync( new Runnable() { public void run() { 
                try {
                  Version currentVersion = getVersion(ExecutionTime.endOfFactoryStart);
                  for (Update u : updates) {
                    Version updatedVersion = u.update(currentVersion, ExecutionTime.endOfFactoryStart);
                    if (!updatedVersion.equals(currentVersion)) {
                      // set new update version, weil update durchgeführt wurde
                      setVersion(ExecutionTime.endOfFactoryStart, updatedVersion);
                      currentVersion = updatedVersion;
                    }
                  }
                } catch (XynaException e) {
                  throw new RuntimeException(e);
                }
              }});
      }

    }
    catch (XynaException e) {
      // TODO update-rollbacken? das muss das update entsprechend unterstützen. idee: updates definieren eine
      // entsprechende funktion und sorgen selbständig für sicherheitskopien etc
      throw new XPRC_GENERAL_UPDATE_ERROR(targetVersion != null ? targetVersion.getString() : "unknown", e);
    }

  }

  
  private Version tryUpdate(Update u, Version version, ExecutionTime executionTime) throws PersistenceLayerException, XynaException {
    try {
      Version updatedVersion = u.update(version, executionTime);
      if (!updatedVersion.equals(version)) {
        storeUpdateTime(updatedVersion, executionTime, null);
      }
      return updatedVersion;
    } catch (XynaException t) {
      storeUpdateTime(u.getVersionAfterUpdate(), executionTime, t);
      throw t;
    } catch (RuntimeException t) {
      storeUpdateTime(u.getVersionAfterUpdate(), executionTime, t);
      throw t;
    } catch (Error t) {
      storeUpdateTime(u.getVersionAfterUpdate(), executionTime, t);
      throw t;
    }
    
  }

  public void checkUpdateMdm() throws XPRC_GENERAL_UPDATE_ERROR, XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException {

    if (!doMDMUpdates) {
      return;
    }

    Version currentMDMVersion = getXMOMVersion();
    try {

      // if the property storage is available and the doMDMupdates property is set, use that one.
      Configuration configuration = Configuration.getConfigurationPreInit();
      String s = configuration.getProperty(XynaProperty.XYNA_PERFORM_MDM_UPDATES);
      if (s != null) {
        doMDMUpdates = Boolean.valueOf(s);
      }

      if (doMDMUpdates) {

        // mdm updates: mdm xmls durchlaufen, version auslesen und ggfs updaten
        // wird eine gui benutzt, die xmls in einem älteren format erstellt, gibt es beim xml parsen entsprechende
        // fehler. die xmls werden dort nicht "gefixt".

        logger.debug("Locating XMOM files potentially relevant for updates...");
        // aus Performance-Gründen werden erst alle XMLs einmal geparst, die potentiell relevant sind.
        Map<String, Document> mapFilenameToDocument = new HashMap<String, Document>();
        
        String savedXMOMPath = VersionDependentPath.getCurrent().getPath(PathType.XMOM, false); //saved-Ordner vom default workspace
        File savedXMOMDir = new File(savedXMOMPath);
        List<File> deployedXMOMDirs = new ArrayList<File>();
        
        File baseFolder = new File(Constants.BASEDIR + Constants.FILE_SEPARATOR + Constants.REVISION_PATH);
        if (baseFolder.exists() && baseFolder.isDirectory()) {
          File[] revisionFolders = baseFolder.listFiles(new FileFilter() {
            public boolean accept(File file) {
              return file.isDirectory() && file.getName().startsWith(Constants.PREFIX_REVISION);
            }
          });
          
          for (File rev : revisionFolders) {
            deployedXMOMDirs.add(new File(rev.getPath() + Constants.FILE_SEPARATOR + Constants.SUBDIR_XMOM));
            
            //für Workspaces müssen auch die xmls im saved-Ordner geupdated werden
            String savedPath = rev.getPath() + Constants.FILE_SEPARATOR + Constants.PREFIX_SAVED + Constants.FILE_SEPARATOR + Constants.SUBDIR_XMOM;
            if (!savedPath.equals(savedXMOMPath)) {
              //zu deployedXMOMDirs hinzufügen, da dann keine Exception geworfen wird, wenn das Verzeichnis nicht existiert
              deployedXMOMDirs.add(new File(savedPath));
            }
          }
        }

        MDMUpdate.addRelevantFileNamesAndDocuments(savedXMOMDir, deployedXMOMDirs, mapFilenameToDocument);
        
        //Updates durchführen
        updateMdm(mapFilenameToDocument);
        
        if (mdmupdates.size() > 0) {
          setXMOMVersion(currentMDMVersion, mdmupdates.get(mdmupdates.size() - 1).getVersionAfterUpdate());
        } else {
          setXMOMVersion(currentMDMVersion, currentMDMVersion);
        }

      }
    } catch (XynaException e) {
      if (e instanceof XPRC_GENERAL_UPDATE_ERROR) {
        throw (XPRC_GENERAL_UPDATE_ERROR)e;
      }
      throw new XPRC_GENERAL_UPDATE_ERROR("unknown", e);
    }
  }

  /**
   * Führt die MDMUpdates für eine Revision durch.
   * @param revision
   */
  public void updateMdm(long revision) throws XPRC_GENERAL_UPDATE_ERROR {
    Map<String, Document> mapFilenameToDocument = new HashMap<String, Document>();
    try {
      MDMUpdate.addRelevantFileNamesAndDocuments(revision, mapFilenameToDocument);
      updateMdm(mapFilenameToDocument);
    } catch (XynaException e) {
      if (e instanceof XPRC_GENERAL_UPDATE_ERROR) {
        throw (XPRC_GENERAL_UPDATE_ERROR)e;
      }
      throw new XPRC_GENERAL_UPDATE_ERROR("unknown", e);
    }
  }
  
  
  private void updateMdm(Map<String, Document> mapFilenameToDocument) throws XPRC_GENERAL_UPDATE_ERROR {
    Version targetVersion = null;
    if (logger.isDebugEnabled()) {
      int size = mapFilenameToDocument.size();
      logger.debug("Found " + size + " XMOM file" + (size != 1 ? "s" : "") + ", performing updates if necessary.");
    }

    // für alle geparsten files alle updates ausführen. die Document-Instanzen bleiben immer die gleichen
    // und werden u.U. mehrfach aktualisiert auf die Platte geschrieben. Hier könnte man noch den Update-Fall
    // dahingehend optimieren, dass aktualisierte DocumentS nur ein mal geschrieben werden.
    int updated = 0;
    
    try{
      for (MDMUpdate mu : mdmupdates) {
        targetVersion = mu.getVersionAfterUpdate();
        updated += mu.update(mapFilenameToDocument);
      }
      
      if (updated > 0) {
        logger.info("Updated " + updated + " XMOM file" + (updated != 1 ? "s" : ""));
      } else {
        logger.debug("No XMOM files needed to be updated.");
      }
    } catch (XynaException e) {
      // TODO update-rollbacken? das muss das update entsprechend unterstützen. idee: updates definieren eine
      // entsprechende funktion und sorgen selbständig für sicherheitskopien etc
      throw new XPRC_GENERAL_UPDATE_ERROR(targetVersion != null ? targetVersion.getString() : "unknown", e);
    }
  }


  /**
   * falls version nicht übereinstimmt, wird ein fehler geworfen
   */
  public void validateMDMVersion(String mdmversion) throws XPRC_VERSION_VALIDATION_ERROR,
      XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException {
    if (mdmversion == null || mdmversion.length() == 0) {
      mdmversion = START_MDM_VERSION;
    }
    Version currentMDMVersion = getXMOMVersion();
    if (currentMDMVersion.equals(new Version(mdmversion))) {
      return;
    }
    throw new XPRC_VERSION_VALIDATION_ERROR(mdmversion, currentMDMVersion.getString());
  }


  public void setDoMDMUpdates(boolean b) {
    doMDMUpdates = b;
  }


  public boolean getDoMDMUpdates() {
    return doMDMUpdates;
  }


  public void setDoGeneralUpdates(boolean b) {
    doGeneralUpdates = b;
  }


  public boolean getDogeneralUpdates() {
    return doGeneralUpdates;
  }

  public boolean isInitialInstallation() {
    return new Version(Updater.START_VERSION).equals(updateStartVersion);
  }

  // ----------------------- Persistence ---------------------------
  @Persistable(primaryKey = VersionStorable.ID, tableName = VersionStorable.TABLE_NAME)
  public static class VersionStorable extends Storable<VersionStorable> {

    private static final long serialVersionUID = 1L;
    public final static String ID = "id";
    public final static String TABLE_NAME = "version";
    
    public final static String COL_FACTORYVERSION = "factoryversion";
    public final static String COL_MDMVERSION = "mdmversion";
    public final static String COL_VERSION_END_OF_FACTORY_START = "updateversion"; //TODO name von spalte hat keine große aussagekraft!
    public final static String COL_VERSION_AFTER_UPDATES= "codegenversion";
    public final static String COL_VERSION_ORDER_BACKUP = "orderbackupversion";
    public final static String COL_VERSION_PRE_INIT = "preinitversion";
    public final static String COL_MUST_REWRITE_EXCEPTIONS = "mustrewriteexceptions";
    public final static String COL_MUST_REWRITE_DATATPYES = "mustrewritedatatypes";
    public final static String COL_MUST_REWRITE_WORKFLOWS = "mustrewriteworkflows";
    public final static String COL_MUST_UPDATE_GENERATED_CLASSES = "mustupdategeneratedclasses";
    
    @Column(name = ID)
    private String id;
    
    @Column(name = COL_FACTORYVERSION)
    private String factoryversion;
    
    @Column(name = COL_MDMVERSION)
    private String mdmversion;

    @Column(name = COL_VERSION_END_OF_FACTORY_START)
    private String updateversion;
    
    @Column(name = COL_VERSION_AFTER_UPDATES)
    private String codegenversion;
    
    @Column(name = COL_VERSION_ORDER_BACKUP)
    private String orderbackupversion;
    
    @Column(name = COL_VERSION_PRE_INIT)
    private String preinitversion;

    @Column(name = COL_MUST_REWRITE_WORKFLOWS)
    private boolean mustrewriteworkflows;
    
    @Column(name = COL_MUST_REWRITE_DATATPYES)
    private boolean mustrewritedatatypes;
    
    @Column(name = COL_MUST_REWRITE_EXCEPTIONS)
    private boolean mustrewriteexceptions;
    
    @Column(name = COL_MUST_UPDATE_GENERATED_CLASSES)
    private boolean mustupdategeneratedclasses;
    
    
    public VersionStorable() {
    }
   
    public VersionStorable(String id, String version, String mdmversion) {
      this(id, version, mdmversion, null, null, null, null);
    }
    
    public VersionStorable(String id, String version, String mdmversion, String updateversion, String codegenversion, String orderbackupversion, String preinitversion) {
      this.id = id;
      this.factoryversion = version;
      this.mdmversion = mdmversion;      
      this.updateversion = updateversion;
      this.codegenversion = codegenversion;
      this.orderbackupversion = orderbackupversion;
      this.preinitversion = preinitversion;
    }
    
    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getFactoryVersion() {
      return factoryversion;
    }

    public void setFactoryVersion(String version) {
      this.factoryversion = version;
    }

    public String getMdmversion() {
      return mdmversion;
    }

    public void setMdmversion(String mdmversion) {
      this.mdmversion = mdmversion;
    }


    @Override
    public ResultSetReader<? extends VersionStorable> getReader() {
      return new ResultSetReader<VersionStorable>() {

        public VersionStorable read(ResultSet rs) throws SQLException {
          VersionStorable v =
              new VersionStorable(rs.getString(ID), rs.getString(COL_FACTORYVERSION), rs.getString(COL_MDMVERSION),
                                  rs.getString(COL_VERSION_END_OF_FACTORY_START), rs.getString(COL_VERSION_AFTER_UPDATES),
                                  rs.getString(COL_VERSION_ORDER_BACKUP), rs.getString(COL_VERSION_PRE_INIT));
          v.mustrewritedatatypes = rs.getBoolean(COL_MUST_REWRITE_DATATPYES);
          v.mustrewriteexceptions = rs.getBoolean(COL_MUST_REWRITE_EXCEPTIONS);
          v.mustrewriteworkflows = rs.getBoolean(COL_MUST_REWRITE_WORKFLOWS);
          v.mustupdategeneratedclasses = rs.getBoolean(COL_MUST_UPDATE_GENERATED_CLASSES);
          return v;
        }
      };
    }
    
    @Override
    public Object getPrimaryKey() {
      return id;
    }
    @Override
    public <U extends VersionStorable> void setAllFieldsFromData(U data) {
      id = data.getId();
      factoryversion = data.getFactoryversion();
      mdmversion = data.getMdmversion();
      updateversion = data.getUpdateversion();
      codegenversion = data.getCodegenversion();
      orderbackupversion = data.getOrderbackupversion();
      preinitversion = data.getPreInitVersion();
      mustrewritedatatypes = data.isMustrewritedatatypes();
      mustrewriteexceptions = data.isMustrewriteexceptions();
      mustrewriteworkflows = data.isMustrewriteworkflows();
      mustupdategeneratedclasses = data.isMustupdategeneratedclasses();
    }

    
    public String getFactoryversion() {
      return factoryversion;
    }

    
    public void setFactoryversion(String factoryversion) {
      this.factoryversion = factoryversion;
    }

    
    public String getUpdateversion() {
      return updateversion;
    }

    
    public void setUpdateversion(String updateversion) {
      this.updateversion = updateversion;
    }

    
    public String getCodegenversion() {
      return codegenversion;
    }

    
    public void setCodegenversion(String codegenversion) {
      this.codegenversion = codegenversion;
    }

    public String getOrderbackupversion() {
      return orderbackupversion;
    }
    
    public void setOrderbackupversion(String orderbackupversion) {
      this.orderbackupversion = orderbackupversion;
    }
    
    public boolean isMustrewriteworkflows() {
      return mustrewriteworkflows;
    }

    
    public void setMustrewriteworkflows(boolean mustrewriteworkflows) {
      this.mustrewriteworkflows = mustrewriteworkflows;
    }

    
    public boolean isMustrewritedatatypes() {
      return mustrewritedatatypes;
    }

    
    public void setMustrewritedatatypes(boolean mustrewritedatatypes) {
      this.mustrewritedatatypes = mustrewritedatatypes;
    }

    
    public boolean isMustrewriteexceptions() {
      return mustrewriteexceptions;
    }

    
    public void setMustrewriteexceptions(boolean mustrewriteexceptions) {
      this.mustrewriteexceptions = mustrewriteexceptions;
    }

    
    public boolean isMustupdategeneratedclasses() {
      return mustupdategeneratedclasses;
    }

    
    public void setMustupdategeneratedclasses(boolean mustupdategeneratedclasses) {
      this.mustupdategeneratedclasses = mustupdategeneratedclasses;
    }

    public String getPreInitVersion() {
      return preinitversion;
    }

    public void setPreInitVersion(String preinitversion) {
      this.preinitversion = preinitversion;  
    }


  }

  protected interface ApplicationUpdate {
    public Version update(Version versionOfApplication, Long revision, ApplicationXmlEntry applicationXml) throws XynaException;
  }
  
  private final List<ApplicationUpdate> appUpdates = new ArrayList<ApplicationUpdate>();
  
  public void addApplicationUpdate(ApplicationUpdate au) {
    appUpdates.add(au);
  }

  public void updateApplicationAtImport(Long revision, ApplicationXmlEntry applicationXml) throws XynaException {
    Version appVersion;
    if (applicationXml.getFactoryVersion() == null || applicationXml.getFactoryVersion().length() == 0) {
      appVersion = new Version(START_VERSION);
    } else {
      appVersion = new Version(applicationXml.getFactoryVersion());;
    }
    for (ApplicationUpdate au : appUpdates) {
      appVersion = au.update(appVersion, revision, applicationXml);
    }
  }

  
  private Version getFactoryVersionNew() throws  PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(VersionStorable.class);
    ODSConnection con = ods.openConnection();
    try {
      VersionStorable v = new VersionStorable("1", null, null);
      try {
        con.queryOneRow(v);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // Daten existieren nicht --> update noch nicht durchgeführt und Version liegt noch in alter Persistenz vor
        return null;
      }
      if (v.getFactoryVersion() == null) {
        return null;
      }
      return new Version(v.getFactoryVersion());
    } finally {
      con.closeConnection();
    }
  }
  
  
  public Version getFactoryVersion() {
    try {
      return updates.get(updates.size()-1).getVersionAfterUpdate();
    } catch (XynaException e) {
      //macht derzeit keinen sinn, dass hier ein fehler passiert
      throw new RuntimeException("Could not get Version", e);
    }
  }
  
  public Version getVersionOfLastSuccessfulUpdate() throws XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException {
    Version version = getFactoryVersionNew();
    if (version == null) {
      Version v = new Version(START_VERSION);
      logger.warn("no version found. assuming version = " + v.getString());
      return v;
    } else {
      return version;
    }
  }
  
  private void setVersionNew(Version newVersion) throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(VersionStorable.class);
    ODSConnection con = ods.openConnection();
    try {
      VersionStorable v = new VersionStorable("1", null, null);
      try {
        con.queryOneRow(v);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        v = new VersionStorable("1", START_VERSION, START_MDM_VERSION, START_VERSION, START_VERSION, START_VERSION, START_VERSION);
      }
      v.setFactoryVersion(newVersion.getString());
      con.persistObject(v);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }  

  
  private void setVersion(Version oldVersion, Version newVersion, ExecutionTime executionTime) throws XynaException {
    switch (executionTime) {
      case initialUpdate :
        setVersionNew(newVersion);
        return;
      case endOfUpdate :
      case endOfFactoryStart : 
      case afterUpdateGeneratedClassesBeforeRewriteOrderBackup:
        setVersion(executionTime, newVersion);
        return;
      default :
        throw new RuntimeException("unsupported executionTime: " + executionTime);
    }
  }

  private Version getXMOMVersionNew() throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(VersionStorable.class);
    ODSConnection con = ods.openConnection();
    try {
      VersionStorable v = new VersionStorable("1", null, null);
      try {
        con.queryOneRow(v);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // Daten existieren nicht --> update noch nicht durchgeführt und Version liegt noch in alter Persistenz vor
        return null;
      }
      
      return new Version(v.getMdmversion());
    } finally {
      con.closeConnection();
    }
  }
  
  
  Version xmomVersion;
  
  public Version getXMOMVersion() throws XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException {
    if (xmomVersion == null) {
      xmomVersion = getXMOMVersionNew();
    }
    if (xmomVersion == null) {
      Version v = new Version(START_MDM_VERSION);
      logger.warn("no xmom version found. assuming version = " + v.getString());
      return v;
    }
    return xmomVersion;
  }

  private boolean setXMOMVersionNew(Version newVersion) throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(VersionStorable.class);
    ODSConnection con = ods.openConnection();
    try {
      VersionStorable v = new VersionStorable("1", null, null);
      try {
        con.queryOneRow(v);
        v.setMdmversion(newVersion.getString());
        con.persistObject(v);
        con.commit();
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // Daten existieren nicht --> update noch nicht durchgeführt und Version liegt noch in alter Persistenz vor
        return false;
      }
      return true;
    } finally {
      con.closeConnection();
    }
  }  
  

  private void setXMOMVersion(Version oldVersion, Version newVersion) throws XynaException {
    setXMOMVersionNew(newVersion);
    xmomVersion = newVersion;
  }

  
  public Version getVersion(ExecutionTime executionTime) throws XynaException {
    Version version = getVersionNew(executionTime);
    if (version == null) {
      Version v = new Version(START_VERSION);
      logger.warn("no xmom version found. assuming version = " + v.getString());
      return v;
    } else {
      return version;
    }
  }

  public Version getVersionNew(ExecutionTime executionTime) throws XynaException {
    ODS ods = ODSImpl.getInstance();
    registerStorable(executionTime);
    ODSConnection con = ods.openConnection();
    try {
      VersionStorable v = new VersionStorable("1", null, null);
      con.queryOneRow(v);

      switch (executionTime) {
        case initialUpdate :
          return new Version(v.getFactoryVersion());
        case endOfUpdate :
          if (v.getCodegenversion() == null) {
            return new Version(Updater.START_VERSION);
          }
          return new Version(v.getCodegenversion());
        case endOfFactoryStart :
          if (v.getUpdateversion() == null) {
            return new Version(Updater.START_VERSION);
          }
          return new Version(v.getUpdateversion());
        case afterUpdateGeneratedClassesBeforeRewriteOrderBackup :
          if (v.getOrderbackupversion() == null) {
            return new Version(Updater.START_VERSION);
          } 
          return new Version(v.getOrderbackupversion());
        default :
          throw new RuntimeException("unsupported executionTime " + executionTime);
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return null;
    } finally {
      con.closeConnection();
    }
  }
  
  
  private void registerStorable(ExecutionTime executionTime) throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(VersionStorable.class);
  }


  public void setVersion(ExecutionTime executionTime, Version newVersion) throws XynaException {
    ODS ods = ODSImpl.getInstance();
    registerStorable(executionTime);
    ODSConnection con = ods.openConnection();
    try {
      VersionStorable v = new VersionStorable("1", null, null, null, null, null, null);
      con.queryOneRow(v);

      switch (executionTime) {
        case initialUpdate :
          v.setFactoryVersion(newVersion.getString());
          break;
        case endOfUpdate :
          v.setCodegenversion(newVersion.getString());
          break;
        case endOfFactoryStart :
          v.setUpdateversion(newVersion.getString());
          break;
        case afterUpdateGeneratedClassesBeforeRewriteOrderBackup :
          v.setOrderbackupversion(newVersion.getString());
          break;
        default :
          throw new RuntimeException("unsupported executionTime " + executionTime);
      }
      con.persistObject(v);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  private void saveMustRewriteOrderBackupAndCronLikeOrders(boolean mustRewriteWorkflows, boolean mustRewriteDatatypes,
                                                           boolean mustRewriteExceptions) throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(VersionStorable.class);
    ODSConnection con = ods.openConnection();
    try {
      VersionStorable v = new VersionStorable("1", null, null, null, null, null, null);
      try {
        con.queryOneRow(v);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        v = new VersionStorable("1", START_VERSION, START_MDM_VERSION, START_VERSION, START_VERSION, START_VERSION, START_VERSION);
      }
      v.setMustrewriteworkflows(mustRewriteWorkflows);
      v.setMustrewritedatatypes(mustRewriteDatatypes);
      v.setMustrewriteexceptions(mustRewriteExceptions);
      con.persistObject(v);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  private void saveMustUpdateGeneratedClasses(boolean b) throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(VersionStorable.class);
    ODSConnection con = ods.openConnection();
    try {
      VersionStorable v = new VersionStorable("1", null, null, null, null, null, null);
      try {
        con.queryOneRow(v);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        v = new VersionStorable("1", START_VERSION, START_MDM_VERSION, START_VERSION, START_VERSION, START_VERSION, START_VERSION);
      }
      v.setMustupdategeneratedclasses(b);
      con.persistObject(v);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  private VersionStorable getCurrentVersionStorable() throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(VersionStorable.class);
    ODSConnection con = ods.openConnection();
    try {
      VersionStorable v = new VersionStorable("1", null, null);
      try {
        con.queryOneRow(v);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        //alte version !?
      }
      return v;
    } finally {
      con.closeConnection();
    }
  }
  

}
