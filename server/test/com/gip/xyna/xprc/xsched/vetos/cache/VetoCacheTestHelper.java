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
package com.gip.xyna.xprc.xsched.vetos.cache;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoAllocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoDeallocationDenied;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;
import com.gip.xyna.xprc.xsched.vetos.AdministrativeVeto;
import com.gip.xyna.xprc.xsched.vetos.VM_SeparateThread;
import com.gip.xyna.xprc.xsched.vetos.VetoAllocationResult;
import com.gip.xyna.xprc.xsched.vetos.VetoInformation;
import com.gip.xyna.xprc.xsched.vetos.VetoManagementAlgorithmType;
import com.gip.xyna.xprc.xsched.vetos.VetoManagementInterface;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache.State;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCacheProcessor.SchedulerNotification;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.ReplicateVetoRequest;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VCP_Remote;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VCP_RemoteInterface;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoRequest;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoResponse;

import junit.framework.TestCase;

public class VetoCacheTestHelper extends TestCase {
  
  
  public static class TestVM implements VetoManagementInterface {
    
    protected VetoCache vc;
    protected VetoCacheProcessor vcp;
    private VM_SeparateThread vm;
    private ExecutorService executorService;
    protected TestVetoCachePersistence persistence;
    
    public TestVM() throws XynaException {
      this(0, false);
    }
    
    public TestVM(boolean startVetoCacheProcessorThread) throws XynaException {
      this(0, startVetoCacheProcessorThread);
    }

    protected TestVM(int binding, boolean startVetoCacheProcessorThread ) throws XynaException {
      vc = new VetoCache();
      vc.init(binding);
      persistence = new TestVetoCachePersistence(binding);
      
      if( binding != 0 ) {
        setVetoCacheProcessor( new VCP_Unsupported(vc, persistence), startVetoCacheProcessorThread);
      } else {
        setVetoCacheProcessor( new VCP_Local(vc, persistence ), startVetoCacheProcessorThread);
      }
     
      vm = new VM_SeparateThread(vc);
    }

    protected void setVetoCacheProcessor(VetoCacheProcessor vcp, boolean startVetoCacheProcessorThread) {
      this.vcp =vcp;
      this.vcp.setSchedulerNotification(new SchedulerNotification() {public void notifyScheduler() {}});
      vc.setVetoCacheProcessor(vcp, startVetoCacheProcessorThread);
    }
    
    
    public VetoAllocationResult allocateVetos(OrderInformation orderInformation, List<String> vetoNames, long urgency) {
      return vm.allocateVetos(orderInformation, vetoNames, urgency);
    }

    public void undoAllocation(OrderInformation orderInformation, List<String> vetos) {
      vm.undoAllocation(orderInformation, vetos);
    }
    
    public void allocateAdministrativeVeto(AdministrativeVeto administrativeVeto)
        throws XPRC_AdministrativeVetoAllocationDenied, PersistenceLayerException {
      vm.allocateAdministrativeVeto(administrativeVeto);
    }

    public String setDocumentationOfAdministrativeVeto(AdministrativeVeto administrativeVeto)
        throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      return vm.setDocumentationOfAdministrativeVeto(administrativeVeto);
    }

    public VetoInformation freeAdministrativeVeto(AdministrativeVeto administrativeVeto)
        throws XPRC_AdministrativeVetoDeallocationDenied, PersistenceLayerException {
      return vm.freeAdministrativeVeto(administrativeVeto);
    }

    public Collection<VetoInformation> listVetos() {
      return vm.listVetos();
    }

    public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException {
      return vm.searchVetos(select, maxRows);
    }
    
    public void finalizeAllocation(OrderInformation orderInformation, List<String> vetos) {
      vm.finalizeAllocation(orderInformation, vetos);
    }

    public boolean freeVetos(OrderInformation orderInformation) {
      return vm.freeVetos(orderInformation);
    }
    
    public boolean freeVetosForced(long orderId) {
      return vm.freeVetosForced(orderId);
    }

    public VetoAllocationResult allocateVetos(TestOrder to) {
      return allocateVetos(to.getOrderInformation(), to.getVetos(), to.getUrgency());
    }
    
    public void undoAllocation(TestOrder to) {
      undoAllocation(to.getOrderInformation(), to.getVetos());
    }
    
