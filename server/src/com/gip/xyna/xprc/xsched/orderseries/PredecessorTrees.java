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
package com.gip.xyna.xprc.xsched.orderseries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.gip.xyna.utils.collections.Tree;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.xsched.orderseries.tasks.OSMTask;


/**
 * PredecessorTrees verwaltet alle bekannten Predecessor-Abhängigkeiten.
 * Die Daten zu den Predecessoren werden in einer Baum-Struktur gehalten, alle Predecessoren
 * eines Knoten bilden dessen Zweige.
 * Jeder Knoten des Baums ist selbst noch eine Zuweisung correlationId-&gt;(orderId,binding).
 *
 * Achtung: nicht threadsafe!
 */
public class PredecessorTrees {

  public static class SisData {
    private long orderId;
    private int binding;
    
    public SisData(SeriesInformationStorable sis) {
      this.orderId = sis.getId();
      this.binding = sis.getBinding();
    }
    public long getOrderId() {
      return orderId;
    }
    public int getBinding() {
      return binding;
    }
    @Override
    public String toString() {
      return "("+orderId+","+binding+")";
    }
  }
  
  public static class TreeNode extends Tree<String,SisData> {
   
    public TreeNode(String correlationId) {
      super(correlationId);
    }
    
    public TreeNode getBranch( String correlationId ) { //wandelt ReturnType für einfachere Verwendung
      return (TreeNode) branches.get(correlationId);
    }

    /**
     * Liefert das Binding (Achtung: vorher mit hasData() prüfen, sonst NPE!)
     * @return 
     */
    public int getBinding() {
      return getValue().getBinding();
    }

    /**
     * Existieren die Daten (orderId,binding)?
     * @return
     */
    public boolean hasData() {
      return getValue() != null;
    }

    /**
     * Liefert die OrderId (Achtung: vorher mit hasData() prüfen, sonst NPE!)
     * @return
     */
    public long getOrderId() {
      return getValue().getOrderId();
    }

     
    @Override
    public String toString() {
      return "TreeNode("+key+"="+value+", "+(branches!=null?branches.size():"?")+" branches)";
    }

 

    
  }
  
  private static class TreeBuilder implements Tree.TreeBuilder<String, SisData, TreeNode> {

    private OSMCache osmCache;
    private Map<String,TreeNode> predecessorTrees;
    private boolean hasCycle;
    private String lastCorrId;
    private SeriesInformationStorable lastSis;
    
    
    public TreeBuilder(OSMCache osmCache, Map<String,TreeNode> predecessorTrees) {
      this.osmCache = osmCache;
      this.predecessorTrees = predecessorTrees;
    }
    
    public TreeNode createTree(String correlationId) {
      //Tree zu correlationId liegt entweder bereits im Cache oder muss neu aufgenommen werden      
      TreeNode cachedTree = predecessorTrees.get(correlationId);
      if( cachedTree == null ) {
        cachedTree = new TreeNode(correlationId);
        predecessorTrees.put(correlationId,cachedTree);
      }
      if( cachedTree.getValue() == null ) { //(orderId,binding) ergänzen
        SeriesInformationStorable sis = getCachedSis(correlationId);
        if( sis != null ) {
          cachedTree.setValue( new SisData(sis) );
          if( sis.getOrderStatus().isFinished() && sis.getSuccessorCorrIds().isEmpty() ) {
            //keiner sollte diesen TreeNode noch im Cache suchen, daher wieder entfernen
            predecessorTrees.remove(correlationId);
          }
        }
      }
      return cachedTree;
    }

    public Collection<String> getBranchKeys(String correlationId) {
      SeriesInformationStorable sis = getCachedSis(correlationId);
      if( sis == null ) {
        return null;
      } else {
        //Hier wird auf sis zugegriffen, ohne dass ein Lock schützt: nur sichere Operationen verwenden
        //sis.getPredecessorCorrIds().isEmpty() wird als sicher angesehen
        if( sis.getPredecessorCorrIds().isEmpty() ) {
          //häufiger Spezialfall: alle Predecessoren sind bereits gelaufen
          return Collections.emptyList(); 
        } else {
          //nun muss auf die Daten in sis.getPredecessorCorrIds() zugegriffen werden: besser locken
          if( osmCache.tryLock( correlationId ) ) { //es wurden bereits Locks geholt
            try {
              return new ArrayList<String>(sis.getPredecessorCorrIds());
            } finally {
              osmCache.unlock(correlationId);
            }
          } else {
            //Lock wurde nicht erhalten, trotzdem versuchen, an die Daten zu gelangen
            //es ist unwahrscheinlich, dass gerade die Daten verändert werden.
            //FIXME das ist so aber nicht so gut...

            ArrayList<String> v1 = new ArrayList<String>(sis.getPredecessorCorrIds());
            if( v1.contains(null) ) {
              v1.remove(null);
            }
            return v1;
          }
        }
      }
    }

    public void cycleDetected(List<String> cycle) {
      hasCycle = true;
      //TODO cycle speichern!
    }
    

