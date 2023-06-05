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
package com.gip.xyna.utils.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;


/**
 * Tree&lt;K,V&gt; ist ein einfache Baumstruktur mit Key-Value-Paaren. 
 * Durch die Verwendung der HashMap zur Speicherung der Zweige sind diese unsortiert.
 * Der Key muss im ganzen Baum eindeutig sein.
 * 
 * Zum Bauen des Baums gibt es die Funktionen
 * <ul>
 * <li> {@link #buildTree}</li>
 * <li> {@link #swapBranches}</li>
 * <li> {@link #addBranch}</li>
 * <li> {@link #removeBranch}</li>
 * <li> {@link #removeAllBranches}</li>
 * <li> {@link #setBranches}</li>
 * <li> {@link #setBranches(List)}</li>
 * <li> {@link #setBranches(Tree)}</li>
 * <li> {@link #setBranchesMap}</li>
 * </ul>
 * Ausserdem gibt es folgende Diagnose-Funktionen
 * <ul>
 * <li> {@link #hasBranches}</li>
 * <li> {@link #hasBranch}</li>
 * <li> {@link #getUnfilled}</li>
 * <li> {@link #hasCycle}</li>
 * <li> {@link #getCycle}</li>
 * <li> {@link #fullTreeAsString}</li>
 * </ul>
 */
public class Tree<K,V> {

  public enum TreeStatus {
    BranchFound,
    Unknown, 
    BranchNotFound,
    LimitReached;
  }
   
  protected Map<K,Tree<K,V>> branches;
  protected K key;
  protected V value;
  
  public Tree(K key) {
    this.key = key;
    this.value = null;
    this.branches = null;
  }
  
  public Tree(K key, V value) {
    this.key = key;
    this.value = value;
    this.branches = null;
  }
 
  /**
   * Kein weiterer Zweig
   */
  public void setBranches() {
    this.branches = Collections.emptyMap();
  }
  /**
   * Eintragen eines weiteren Zweigs
   */
  public void setBranches( Tree<K,V> branch ) {
    createBranchesMap();
    this.branches.put( branch.getKey(), branch );
  }

  /**
   * Eintragen zwei weiterer Zweige
   */
  public void setBranches( Tree<K,V> branch1, Tree<K,V> branch2 ) {
    createBranchesMap();
    this.branches.put( branch1.getKey(), branch1 );
    this.branches.put( branch2.getKey(), branch2 );
  }
  /**
   * Eintragen der weiteren Zweige
   */
  public void setBranches( Tree<K,V> ... branches ) {
    createBranchesMap();
    for( Tree<K,V> branch : branches ) {
      this.branches.put( branch.getKey(), branch );
    }
  }
  
  /**
   * Eintragen der weiteren Zweige
   * 
   */
  public void setBranches(List<? extends Tree<K,V>> branches) {
    createBranchesMap();
    for( Tree<K,V> branch : branches ) {
      this.branches.put( branch.getKey(), branch );
    }
  }

  
  /**
   * Anlegen der Map
   */
  protected void createBranchesMap() {
    if( branches == null ) {
      branches = new HashMap<K,Tree<K,V>>();
    }
  }

  /**
   * Vorgabe der Branches-Map (hier kann eine spezielle Map-Implementierung gesetzt werden)
   * @param map
   */
  public void setBranchesMap( Map<K,Tree<K,V>> map ) {
    if( branches == null ) {
      this.branches = map;
    } else {
      throw new IllegalStateException("BranchesMap is already created");
    }
  }

  
  
  /**
   * Eintragen eines weiteren Zweigs
   */
  public void addBranch( Tree<K,V> branch ) {
    if( branches == null ) {
      createBranchesMap();
    }
    this.branches.put( branch.getKey(), branch );
  }

  public K getKey() {
    return key;
  }
  
  public V getValue() {
    return value;
  }
  
  /**
   * Setzt den Value auf den neuen Wert
   * @param value
   * @return alter Wert
   */
  public V setValue(V value) {
    V old = this.value;
    this.value = value;
    return old;
  }
  
  /**
   * Entfernen des Zweigs key
   * @param key
   * @return TreeStatus.{Unknown,BranchNotFound,BranchFound}
   */
  public TreeStatus removeBranch( K key ) {
    if( branches == null ) {
      return TreeStatus.Unknown;
    }
    Tree<K,V> branch = branches.remove(key);
    return branch == null ? TreeStatus.BranchNotFound : TreeStatus.BranchFound;
  }
  
  /**
   * Entfernen aller Branches
   * @return TreeStatus.{Unknown,BranchNotFound,BranchFound}
   */
  public TreeStatus removeAllBranches() {
    if( branches == null ) {
      return TreeStatus.Unknown;
    } else {
      boolean isEmpty = branches.isEmpty();
      branches.clear();
      return isEmpty ? TreeStatus.BranchNotFound : TreeStatus.BranchFound;
    }
  }
  
  
  @Override
  public boolean equals(Object obj) {
    if( obj instanceof Tree ) {
      return key.equals( ((Tree<?,?>)obj).key );
    }
    return false;
  }
  
