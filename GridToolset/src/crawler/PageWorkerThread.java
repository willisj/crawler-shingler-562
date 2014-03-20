package crawler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.io.File;

import norbert.NoRobotClient;

import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

public class PageWorkerThread extends Thread {
	static NoRobotClient rob = new NoRobotClient("");
	ConcurrentLinkedQueue<PageLW> pagesIn = new ConcurrentLinkedQueue<PageLW>();
	ConcurrentLinkedQueue<Vector<PageLW>> pagesOut = new ConcurrentLinkedQueue<Vector<PageLW>>();
	ConcurrentLinkedQueue<String> completeUrls = new ConcurrentLinkedQueue<String>();
	ConcurrentSkipListSet<String> seenUrls;
	TopologyOutputController topo;
	private AtomicBoolean waitingForUrls = new AtomicBoolean(true);
	public AtomicLong lastUpdate = new AtomicLong();

	// CONSTANTS
	final boolean behaviourSkipLinksWithSubdomain = true;
	final boolean behaviourStayInDomain = false;
	final boolean behaviourSkipOnCacheMiss = false;
	final boolean behaviourCacheRequests = false;
	final boolean verbose = false;
	public boolean running = true;
	final String cachePath;
	final String seedHost;
	final String storePath;
	final String[] behaviourDisallowedExtensions = ("jpg gif png pdf exe zip tar.gz gz tar")
			.split("\b");

	PageWorkerThread(ConcurrentSkipListSet<String> seenUrls,
			TopologyOutputController topo, String seedHost, String cachePath,
			String storePath) {
		this.storePath = storePath;
		this.seedHost = seedHost;
		this.seenUrls = seenUrls;
		this.cachePath = cachePath;
		this.topo = topo;
		pagesIn = new ConcurrentLinkedQueue<PageLW>();
		lastUpdate.set(System.currentTimeMillis() + 10 * 1000); // give 3 sec
																	// for the
																	// thread to
																	// check
	}

	public boolean checkWaiting() {
		return waitingForUrls.get();
	}

	public void run() {
		PageLW page;
		while (running) {

			try {
				waitingForUrls.set(true);
				lastUpdate.set(System.currentTimeMillis());
				page = pagesIn.poll();
				if (page != null) {
					URL url = requestPage(page);
					if (url == null)
						continue; // ToDo: count this

					if (!findChildren(page, url)) {
						util.writeLog("findChildren failed for " + page.url);
						continue;
					}

					completeUrls.add(page.url);
				} else {
					Thread.sleep(500);
				}
			} catch (InterruptedException e) {
				util.writeLog("Worker Thread Interupted", true);
				return;
			}
		}
	}

	private boolean findChildren(PageLW page, URL url) {
		// set properties for the HTML parser
		CleanerProperties prop = new CleanerProperties();
		prop.setTranslateSpecialEntities(true);
		prop.setTransResCharsToNCR(true);
		prop.setOmitComments(true);

		// create the parser
		HtmlCleaner hc = new HtmlCleaner(prop);

		// parse the page
		TagNode root = hc.clean(page.source);

		// find the page title
		// TODO: make this faster, look at body/title directly
		TagNode[] tags = root.getElementsByName("title", true);
		if (tags.length == 1) {
			page.title = tags[0].getText().toString();
		}

		// find all <a> tags
		tags = root.getElementsByName("a", true);

		// traverse the list of a tags and grab all links
		for (int i = 0; tags != null && i < tags.length; i++) {
			String link = tags[i].getAttributeByName("href");
			String rel = tags[i].getAttributeByName("rel");

			if (rel != null && rel.compareTo("nofollow") == 0)
				continue;

			String anchorText = tags[i].getText().toString();
			if (link != null && link.length() > 0) {

				// first we fix the link to make sure it's absolute
				link = fixLink(url, link);
				URL childURL;
				try {
					childURL = new URL(link);
				} catch (MalformedURLException e) {
					continue;
				}

				if (childURL != null) {
					topo.addLink(url, childURL);
					String[] urlParts;
					if (behaviourSkipLinksWithSubdomain) {
						String domain = childURL.getAuthority();
						if (domain == null)
							continue;

						urlParts = domain.split("\\.");
						if (urlParts.length > 3
								|| urlParts.length < 2
								|| (urlParts.length == 3 && !urlParts[0]
										.equalsIgnoreCase("www")))
							continue;
					}

					PageLW childPage = new PageLW();

					if (urlParts[urlParts.length - 1].contains(":"))
						childPage
								.setPage(
										link,
										urlParts[urlParts.length - 2]
												+ "."
												+ urlParts[urlParts.length - 1]
														.substring(
																0,
																urlParts[urlParts.length - 1]
																		.indexOf(':')),
										page);

					childPage.setPage(link, urlParts[urlParts.length - 2] + "."
							+ urlParts[urlParts.length - 1], page, anchorText);

					if (behaviourStayInDomain
							&& !childPage.host.equals(seedHost))
						continue;
					// then we test to see if we wish to add this link to our
					// pool
					if (!testLink(childPage, childURL))
						continue;
					page.children.add(childPage);
				}
			}

		}
		pagesOut.add(page.children);
		page.save(storePath);
		return true;
	}

