package com.pi.client.database.webfiles;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.pi.client.Client;
import com.pi.client.database.Paths;

public class GraphicsLoader {
	private static Map<Integer, Float> getCurrentVersions(File filelistCurrent)
			throws IOException {
		HashMap<Integer, Float> currentVersions = new HashMap<Integer, Float>();
		if (filelistCurrent.exists()) {
			BufferedReader reader = new BufferedReader(new FileReader(
					filelistCurrent));
			while (reader.ready()) {
				String[] line = reader.readLine().split("\t");
				String cleanedName = line[0];
				for (String s : Paths.imageFiles)
					cleanedName = cleanedName.replace("." + s, "");
				try {
					Integer name = Integer.valueOf(cleanedName);
					float ver;
					ver = Float.valueOf(line[1]);
					if (Paths.getGraphicsFile(name) != null) {
						currentVersions.put(name, ver);
					}
				} catch (Exception e) {
					continue;
				}
			}
			reader.close();
		}
		return currentVersions;
	}

	public static void load(Client client) {
		try {
			File filelistNew = new File(Paths.getGraphicsDirectory(),
					"filelist_new");
			File filelistCurrent = new File(Paths.getGraphicsDirectory(),
					"filelist");
			client.getLog().info("Downloading filelist...");
			download(new URL(ServerConfiguration.fileList), filelistNew);
			Map<Integer, Float> currentVersions = getCurrentVersions(filelistCurrent);
			BufferedReader reader = new BufferedReader(new FileReader(
					filelistNew));
			while (reader.ready()) {
				String[] line = reader.readLine().split("\t");
				String rawName = line[0];
				String cleanedName = rawName;
				for (String s : Paths.imageFiles)
					cleanedName = cleanedName.replace("." + s, "");
				Integer name = Integer.valueOf(cleanedName);
				float ver;
				try {
					ver = Float.valueOf(line[1]);
				} catch (Exception e) {
					client.getLog().printStackTrace(e);
					continue;
				}
				Float cVer = currentVersions.get(name);
				if (cVer == null || cVer.floatValue() < ver) {
					client.getLog()
							.info(name + " is outdated.  Upgrading to version "
									+ ver);
					try {
						File dest = new File(Paths.getGraphicsDirectory(),
								rawName);
						if (!dest.exists())
							dest.createNewFile();
						download(new URL(ServerConfiguration.graphicsFolder
								+ name), dest);
					} catch (IOException e) {
						client.getLog().printStackTrace(e);
					}
				}
			}
			reader.close();
			filelistCurrent.delete();
			filelistNew.renameTo(filelistCurrent);
			client.getLog().info("Finished checking graphic versions!");
		} catch (NumberFormatException e) {
			client.getLog()
					.severe("There appears to be a number format error server side!  Please report this.");
		} catch (Exception e) {
			client.getLog().printStackTrace(e);
			client.getLog().info("Failed to check for graphical updates!");
		}
	}

	private static void download(URL url, File dest) throws IOException {
		BufferedInputStream in = null;
		FileOutputStream fout = null;
		File f = new File(dest.getAbsolutePath() + ".part");
		f.createNewFile();
		try {
			in = new BufferedInputStream(url.openStream());
			fout = new FileOutputStream(f);
			byte data[] = new byte[1024];
			int count;
			while ((count = in.read(data, 0, 1024)) != -1) {
				fout.write(data, 0, count);
			}
		} finally {
			if (in != null)
				in.close();
			if (fout != null)
				fout.close();
		}
		dest.delete();
		f.renameTo(dest);
	}
}