  @Override
  public int hashCode() {
    return key.hashCode();
  }
  
  
  /**
   * Existieren Branches?
   * @return TreeStatus.Unknown, falls dies nicht bekannt ist, weil nicht vollständig initialsiert, 
   *   TreeStatus.BranchNotFound, falls es keine Branches gibt
   *   TreeStatus.BranchFound, falls Branches vorliegen
   */
  public TreeStatus hasBranches() {
    if( branches == null ) {
      return TreeStatus.Unknown;
    }
    if( branches.isEmpty() ) {
      return TreeStatus.BranchNotFound;
    }
    return TreeStatus.BranchFound;
  }

  public Tree<K, V> getBranch( K key ) {
    return branches.get(key);
  }
  
  /**
   * 
   */
  public Collection<Tree<K, V>> getBranches() {
    if( branches == null ) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableCollection(branches.values());
  }
    
  
  /**
   * Hat der Baum einen Zweig mit dem angegebenen Namen? Es werden nur Unterzweige überprüft, 
   * nicht der aktuelle Zweig.  
   * @param branchKey
   * @param depthLimit
   * @return TreeStatus.BranchFound, wenn ein Zweig gefunden wurde.
   *   Ansonsten TreeStatus.Unknown, wenn ungefüllte Zweige existieren;
   *   TreeStatus.LimitReached, wenn die Suchtiefe depthLimit überschritten wurde;
   *   oder TreeStatus.BranchNotFound, wenn sicher ist, dass so ein Zweig nicht exisitiert.
   */
  public TreeStatus hasBranch( K branchKey, int depthLimit) {
    if( branches == null ) {
      return TreeStatus.Unknown;
    }
    if( branches.isEmpty() ) {
      return TreeStatus.BranchNotFound;
    }
    for( Map.Entry<K,Tree<K,V>> entry : branches.entrySet() ) {
      if( entry.getKey().equals( branchKey ) ) {
        return TreeStatus.BranchFound;
      }
    }
    if( depthLimit <= 1 ) {
      return TreeStatus.LimitReached;
    }
    boolean unknown = false;
    boolean limitReached = false;
    for( Map.Entry<K,Tree<K,V>> entry : branches.entrySet() ) {
      TreeStatus ns = entry.getValue().hasBranch( branchKey, depthLimit-1 );
      switch( ns ) {
        case BranchFound:
          return TreeStatus.BranchFound;
        case LimitReached:
          limitReached = true;
          break;
        case Unknown:
          unknown = true;
          break;
      }
    }
    if( unknown ) {
      return TreeStatus.Unknown;
    } else if( limitReached ) {
      return TreeStatus.LimitReached;
    } else {
      return TreeStatus.BranchNotFound;
    }
  }

  /**Gibt aus der Predecessor-Hierarchie den ersten gefunden Eintrag zurück, 
   * der entweder nicht gefüllt ist oder das depthLimit überschreitet.
   * @param depthLimit
   * @return gefundener Eintrag oder null, falls nichts gefunden
   */
  public Tree<K,V> getUnfilled(int depthLimit) {
    if( branches == null ) {
      return this;
    }
    if( branches.isEmpty() ) {
      return null;
    }
    if( depthLimit <= 1 ) {
      return this;
    }
    for( Map.Entry<K,Tree<K,V>> entry : branches.entrySet() ) {
      Tree<K,V> branch = entry.getValue().getUnfilled(depthLimit-1);
      if( branch != null ) {
        return branch;
      }
    }    
    return null;
  }

  /** 
   * Ausgegeben wird "Tree(key=value, 2 branches)" oder Tree(key=value, unknown) 
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Tree("+key+"="+value+", "+(branches != null?(branches.size()+" branches"):"unknown" )+")";
  }
  
  
  /**
   * Liefert alle Knoten des Baums zurück
   * @param depthLimit
   * @return
   */
  protected HashMap<Tree<K,V>,Integer> getAllTreeNodes(int depthLimit) {
    HashMap<Tree<K,V>,Integer> all = new HashMap<Tree<K,V>,Integer>();
    all.put( this, Integer.valueOf(1) );
    fillAllTreeNodes(all, 2, depthLimit );
    return all;
  }
  
