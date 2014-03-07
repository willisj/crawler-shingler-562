package crawler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TopologySQLController implements Runnable {

	// "jdbc:mysql://localhost/test?user=monty&password=greatsqldb"
	static Connection conn = null;
	static int MINSENDSIZE = 50;
	Queue<String> queriesToSend = new ConcurrentLinkedQueue<String>();
	private boolean useSQL = true;
	File sqlFile;

	TopologySQLController(File sqlFile) {
		this.sqlFile = sqlFile;
		useSQL = false;
	}

	TopologySQLController(String connectionString) {
		try {
			conn = DriverManager.getConnection(connectionString);
		} catch (SQLException ex) {
			// handle any errors
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		}
	}

	public void run() {
		String q;

		if (useSQL) {
			Statement st;
			int counter = 0;

			String burst = "";
			try {
				st = conn.createStatement();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return;
			}
			while (true) {

				q = queriesToSend.poll();
				if (q != null) {
					burst = q + "; ";
					++counter;
					if (counter >= MINSENDSIZE) {
						try {
							st.execute(burst);

						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				}
			}
		} else {

			PrintWriter writer;
			try {
				writer = new PrintWriter(sqlFile.toPath().toString());
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				return;
			}
			try {
				while (true) {
					q = queriesToSend.poll();
					if (q != null) {
						writer.println(q + "; ");
					}

				}
			} finally {
				writer.close();
			}

		}
	}

	public void addURL(URL url, boolean explored) {
		queriesToSend.add(getQueryInsertIgnoreDomain(getDomain(url)));
		queriesToSend.add(getQueryInsertReplaceURL(url, explored));
	}

	public void markURLExplored(URL url) {
		queriesToSend.add(getQueryUpdateURLStatus(url, true));
	}

	public void createChildAndLink(URL parent, URL child) {
		queriesToSend.add(getQueryInsertIgnoreDomain(getDomain(child)));
		queriesToSend.add(getQueryInsertReplaceURL(child, false));
		queriesToSend.add(getQueryInsertIgnoreLink(parent, child));
	}

	public void createChildrenAndLink(URL parent, Set<URL> children) {
		queriesToSend.add(getQueryUpdateURLStatus(parent, true));
		for (URL child : children) {
			queriesToSend.add(getQueryInsertIgnoreDomain(getDomain(child)));
			queriesToSend.add(getQueryInsertReplaceURL(child, false));
			queriesToSend.add(getQueryInsertIgnoreLink(parent, child));
		}

	}

	// * sql constructions below

	private static String getQueryInsertIgnoreLink(URL parent, URL child) {
		return "INSERT INTO links (`src`,`dst`) VALUES(("
				+ getQuerySelectURLID(parent) + "),("
				+ getQuerySelectURLID(child) + "))";
	}

	private static String getQuerySelectURLID(URL url) {
		return "SELECT urlid FROM urls WHERE url='" + fixURL(url) + "' LIMIT 1";
	}

	private static String getQueryUpdateURLStatus(URL url, boolean explored) {
		return "UPDATE urls SET explored=" + (explored ? 1 : 0)
				+ " WHERE url='" + fixURL(url) + "'";
	}

	private static String getQueryInsertIgnoreDomain(String domain) {
		return "INSERT IGNORE INTO domains (`domain`) VALUES('" + domain + "')";
	}

	private static String getQueryGetDomainID(String domain) {
		return "SELECT domainid FROM domains WHERE domain='" + domain
				+ "'  LIMIT 1";
	}

	private static String getQueryInsertReplaceURL(URL url, boolean explored) {
		return "REPLACE INTO urls (`url`,`domainid`,`explored`) VALUES('"
				+ fixURL(url) + "',(" + getQueryGetDomainID(getDomain(url))
				+ "), " + (explored ? "1" : "0") + ")";
	}

	public static String getDomain(URL url) {
		return url.getAuthority();
	}

	public int getBufferSize() {
		return queriesToSend.size();
	}

	public static String fixURL(URL url) {

		return url.toString().replace('\'', '"');
	}
}
