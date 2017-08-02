# imapsize
This software is insipred by http://www.broobles.com/imapsize. It has some key differences from the original:
* console application
* runs on any platform where JRE 1.8 is installed
* understands UTF-8 IMAP folder names and restores them correctly
* understands folder UIDs and makes message backups in original MIME message format
* ckecks MIME Message-ID or calculates MD5 if Message-ID is missing to avoid message duplicates

# Dependencies
* JAVA JDK 1.8+

# Bulding and running
./gradlew jar  --- to build the project \
./run.cmd      --- to run and show menu

NB! before running the program edit build/resources/main/mbox.properties file to describe your IMAP account configuration
