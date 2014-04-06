/*
 * -- MultiTool --
 * 
 * This class represents the main interface 
 * to the other tools in the toolkit.
 * 
 * -- Crawler
 * -- Shingler
 * -- Comparator
 * 
 * */

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import crawler.PageLW;
import toolset.CrawlerIface;
import toolset.ShingleComparator;
import toolset.Shingler;

/**
 * Add your name if you make edits.
 * 
 * @author Jordan Willis
 * 
 */
public class MultiTool {
	// TODO: implement a main to handle args and call the correct

	public static void main(String[] args) {

		final String helpBlock = "Crawler Shingler Tool -- V 0.1 \n\n"
				+ "\tUSAGE\n"
				+ "\t\tjava -jar MultiTool.jar crawl <sessionPath> <http://seedDomain> <pagesPerCrawl> \n"
				+ "\t\tjava -jar MultiTool.jar shingle <path-to-file>.pgf \n"
				+ "\t\tjava -jar MultiTool.jar info <path-to-file>.pgf \n"
				+ "\t\tjava -jar MultiTool.jar compare <path-to-file-1> <path-to-file-2>";

		if (args.length > 0) {

			if (args[0].equals("crawl")) {

				if (new File(args[1]).exists()) // check that the session path
												// doesn't exist
					System.err.println("Error: store path already exists \""
							+ args[1] + "\"");
				else {
					new File(args[1]).mkdirs(); // create the store directory

					try {
						CrawlerIface.startCrawler(args[1], args[2], args[1]
								+ "/foundDomains.txt", 1, 5,
								Integer.parseInt(args[3]));
					} catch (MalformedURLException e) {
						System.err.println("Bad seed URL.");
					}
				}

			} else if (args[0].equals("shingle")) {

				// SHINGLE MODE
				if (!new File(args[1]).exists()) // check that the file exists
					System.err.println("Error: file not found \"" + args[1]
							+ "\"");
				else if (Integer.valueOf(args[2]) <= 0)
					System.err.println("Error: invalid shingle size");
				else {
					PageLW page = PageLW.load(new File(args[1]));
					String[] shingles = Shingler.shingle(page.cleanSource,
							Integer.valueOf(args[2]));
					if (shingles == null || shingles.length == 0)
						return;

					for (String s : shingles)
						System.out.println(s);
				}

			} else if (args[0].equals("info")) {

				// SHINGLE MODE
				if (!new File(args[1]).exists()) // check that the file exists
					System.err.println("Error: file not found \"" + args[1]
							+ "\"");
				else {
					PageLW page = PageLW.load(new File(args[1]));
					System.out.println(page.url + "\t\"" + page.title + "\"");
				}
			} else if (args[0].equals("compare")) {
				// SHINGLE MODE
				if (!new File(args[1]).exists()) // check that the file exists
					System.err.println("Error: file not found \"" + args[1]
							+ "\"");
				else if (!new File(args[2]).exists()) // check that the file
														// exists
					System.err.println("Error: file not found \"" + args[2]
							+ "\"");
				else
					try {
						System.out.println(ShingleComparator.compare(
								readFile(args[1], StandardCharsets.UTF_8)
										.split("\\n"),
								readFile(args[2], StandardCharsets.UTF_8)
										.split("\\n")));
					} catch (IOException e) {
						System.err
								.println("There was a problem reading from file: "
										+ e.getMessage());
					}
			} else
				System.out.println(helpBlock);
		} else
			System.out.println(helpBlock);

	}

	// http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file
	static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}

}
