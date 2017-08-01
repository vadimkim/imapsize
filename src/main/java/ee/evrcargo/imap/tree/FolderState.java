package ee.evrcargo.imap.tree;

public class FolderState {
    private long size = 0L;
    private int current;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }
}
