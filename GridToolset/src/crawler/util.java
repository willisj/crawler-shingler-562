package crawler;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import utilities.TaskDistributor;
import utilities.WorkerThread;
import crawler.PageLW;

public class util {
	public static AtomicBoolean pauseOutput = new AtomicBoolean(false);
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	// http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file
	static public String readFile(String path, Charset encoding)
			throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}

	// http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
	public static String bytesToHex(byte[] bytes) {

		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static long folderSize(String s) {
		return folderSize(new File(s));
	}

	// http://stackoverflow.com/questions/2149785/size-of-folder-or-file
	public static long folderSize(File source) {
		long length = 0;
		for (File file : source.listFiles()) {
			if (file.isFile())
				length += file.length();
			else
				length += folderSize(file);
		}
		return length;
	}

	public static void writeLog(String s) {
		writeLog(s, false);
	}

	public static void writeLog(String s, boolean error) {
		if (pauseOutput.get())
			return;
		// s = timer.toString() + "\t" + s;

		if (error)
			System.err.println(s);
		else
			System.out.println(s);
	}

	public static String md5(String s) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");

			return util.bytesToHex(md.digest(s.getBytes()));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static ArrayList<File> loadFiles(String pathToDomainFolder, int max) {
		ArrayList<File> pages = new ArrayList<File>();
		File source = new File(pathToDomainFolder);

		if (!source.exists())
			System.err.println("Improper Page import directory set. DNE: "
					+ source.toString());

		long count = 0;
		for (File f : source.listFiles()) {
			if (f.isFile()) {
				pages.add(f.getAbsoluteFile());
				++count;

				if (count >= max)
					break;
			}
		}
		return pages;
	}

	public static PageLW[] loadAllFiles(String pathToStore, int max, int threads) {
		util.writeLog("Enumerating File List");
		ArrayList<File> files = new ArrayList<File>();
		File source = new File(pathToStore);

		if (!source.exists())
			System.err.println("Improper Page import directory set. DNE: "
					+ source.toString());

		for (File f : source.listFiles()) {
			if (!f.isFile()) {
				files.addAll(loadFiles(f.getName(), max - files.size()));

				if (files.size() >= max)
					break;
			}
		}

		PageLW[] pages;
		util.writeLog("Loading Files");
		pages = bulkLoadfiles(files, threads);

		return pages;
	}

	public static PageLW[] loadAllFilesInDomain(String pathToDomainDir,
			int threads) {
		util.writeLog("Enumerating File List");
		ArrayList<File> files = new ArrayList<File>();

		files.addAll(loadFiles(pathToDomainDir, Integer.MAX_VALUE));

		PageLW[] pages;
		util.writeLog("Loading Files");
		pages = bulkLoadfiles(files, threads);

		return pages;
	}

	public static PageLW[] bulkLoadfiles(ArrayList<File> filePaths, int threads) {

		WorkerThread<File, PageLW> w = new PageLWLoader();
		TaskDistributor<File, PageLW, PageLWLoader> dist = new TaskDistributor<File, PageLW, PageLWLoader>(
				threads, w);

		dist.inQueue.addAll(filePaths);
		dist.exitOnEmpty();

		util.writeLog("Waiting for threads to work");
		dist.work();
		return dist.outQueue.toArray(new PageLW[dist.outQueue.size()]);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static LinkedHashMap sortHashMapByValuesD(HashMap passedMap) {
		List mapKeys = new ArrayList(passedMap.keySet());
		List mapValues = new ArrayList(passedMap.values());
		Collections.sort(mapValues);
		Collections.sort(mapKeys);

		LinkedHashMap sortedMap = new LinkedHashMap();

		Iterator valueIt = mapValues.iterator();
		while (valueIt.hasNext()) {
			Object val = valueIt.next();
			Iterator keyIt = mapKeys.iterator();

			while (keyIt.hasNext()) {
				Object key = keyIt.next();
				String comp1 = passedMap.get(key).toString();
				String comp2 = val.toString();

				if (comp1.equals(comp2)) {
					passedMap.remove(key);
					mapKeys.remove(key);
					sortedMap.put((String) key, (Double) val);
					break;
				}

			}

		}
		return sortedMap;
	}
}
