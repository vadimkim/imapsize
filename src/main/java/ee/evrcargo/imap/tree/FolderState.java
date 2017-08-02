package ee.evrcargo.imap.tree;

public class FolderState {
    private long size = 0L;
    private int currentImap;
    private int currentFile;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getCurrentImap() {
        return currentImap;
    }

    public void setCurrentImap(int currentImap) {
        this.currentImap = currentImap;
    }

    public int getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(int currentFile) {
        this.currentFile = currentFile;
    }
}
