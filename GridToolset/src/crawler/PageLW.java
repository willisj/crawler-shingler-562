package crawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

public class PageLW implements Serializable {
	public String title;
	public String anchorText;
	public String url;
	public String urlHash;
	public String host; // todo: refactor this to 'host'
	public String source;
	public String cleanSource;
	public int depth = 0;
	private static final long serialVersionUID = 2L;

	public Vector<PageLW> children = new Vector<PageLW>();

	public void setPage(String url, String authority) {
		this.url = url;
		this.host = authority;
		this.urlHash = util.md5(url);
	}

	public void setPage(String url, String authority, PageLW parent) {
		this.url = url;
		this.urlHash = util.md5(url);
		this.host = authority;
		if (parent.host == authority) {
			depth = parent.depth + 1;
		}
	}

	public void setSource(String title, String source, Vector<PageLW> children) {
		this.title = title;
		this.source = source;
		this.children = children;

	}

	public boolean save(String storePath) {
		String path = storePath + File.separator + host
				+ File.separator;
		String filename = urlHash + ".pgf"; // no slashes

		if (!new File(path).exists()) {
			if (!new File(path).mkdirs()) {
				util.writeLog("Unable to create directory for domain: " + path);
			}
		}

		try {
			FileOutputStream fout = new FileOutputStream(path + filename);
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(this);
			oos.close();

		} catch (Exception ex) {
			util.writeLog("Error writing page to file: " + path + filename);
			return false;
		}
		return true;
	}

	static public PageLW load(File path) {

		PageLW page = null;
		try {

			FileInputStream fin = new FileInputStream(path);
			ObjectInputStream ois = new ObjectInputStream(fin);
			page = (PageLW) ois.readObject();
			ois.close();

		} catch (Exception ex) {
			//path.delete();
			util.writeLog(
					"Found file with stream corruption: " + path.toString(),
					true);
			return null;
		}
		return page;
	}

	public void setPage(String url) throws MalformedURLException {
		URL u = new URL(url);
		this.url = url;
		this.urlHash = util.md5(url);
		this.host = u.getHost();

	}

	public void setPage(String link, String authority, PageLW parent,
			String anchorText) {
		this.anchorText = anchorText;
		setPage(link, authority, parent);

	}

}
