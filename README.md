# imapsize
This software is insipred by http://www.broobles.com/imapsize. Imapsize is able to check IMAP mailbox folder structure and calculate the size, backup mailbox using IMAP protocol and restore mailbox to any IMAP server.

Imapsize has some key differences from the original:
* console application
* runs on any platform where JRE 11. is installed
* understands UTF-8 IMAP folder names and restores them correctly
* doesn't have 2Gb limit for the folder size
* understands folder UIDs and makes message backups using original MIME message format
* ckecks MIME Message-ID or calculates MD5 if Message-ID is missing to avoid message duplicates
* user can define which folders to backup/restore by making filters. Default filter is applied to Calendar, Contacts and Deleteted Items

# Dependencies
* JAVA JDK 1.11

# Bulding and running
./gradlew jar  --- to build the project \
./run.cmd      --- to run and show menu

NB! before running the program edit build/resources/main/mbox.properties file to describe your IMAP account configuration

# Coverity scan result
<a href="https://scan.coverity.com/projects/evrcargo-imapsize">
  <img alt="Coverity Scan Build Status"
       src="https://scan.coverity.com/projects/14799/badge.svg"/>
</a>
