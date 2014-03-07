package crawler;

import java.io.File;

import utilities.WorkerThread;
import crawler.PageLW;

public class PageLWLoader extends WorkerThread<File,PageLW> {

	// this class is an example of implementing a worker
	PageLWLoader() {
		super();
	}

	PageLWLoader(File[] x) {
		super(x);
	}

	@Override
	public PageLW work(File f) {
		return PageLW.load(f);
	}

	@Override
	public void beforeRunning() {
		
	}

	@Override
	public void beforeExit() {
		if(showOutput)
			System.out.println(this.getName() + " Exiting");
	}

	@Override
	public WorkerThread<File,PageLW> clone() {
		return new PageLWLoader();
	}

}