    /**
     * Liefert den gecachten SeriesInformationStorable oder liest ihn neu
     * (Lokaler Cache ist nötig, da meist die beiden Aufrufe createTree und getBranchKeys
     * direkt nacheinander kommen und vor allem die teure Suche fehlender Einträge im OSMCache
     * dadurch gespart wird.
     * @param correlationId
     * @return
     */
    private SeriesInformationStorable getCachedSis(String correlationId) {
      if( ! correlationId.equals( lastCorrId ) ) {
        lastCorrId = correlationId;
        //Holen des SeriesInformationStorable aus dem Cache:
        //-> teure Suche, wenn der Eintrag nicht im Cache ist
        lastSis = osmCache.get(correlationId);
      }
      return lastSis;
    }

    /**
     * Wurde beim Bau des Baums ein Zirkel festgestellt?
     * @return
     */
    public boolean hasCycle() {
      return hasCycle;
    }

    
  }
  
  
  private HashMap<String,TreeNode> predecessorTrees;
  private OSMCache osmCache;
  private Queue<OSMTask> internalQueue;
  
  public PredecessorTrees(OSMCache osmCache, Queue<OSMTask> internalQueue) {
    this.osmCache = osmCache;
    this.internalQueue = internalQueue;
    predecessorTrees = new HashMap<String,TreeNode>(1024);
  }
  
  /**
   * Baut den Baum zur angebenen correlationId
   * @param correlationId
   * @return true, falls eine zyklische Abhängigkeit besteht
   */
  public synchronized boolean buildTree( String correlationId ) {
    TreeBuilder treeBuilder = new TreeBuilder(osmCache,predecessorTrees);
    TreeNode tree = Tree.buildTree(correlationId, treeBuilder);
    predecessorTrees.put( correlationId, tree);
    if( predecessorTrees.size() > XynaProperty.ORDER_SERIES_MAX_PRE_TREES_IN_CACHE.get() ) {
      internalQueue.add( OSMTask.cleanPredecessorTrees() );
    }
    return treeBuilder.hasCycle();
  }
  
  /**
   * Baut einen Baum zur angebenen correlationId, der nur aus einem TreeNode
   * besteht. 
   * @param correlationId
   * @return TreeNode
   */
  public synchronized TreeNode buildShortTree(String correlationId) {
    TreeBuilder treeBuilder = new TreeBuilder(osmCache,predecessorTrees);
    return treeBuilder.createTree(correlationId);
  }

  
  
  public synchronized TreeNode removeTree(String correlationId) {
    return predecessorTrees.remove(correlationId);
  }

  public synchronized TreeNode getTree(String correlationId) {
    return predecessorTrees.get(correlationId);
  }

  public synchronized int getNumberOfTrees() {
    return predecessorTrees.size();
  }

  public synchronized List<String> getCycle(String correlationId) {
    return predecessorTrees.get(correlationId).getCycle();
  }

  public synchronized void finish(String correlationId) {
    TreeNode tn = predecessorTrees.remove(correlationId);
    if( tn == null ) {
      //TreeNode existierte bereits nicht mehr
    } else {
      tn.removeAllBranches();
    }
  }
  
  @Override
  public synchronized String toString() {
    StringBuilder sb = new StringBuilder(100);
    sb.append("PredecessorTrees(");
    sb.append("size=").append( predecessorTrees.size() );
    //sb.append("trees=").append( predecessorTrees ); //dies kann serh umfangreich sein!
    sb.append(")");
    return sb.toString();
  }

  /**
   * Aussortieren aller PredecessorTrees, die nicht mehr benötigt werden
   * Benötigt werden alle PredecessorTrees mit eigenem Binding
   * Benötigt werden alle von benötigten PredecessorTrees referenzierten PredecessorTrees. 
   * -&gt; nur PredecessorTrees zu fremdem Binding werden also entfernt
   * @param ownBinding
   */
  public synchronized void prune(int ownBinding) {
    HashMap<String,TreeNode> predecessorTreesNew = new HashMap<String,TreeNode>(1024);
    for( TreeNode tn : predecessorTrees.values() ) {
      if( tn.hasData() && tn.getBinding() == ownBinding ) {
        repotTree( predecessorTreesNew, tn );
      }
    }
    predecessorTrees = predecessorTreesNew; //Umkopieren, alte Daten aus 
  }

  /**
   * Verpflanzen des Trees tn nach predecessorTreesNew
   * @param predecessorTreesNew
   * @param tn
   */
  private void repotTree(HashMap<String, TreeNode> predecessorTreesNew, TreeNode tn) {
    String corrId = tn.getKey();
    if( predecessorTreesNew.containsKey(corrId) ) {
      return; //nichts zu tun, Tree ist bereits verpflanzt
    }
    predecessorTreesNew.put(corrId, tn);
    for( Tree<String,SisData> branch : tn.getBranches() ) {
      if( branch instanceof TreeNode ) {
        repotTree(predecessorTreesNew, (TreeNode) branch );
      }
    }
  }

  public synchronized void shrinkToSize(int size) {
    int current = predecessorTrees.size();
    Iterator<String> iter = predecessorTrees.keySet().iterator();
    for( int i=0; i< current-size && iter.hasNext(); ++i ) {
      iter.next();
      iter.remove();
    }
  }
  
  
}