  private void fillAllTreeNodes(HashMap<Tree<K, V>, Integer> all, int depth, int depthLimit ) {
    if( branches == null ) {
      return;
    }
    if( branches.isEmpty() ) {
      return;
    }
    if( depthLimit <= 1 ) {
      return;
    }
    //Reihenfolge hier etwas komplizierter, dafür aber immer Einträge mit minimaler depth 
    ArrayList<Tree<K,V>> notInserted = new ArrayList<Tree<K,V>>();
    for( Map.Entry<K,Tree<K,V>> entry : branches.entrySet() ) {
      if( ! all.containsKey( entry.getValue() ) ) {
        notInserted.add( entry.getValue() );
      }
    }
    for( Tree<K,V> tree : notInserted ) {
      all.put( tree, Integer.valueOf(depth) );
    }
    for( Tree<K,V> tree : notInserted ) {
      tree.fillAllTreeNodes(all, depth+1, depthLimit-1 );
    }
  }

  /**
   * Ausgegeben wird die maximale Tiefe des Baums. Falls depthLimit ausgegeben wird, ist 
   * wahrscheinlich die Maximale Suchtiefe überschritten worden.
   * @param depthLimit
   * @return
   */
  public int getDepth( int depthLimit ) {
    HashMap<Tree<K,V>,Integer> all = getAllTreeNodes(depthLimit);
    int maxDepth = 0;
    for( Map.Entry<Tree<K,V>,Integer> entry : all.entrySet() ) {
      int d = entry.getValue().intValue();
      maxDepth = maxDepth > d ? maxDepth : d;
    }
    return maxDepth;
  }
  
  /**
   * Anzahl der Knoten im Baum bis zur Suchtiefe depthLimit
   * @param depthLimit
   * @return
   */
  public int getSize( int depthLimit ) {
    HashMap<Tree<K,V>,Integer> all = getAllTreeNodes(depthLimit);
    return all.size();
  }
  
  /**
   * Liefert den kompletten Baum bis zu einer angegeben Tiefe als String aus.
   * Format: A(B(C(limit reached),D()),E(F(unknown),G()),H())
   * "limit reached" wird eingetragen, falls der Zweig noch Kinder hat
   * "unknown" wird eingetragen, wenn nicht bekannt ist, ob der Zweig Kinder hat
   * @return
   */
  public String fullTreeAsString(int depthLimit) {
    StringBuilder sb = new StringBuilder();
    fullTreeAsString(sb,depthLimit);
    return sb.toString();
  }
  
  protected void fullTreeAsString(StringBuilder sb, int depthLimit) {
    sb.append(key);
    if( branches == null ) {
      sb.append("(unknown)");
      return;
    }
    if( branches.isEmpty() ) {
      sb.append("()");
      return;
    }
    if( depthLimit <= 1 ) {
      sb.append("(limit reached)");
      return;
    }
    String sep = "(";
    for( Map.Entry<K,Tree<K,V>> entry : branches.entrySet() ) {
      sb.append(sep);
      sep = ",";
      entry.getValue().fullTreeAsString(sb, depthLimit-1);
    }  
    sb.append(")");
  }

  /**
   * Untersucht, ob in dem Baum ein Zirkel ist. 
   * Achtung: diese Operation ist recht teuer, da sie den gesamten 
   * Baum durchsuchen muss.
   * Daher nur im Notfall einsetzen.
   * Sie gibt unter Umständen ein falsches Ergebnis "false" zurück,
   * wenn im Baum noch ungefüllte Zweige vorliegen. 
   * @return
   */
  public boolean hasCycle() {
    HashSet<K> cycleData = new HashSet<K>();
    return hasCycle(cycleData);
  }

  protected boolean hasCycle(HashSet<K> cycleData) {
    if( cycleData.contains(key) ) {
      return true;
    }
    if( branches == null || branches.isEmpty() ) {
      return false;
    }
    
    cycleData.add(key);
    for( Map.Entry<K,Tree<K,V>> entry : branches.entrySet() ) {
      if( entry.getValue().hasCycle(cycleData) ) {
        return true;
      }
    }
    cycleData.remove(key); //wieder austragen, damit Nachbarzweige nicht gewertet werden
    return false;
  }
  
  public List<K> getCycle() {
    LinkedHashSet<K> cycleData = new LinkedHashSet<K>();
    return getCycle(cycleData);
  }
  protected List<K> getCycle(LinkedHashSet<K> cycleData) {
    if( cycleData.contains(key) ) {
      return createCycleList( cycleData, key );
    }
    if( branches == null || branches.isEmpty() ) {
      return null;
    }
    
    cycleData.add(key);
    for( Map.Entry<K,Tree<K,V>> entry : branches.entrySet() ) {
      List<K> cycle = entry.getValue().getCycle(cycleData);
      if( cycle != null) {
        return cycle;
      }
    }
    cycleData.remove(key); //wieder austragen, damit Nachbarzweige nicht gewertet werden
    return null;
  }