    public boolean freeVetos(TestOrder to) {
      return vm.freeVetos(to.getOrderInformation() );
    }

    public String showVetoCache() {
      return vc.showVetoCache();
    }
    
    public String showVetoQueue() {
      return vc.showVetoQueue()+"-"+vcp.showBatch();
    }

    public void executeVetoCacheProcessor() {
      vcp.exec();
    }

    public void finalizeAllocation(TestOrder to) {
      vm.finalizeAllocation(to.getOrderInformation(), to.getVetos());
    }

    public void beginScheduling(long currentSchedulingRun) {
      vc.beginScheduling(currentSchedulingRun);
    }
    
    public void endScheduling() {
      vc.endScheduling();
    }
    
    public void schedule(long schedulingRun, TestOrder ... orders ) {
      beginScheduling(schedulingRun);
      for( TestOrder to : orders ) {
        trySchedule(to);
      }
      endScheduling();
    }

    public void trySchedule(TestOrder to) {
      VetoAllocationResult result = allocateVetos(to);
      if( result.isAllocated() ) {
        //Auftrag starten
        if( to.getRunnable() != null && executorService != null ) {
          executorService.execute(to.getRunnable());
        }
        if( to.isSchedulingUndo() ) {
          undoAllocation(to);
        } else {
          finalizeAllocation(to);
        }
      }
    }

    public void setExecutor(ExecutorService executorService) {
      this.executorService = executorService;
    }

    @Override
    public VetoManagementAlgorithmType getAlgorithmType() {
      return vm.getAlgorithmType();
    }
    
    public String showVetoPersistence() {
      return persistence.show();
    }

