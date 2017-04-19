package utils;

import model.ObexFolder;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public class PathUtils {

    public PathUtils() {}

    public static String preparePath(String path) {
        path = path.replace('\\', '/'); //
        if (path.startsWith("a:/")) {
            path = path.replaceFirst("a:", "");
        }
        return removeLastSlash(path);
    }

    public static String removeLastSlash(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public static String getRelativePath(final String absolutePath, final String actualFolder) {
        StringBuilder newPath = new StringBuilder();
        boolean notEq = false;
        String newFolderList[] = absolutePath.split("/");
        String actFolderList[] = actualFolder.split("/");
        int j = actFolderList.length;
        for (int i = 0; i < actFolderList.length; i++) {
            String actFolder = actFolderList[i];
            String newFolder = i < newFolderList.length ? newFolderList[i] : null;
            if (notEq || !actFolder.equalsIgnoreCase(newFolder)) {
                newPath.append("../");
                if (!notEq) {
                    notEq = true;
                    j = i;
                }
            }
        }
        for (; j < newFolderList.length; j++) {
            newPath.append(newFolderList[j]).append("/");
        }
        return removeLastSlash(newPath.toString());
    }

    public static String getLastFolder(String path) {
        path = removeLastSlash(path);
        path = path.substring(path.lastIndexOf("/") + 1, path.length());
        return path;
    }

    public static String removeLastFolder(String path) {
        path = removeLastSlash(path);
        int n = path.lastIndexOf("/");
        if (n > 0) {
            path = path.substring(0, n);
        }
        return path;
    }

    public static ObexFolder createSimbolicFolderTree(String absolutePath) {
        if (!(absolutePath.startsWith("/") || absolutePath.startsWith("a:"))) {
            absolutePath = "a:/" + absolutePath;
        }

        ObexFolder folder = ObexFolder.ROOT_FOLDER;
        String pathList[] = absolutePath.split("/");
        for (int i = 0; i < pathList.length; i++) {
            if (!pathList[i].startsWith("a:")) {
                folder = folder.addFolder(pathList[i]);
            }
        }
        return folder;
    }
}
