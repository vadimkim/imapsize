package ee.evrcargo.imap;

import ee.evrcargo.imap.task.BackupMailbox;
import ee.evrcargo.imap.task.CheckMailbox;
import ee.evrcargo.imap.task.RestoreMailbox;
import ee.evrcargo.imap.task.Task;

import javax.mail.MessagingException;

/**
 * Created by vadim.kimlaychuk on 10-Jul-17.
 */
public class Runner {

    private static Task task;

    public static void main(String[] args) throws MessagingException {
        long startTime = System.currentTimeMillis();
        if (args.length == 0) {
            System.out.println("IMAP mailbox backup/restore util.");
            System.out.println("Edit build/resources/mbox.properties file before executing commands");
            System.out.println("NB! Folders with size = 0 are excluded from processing. Calendar folder considered to be unnecessary and also not processed.");
            System.out.println("BSD License. Written by VK Copyright (c) 2017 EVR Cargo.");
            System.out.println();
            System.out.println("-b  backup mailbox");
            System.out.println("-c  check mailbox structure and calculate size");
            System.out.println("-r  restore mailbox");
            System.out.println();
            System.out.println("--config <filename> Path to mbox.properties file");
            System.out.println();
            System.out.println("Example: run.sh -c");
            System.exit(0);
        }
        String config = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-b":
                    task = new BackupMailbox(config);
                    break;
                case "-c":
                    task = new CheckMailbox(config);
                    break;
                case "-r":
                    task = new RestoreMailbox(config);
                    break;
                case "--config":
                    if (i == args.length-1) {
                        System.out.println("Missing config file");
                        System.exit(-1);
                    }
                    config = args[++i];
                    break;
                default:
                    System.out.println("Unknown option");
                    System.exit(-1);
            }
        }
        task.execute();
        System.out.println("Execution time: " + (System.currentTimeMillis() - startTime) / 1000.0 + " sec.");
        System.exit(0);
    }
}
