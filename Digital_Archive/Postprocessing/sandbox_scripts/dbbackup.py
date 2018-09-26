#!/usr/bin/python
###########################################################

import os
import time
import logging
import smtplib


DB_HOST = 'localhost'
DB_USER = 'root'
DB_USER_PASSWORD = '1234asdf$'
DB_NAME = 'manuscritos'
BACKUP_PATH = '/home/user/DIGITAL_LIBRARY/BACKUPS/MYSQL_BACKUP/'


def alertByMail(MESSAGE):
    SUBJECT = "Alert MySQL backup Daguestan"
    s = smtplib.SMTP('smtp.gmail.com', 587)
    s.starttls()
    s.login("alerts@jpereira.net", "AbCdEfG2018")
    message = 'Subject: {}\n\n{}'.format(SUBJECT, MESSAGE)
    s.sendmail("alerts@jpereira.net", "info@jpereira.net", message)
    s.quit()


LOGFILE = os.path.join(BACKUP_PATH, "backupshistory.txt" )
        
logging.root.handlers = []
logging.basicConfig(filename=LOGFILE,
                    filemode='a',
                    format='%(asctime)s %(name)s %(levelname)s %(message)s',
                    datefmt= '%d/%m/%Y %H:%M:%S',
                    level=logging.DEBUG)


DATETIME = time.strftime('%m%d%Y-%H%M%S')

BKFILENAME = os.path.join(BACKUP_PATH, DATETIME+".gz" )

dumpcmd = "mysqldump --force --opt --user={0} -p{1}  --databases {2} | gzip > {3}".format(DB_USER, DB_USER_PASSWORD, DB_NAME, BKFILENAME  )

os.system(dumpcmd)


if os.path.isfile(BKFILENAME) and os.access(BKFILENAME, os.R_OK):
    size = os.path.getsize( BKFILENAME ) / 1024
    file_name = os.path.basename(BKFILENAME)
    if size > 0:
        logging.info("{0} file exist, with size {1} Kb".format(file_name, size) )
        print "Backup script completed"
        print "Your backups has been created in '" + BKFILENAME + "' directory"
    else:
        logging.error("{0} FILESIZE WRONG!!".format(file_name) )
        alertByMail( "{0} FILESIZE WRONG!!".format(file_name) )
else:   
    logging.error("{0} file does NOT exist".format(file) )
    alertByMail( "{0} file does NOT exist".format(file) )
