package ee.evrcargo.imap.tree;

import ee.evrcargo.imap.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class FolderTree implements Tree {
    private List<FolderPath> mailboxMap = new ArrayList<>();
    private Properties conf = Configuration.getInstance().getProps();

    @Override
    public List<FolderPath> build() {
        System.out.println("Reading local folder structure...");
        String basePath = conf.getProperty("mailbox.restore.base");
        dumpFolder(basePath, 0);

        // Filter list according to filter patterns
        String[] patterns = conf.getProperty("mailbox.ignored.folders").split(",");
        List<String> folderFilters = Arrays.stream(patterns)
                .map(String::trim)
                .collect(Collectors.toList());

        mailboxMap.removeIf(path -> folderFilters.contains(path.getPath()));
        return mailboxMap;
    }

    /**
     * Create folder structure to restore
     *
     * @param diskFolder - local folder name
     */
    private void dumpFolder(String diskFolder, int depth) {
        try {

            // Check if there are any subfolder to explore
            List<String> existFolders = Files.walk(Paths.get(diskFolder), 1)
                    .skip(1L)
                    .filter(Files::isDirectory)
                    .map(Path::toFile)
                    .map(File::getName)
                    .collect(Collectors.toList());

            // Copy to output list
            for (int i = 0; i < existFolders.size(); i++) {
                String base = diskFolder.replace(conf.getProperty("mailbox.restore.base"), "");
                FolderPath fp;
                if (base.length() == 0) {
                    fp = new FolderPath(existFolders.get(i), depth);
                } else {
                    fp = new FolderPath(base.substring(1) + File.separator + existFolders.get(i), depth);
                }
                mailboxMap.add(fp);
            }

            if (existFolders.size() > 0) {
                for (String folder : existFolders) {
                    dumpFolder(diskFolder + File.separator + folder, depth + 1);
                }
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
