package student;

import java.util.Queue;

import cs439.lab2.lock.FIFOLock;
import cs439.lab2.lock.IStatsLock;
import cs439.lab2.lock.LockProtocolViolation;
import cs439.lab2.lock.ScheduledThread;

public class RecursiveLock implements IStatsLock {

	private String name;

	private static long totalTime;
	private static long timeLockAcquired;
	
	private static int instanceCount;
	
	private int recursiveDepth;
	private int useCount;
	private int failCount;
	private int successCount;
	private int waitersCount;

	private FIFOLock FIFO_Queue;
	private FIFOLock FIFO_Synch;

	private Thread owner;

	public RecursiveLock(String name) {
		synchronized(this){
			this.name = name;
			FIFO_Queue = new FIFOLock("queue");
			FIFO_Synch = new FIFOLock("synch"); 
			++instanceCount;
		}
	}

	/* (non-Javadoc)
	 * ADVISORY ONLY.
	 * DOES NOT NEED TO BE THREAD SAFE
	 * DO NOT SYNCHRONIZE 
	 */
	public static int getInstanceCount() {
		// return the total number of instances created (since last loaded by JVM)
		return instanceCount;
	}

	/* (non-Javadoc)
	 * @see ILock#acquire()
	 */
	public int acquire() {
		if(owner==Thread.currentThread()){
			FIFO_Synch.acquire();
			++recursiveDepth;
			FIFO_Synch.release();
			return recursiveDepth;
		}
		//now either no one owns it, or someone else owns it
		FIFO_Synch.acquire();
		++waitersCount;
		FIFO_Synch.release();
		FIFO_Queue.acquire();
		FIFO_Synch.acquire();
		//start tracking time of lock
		timeLockAcquired = ScheduledThread.getTime();
		//new lock owner, one less waiter
		--waitersCount;
		//current thread is now the owner
		owner = Thread.currentThread();
		//increase depth
		++recursiveDepth;
		//release Synch and return recursive depth
		FIFO_Synch.release();
		return recursiveDepth;
	}

	/* (non-Javadoc)
	 * @see ILock#acquire_try()
	 */
	public int release() {
		//start synch if you don't own this lock, throw a violation
		if(owner != Thread.currentThread()){
			throw new LockProtocolViolation(this,owner,Thread.currentThread());
		}
		FIFO_Synch.acquire();
		//we do own this lock, decrease recursiveDepth
		--recursiveDepth;
		FIFO_Synch.release();
		//if no recursiveDepth, we give up the lock completely
		if(recursiveDepth==0)
		{
			FIFO_Synch.acquire();
			//succsful use of lock
			++useCount;
			//current thread no longer owns lock
			owner = null;
			//total time lock has been in use
			totalTime += (ScheduledThread.getTime() - timeLockAcquired);
			FIFO_Synch.release();
			//release Q
			FIFO_Queue.release();
		}
		//release synch and return recursive depth
		return recursiveDepth;
	}

	/* (non-Javadoc)
	 * @see ILock#acquire_try()
	 */
	public int acquire_try() {
		//synch lock if current thread owns lock, 
		if(owner == Thread.currentThread()){
			//////// Synch LOCK  START ///////////////
			FIFO_Synch.acquire();
			//increase lock depth
			++recursiveDepth;
			//successful acquire_try()
			++successCount;
			//relase synch and return new recursiveDepth
			FIFO_Synch.release();
			//////// Synch LOCK  START ///////////////
			return recursiveDepth;
		}
		//current thread is not owner 
		if(owner == null) 
		{
			//no one owns lock, give it to current thread
			FIFO_Queue.acquire();
			//start the time it was aquired
			timeLockAcquired = ScheduledThread.getTime();
			//set current thread as owner
			owner = Thread.currentThread();
			//////// Synch LOCK  START //////////////////
			FIFO_Synch.acquire();
			//increment recursiveDepth
			++recursiveDepth;
			//increment successCount 
			++successCount;
			//release synch and return recursiveDepth
			FIFO_Synch.release();
			//////// Synch LOCK  END //////////////////
			return recursiveDepth;
		}
		FIFO_Synch.acquire();
		//another thread owns lock, this fails, release SYNCH and return 0
		++failCount;
		FIFO_Synch.release();
		return 0;
	}

	/* (non-Javadoc)
	 * @see ILock#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see IStatsLock#getWaitersCount()
	 * ADVISORY ONLY.
	 * DOES NOT NEED TO BE THREAD SAFE
	 * DO NOT SYNCHRONIZE 
	 */
	public int getWaitersCount() {
		return waitersCount;
	}

	/* (non-Javadoc)
	 * @see IStatsLock#getTrySuccessCount()
	 * ADVISORY ONLY.
	 * DOES NOT NEED TO BE THREAD SAFE
	 * DO NOT SYNCHRONIZE 
	 */
	public int getTrySuccessCount() {
		return successCount;
	}

	/* (non-Javadoc)
	 * @see IStatsLock#getTryFailCount()
	 * ADVISORY ONLY.
	 * DOES NOT NEED TO BE THREAD SAFE
	 * DO NOT SYNCHRONIZE 
	 */
	public int getTryFailCount() {
		return failCount;
	}

	/* (non-Javadoc)
	 * @see IStatsLock#getTotalLockHeldTime()
	 * ADVISORY ONLY.
	 * DOES NOT NEED TO BE THREAD SAFE
	 * DO NOT SYNCHRONIZE 
	 */
	public long getTotalLockHeldTime() {
		return totalTime;
	}

	/* (non-Javadoc)
	 * @see IStatsLock#getUseCount()
	 * ADVISORY ONLY.
	 * DOES NOT NEED TO BE THREAD SAFE
	 * DO NOT SYNCHRONIZE 
	 */
	public int getUseCount() {
		//complete
		return useCount;
	}

}