    @Override
    public String showInformation() {
      return (vc.isStarted()?"started":"")+", "+vcp.showInformation();
    }

  }
  
  public static class ClusteredVM extends TestVM {

    private static final Logger logger = CentralFactoryLogging.getLogger(VetoCacheTestHelper.ClusteredVM.class);
    private boolean separateEndBatch;
    private VCP_Clustered_SeparateEndBatch vcpEndBatch;
    
    public ClusteredVM(int binding, boolean startVetoCacheProcessorThread) throws XynaException {
      super(binding, startVetoCacheProcessorThread);
    }
    public ClusteredVM(int binding, boolean startVetoCacheProcessorThread, boolean separateEndBatch) throws XynaException {
      super(binding, startVetoCacheProcessorThread);
      this.separateEndBatch = separateEndBatch;
    }

    public void setClustered(ClusteredVM partner) {
      VCP_Clustered vcpc = (VCP_Clustered)vcp;
      VCP_Clustered vcpc_partner =  (VCP_Clustered)partner.vcp;
      vcpc.setRemoteImpl( new TestRemoteImpl( vc, vcpc_partner.getLocal()) );
    }
    
    public void setUnclustered() {
      setVetoCacheProcessor( new VCP_Local(vc, persistence ), vc.isStarted() );
    }
    
    public void switchClustered(ClusteredVM vm2) {
      
      VCP_Clustered vcpc = createVCP_Clustered();
      VCP_Clustered vcpc_partner = vm2.createVCP_Clustered();
      
      vcpc.setRemoteImpl( new TestRemoteImpl( vc, vcpc_partner.getLocal()) );
      vcpc_partner.setRemoteImpl( new TestRemoteImpl( vc, vcpc.getLocal()) );
      
      setVetoCacheProcessor(vcpc, vc.isStarted() );
      vm2.setVetoCacheProcessor(vcpc_partner, vm2.vc.isStarted() );
    }
    
    
    
    private VCP_Clustered createVCP_Clustered() {
      if( separateEndBatch ) {
        this.vcpEndBatch = new VCP_Clustered_SeparateEndBatch(vc, persistence, vc.getOwnBinding());
        return this.vcpEndBatch;
        
      } else {
        return new VCP_Clustered(vc, persistence, vc.getOwnBinding());
      }
    }

    private static class VCP_Clustered_SeparateEndBatch extends VCP_Clustered {

      private TestRemoteImpl ownRI;

      public VCP_Clustered_SeparateEndBatch(VetoCache vc, TestVetoCachePersistence persistence, int ownBinding) {
        super(vc,persistence,ownBinding);
      }
      
      @Override
      public void setRemoteImpl(VCP_RemoteInterface remoteImpl) {
        super.setRemoteImpl(remoteImpl);
        this.ownRI = (TestRemoteImpl)remoteImpl;
      }
      
      @Override
      protected void endBatch() {
        //nichts tun
      }
      
      public VetoResponse executeEndBatch() {
        ownRI.removeLastResponse();
        super.endBatch();
        return ownRI.getLastResponse();
      }
      
      
      
    }


    private static class TestRemoteImpl implements VCP_RemoteInterface {

      private VCP_Remote local;
      private VetoCache vc;
      private VetoResponse lastResponse;
      
      public TestRemoteImpl(VetoCache vc, VCP_Remote local) {
        this.vc = vc;
        this.local = local;
      }

      public void removeLastResponse() {
        lastResponse = null;
      }

      public VetoResponse processRemoteVetoRequest(VetoRequest vetoRequest) throws RemoteException {
        logger.debug( vetoRequest.getBinding() +": " + vetoRequest );
        VetoResponse response = local.processRemoteVetoRequest(vetoRequest);
        logger.debug( "=> "+ response +" VetoCache: "+ vc);
        this.lastResponse = response;
        return response;
      }

      @Override
      public void replicate(ReplicateVetoRequest replicateVetoRequest) throws RemoteException {
        local.replicate(replicateVetoRequest);
      }
      
      public VetoResponse getLastResponse() {
        return lastResponse;
      }
      
    }

    
    
    public static void executeVetoCacheProcessorSimultaneous(ClusteredVM vm1, ClusteredVM vm2) {
      SimultaneousThreads st = new SimultaneousThreads();
      st.addRunnable(new ExecuteVetoCacheProcssorRunnable(st.getLatch(),vm1));
      st.addRunnable(new ExecuteVetoCacheProcssorRunnable(st.getLatch(),vm2));
      st.startSimultaneous();
      st.join();
    }
    
    public static class SimultaneousThreads {
      private List<Thread> threads = new ArrayList<>();
      private CountDownLatch cdl = new CountDownLatch(1);
      
      public void startSimultaneous() {
        for( Thread t : threads ) {
          t.start();
        }
        cdl.countDown();
      }

      public CountDownLatch getLatch() {
        return cdl;
      }

      public void addRunnable(Runnable runnable) {
        threads.add( new Thread(runnable) );
      }

      public void join() {
        for( Thread t : threads ) {
          try {
            t.join();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
      
    }
    
    
    private static class ExecuteVetoCacheProcssorRunnable implements Runnable {

      private CountDownLatch cdl;
      private ClusteredVM vm;

      public ExecuteVetoCacheProcssorRunnable(CountDownLatch cdl, ClusteredVM vm) {
        this.cdl = cdl;
        this.vm = vm;
      }

      public void run() {
        try {
          cdl.await();
        } catch (InterruptedException e) {
        }
        vm.executeVetoCacheProcessor();
      }
      
    }

    public Thread allocateAdministrativeVetoInOwnThread(final AdministrativeVeto av) {
      Runnable r = new Runnable() {
        public void run(){ 
          try {
            allocateAdministrativeVeto(av);
          } catch (XPRC_AdministrativeVetoAllocationDenied | PersistenceLayerException e) {
            e.printStackTrace();
          }
        }
      };
      
      Thread t = new Thread(r);
      t.start();
      return t;
    }

    public Thread freeAdministrativeVetoInOwnThread(final AdministrativeVeto av) {
      Runnable r = new Runnable() {
        public void run(){ 
          try {
            freeAdministrativeVeto(av);
          } catch (XPRC_AdministrativeVetoDeallocationDenied | PersistenceLayerException e) {
            e.printStackTrace();
          }
        }
      };
      
      Thread t = new Thread(r);
      t.start();
      return t;
    }

    public Thread setDocumentationOfAdministrativeVetoInOwnThread(final AdministrativeVeto av) {
      Runnable r = new Runnable() {
        public void run(){ 
          try {
            setDocumentationOfAdministrativeVeto(av);
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY | PersistenceLayerException e) {
            e.printStackTrace();
          }
        }
      };
      
      Thread t = new Thread(r);
      t.start();
      return t;
    }

    
    public static void cluster(final ClusteredVM vm, final ClusteredVM vm2) {
      //vm.setClustered(vm2);
      //vm2.setClustered(vm);
      //Klappt leider nicht singlethreaded wegen wechselseitigem Warten auf CountDownLatch -> Deadlock
      
      vm.switchClustered(vm2);
      Runnable r = new Runnable() {
        public void run(){ 
          vm.setClustered(vm2);
          vm.executeVetoCacheProcessor();
        }
      };
      Thread t = new Thread(r);
      t.start();

      vm2.setClustered(vm);
      vm2.executeVetoCacheProcessor();
      
      try {
        t.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    public void executeVetoCacheProcessorInOwnThread(long delay) {
      Runnable r = new Runnable() {
        public void run(){ 
          executeVetoCacheProcessor();
        }
      };
      
      Thread t = new Thread(r);
      t.start();
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
      }
    }
    
    public String executeRemoteRequests(boolean keepRequestId) {
      if( vcpEndBatch != null ) {
        VetoResponse vr = vcpEndBatch.executeEndBatch();
        if( vr == null ) {
          return "none";
        }
        if( keepRequestId ) {
          return vr.toString();
        } else {
          return "VetoResponse(?,"+vr.getResponses()+")";
        }
      }
      return null;
    }
    
  }
  
  public static class TestOrder {
    OrderInformation orderInformation;
    List<String> vetos;
    private long urgency;
    private Runnable runnable;
    private boolean schedulingUndo;
    
    public TestOrder(long orderId, String orderType) {
      orderInformation = new OrderInformation(orderId, orderId, orderType);
      vetos = Collections.emptyList();
    }
    
    public TestOrder vetos(String ... vetos) {
      this.vetos = Arrays.asList( vetos );
      return this;
    }
    
    public TestOrder urgency(long urgency) {
      this.urgency = urgency;
      return this;
    }
    
    public TestOrder runnable(Runnable runnable) {
      this.runnable = runnable;
      return this;
    }
    
    public TestOrder schedulingUndo() {
      this.schedulingUndo = true;
      return this;
    }
    public TestOrder schedulingUndo(boolean schedulingUndo) {
      this.schedulingUndo = schedulingUndo;
      return this;
    }
    
    public OrderInformation getOrderInformation() {
      return orderInformation;
    }

    public long getUrgency() {
      return urgency;
    }

    public List<String> getVetos() {
      return vetos;
    }

    public Runnable getRunnable() {
      return runnable;
    }
    
    public boolean isSchedulingUndo() {
      return schedulingUndo;
    }

    
  }

  public static class TestVetoCachePersistence implements VetoCachePersistence {

    private int binding;
    private HashMap<String,VetoCacheEntry> storage = new HashMap<String,VetoCacheEntry>();

    public TestVetoCachePersistence(int binding) {
      this.binding = binding;
    }

    @Override
    public void init() throws XynaException {
    }

    @Override
    public int getOwnBinding() {
      return binding;
    }
    
    @Override
    public void initVetoCache(int binding) {
      //hier nicht möglich
    }
    
    @Override
    public List<VetoCacheEntry> persist(List<VetoCacheEntry> toPersist, List<VetoCacheEntry> toDelete) {
      for( VetoCacheEntry vce : toPersist ) {
        storage.put( vce.getName(), cloneVetoCacheEntry(vce) );
      }
      for( VetoCacheEntry vce : toDelete ) {
        storage.remove( vce.getName() );
      }
      return Collections.emptyList();
    }
    
    
    private VetoCacheEntry cloneVetoCacheEntry(VetoCacheEntry vce) {
      VetoCacheEntry clone = new VetoCacheEntry(vce.getName(), State.Used );
      if( vce.getVetoInformation() != null ) {
        VetoInformation vi = vce.getVetoInformation();
        VetoInformation viClone = new VetoInformation(vi.getName(), vi.getOrderInformation(), 
            vi.getDocumentation(), vi.getBinding() );
        clone.setVetoInformation(viClone);
      }
      return clone;
    }
    
    public String show() {
      return storage.toString();
    }

    @Override
    public void cleanupVetoCache() {
      //hier nicht möglich
    }

    @Override
    public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException {
      throw new RuntimeException();
    }

    @Override
    public void appendInformation(StringBuilder sb) {
      sb.append("persistent(").append(storage.size()).append(" entries)");
    }

  }
  

}
