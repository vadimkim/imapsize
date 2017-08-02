package ee.evrcargo.imap.tree;

import ee.evrcargo.imap.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class FolderTree implements Tree {
    private List<FolderPath> mailboxMap = new ArrayList<>();
    private Configuration conf;

    public FolderTree (Configuration conf) {
        this.conf = conf;
    }

    @Override
    public List<FolderPath> build() {
        System.out.println("Reading local folder structure...");
        String basePath = conf.getProperty("mailbox.restore.base");
        dumpFolder(basePath, 0);
        return mailboxMap;
    }

    /**
     * Create folder structure to restore
     *
     * @param diskFolder - local folder name
     */
    private void dumpFolder(String diskFolder, int depth) {
        try {
            String[] patterns = conf.getProperty("mailbox.ignored.folders").split(",");
            Set<String> folderFilters = new HashSet<>(Arrays.asList(patterns));

            // Check if there are any subfolder to explore
            List<String> existFolders = Files.walk(Paths.get(diskFolder), 1)
                    .skip(1L)
                    .filter(Files::isDirectory)
                    .filter(p -> !folderFilters.contains(p.toFile().getName().trim())) // Filter ignored folders
                    .map(Path::toFile)
                    .map(File::getName)
                    .collect(Collectors.toList());


            // Copy to output list
            for (int i=0; i< existFolders.size(); i++) {
                String base = diskFolder.replace(conf.getProperty("mailbox.restore.base"), "");
                FolderPath fp;
                if (base.length() == 0) {
                    fp = new FolderPath(existFolders.get(i), depth);
                } else {
                    fp = new FolderPath(base.substring(1) + "/" + existFolders.get(i), depth);
                }
                mailboxMap.add(fp);
            }

            if (existFolders.size() > 0) {
                for (String folder : existFolders) {
                    dumpFolder(diskFolder + "/" + folder, depth + 1);
                }
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
