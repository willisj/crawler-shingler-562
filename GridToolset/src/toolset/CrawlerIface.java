package toolset;

import java.net.MalformedURLException;

import crawler.Crawler;
import crawler.PageLW;

public class CrawlerIface {
	/**
	 * Crawler Planning
	 * 
	 * POOLS: URLPOOL - urls to be downloaded SEENPOOL - urls already seen, do
	 * not re-queue these
	 * 
	 * @throws MalformedURLException
	 * 
	 * */

	public static void startCrawler(String storePath, String urlPoolFile,
			String seenURLsPool, int maxDepth, int threads)
			throws MalformedURLException {
		final int maxDomainPerCrawl = 5;

		Crawler crawler = new Crawler(null, storePath, urlPoolFile, true, false,
				maxDomainPerCrawl, null);
		
		crawler.crawl(maxDepth, threads);
	}
}