  /**
   * @param cycleData
   * @param key
   * @return
   */
  protected static <K> List<K> createCycleList(LinkedHashSet<K> cycleData, K key) {
    ArrayList<K> cycle = new ArrayList<K>();
    boolean cycleStarted = false;
    for( K k : cycleData ) {
      if( key.equals(k) ) {
        cycleStarted = true;
      }
      if( cycleStarted ) {
        cycle.add(k);
      }
    }
    cycle.add(key);
    return cycle;
  }

  /**
   * Austausch der Zweige der beiden Bäume
   * @param tree1
   * @param tree2
   */
  public static <K,V> void swapBranches(Tree<K,V> tree1, Tree<K,V> tree2) {
    Map<K,Tree<K,V>> help = tree1.branches;
    tree1.branches = tree2.branches;
    tree2.branches = help;
  }
  
  
  /**
   * Bau des Baums 
   * @param key Identifikation des untersten Baumabschnitts 
   * @param treeBuilder Interface, welches die konkreten Einzelaufgaben beim Baum-Bauen löst
   * @return fertiger Baum (halbfertig, wenn beim Bauen ein Zirkel entdeckt wird)
   */
  public static <K,V,T extends Tree<K,V>> T buildTree( K key, TreeBuilder<K,V,T> treeBuilder ) {
    RecursiveTreeBuilder<K,V,T> rsb = new RecursiveTreeBuilder<K,V,T>(treeBuilder);
    T tree = rsb.buildTree( key );
    return tree;
  }
  
  private static class RecursiveTreeBuilder<K,V,T extends Tree<K,V>> {
    private HashSet<Tree<K,V>> unfillableTrees = new HashSet<Tree<K,V>>();
    private HashSet<Tree<K,V>> alreadyFilledTrees = new HashSet<Tree<K,V>>();
    private TreeBuilder<K, V, T> treeBuilder;
    private LinkedHashSet<K> cycleData = new LinkedHashSet<K>(); //cycleData dient zur Detektion 
                                                                 //von zirkulären Referenzen beim Füllen des Baums.
    public RecursiveTreeBuilder(TreeBuilder<K, V, T> treeBuilder) {
      this.treeBuilder = treeBuilder;
    }
    
    public T buildTree( K key ) {
      T tree = treeBuilder.createTree(key);
      try {
        fillTree( tree );
      } catch( CycleException e ) {
        treeBuilder.cycleDetected(createCycleList(cycleData,key) );
      }
      return tree;
    }
    
    private void fillTree(T tree) throws CycleException {
      if( cycleData.contains(tree.getKey() )) {
        throw new CycleException();
      }
      cycleData.add(tree.getKey());
      TreeStatus treeStatus = tree.hasBranches();
      switch( treeStatus ) {
        case BranchFound:
          //es liegen bereits Branches vor
          fillTreeBranches( tree );
          break;
        case BranchNotFound:
          //es ist bekannt, dass keine Unterzweige existieren
          break;
        case Unknown:
          //Branches neu anlegen
          createTreeBranches( tree );
          break;
      }
      cycleData.remove(tree.getKey());
    }
    
    private void createTreeBranches(T tree) {
      if( unfillableTrees.contains(tree) ) {
        return; //nicht nochmal versuchen
      }
      Collection<K> branches = treeBuilder.getBranchKeys(tree.getKey());
      if( branches == null ) {
        //tree muss weiter ungefüllt bleiben
        unfillableTrees.add(tree);
      } else {
        //Branches anlegen und füllen
        tree.createBranchesMap();
        for( K branchKey : branches ) {
          T branch = buildTree( branchKey );
          tree.addBranch( branch );
        }
      }
    }
    
    @SuppressWarnings("unchecked")
    private void fillTreeBranches(T tree) throws CycleException {
      if( alreadyFilledTrees.contains(tree) ) {
        return; //nicht nochmal versuchen
      }
      for( Tree<K,V> t : tree.getBranches() ) {
        fillTree((T)t);
      }
      alreadyFilledTrees.add(tree);
    }
    
  }
  
  /**
   * Interface, mit dem das Erzeugen von Baumknoten und das Ermitteln der Zweige angepasst 
   * werden können.
   */
  public interface TreeBuilder<K,V,T extends Tree<K,V>> {
    
    /**
     * Liefert einen Tree-Knoten zurück, entweder neu gebaut oder ein bereits bestehender Baum. 
     * @param key
     * @return
     */
    T createTree( K key );
    
    /**
     * Wird für ungefüllte Tree-Knoten gerufen, um diese zu füllen
     * @param key Identifikation des Tree-Knotens
     * @return alle Branch-Keys, null falls Tree-Knoten ungefüllt bleiben muss
     */
    Collection<K> getBranchKeys( K key );
        
    void cycleDetected( List<K> cycle );
  }
  
  private static class CycleException extends Exception {
    private static final long serialVersionUID = 1L;
  }
  
}
