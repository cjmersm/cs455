package cs455.cdn.threads;

import java.util.LinkedList;

public class ThreadPool {

	LinkedList<Runnable> taskQueue;

	public ThreadPool(int size){
		taskQueue = new LinkedList<Runnable>();

		for(int i = 0;i<size;i++){
			new Thread(new WorkerThread(this)).start();
		}
	}

	public synchronized void runTask(Runnable task) {
		if (task != null) {
			taskQueue.add(task);
			notify();
		}
	}

	protected synchronized Runnable getTask() throws InterruptedException {
		while (taskQueue.size() == 0) {
			wait();
		}
		return (Runnable) taskQueue.removeFirst();
	}
	
	
}
