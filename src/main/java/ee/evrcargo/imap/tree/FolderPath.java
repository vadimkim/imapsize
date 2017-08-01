package ee.evrcargo.imap.tree;

public class FolderPath {
    String path;
    int depth;
    int msgCount;

    public FolderPath(String path, int depth, int msgCount) {
        this.path = path;
        this.depth = depth;
        this.msgCount = msgCount;
    }

    public String getPath() {
        return path;
    }

    public int getDepth() {
        return depth;
    }

    public int getMsgCount() {
        return msgCount;
    }
}
