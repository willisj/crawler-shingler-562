package crawler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public class TopologyOutputController implements Runnable {

	static Connection conn = null;
	static int MINSENDSIZE = 50;
	public boolean running;

	Queue<String> seenLinksToProcess = new ConcurrentLinkedQueue<String>();
	Queue<String> newLinksToProcess = new ConcurrentLinkedQueue<String>();
	Queue<String> newURLsToProcess = new ConcurrentLinkedQueue<String>();
	HashMap<String, String> seen = new HashMap<String, String>();
	File linkFile;
	File urlFile;

	TopologyOutputController(File linkFile, File urlFile) {
		this.linkFile = linkFile;
		this.urlFile = urlFile;

		//loadSeenLinks(urlFile);
	}

	public void run() {
		String q;
		running = true;
		PrintWriter linkWriter, urlWriter;
		try {
			linkWriter = new PrintWriter(new FileOutputStream(linkFile), true);
			urlWriter = new PrintWriter(new FileOutputStream(urlFile), true);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		}

		try {
			while (running) {
				q = seenLinksToProcess.poll();
				if (q != null) {
					seen.put(hashURL(q), q);
				}

				q = newLinksToProcess.poll();
				if (q != null) {
					linkWriter.println(q);
				}

				q = null;
				q = newURLsToProcess.poll();
				if (q != null) {
					urlWriter.println(q);
				}
			}
		} finally {
			linkWriter.close();
			urlWriter.close();
		}

	}

	private String hashURL(String u) {
		MessageDigest digest;
		
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			digest = null;
		}
		return util.bytesToHex(digest.digest(u.getBytes()));
	}

	public void addURL(URL url) {
		newURLsToProcess.add(url.toString());
	}

	public String hashURL(URL u) {
		MessageDigest digest;
		
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			digest = null;
		}
		return util.bytesToHex(digest.digest(u.toString().getBytes()));
	}

	public void addLink(URL parent, URL child) {
		newLinksToProcess.add(parent + "\t" + child);
	}


	// * sql constructions below

	public static String getDomain(URL url) {
		return url.getAuthority();
	}

	public int getBufferSize() {
		return newLinksToProcess.size();
	}

	public void loadSeenLinks(File file) {
		String[] urllist;
		try {
			urllist = util.readFile(file.toString(), Charset.defaultCharset())
					.split("\n");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		for (String s : urllist)
			seen.put(hashURL(s), s);
		util.writeLog(seen.size() + " seen URLs loaded");
	}
}
