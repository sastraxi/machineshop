package com.sastraxi.machineshop.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.List;

import android.webkit.MimeTypeMap;

public class PathUtils {
	/**
	 * By default File#delete fails for non-empty directories, it works like
	 * "rm". We need something a little more brutual - this does the equivalent
	 * of "rm -r"
	 * 
	 * @param path
	 *            Root File Path
	 * @return true iff the file and all sub files/directories have been removed
	 * @throws FileNotFoundException
	 */
	public static boolean deleteRecursive(File path)
			throws FileNotFoundException {
		if (!path.exists())
			throw new FileNotFoundException(path.getAbsolutePath());
		boolean ret = true;
		if (path.isDirectory()) {
			for (File f : path.listFiles()) {
				ret = ret && PathUtils.deleteRecursive(f);
			}
		}
		return ret && path.delete();
	}

	public static String streamToString(java.io.InputStream is) {
		try {
			return new java.util.Scanner(is).useDelimiter("\\A").next();
		} catch (java.util.NoSuchElementException e) {
			return "";
		}
	}

	/**
	 * Note: only works with UNIX-style paths (/).
	 */
	public static File getDescendantFile(File file, String path) {

		// strip off slashes at the start or end.
		int start = 0;
		int end = path.length();
		if (path.endsWith("/"))
			end -= 1;
		if (path.startsWith("/"))
			start += 1;
		path = path.substring(start, end);

		for (String part : path.split("/")) {
			file = new File(file, part);
		}
		return file;

	}

	public static String pathFrom(String path, String root) {
		if (!path.startsWith(root))
			return path;
		String rp = path.substring(root.length());
		if (rp.startsWith("/")) {
		    rp = rp.substring(1);
		}
		return rp;
	}

	public static String getExtension(String fileName) {
		if (fileName == null)
			return null;
		if (fileName.lastIndexOf(".") != -1) {
			return fileName.substring(fileName.lastIndexOf(".") + 1);
		}
		return null;
	}

    public static String[] pathParts(String path) {
        if (path == null) {
            return new String[] {};
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path.split("/", -1);
    }

    public static String fromParts(List<String> parts) {
        StringBuilder path = new StringBuilder("/");
        for (Iterator<String> it = parts.iterator(); it.hasNext();) {
            path.append(it.next());
            if (it.hasNext()) {
                path.append("/");
            }
        }
        return path.toString();       
    }

    public static String joinPath(String a, String b) {
        if (a.endsWith("/")) {
            return a + b;
        } else {
            return a + "/" + b;
        }
    }

    public static String getMimeType(File file) {
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String ext = MimeTypeMap.getFileExtensionFromUrl("http://localhost/" + file.getName());
        if (ext.equals("js")) return "text/javascript";
        if (ext.equals("cs")) return "text/x-csharp";
        if (!mime.hasExtension(ext)) return "text/plain";
        return mime.getMimeTypeFromExtension(ext);
    }

}