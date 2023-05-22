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
package com.gip.xyna.xprc.xsched.vetos;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.Documentation.DocumentedEnum;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xprc.xsched.vetos.cache.VCP_Abstract;
import com.gip.xyna.xprc.xsched.vetos.cache.VCP_Clustered;
import com.gip.xyna.xprc.xsched.vetos.cache.VCP_Local;
import com.gip.xyna.xprc.xsched.vetos.cache.VCP_Unsupported;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCachePersistence;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCachePersistenceImpl;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCacheProcessor;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VCP_RemoteImpl;

public enum VetoManagementAlgorithmType implements DocumentedEnum {

  Storable( Documentation.
      en("Usage of veto storable only, legacy implementation").
      de("Alleinige Speicherung als Veto-Storable, herk�mmliche Implementierung").
      build()
      ) {
    @Override
    public VetoManagementInterface instantiate(ClusterMode mode) throws XynaException {
      VetoStorableAccess vetoStorableAccess = new VetoStorableAccess();
      vetoStorableAccess.init();
      return vetoStorableAccess;
    }
  },
  
  Cache( Documentation.
      en("Usage of veto cache only, not persistent!").
      de("Alleinige Speicherung im Cache, nicht persistent").
      build()
      ) {
    @Override
    public VetoManagementInterface instantiate(ClusterMode mode) {
      return new VM_Cache(0); //TODO woher binding?
    }
  },
  
  CachedStorable( Documentation.
      en("Usage of veto cache and storable").
      de("Speicherung im Cache und als Veto-Storable").
      build()
      )  {
    @Override
    public VetoManagementInterface instantiate(ClusterMode mode) throws XynaException {
      VetoStorableAccess vetoStorableAccess = new VetoStorableAccess();
      vetoStorableAccess.init();
      VM_CachedStorable vma = new VM_CachedStorable( vetoStorableAccess, new VM_Cache(vetoStorableAccess.getOwnBinding()) );
      vma.init();
      return vma;
    }
  },
  SeparateThread( Documentation.
      en("Separation of costly veto treatment from scheduler thread").
      de("Abtrennung der teuren Veto-Bearbeitung vom Scheduler-Thread").
      build()
      ) {
    @Override
    public boolean isClusterable() {
      return true;
    }
    @Override
    public VetoManagementInterface instantiate(ClusterMode mode) throws XynaException {
      VetoCache vc = new VetoCache();
      XynaFactory.getInstance().getProcessing().getXynaScheduler().getSchedulerCustomisation().getSchedulerCustomisationVetos().setVetoCache( vc );
      
      VetoCachePersistence persistence = new VetoCachePersistenceImpl(vc);
      persistence.init();
      vc.init(persistence.getOwnBinding());
      
      VetoCacheProcessor vcp = null;
      if( mode == ClusterMode.Local ) {
        vcp = new VCP_Local(vc, persistence);
        persistence.initVetoCache(-1);
        persistence.cleanupVetoCache();
      } else {
        vcp = new VCP_Unsupported(vc, persistence);
      }
      
      vc.setVetoCacheProcessor(vcp, true);
      return new VM_SeparateThread(vc);
    }
    
    public void switchClusteredLocal(VetoManagementInterface vmAlgorithm, RMIClusterProvider clusterProvider, ClusterMode mode) {
      VM_SeparateThread vmst = (VM_SeparateThread)vmAlgorithm;
      VetoCache vc = vmst.getVetoCache();
      VetoCacheProcessor vcpOld = vc.getVetoCacheProcessor();
      ClusterMode oldMode = getMode(vcpOld);
      if( oldMode == mode ) {
        return; //Mode ist bereits gesetzt
      }
      logger.info("switch veto algorithm from "+oldMode+" to " +mode);
      
      VetoCachePersistence persistence = ((VCP_Abstract)vcpOld).getPersistence();
      VetoCacheProcessor vcp = null;
      
      switch( mode ) {
        case Clustered:
          VCP_Clustered vcpc = new VCP_Clustered(vc, persistence, persistence.getOwnBinding() );
          vcpc.setClusterProvider( clusterProvider );
          vcpc.setRemoteImpl(new VCP_RemoteImpl(clusterProvider, vcpc.getLocal()) );
          vcp = vcpc;
          break;
        case Local:
          vcp = new VCP_Local(vc, persistence);
          break;
        case Unsupported:
          vcp = new VCP_Unsupported(vc, persistence);
          break;
      }
      
      if( oldMode == ClusterMode.Unsupported ) {
        switch(mode) {
        case Clustered:
          persistence.initVetoCache(persistence.getOwnBinding());
          break;
        case Local:
          persistence.initVetoCache(-1);
          break;
        case Unsupported:
          break;
        }
      }
      
      if( oldMode == ClusterMode.Clustered && mode == ClusterMode.Local ) {
        persistence.cleanupVetoCache();
      }
      
      vc.setVetoCacheProcessor(vcp, true);
      vc.notifyProcessor();
      
      switch( mode ) {
      case Clustered:
        break;
      case Local:
        vc.reprocessAll(); //alle Vetos im Cache nochmal bearbeiten, um Cluster-Relikte zu beseitigen
        break;
      case Unsupported:
        break;
      }
      
      if( oldMode == ClusterMode.Clustered ) {
        VCP_Clustered vcpc = (VCP_Clustered)vcpOld;
        vcpc.close(); //RMI
        vcpc.setReplicated(-1, -1); //evtl verbliebenes Latch l�sen
      }
    }
    
    private ClusterMode getMode(VetoCacheProcessor vcpOld) {
      if( vcpOld instanceof VCP_Local ) {
        return ClusterMode.Local;
      }
      if( vcpOld instanceof VCP_Unsupported ) {
        return ClusterMode.Unsupported;
      }
      if( vcpOld instanceof VCP_Clustered ) {
        return ClusterMode.Clustered;
      }
      return ClusterMode.Unsupported;
    }
    
  }
  
  
  ;
  
  
  private static final Logger logger = CentralFactoryLogging.getLogger(VetoManagementAlgorithmType.class);

  private Documentation doc;

  private VetoManagementAlgorithmType( Documentation doc) {
    this.doc = doc;
  }
  
  public Documentation getDocumentation() {
    return doc;
  }
  
  public abstract VetoManagementInterface instantiate(ClusterMode mode) throws XynaException;
  
  public boolean isClusterable() {
    return false;
  }
  
  public void switchClusteredLocal(VetoManagementInterface vmAlgorithm, RMIClusterProvider clusterProvider, ClusterMode mode) {
  }
  
  
  public static String documentation(DocumentationLanguage lang) {
    StringBuilder sb = new StringBuilder();
    String sep = "";
    for( VetoManagementAlgorithmType vmat : values() ) {
      sb.append(sep).append("\"").append(vmat).append("\": ").
      append( vmat.getDocumentation().get(lang) );
      sep = "; ";
    }
    return sb.toString();
  }
  
  public enum ClusterMode {
    Local, Clustered, Unsupported;
  }
  
}
