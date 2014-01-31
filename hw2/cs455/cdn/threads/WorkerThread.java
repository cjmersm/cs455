package cs455.cdn.threads;

/**
 * This is the worker thread that will be created within the thread pools
 * @author mersman
 *
 */
public class WorkerThread implements Runnable{

	ThreadPool tp;

	public WorkerThread(ThreadPool tp){
		this.tp = tp;
	}

	public void run(){
		while (true) {

			Runnable task = null;
			try {
				task = tp.getTask();
			} catch (InterruptedException ex) {
			}

			System.out.println("Got Task");

			if (task == null) {
				System.out.println("Something went wrong?");
				return;
			}

			try {
				task.run();
				tp.runTask(task);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
}
