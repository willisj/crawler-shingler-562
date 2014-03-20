/**
 * 
 */
package crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Jordan
 * 
 */
public class Crawler extends Thread {
	boolean messagePrinted = false;

	// pages that have yet to be requested
	HashMap<String, Vector<PageLW>> urlPool = new HashMap<String, Vector<PageLW>>();

	// worker threads
	ConcurrentHashMap<PageWorkerThread, Thread> workerThreads;

	// pages that have been requested
	Vector<String> requestedPages = new Vector<String>();

	// pages currently threaded to request pages
	Vector<Thread> running = new Vector<Thread>();

	// URLs that have already been scraped
	ConcurrentSkipListSet<String> seenUrls;

	private static Random rand = new Random();
	public AtomicBoolean crawlerRunning = new AtomicBoolean(false);

	public TopologyOutputController topo;
	public Thread topoThread;
	private boolean notAddingNew = false;
	private final boolean displayStatus, verbose;
	private long poolSize = 0;
	String seedHost;
	String storePath;
	final int maxDomainPerCrawl;
	public int threadTimeout = 10;// in seconds
	String cachePath;

	/**
	 * @param seed
	 *            the url from which to start scraping
	 * @throws MalformedURLException
	 *             invalid seed url
	 */

	public Crawler(String seed, String storePath, String urlPoolFile,
			boolean displayStatus, boolean verbose, int maxDomainPerCrawl,
			String cachePath) throws MalformedURLException {

		this.displayStatus = displayStatus;
		this.verbose = displayStatus && verbose;
		this.maxDomainPerCrawl = maxDomainPerCrawl;
		this.cachePath = cachePath;
		this.storePath = storePath;

		// load seen urls
		if (displayStatus)
			util.writeLog("Loading seen URLs");
		seenUrls = new ConcurrentSkipListSet<String>();

		// TODO: load seen URLS from file

		if (displayStatus) {
			util.writeLog("Loaded " + seenUrls.size() + " seen urls");
			util.writeLog("Loading URL Pool");
		}

		Long urlPoolSize = 0l;
		if (urlPoolFile != null)
			urlPoolSize = loadPoolURLs(new File(urlPoolFile));

		if (displayStatus)
			util.writeLog("Populated URL pool with " + urlPool.size()
					+ " Domains and " + urlPoolSize + " URLs");
		util.writeLog("DiP = Domains in Pool");

		topo = new TopologyOutputController(new File(storePath + File.separator
				+ "TopologyLinkFile"), new File(storePath + File.separator
				+ "TopologyURLFile"));

		if (seed != null) {
			PageLW seedPage = new PageLW();
			seedPage.setPage(seed);
			seedHost = seedPage.host;
			addPage(seedPage);
		}
	}