	private String fixLink(URL baseURL, String link) {
		// filter mailto links out
		if (link.startsWith("mailto:") || link.startsWith("javascript:"))
			return null;

		// these are already absolute
		if (link.startsWith("http") || link.startsWith("https"))
			return link;

		// just missing the protocol
		if (link.startsWith("www.")) {
			return "http://" + link;
		}

		// check that this isn't an explicitly disallowed extension
		// ToDo: record this statistic
		for (String s : behaviourDisallowedExtensions) {
			if (link.endsWith(s))
				return null;
		}

		// this is a link that is relative to the baseurl
		if (link.startsWith("/")) {
			return baseURL.getProtocol() + "://" + baseURL.getAuthority()
					+ link;
		}

		// otherwise we assume the link is relative to the current path
		return baseURL.toString() + "/" + link;
	}

	private boolean testLink(PageLW p, URL url) {
		if (p.url == null || p.url == "" || !rob.isUrlAllowed(url)
				|| seenUrls.contains(p.url))
			return false;
		return true;
	}

	private URL requestPage(PageLW page) {

		URL url;
		try {
			url = new URL(page.url);
		} catch (MalformedURLException e2) {
			return null;
		}

		if (!behaviourCacheRequests || !loadCache(page)) {
			if (behaviourSkipOnCacheMiss)
				return null;

			BufferedReader in;
			String inputLine;
			StringBuffer response = new StringBuffer();

			try { // open the stream to the URL
				in = new BufferedReader(new InputStreamReader(url.openStream()));
			}

			catch (IOException | IllegalArgumentException e1) {
				if (verbose)
					util.writeLog("Failed to open stream to URL:" + page.url,
							true);
				return null;
			}

			try { // iterate over input stream until EOF
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine + "\n");
				}
				in.close();
			} catch (IOException e) {

				util.writeLog("Failed to read page source for URL:" + page.url,
						true);
				return null;
			}

			page.source = response.toString(); // set the page source
			page.cleanSource = stripHtml(page.source);

			if (behaviourCacheRequests)
				storeCache(page);
		}
		return url;
	}

	public boolean storeCache(PageLW page) {
		if (page.source == null)
			return false;

		PrintWriter out;
		try {
			out = new PrintWriter(cachePath + page.urlHash);
			out.write(page.source);
		} catch (FileNotFoundException e) {
			return false;
		}

		out.close();
		return true;
	}

	/**
	 * Get the page from cache
	 * 
	 * @return true if successful cache hit, false otherwise
	 */
	public boolean loadCache(PageLW page) {

		try {
			page.source = util.readFile(cachePath + page.urlHash,
					Charset.defaultCharset());
		} catch (IOException e) {
			return false; // file not found
		}
		return true;
	}

	// htmlCharFilter.
	// http://massapi.com/class/ht/HTMLStripCharFilter.html
	public static String stripHtml(String src) {
		HTMLStripCharFilter htmlCharFilter = new HTMLStripCharFilter(
				new StringReader(src));
		StringBuilder out;

		out = new StringBuilder();
		char[] cbuf = new char[1024 * 10];
		try {
			while (true) {
				int count;

				count = htmlCharFilter.read(cbuf);

				if (count == -1)
					break; // end of stream mark is -1
				if (count > 0)
					out.append(cbuf, 0, count);
			}

			htmlCharFilter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out.toString();
	}

}
