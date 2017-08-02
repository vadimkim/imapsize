package ee.evrcargo.imap.tree;

public class FolderPath {
    String path;
    int depth;

    public FolderPath(String path, int depth) {
        this.path = path;
        this.depth = depth;
    }

    public String getPath() {
        return path;
    }

    public int getDepth() {
        return depth;
    }

}
