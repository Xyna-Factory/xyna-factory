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

package com.gip.xyna.xprc;



import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.queues.RingbufferBlockingQueue;
import com.gip.xyna.utils.collections.queues.RingbufferBlockingQueue.QueueFilter;
import com.gip.xyna.utils.collections.queues.WrappedBlockingQueue;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_StatisticAlreadyRegistered;
import com.gip.xyna.xfmg.statistics.XynaStatistics;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.SNMPVarTypeLegacy;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.StatisticsReportEntryLegacy;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.StatisticsReporterLegacy;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PullStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.IntegerStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.LongStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StringStatisticsValue;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;


//TODO XynaRunnables sollten entscheiden können, ob sie gequeued werden dürfen
public class XynaThreadPoolExecutor extends ThreadPoolExecutor implements StatisticsReporterLegacy {

  private static final Logger logger = CentralFactoryLogging.getLogger(XynaThreadPoolExecutor.class);
  private String stringIdentifier;
  private AtomicLong rejectedCalls;
  private int corePoolSize;
  
  private static final XynaRunnable EMPTY_XYNARUNNABLE = new XynaRunnable() {
    
    public void run() {
    }
  };

  private static Field threadLocalsField;
  private static Field tidField;
  private static Method createTidMethod;


  static {
    try {
      threadLocalsField = Thread.class.getDeclaredField("threadLocals");
      threadLocalsField.setAccessible(true);
      tidField = Thread.class.getDeclaredField("tid");
      tidField.setAccessible(true);
      createTidMethod = Thread.class.getDeclaredMethod("nextThreadID");
      createTidMethod.setAccessible(true);
    } catch (SecurityException e) {
      throw new RuntimeException("Failed to initialize " + XynaThreadPoolExecutor.class.getName(), e);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException("Failed to initialize " + XynaThreadPoolExecutor.class.getName(), e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Failed to initialize " + XynaThreadPoolExecutor.class.getName(), e);
    }
  }

  private ThreadPoolUsageStrategy threadPoolUsageStrategy;
  private QueueingStrategy queueingStrategy;
  private ThreadPoolSizeStrategy threadPoolSizeStrategy;
  
  public interface ThreadPoolUsageStrategy {

    /**
     * Prüfung, ob XynaRunnable direkt von einem Thread ausgeführt werden darf
     * @param xynaRunnable
     * @return
     */
    boolean isExecutionPossible(XynaRunnable xynaRunnable);

    /**
     * Vor dem Ausführen des XynaRunnable gerufen
     * @param xynaRunnable
     */
    void beforeExecute(XynaRunnable xynaRunnable);

    /**
     * Vor dem Ausführen des XynaRunnable gerufen
     * @param xynaRunnable
     */
    void afterExecute(XynaRunnable xynaRunnable);
    
  }
  
  public static class DefaultThreadPoolUsageStrategy implements ThreadPoolUsageStrategy {

    public boolean isExecutionPossible(XynaRunnable xynaRunnable) {
      return true;
    }

    public void beforeExecute(XynaRunnable xynaRunnable) {
    }

    public void afterExecute(XynaRunnable xynaRunnable) {
    }
    
  }

  public interface ThreadPoolSizeStrategy {

    public void adaptThreadPoolSize();
    
  }
  
  /**
   * Weitere Threads über corePoolSize werden erst verwendet, wenn Queue voll ist
   *
   */
  public static class DefaultThreadPoolSizeStrategy implements ThreadPoolSizeStrategy {

    public void adaptThreadPoolSize() {
      //nichts zu tun, ThreadPoolSize wird nicht aktiv angepasst
    }
    
    @Override
    public String toString() {
      return "DefaultThreadPoolSizeStrategy";
    }
  }
  
  public static abstract class AbstractThreadPoolSizeStrategy implements ThreadPoolSizeStrategy {

    protected XynaThreadPoolExecutor executor;
    protected int corePoolSize;
    protected int maximumPoolSize;
    protected int currentPoolSize;

    public AbstractThreadPoolSizeStrategy(XynaThreadPoolExecutor executor) {
      this.executor = executor;
      this.corePoolSize = executor.getCorePoolSize();
      this.maximumPoolSize = executor.getMaximumPoolSize();
      this.currentPoolSize = this.corePoolSize;
    }
    
    public synchronized void adaptThreadPoolSize() {
      int nextPoolSize;
      int waitingTasks = executor.getWaitingCount();
      if( waitingTasks > 0 ) {
        //muss Pool wirklich vergrößert werden?
        int effectivelyWaiting = getEffectivelyWaitingTaskCount();
        if( effectivelyWaiting > 0 ) {
          nextPoolSize = Math.min( calculateNextPoolSize(effectivelyWaiting), maximumPoolSize );
        } else {
          nextPoolSize = currentPoolSize;
        }
      } else {
        //Größe wieder veringern
        nextPoolSize = corePoolSize;
      }
      adaptPoolSize(nextPoolSize);
    }
   
    protected abstract int calculateNextPoolSize(int effectivelyWaiting);

    /**
     * Liefert die Anzahl der tatsächlich wartenden Tasks. (es können sich gleichzeitig Task in der Queue
     * befinden und Threads warten, dass sie etwas auslesen können)
     * @return
     */
    protected int getEffectivelyWaitingTaskCount() {
      int activeThreads = executor.getActiveCount();
      int waitingThreads = currentPoolSize - activeThreads;
      return executor.getWaitingCount() - waitingThreads;
    }

    protected void adaptPoolSize(int nextPoolSize) {
      if( nextPoolSize != currentPoolSize ) {
        executor.setCorePoolSize(nextPoolSize);
        currentPoolSize = nextPoolSize;
      }
    }

  }
  
  /**
   * Pool soweit vergrößern, dass alle wartenden Tasks ausgeführt werden können:
   * Alle Tasks werden sofort neuen Thread gegeben, die Queue wird erst gefüllt, wenn die
   * maximale Größe des ThreadPools erreicht ist.
   */
  public static class EagerThreadPoolSizeStrategy extends AbstractThreadPoolSizeStrategy {

    public EagerThreadPoolSizeStrategy(XynaThreadPoolExecutor executor) {
      super(executor);
    }
    
    @Override
    protected synchronized int calculateNextPoolSize(int effectivelyWaiting) {
      //Pool soweit vergrößern, dass alle wartenden Aufträge ausgeführt werden können
      return currentPoolSize+effectivelyWaiting;
    }

    @Override
    public String toString() {
      return "EagerThreadPoolSizeStrategy";
    }

  }
  
  /**
   * ThreadPool wird nur vergrößert, wenn genügend Tasks in der Queue warten:
   * Größe des Pools = CorePoolSize + Queue-Size/Lazyness
   */
  public static class LazyThreadPoolSizeStrategy extends AbstractThreadPoolSizeStrategy {

    private int lazyness;

    public LazyThreadPoolSizeStrategy(XynaThreadPoolExecutor executor, int lazyness) {
      super(executor);
      if( lazyness <= 0 ) {
        throw new IllegalArgumentException("lazyness must not be <= 0");
      }
      this.lazyness = lazyness;
    }
    
    @Override
    protected synchronized int calculateNextPoolSize(int effectivelyWaiting) {
      //Pool soweit vergrößern, dass ein Teil der wartenden Aufträge ausgeführt werden kann
      return corePoolSize+effectivelyWaiting/lazyness;
    }

    @Override
    public String toString() {
      return "LazyThreadPoolSizeStrategy("+lazyness+")";
    }

  }

  public interface QueueingStrategy {

    Runnable exchangeRejected(Runnable r);
    
  }
  
  public static class DefaultQueueingStrategy implements QueueingStrategy {
    
    public Runnable exchangeRejected(Runnable r) {
      return r;
    }
    
  }
  
  public static class RingBufferQueueingStrategy implements QueueingStrategy {

    private PrioritizedCallerQueue queue;
    private QueueFilter<Runnable> pollFilter = new QueueFilter<Runnable>() {
      public boolean filter(Runnable e) {
        if( e instanceof XynaRunnable ) {
          return ((XynaRunnable)e).isRejectable();
        }
        return false;
      }};
    
    public RingBufferQueueingStrategy(XynaThreadPoolExecutor executor) {
      this.queue = (PrioritizedCallerQueue) executor.getQueue();
    }

    /**
     * gebe runnable zurück, welches nicht ausgeführt, aber auf dem noch nicht "rejected()" ausgeführt wurde oder null, falls das runnable gequeued werden konnte.
     * &lt;=&gt; gebe nur runnable zurück, wenn das übergebene runnable nicht gequeued werden konnte.
     */
    public Runnable exchangeRejected(Runnable r) {
      //anstelle des neuen nicht ausführbaren runnables wird das älteste vorhandene in der queue verworfen
      Runnable eldest = queue.offerOrFilteredExchange( pollFilter, r );
      if( eldest == null ) {
        return null; //Queue wurde frei
      } else if( eldest == r ) {
        return r; //Austausch hat nicht geklappt -> neues Runnable rejecten
      } else {
        if( eldest instanceof XynaRunnable ) {
          ((XynaRunnable) eldest).rejected();
          return null; //Rejection wurde durchgeführt
        } else {
          throw new IllegalStateException( "offerOrFilteredExchange returned no XynaRunnable" );
        }
      }
    }
    
  }
  

  public XynaThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                final BlockingQueue<Runnable> workQueue, 
                                ThreadFactory threadFactory, String identifier) {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, 
         threadFactory, identifier, false, -1);
  }


  public XynaThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                final BlockingQueue<Runnable> workQueue, 
                                ThreadFactory threadFactory, String identifier, boolean useRingBuffer) {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, 
         threadFactory, identifier, useRingBuffer, -1);
  }


  public XynaThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                int workQueueSize, 
                                ThreadFactory threadFactory, String identifier, boolean useRingBuffer) {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, null, 
         threadFactory, identifier, useRingBuffer, workQueueSize);
  }

  /**
   * konstruktor mit ringbuffer support.
   * nach aussen merkt man davon fast nichts. ausnahme: das runnable ist zur laufzeit ein anderes.
   */
  public XynaThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                BlockingQueue<Runnable> _workQueue, ThreadFactory threadFactory,
                                final String identifier, boolean queueAsRingBuffer, int workQueueSize) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, createPrioritizedQueueIfNecessary(_workQueue,
                                                                                                workQueueSize, queueAsRingBuffer),
          threadFactory);
    this.corePoolSize = corePoolSize;
    BlockingQueue<Runnable> createdQueue = getQueue();
    if (createdQueue instanceof PrioritizedCallerQueue) {
      PrioritizedCallerQueue pcq = (PrioritizedCallerQueue) createdQueue;
      pcq.setThreadPoolExecutor(this);
    }
    
    this.rejectedCalls = new AtomicLong(0);
    this.threadPoolUsageStrategy = new DefaultThreadPoolUsageStrategy();
    if( queueAsRingBuffer ) {
      this.queueingStrategy = new RingBufferQueueingStrategy(this);
    } else {
      this.queueingStrategy = new DefaultQueueingStrategy();
    }
    this.threadPoolSizeStrategy = new DefaultThreadPoolSizeStrategy();
    
    super.setRejectedExecutionHandler( new WrappedRejectedExecutionHandler(super.getRejectedExecutionHandler()) );
    
    stringIdentifier = identifier;
    FutureExecution fe = XynaFactory.getInstance().getFutureExecution();
    fe.addTask(fe.nextId(), identifier ).
       after(XynaStatistics.FUTUREEXECUTION_ID).
       execNowOrAsync( //damit man threadpools auch nach dem serverstart noch benutzen kann.
         new Runnable(){ public void run() { registerStatistics(); }}); 
  }

  private static class PrioritizedCallerQueue extends WrappedBlockingQueue<Runnable> {

    private boolean prioritize;
    private XynaThreadPoolExecutor executor;
    private boolean isSynchronous;
    private boolean useSpecialPrioFeatures;
    private QueueFilter<Runnable> filter;
    private Object offerNotification = new Object();
    
    public PrioritizedCallerQueue(int workQueueSize) {
      super( workQueueSize == 0 ? new SynchronousQueue<Runnable>() : new RingbufferBlockingQueue<Runnable>(workQueueSize) );
      this.isSynchronous = workQueueSize == 0;
      this.useSpecialPrioFeatures = false;
      this.filter = new QueueFilter<Runnable>() {
        public boolean filter(Runnable r) {
          XynaRunnable xr = getXynaRunnable(r);
          return executor.getThreadPoolUsageStrategy().isExecutionPossible(xr);
        }
      };
    }
    
    public PrioritizedCallerQueue(LinkedBlockingQueue<Runnable> queue) {
      super(new RingbufferBlockingQueue<Runnable>(queue));
      this.isSynchronous = false;
      this.useSpecialPrioFeatures = false;
      this.filter = new QueueFilter<Runnable>() {
        public boolean filter(Runnable r) {
          XynaRunnable xr = getXynaRunnable(r);
          return executor.getThreadPoolUsageStrategy().isExecutionPossible(xr);
        }
      };
    }

    public Runnable offerOrFilteredExchange(QueueFilter<Runnable> filter, Runnable r) {
      if( isSynchronous ) {
        return r;
      } else {
        return ((RingbufferBlockingQueue<Runnable>)wrapped).offerOrFilteredExchange(filter, r);
      }
    }

    public Runnable offerOrExchange(Runnable r) {
      if( isSynchronous ) {
        return r;
      } else {
        return ((RingbufferBlockingQueue<Runnable>)wrapped).offerOrExchange(r);
      }
    }

    public void setPrioritize(boolean prioritize) {
      this.prioritize = prioritize;
      this.useSpecialPrioFeatures = prioritize && ! isSynchronous;
    }
    
    public boolean isPrioritize() {
      return prioritize;
    }
    
    public Runnable poll() {
      if (useSpecialPrioFeatures) {
        return ((RingbufferBlockingQueue<Runnable>)wrapped).filteredPoll(filter);//FIXME performance
      } else {
        return super.poll();
      }
    }
    
    public Runnable pollNoCheck() {
      return super.poll();
    }

    public Runnable peek() {
      if (useSpecialPrioFeatures) {
        return ((RingbufferBlockingQueue<Runnable>)wrapped).filteredPeek(filter);//FIXME performance
      } else {
        return super.peek();
      }
    }


    public boolean offer(Runnable o) {
      boolean b = super.offer(o);
      notifyPoll();
      return b;
    }


    private void notifyPoll() {
      synchronized( offerNotification ) {
        offerNotification.notifyAll();
      }
    }

    public boolean offer(Runnable o, long timeout, TimeUnit unit) throws InterruptedException {
      notifyPoll();
      boolean b = super.offer(o, timeout, unit);
      notifyPoll();
      return b;
    }


    public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
      if (useSpecialPrioFeatures) {
        long end = System.currentTimeMillis() + unit.toMillis(timeout);
        while (true) {
          Runnable r = poll();
          if (r != null) {
            return r;
          }
          long wait = end - System.currentTimeMillis();
          if (wait > 0) {
            synchronized (offerNotification) {
              offerNotification.wait(wait);
            }
          } else {
            break;
          }
        }
        return null;
      } else {
        return super.poll();
      }
    }

    public Runnable take() throws InterruptedException {
      if (useSpecialPrioFeatures) {
        while (true) {
          Runnable r = poll(5000, TimeUnit.MILLISECONDS);
          if (r != null) {
            return r;
          }
        }
      } else {
        return super.take();
      }
    }

    public void put(Runnable o) throws InterruptedException {
      super.put(o);
      notifyPoll();
    }
    
    public boolean add(Runnable o) {
      boolean b = super.add(o);
      notifyPoll();
      return b;
    }

    public void setThreadPoolExecutor(XynaThreadPoolExecutor executor) {
      this.executor = executor;
    }
    
  }
  
  
  private void registerStatistics() {
        StatisticsPath basePath = PredefinedXynaStatisticsPath.THREADPOOLINFO;

        PullStatistics<String, StringStatisticsValue> nameStats =
            new PullStatistics<String, StringStatisticsValue>(basePath.append(stringIdentifier).append("Name")) {
              private final StringStatisticsValue name = new StringStatisticsValue(stringIdentifier);
              @Override
              public StringStatisticsValue getValueObject() {
                return name;
              }
              @Override
              public String getDescription() {
                return "";
              }
            };

        PullStatistics<Integer, IntegerStatisticsValue> currentSizeStats =
            new PullStatistics<Integer, IntegerStatisticsValue>(basePath.append(stringIdentifier).append("CurrentSize")) {
              @Override
              public IntegerStatisticsValue getValueObject() {
                return new IntegerStatisticsValue(getActiveCount());
              }
              @Override
              public String getDescription() {
                return "";
              }
            };

        PullStatistics<Integer, IntegerStatisticsValue> maxSizeStats =
            new PullStatistics<Integer, IntegerStatisticsValue>(basePath.append(stringIdentifier).append("Max")) {
              @Override
              public IntegerStatisticsValue getValueObject() {
                return new IntegerStatisticsValue(getMaximumPoolSize());
              }
              @Override
              public String getDescription() {
                return "";
              }
            };

        PullStatistics<Long, LongStatisticsValue> completedStats =
            new PullStatistics<Long, LongStatisticsValue>(basePath.append(stringIdentifier).append("Completed")) {
              @Override
              public LongStatisticsValue getValueObject() {
                return new LongStatisticsValue(getCompletedTaskCount());
              }
              @Override
              public String getDescription() {
                return "";
              }
            };

        PullStatistics<Long, LongStatisticsValue> rejectedStats =
            new PullStatistics<Long, LongStatisticsValue>(basePath.append(stringIdentifier).append("Rejected")) {
              @Override
              public LongStatisticsValue getValueObject() {
                return new LongStatisticsValue(getRejectedTasks());
              }
              @Override
              public String getDescription() {
                return "";
              }
            };

        try {
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics()
                          .registerStatistic(nameStats);
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics()
                          .registerStatistic(currentSizeStats);
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics()
                          .registerStatistic(maxSizeStats);
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics()
                          .registerStatistic(completedStats);
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics()
                          .registerStatistic(rejectedStats);
        } catch (XFMG_InvalidStatisticsPath e) {
          throw new RuntimeException("", e);
        } catch (XFMG_StatisticAlreadyRegistered e) {
        }
        

        XynaFactory.getInstance().getFactoryManagement().getXynaStatisticsLegacy()
            .registerNewStatistic("ThreadPool." + stringIdentifier, XynaThreadPoolExecutor.this);
  }


  private static BlockingQueue<Runnable> createPrioritizedQueueIfNecessary(BlockingQueue<Runnable> queue,
                                                                           int workQueueSize, boolean queueAsRingBuffer) {
    if( queue instanceof PrioritizedCallerQueue ) {
      return queue; // ist bereits gewünschte Queue 
    } else if( queue == null ) {
      //es wird eine Queue benötigt, daher anlegen 
      return new PrioritizedCallerQueue(workQueueSize);
    } else {
      //Queue existiert schon
      if( queueAsRingBuffer ) {
        //es sollte aber eine PrioritizedCallerQueue sein!
        if( queue instanceof LinkedBlockingQueue ) {
          return new PrioritizedCallerQueue((LinkedBlockingQueue<Runnable>)queue);
        } else {
          throw new IllegalArgumentException("Queue must be instanceof LinkedBlockingQueue");
        }
      } else {
        return queue;
      }
    }
  }


  @Override
  public void setRejectedExecutionHandler(final RejectedExecutionHandler handler) {
    super.setRejectedExecutionHandler( new WrappedRejectedExecutionHandler(handler) );
  }

  private static class WrappedRejectedExecutionHandler implements RejectedExecutionHandler {

    private RejectedExecutionHandler wrapped;

    public WrappedRejectedExecutionHandler(RejectedExecutionHandler wrapped) {
      this.wrapped = wrapped;
    }

    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
      if( logger.isTraceEnabled() ) {
        logger.trace( "WrappedRejectedExecutionHandler "+r );
      }
      if( executor instanceof XynaThreadPoolExecutor ) {
        //FIXME: exchange sollte nicht das "rejected()" ausführen, sondern das gehört hier in die methode.
        XynaThreadPoolExecutor xtpe = (XynaThreadPoolExecutor)executor;
        Runnable rejected = xtpe.getQueueingStrategy().exchangeRejected( r );
        if( rejected != null ) {
          //rejected kann nur == r sein, deshalb ist es ok, unten den rejectedexecutionhandler aufzurufen (der ggfs ne exception weiterwirft)
          if( logger.isTraceEnabled() ) {
            logger.trace( "WrappedRejectedExecutionHandler "+rejected +" has to be rejected" );
          }
          xtpe.rejectedCalls.incrementAndGet();
          if (((XynaRunnable) r).mayCallRejectionHandlerOnRejection()) {
            wrapped.rejectedExecution(rejected, executor);
          }
        }
      } else {
        if (((XynaRunnable) r).mayCallRejectionHandlerOnRejection()) {
          wrapped.rejectedExecution(r, executor);
        }
      }
      
    }

    public RejectedExecutionHandler getWrappedRejectedExecutionHandler() {
      return wrapped;
    }
    
  }
  
  @Override
  public RejectedExecutionHandler getRejectedExecutionHandler() {
    return super.getRejectedExecutionHandler();
  }

  static XynaRunnable getXynaRunnable(Runnable r) {
    if (r instanceof XynaRunnable) {
      return (XynaRunnable) r;
    } else {
      throw new RuntimeException("unexpected runnable class: " + r.getClass().getName());
    }
  }


  @Override
  protected void beforeExecute(Thread t, Runnable r) {
    if (t == null) {
      logger.warn("no thread provided");
    }

    super.beforeExecute(t, r);

    threadPoolUsageStrategy.beforeExecute(getXynaRunnable(r));

    getXynaRunnable(r).setExecutingThread(t);
  }

  
  // TODO submit doesn't work, overwrite it and wrap FutureTask?

  @Override
  public void execute(Runnable command) {
    if (command == null) {
      throw new NullPointerException();
    }
    
    XynaRunnable xr = getXynaRunnable(command);
    if( threadPoolUsageStrategy.isExecutionPossible(xr) ) {
      super.execute(command);
    } else {
      if (!getQueue().offer(command)) {
        if (logger.isDebugEnabled()) {
          logger.debug("offer returned false for "+xr );
          if( logger.isTraceEnabled() ) {
            logger.trace("current queue: "+getQueue() );
          }
        }
        getRejectedExecutionHandler().rejectedExecution(command, this);
      } else {
        if (getActiveCount() == 0 && getQueue().size() > 0) {
          //bugz 18281: racecondition, dass executionpossible false zurückgegeben hat,
          //nun aber keine aktiven threads mehr existieren und deshalb das runnable nicht aus der queue entnommen wird
          //mit dem einstellen eines leeren runnables wird getriggert, dass weitere runnables aus der queue verarbeitet werden können.
          super.execute(EMPTY_XYNARUNNABLE);
        }
      }
    }
    threadPoolSizeStrategy.adaptThreadPoolSize();
  }


  @Override
  protected void afterExecute(Runnable r, Throwable t) {
    super.afterExecute(r, t);
    XynaRunnable xr = getXynaRunnable(r);
    Thread currentThread = xr.getExecutingThread();
    xr.setExecutingThread(null);
    cleanThreadLocal(currentThread);
    threadPoolUsageStrategy.afterExecute(xr);
  }
  
  private static final XynaPropertyBoolean skipThreadLocalCleanup = new XynaPropertyBoolean("xyna.threadpool.threadlocalcleanup.skip", false);

  /**
   * workaround dafür, dass reentrantreadwritelock (und ggfs andere klassen, die threadlocal benutzen) ihren kontext
   * daraus nicht korrekt entfernen und dadurch unkontrolliert der benötigte speicherplatz anwachsen kann
   */
  private void cleanThreadLocal(Thread currentThread) {
    if (currentThread == null) {
      logger.error("The thread to clean up was not found!");
      return;
    }
    if (skipThreadLocalCleanup.get()) {
      return;
    }
    try {
      threadLocalsField.set(currentThread, null);

      //workaround dafür, dass in reentrantreadwritelock ein cache für objekte die threadlokal gehalten werden benutzt wird.
      //wenn man dies nicht macht, ist der cache != dem threadlokal gehaltenen objekt und es kann illegalmonitorstateexceptions geben.
      //usecase: thread1 macht readlock.lock(), readlock.unlock() => cache ist vorhanden mit holdcount = 0
      //         dann ist thread zuende und threadlocalmap wird geleert
      //         dann macht thread1 readlock.lock => gecachter holdcount = 1
      //         dann kommt thread2 macht readlock.lock => cache wird entfernt und durch einen für thread2 ersetzt => information von holdcount = 1 geht verloren.
      //         thread1 readlock.unlock => illegalmonitorstateexception, weil holdcount in threadlocal = 0.
      //mit der neuen threadid wird forciert, dass der cache nicht nach wiederverwendet werden kann, falls der thread aus dem threadpool
      //einmal fertiggelaufen war und beim erneuten execute das gleiche reentrantlock benutzt.
      //das verhalten ist dann genauso wie man es ohne threadpoolbenutzung erwarten würde (immer andere threadids)
      long newTid = (Long) createTidMethod.invoke(null);
      tidField.set(currentThread, newTid);
    } catch (IllegalArgumentException e) {
      logger.error("Failed to reset thread local field and id", e);
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      logger.error("Failed to reset thread local field and id", e);
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      logger.error("Failed to reset thread local field and id", e);
      throw new RuntimeException(e);
    }
  }



  public long getRejectedTasks() {
    return rejectedCalls.get();
  }


  public boolean isRingBuffer() {
    return queueingStrategy instanceof RingBufferQueueingStrategy;
  }


  public StatisticsReportEntryLegacy[] getStatisticsReportLegacy() {
    StatisticsReportEntryLegacy[] report = new StatisticsReportEntryLegacy[2];
    report[0] = new StatisticsReportEntryLegacy() {

      public Object getValue() {
        return getCompletedTaskCount();
      }


      public SNMPVarTypeLegacy getType() {
        return SNMPVarTypeLegacy.UNSIGNED_INTEGER;
      }


      public String getDescription() {
        return "Count of completed tasks";
      }
    };

    report[1] = new StatisticsReportEntryLegacy() {

      public Object getValue() {
        return getRejectedTasks();
      }


      public SNMPVarTypeLegacy getType() {
        return SNMPVarTypeLegacy.UNSIGNED_INTEGER;
      }


      public String getDescription() {
        return "Count of rejected tasks";
      }
    };

    return report;
  }

  
  public void setThreadPoolUsageStrategy(ThreadPoolUsageStrategy threadPoolUsageStrategy) {
    if( threadPoolUsageStrategy == null ) {
      this.threadPoolUsageStrategy = new DefaultThreadPoolUsageStrategy();
      if ( getQueue() instanceof PrioritizedCallerQueue) {
        ((PrioritizedCallerQueue) getQueue()).setPrioritize(false);
      }
    } else {
      if (!(getQueue() instanceof PrioritizedCallerQueue)) {
        throw new RuntimeException("unsupported");
      }
      ((PrioritizedCallerQueue) getQueue()).setPrioritize(true);
      this.threadPoolUsageStrategy = threadPoolUsageStrategy;
    }
  }
  
  public ThreadPoolUsageStrategy getThreadPoolUsageStrategy() {
    return threadPoolUsageStrategy;
  }
  
  public QueueingStrategy getQueueingStrategy() {
    return queueingStrategy;
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("XynaThreadPoolExecutor(name=").append(stringIdentifier).append(", ");
    sb.append("queueingStrategy=").append(queueingStrategy.getClass().getSimpleName()).append(", ");
    sb.append("queue="+getQueue().getClass().getSimpleName()).append(", ");
    sb.append("rejectedExecutionHandler=").append(getRejectedExecutionHandler().getClass().getName()).append(", ");
    sb.append("threadPoolUsageStrategy=").append(threadPoolUsageStrategy.getClass().getSimpleName()).append(", ");
    sb.append("corePoolSize=").append(getCorePoolSize()).append(", ");
    sb.append("maximumPoolSize=").append(getMaximumPoolSize()).append(", ");
    sb.append("keepAliveTime=").append(getKeepAliveTime(TimeUnit.SECONDS)).append(" s, ");
    //TODO  int workQueueSize
    sb.append("rejectedCalls=").append(rejectedCalls.get());
    sb.append(")");
    return sb.toString();
  }


  public int getWaitingCount() {
    return getQueue().size();
  }
  
  
  @Override
  public int getCorePoolSize() {
    //muss überschrieben werden, da interne corePoolSize geändert wird
    return corePoolSize;
  }

  public void setThreadPoolSizeStrategy(ThreadPoolSizeStrategy threadPoolSizeStrategy) {
    this.threadPoolSizeStrategy = threadPoolSizeStrategy;
  }


  public ThreadPoolSizeStrategy getThreadPoolSizeStrategy() {
    return threadPoolSizeStrategy;
  }
  
}