	private long loadPoolURLs(File source) {
		if (source == null || !source.exists()) {
			System.err.println("Missing URL Pool file");
			return 0;
		}

		long count = 0;
		BufferedReader br = null;
		PageLW page;
		try {
			br = new BufferedReader(new FileReader(source));

			String url;
			while ((url = br.readLine()) != null) {
				page = new PageLW();
				page.setPage(url);
				addPage(page);
				count++;
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		return count;
	}

	public void onShutdown() {
		// the program is shutting down, save to files

		// write pool to file
		PrintWriter writer;
		crawlerRunning.set(false);
		try {
			writer = new PrintWriter(storePath + File.separator + "urlPool",
					"UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}
		while (workerThreads.size() > 0) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			util.writeLog("Waiting for threads to exit ("
					+ workerThreads.size() + ")");
		}

		for (String s : urlPool.keySet())
			for (PageLW p : urlPool.get(s))
				writer.println(p.url);
		writer.close();

	}

	private long loadSeenURLs(File source) {

		if (source == null)
			return 0;

		if (source.exists()) {
			if (source.list() == null)
				return 0;
		} else
			System.err.println("Improper Page import directory set. DNE");

		long count = 0;

		for (String f : source.list()) {
			for (File file : new File(source.getAbsolutePath() + File.separator
					+ f).listFiles()) {

				if (file.isFile())
					seenUrls.add(file.getName().substring(0, 32));

			}
		}
		return count;
	}

	public void addPage(PageLW page) {
		if (notAddingNew)
			return;

		if (urlPool.size() >= maxDomainPerCrawl) {
			notAddingNew = true;
			return;
		}

		// does the domain exist?
		seenUrls.add(page.urlHash);
		if (!urlPool.containsKey(page.host)) {
			if (verbose)
				util.writeLog("Domain Found: " + page.host);
			urlPool.put(page.host, new Vector<PageLW>()); // if not add it
		}
		// add the PageLW to the right domain's Vector
		urlPool.get(page.host).add(page);
		++poolSize;
	}

	private PageLW getRandomURL() {
		Vector<PageLW> v;
		PageLW p;

		do {
			// get a random domain
			v = urlPool.get(urlPool.keySet().toArray()[rand.nextInt(urlPool
					.size())]);

		} while (v.size() < 0 && urlPool.size() > 0);

		p = v.remove(0);
		--poolSize;
		if (v.size() == 0) {
			urlPool.remove(p.host);
		}

		return p;
	}

	/**
	 * 
	 * @param maxDepth
	 *            The max depth the spider will search
	 */
	public Vector<String> crawl(int maxDepth, int maxThreads) {

		long lastPrint = 0;
		int workerThreadsWithTasks = 0;

		topoThread = new Thread(topo);
		topoThread.start();

		// Set up the worker threads
		workerThreads = new ConcurrentHashMap<PageWorkerThread, Thread>();
		crawlerRunning.set(true);
		while (crawlerRunning.get() || workerThreads.size() > 0) {

			if (crawlerRunning.get() && workerThreads.size() < maxThreads
					&& urlPool.size() > 0) {
				PageWorkerThread pwt = new PageWorkerThread(seenUrls, topo,
						seedHost, cachePath, storePath);
				Thread t = new Thread(pwt);
				workerThreads.put(pwt, t);
				t.start();
				workerThreadsWithTasks++;
			}

			// pwt - page worker thread
			for (Entry<PageWorkerThread, Thread> pwt : workerThreads.entrySet()) {

				// if our thread has stopped, report this and remove it
				if (pwt.getValue().getState() == Thread.State.TERMINATED
						|| System.currentTimeMillis()
								- pwt.getKey().lastUpdate.get() > (1000 * threadTimeout)) {
					util.writeLog("Worker Thread Crashed or Timed Out", true);
					pwt.getKey().interrupt();
					workerThreads.remove(pwt.getKey());

					if (--workerThreadsWithTasks == 0 && urlPool.size() == 0) {
						crawlerRunning.set(false);
					}

					continue;
				}

				// otherwise check to see if this thread has children to send
				// back
				Vector<PageLW> polledPage = pwt.getKey().pagesOut.poll();
				while (polledPage != null) {
					for (PageLW p : polledPage) {
						// if we haven't reached the max depth yet process
						// the children
						if (p.depth < maxDepth) {
							addPage(p);
						}
					}
					polledPage = pwt.getKey().pagesOut.poll();

					if (crawlerRunning.get() && poolSize == 0) {
						crawlerRunning.set(false);
					}
				}

				if (System.currentTimeMillis() - lastPrint > 1000) {
					lastPrint = System.currentTimeMillis();
					util.writeLog("Req: " + requestedPages.size() + "\tPool: "
							+ poolSize + "\tDIP: " + urlPool.size()
							+ "\tThrd: " + workerThreads.size());
				}

				// also mark the recieved urls as requested
				String recievedPage = pwt.getKey().completeUrls.poll();
				while (recievedPage != null) {
					requestedPages.add(recievedPage);
					recievedPage = pwt.getKey().completeUrls.poll();
				}

				// as long as this thread's in-queue isn't full
				if (crawlerRunning.get() && pwt.getKey().pagesIn.size() < 3
						&& urlPool.size() > 0)
					pwt.getKey().pagesIn.add(getRandomURL());// add a url to it

				// if we're shutting down and this thread's queue is done
				// working so kill it
				if (!crawlerRunning.get() && pwt.getKey().checkWaiting()) {
					pwt.getValue().interrupt();
				}

			}

		}

		// calculate and display the running time

		if (displayStatus) {
			util.writeLog("Crawl Complete");
			util.writeLog(requestedPages.size() + " pages retreived");
		}

		boolean go;
		do {
			topo.running = false;
			for (Entry<PageWorkerThread, Thread> pwt : workerThreads.entrySet()) {
				pwt.getKey().running = false;
				pwt.getValue().stop();
			}

			go = false;

			for (Entry<PageWorkerThread, Thread> pwt : workerThreads.entrySet()) {
				if (pwt.getValue().isAlive()) {
					go = true;
				}
			}

		} while (go);

		return requestedPages;
	}
}
