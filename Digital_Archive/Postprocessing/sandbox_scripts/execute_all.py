#!/usr/bin/python -tt
# -*- coding: utf-8 -*-
"""
Archivo para procesar raws con gestion del color y creacion de derivados jpeg y tiff
by jpereira :: www.jpereira.net :: info@jpereira.net
"""
import os, sys,filecmp,time, errno, glob
import logging
import subprocess
from distutils.dir_util import copy_tree
import mysql.connector
from mysql.connector import errorcode
from time import gmtime, strftime
import xml.dom.minidom
from datetime import datetime
import smtplib
import shutil
import socket
#from shutil import copytree, ignore_patterns

"""
#Para desarrollo y pruebas
# Repositorio con los archivos originales
ORIGPATH = "/Volumes/Macintosh_HD_DATA/ORIGIN";
# Directorio para el procesado temporal
TEMPROOT = "/Volumes/Macintosh_HD_DATA/TEMP/";
# Repositior para el archivo profundo o definitivo
TARGETROOT = "/Volumes/Macintosh_HD_DATA/DEEP/";
# Directorio para almacenar ciertos archvos de logs
REPORT = "/Volumes/Macintosh_HD_DATA/REPORTS/";
# Repositorio de la web
WEBSITEREPO = "/Users/jpereira/Sites/workspace/manuscript/files"
# directorio temporal para IM
IMTEMP = "/Volumes/Macintosh_HD_DATA/IMTEMP"

USER_DB = "root"
PASS_DB = "pereira"
DATABASE = "manuscritos"
HOST_DB = "127.0.0.1"
"""
# Repositorio con los archivos originales
ORIGPATH = "/home/user/DIGITAL_LIBRARY/SANDBOX/FOLDER14/";
# Directorio para el procesado temporal
TEMPROOT = "/home/user/DIGITAL_LIBRARY/SANDBOX/TEMP/IMG_TEMP/";
# Repositior para el archivo profundo o definitivo
TARGETROOT = "/home/user/DIGITAL_LIBRARY/SANDBOX/ARCHIVE/DEEP_ARCHIVE/";
# Directorio para almacenar ciertos archvos de logs
REPORT = "/home/user/DIGITAL_LIBRARY/SANDBOX/REPORTS/";
# Repositorio de la web
WEBSITEREPO = "/home/user/DIGITAL_LIBRARY/SANDBOX/ARCHIVE/WEBSITE_ARCHIVE/"
# Directorio temporal para IM
IMTEMP = "/home/user/DIGITAL_LIBRARY/SANDBOX/TEMP/IMK_TEMP/"

USER_DB = "root"
PASS_DB = "1234asdf$"
DATABASE = "sandbox"
HOST_DB = "127.0.0.1"

#NOFOLLOW = ["Archive_scripts","PENDING","test","1009","1019","1064","1068","146","115","174","2576","1029","514","504","511","531","589","604","2576","533","62","628","668","676","685","782"] 
NOFOLLOW = ["Archive_scripts","PENDING","test","1068"] 

def verifyDir(PATH1, PATH2):
    
        comparison = filecmp.dircmp(PATH1, PATH2)
        noIguales = comparison.diff_files
        enDerechaSolo = comparison.right_only
        enIzquierdaSolo = comparison.left_only
        
        NnoIguales = len(noIguales)
        NenDerechaSolo = len(enDerechaSolo)
        NenIzquierdaSolo = len(enIzquierdaSolo)
        
        output = 0
        if NnoIguales > 0:
            output += 1
            noIguales = " ".join( noIguales )
            logging.error("Files that do not match: {0}".format( noIguales ) )

        if NenDerechaSolo > 0:
            output += 1
            enDerechaSolo = " ".join( enDerechaSolo )
            logging.error("Files only on the right directory:  {0}".format( enDerechaSolo ) )
 
        if NenIzquierdaSolo > 0:
            output += 1
            enIzquierdaSolo = " ".join( enIzquierdaSolo )
            logging.error("Files only on the left directory:  {0}".format( enIzquierdaSolo ) )
            
        if output > 0:
            std = False
            logging.error("There are errors in copying")
            print("There are errors in copying")
        else:
            std = True
            print("Directories exported without errors")
            logging.info("Directories exported without errors" )
            
        return std
    
def folderExists(PATH,PROCESS,ID):
    #alerta sobre si y existe un directorio con el mismo nombre
    if os.path.isdir( PATH ):
        errTXT = "PATH {0} EXISTS AT PROCESS {1}".format(PATH, PROCESS )
        logging.error(errTXT)
        update_sql = "UPDATE process SET {0} = 'FAIL-FF',status='ABORT',notes='Folder exists {2} at process {0}'  WHERE idProcess ={1}".format(PROCESS,ID,PATH)
        my = myConn()
        my["cursor"].execute(update_sql)
        my["cnx"].commit()
        myConnClos(my["cursor"], my["cnx"])
        alertByMail(errTXT)
        sys.exit( errTXT )
        return False
    else:
        return True
    
def countPreviousFiles(PATH,FOLDER, EXT):
    if FOLDER is not None:
        PATH = os.path.join(PATH,FOLDER)
        
    return len(glob.glob1(PATH,"*."+EXT))
    
def countFiles(PATH, FOLDER, PROJECT,TOOL, ID, nFILES):
    
    PATH = os.path.join(PATH, FOLDER)
    NUMBERFILES = 0
    if os.path.isdir( PATH ):
        #NUMBERFILES =  len([name for name in os.listdir(PATH) if os.path.isfile(os.path.join(PATH, name))])
        NUMBERFILES =  len([name for name in os.listdir(PATH) if not name.startswith(".") and os.path.isfile(os.path.join(PATH, name))])
        
        if NUMBERFILES == nFILES or (TOOL == 'process_color' and NUMBERFILES > 0):
            logging.info("Folder {2}/{0} has {1} files ({3})".format(FOLDER,NUMBERFILES,PROJECT,PATH ) )
            p = ""
            if TOOL == "process_raw":
                p = ",process_tiff='NA' "
            elif TOOL == "process_tiff":
                p = ",process_raw='NA' "
                
            update_sql = "UPDATE process SET {0} = 'OK',status='PROCESS'{2} WHERE idProcess ={1}".format(TOOL,ID,p)
            my = myConn()
            my["cursor"].execute(update_sql)
            my["cnx"].commit()
            myConnClos(my["cursor"], my["cnx"])
        else:
            errTXT = "Folder {1}/{0} NUMBER OF FILES NOT MATCH. ORIG {3} TARGET {4}  ({2})".format(FOLDER,PROJECT,PATH,nFILES, NUMBERFILES )
            logging.error(errTXT )
            update_sql = "UPDATE process SET {0} = 'FAIL-F',status='ABORT',notes='Files not match at {2} ORIG {3} TARGET {4}'  WHERE idProcess ={1}".format(TOOL,ID,FOLDER,nFILES, NUMBERFILES)
            my = myConn()
            my["cursor"].execute(update_sql)
            my["cnx"].commit()
            myConnClos(my["cursor"], my["cnx"])
            
            alertByMail(errTXT)
            sys.exit( errTXT )
    else:
        errTXT = "Folder {1}/{0} NO EXISTS ({2})".format(FOLDER,PROJECT,PATH )
        logging.error(errTXT )
        update_sql = "UPDATE process SET {0} = 'FAIL-D',status='ABORT',notes='Folder no exists {2}' WHERE idProcess ={1}".format(TOOL,ID,FOLDER)
        my = myConn()
        my["cursor"].execute(update_sql)
        my["cnx"].commit()
        myConnClos(my["cursor"], my["cnx"])
        alertByMail(errTXT)
        sys.exit(errTXT )
        
def check_xml(PATH):
    ERROR = 0 
    if os.path.isfile(PATH):
        DOMTree = xml.dom.minidom.parse(PATH)
        collection = DOMTree.documentElement

        meta = collection.getElementsByTagName("metadata")
            
        if (len(meta[0].getElementsByTagName("code"))==0):
            ERROR = ERROR + 1
        elif (len(meta[0].getElementsByTagName("name"))==0):
            ERROR = ERROR + 1               
        elif (len(meta[0].getElementsByTagName("timestamp"))==0):
            ERROR = ERROR + 1    
        elif (len(meta[0].getElementsByTagName("target_directory"))==0):
            ERROR = ERROR + 1            
        elif (len(meta[0].getElementsByTagName("target_subdirectory"))==0):
            ERROR = ERROR + 1                            
        elif (len(meta[0].getElementsByTagName("source"))==0):
            ERROR = ERROR + 1
                
        if (ERROR == 0):
            #print("_no_error")
            return True
        else:
            #print("_ERROR_")
            logging.error("XML ERROR NOT TAGs path: {0}".format(PATH ) )
            
            return False
            
    else:
        #print("_NO_FILE_")
        logging.error("XML ERROR NOT project.xm FILE path: {0}".format(PATH ) )
        return False

def parse_xml(PATH):

    DOMTree = xml.dom.minidom.parse(PATH)
    collection = DOMTree.documentElement
    metaInfo = {}
    meta = collection.getElementsByTagName("metadata")
    metaInfo['name'] = meta[0].getElementsByTagName('name')[0].childNodes[0].data
    #bookname.childNodes[0].data
    metaInfo['code'] = meta[0].getElementsByTagName('code')[0].childNodes[0].data
    metaInfo['timestamp']  = meta[0].getElementsByTagName('timestamp')[0].childNodes[0].data
    
    target_directory = meta[0].getElementsByTagName('target_directory')[0].childNodes[0].data
    target_subdirectory = meta[0].getElementsByTagName('target_subdirectory')[0].childNodes[0].data
    
    metaInfo['target'] = os.path.join(target_directory, target_subdirectory)
    metaInfo['uploaded'] = str_to_bool(meta[0].getElementsByTagName('uploaded')[0].childNodes[0].data)
    metaInfo['source'] = meta[0].getElementsByTagName('source')[0].childNodes[0].data

    return metaInfo

def str_to_bool(s):
    if s == 'True':
        return True
    elif s == 'False':
        return False
    else:
        raise ValueError
    
def getCharsAndItems(ORIGPATH,fname):
    
    projectXML = os.path.join(ORIGPATH,fname, "project.xml")
    DOMTree = xml.dom.minidom.parse(projectXML)
    collection = DOMTree.documentElement
    items = collection.getElementsByTagName("item")
    bookcode = collection.getElementsByTagName('code')[0].childNodes[0].data
    timeStamp  = collection.getElementsByTagName('timestamp')[0].childNodes[0].data
    fullBookName = createFullNameProject(bookcode, timeStamp)

    seen = set()
    i = 0
    j = 0
    h = 0
    for item in items:
    
        if item.getElementsByTagName('type')[0].childNodes[0].data == "chart":
            i = i+1

        if item.getElementsByTagName('type')[0].childNodes[0].data == "noPage":
            h = h+1
    
        if item.getElementsByTagName('type')[0].childNodes[0].data == "page":
            if item.getElementsByTagName('image_left')[0].childNodes:
                rawLeft = os.path.join(ORIGPATH,fname, item.getElementsByTagName('image_left')[0].childNodes[0].data)
                if os.path.isfile(rawLeft):
                    if rawLeft not in seen:
                        #print(item.getElementsByTagName('image_left')[0].childNodes[0].data)
                        seen.add( rawLeft )
                        j = j+1
                    else:
                        itemCount = (j/2)+1
                        logging.error("_DUPLICATE_IMAGE at item {0} image_LEFT".format(itemCount ) ) 
                        alertByMail( "_DUPLICATE_IMAGE at item {0} image_LEFT".format(itemCount ) ) 
                        updateMsgDB(fullBookName, "_DUPLICATE_IMAGE at item {0} image_LEFT".format(itemCount ) )
                else:
                    itemCount = (j/2)+1
                    logging.error("_IMAGE_MISING_AT_FILESYSTEM at item {0} image_left".format(itemCount ) ) 
                    alertByMail( "_IMAGE_MISING_AT_FILESYSTEM at item {0} image_left at project {1}".format(itemCount,bookcode ) )                      
                    updateMsgDB(fullBookName, "_IMAGE_MISING_AT_FILESYSTEM at item {0} image_left at project {1}".format(itemCount,bookcode ) )
            else:
                itemCount = (j/2)+1
                logging.error("_IMAGE_MISING_AT_XML at item {0} image_left".format(itemCount ) ) 
                alertByMail( "_IMAGE_MISING_AT_XML at item {0} image_left at project {1}".format(itemCount,bookcode ) )                      
                updateMsgDB(fullBookName, "_IMAGE_MISING_AT_XML at item {0} image_left at project {1}".format(itemCount,bookcode ) )
   
                
            if item.getElementsByTagName('image_right')[0].childNodes:
                rawRight = os.path.join(ORIGPATH,fname, item.getElementsByTagName('image_right')[0].childNodes[0].data)
                if os.path.isfile(rawRight):
                    if rawRight not in seen:
                        seen.add( rawRight )
                        #print(item.getElementsByTagName('image_right')[0].childNodes[0].data)
                        j = j+1
                    else:
                        itemCount = (j/2)+1
                        logging.error("_DUPLICATE_IMAGE at item {0} image_RIGHT".format(itemCount ) )
                        alertByMail( "_DUPLICATE_IMAGE at item {0} image_RIGHT".format(itemCount ) )
                        updateMsgDB(fullBookName, "_DUPLICATE_IMAGE at item {0} image_RIGHT".format(itemCount ) )
 
                else:
                    itemCount = (j/2)+1
                    logging.error("_IMAGE_MISING_AT_FILESYSTEM at item {0} image_right".format(itemCount ) ) 
                    alertByMail( "_IMAGE_MISING_AT_FILESYSTEM at item {0} image_right at project {1}".format(itemCount,bookcode ) )                      
                    updateMsgDB(fullBookName, "_IMAGE_MISING_AT_FILESYSTEM at item {0} image_right at project {1}".format(itemCount,bookcode ))
            else:
                itemCount = (j/2)+1
                logging.error("_IMAGE_MISING_AT_XML at item {0} image_right".format(itemCount ) ) 
                alertByMail( "_IMAGE_MISING_AT_XML at item {0} image_right at project {1}".format(itemCount,bookcode ) )
                updateMsgDB(fullBookName, "_IMAGE_MISING_AT_XML at item {0} image_right at project {1}".format(itemCount,bookcode ) )
            
    o = {}

    o['chars'] = i * 2
    if o['chars'] == 0:
        logging.error("_NO_CHARS_FOUND" )
        updateMsgDB(fullBookName, "_NO_COLOR_CHARS_FOUND at project {}".format(bookcode) )
        alertByMail( "_NO_COLOR_CHARS_FOUND at project {}".format(bookcode) )

        
    o['pages'] = j
    o['noPages'] = h*2
    o['total'] = o['chars'] + o['pages'] + o['noPages']
    
    return o    

def alertByMail(MESSAGE):
    
    if isOpen('smtp.gmail.com', 587) :
        SUBJECT = "Process Alert from Dagestan"
        s = smtplib.SMTP('smtp.gmail.com', 587)
        s.starttls()
        s.login("alerts@jpereira.net", "AbCdEfG2018")
        message = 'Subject: {}\n\n{}'.format(SUBJECT, MESSAGE)
        problems = s.sendmail("alerts@jpereira.net", "info@jpereira.net", message)
        print(problems)
        s.quit()
    else:
        logging.error("NOT INTERNET CONNECTION AVAILABLE" )


def myConn():
    con ={}
    con["cnx"] =  mysql.connector.connect(user=USER_DB, password=PASS_DB, host=HOST_DB, database=DATABASE)
    con["cursor"] =  con["cnx"].cursor()
    return con
    
def myConnClos(cursor, cnx):
    cursor.close()
    cnx.close() 

# comprueba si un server esta disponivle
def isOpen(ip,port):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        s.connect((ip, int(port)))
        s.shutdown(2)
        return True
    except:
        return False

# funciones para purgar el proceso de procesos incompletos
def get_lastRecord():    
    lastStatus = ("SELECT idProcess, status, projectCode, path FROM process ORDER BY idProcess DESC LIMIT 1")
    my = myConn()
    my["cursor"].execute(lastStatus, multi=False)
    rows = my["cursor"].fetchone()     
    if rows != None:
        lastID = rows         
        myConnClos(my["cursor"], my["cnx"]) 
    else:
        lastID = []  
    return lastID

def removeTempFolders(lastProjectCode, lastProjectPath):
    pos = lastProjectCode.find('_')
    projectCode = lastProjectCode[0:pos]
    
    pathToTempFolder = os.path.join(TEMPROOT,projectCode)
    if os.path.isdir(pathToTempFolder):
        shutil.rmtree( pathToTempFolder )
        logging.info("_PURGE SAIDS DELETE TEMP FOLDER" ) 
        updateMsgDB(lastProjectCode, "_PURGE SAIDS DELETE TEMP FOLDER" )
        print("borrando carpeta temporal")
        
    pathToWebFolder = os.path.join(WEBSITEREPO, lastProjectPath,lastProjectCode)
    if os.path.isdir(pathToWebFolder):
        shutil.rmtree( pathToWebFolder )
        updateMsgDB(lastProjectCode, "_PURGE SAIDS DELETE WEB FOLDER" )
        print("borrando carpeta web temporal")
        
    pathToDeepFolder = os.path.join(TARGETROOT, lastProjectPath,lastProjectCode)  
    if os.path.isdir(pathToDeepFolder):
        shutil.rmtree( pathToDeepFolder )
        updateMsgDB(lastProjectCode, "_PURGE SAIDS DELETE DEEP FOLDER" )
        print("borrando carpeta deep temporal") 
                
    pathToOringFolder = os.path.join(ORIGPATH,projectCode) 
        
    if os.path.isfile( os.path.join(pathToOringFolder,"project.xml.backup_meta") ):
        os.remove( os.path.join(pathToOringFolder,"project.xml") )
        os.rename( os.path.join(pathToOringFolder,"project.xml.backup_meta"), os.path.join(pathToOringFolder,"project.xml") )
        logging.info("_PURGE SAIDS RECOVERY PROJECT.XML" ) 
        updateMsgDB(lastProjectCode, "_PURGE SAIDS RECOVERY PROJECT.XML" )
        print("renombrado project.xml")
        
    if os.path.isdir(pathToTempFolder):
        return False
    else:
        return True  
    
    
def removeTempFiles():
    lastRecord = get_lastRecord()
    if len(lastRecord) > 0:
        lastID = lastRecord[0]
        lastStatus = lastRecord[1]
        lastProjectCode = lastRecord[2]
        lastProjectPath = lastRecord[3]
        status = False
        if lastStatus == "start" or lastStatus == "PROCESS":
            if removeTempFolders(lastProjectCode,lastProjectPath ):
                delstatmt = "DELETE FROM process WHERE idProcess = %(idp)s"
                my = myConn()
                my["cursor"].execute(delstatmt, { 'idp': lastID } )
                my["cnx"].commit()   
                
                status = True
    
        if lastStatus == "ABORT":
            removeTempFolders(lastProjectCode)
            status = True
            
        if lastStatus == "FINISH":
            status = True
    else:
        status = True
        
    return status 
# fin de funciones de purga

#inserta notas de errores en el proyecto

def updateMsgDB(code, note):
    add_note = ("INSERT INTO processNotes (projectCode, note ) VALUES (%s, %s)")   
    dataFile = ( code, note  )
    my = myConn()
    my["cursor"].execute(add_note, dataFile)
    my["cnx"].commit()
    myConnClos(my["cursor"], my["cnx"])
    
def createFullNameProject(code, timeStamp):
    timemark = datetime.strptime('{:<011d}'.format( int(timeStamp ) ), '%Y%m%d%H%M%S').strftime('%Y%m%d%H%M%S')
    #timemark = datetime.strptime(bookMeta['timestamp'], '%Y%m%d%H%M%S').strftime('%Y%m%d%H%M%S')
    return "{0}_{1}".format(code, timemark )
    

if __name__ == '__main__':


    try:
    
        print("******************************")
        print("------- SANDBOX SCRIPT -------")
        print("******************************")
        
        date = time.strftime("%Y%m%d")    
        now = strftime("%Y-%m-%d %H:%M:%S", gmtime())
        
        LOGFILE = os.path.join(REPORT, "process-report-"+date+".txt" )
    
        logging.root.handlers = []
        logging.basicConfig(filename=LOGFILE,
                            filemode='a',
                            format='%(asctime)s %(name)s %(levelname)s %(message)s',
                            datefmt= '%d/%m/%Y %H:%M:%S',
                            level=logging.DEBUG)
        
        add_project = ("INSERT INTO process "
                    "(projectCode, get_files, process_color, process_raw,process_tiff,create_pdf,export_project,move_folders,status,datetime,path ) "
                    "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s,%s)")
        
        selectProject = ("SELECT idProcess, status, projectCode FROM process WHERE projectCode = %(idp)s ORDER BY idProcess DESC LIMIT 1  ")
        
                
        lst = os.listdir(ORIGPATH)
        lst.sort()   
        
        tmpStatus = removeTempFiles()   

        if  tmpStatus:
             
            for fname in lst:
                
                if not fname.startswith('.') and fname not in NOFOLLOW:
                    
                    ORIG = os.path.join(ORIGPATH, fname)
                    TARGET = os.path.join(TEMPROOT, fname)
                    
                    
                    projectXML = os.path.join(ORIGPATH,fname, "project.xml")
                    print(fname)
                    isValid = check_xml(projectXML)
                    if not isValid:
                        subprocess.call(['python','update_xml_head.py', projectXML, REPORT ])
                        isValid = check_xml(projectXML)
                        
                    if (isValid):
                        bookMeta = parse_xml(projectXML)
        
                        #timemark = datetime.strptime('{:<011d}'.format( int(bookMeta['timestamp'] ) ), '%Y%m%d%H%M%S').strftime('%Y%m%d%H%M%S')
                        #timemark = datetime.strptime(bookMeta['timestamp'], '%Y%m%d%H%M%S').strftime('%Y%m%d%H%M%S')
                        #BOOKDIRNAME = "{0}_{1}".format(bookMeta['code'], timemark )
                        BOOKDIRNAME = createFullNameProject( bookMeta['code'] ,bookMeta['timestamp'] ) 
                        
                        TARGET_DEEP = os.path.join(TARGETROOT, bookMeta['target'], BOOKDIRNAME)
                        WEBSITE_REPO = os.path.join(WEBSITEREPO, bookMeta['target'])
                        
                        #print("TARGET "+TARGET)
                        #print("BOOKDIRNAME "+BOOKDIRNAME)
                        #print("TARGET_DEEP "+TARGET_DEEP)
                        #print("WEBSITE_REPO "+WEBSITE_REPO)
                                            
                        #bypass
                        moveFromOrigin = True
                        processColor = True
                        processRaw = True
                        processTiff = True
                        processPDF = True
                        processExport = True
                        processMoveFolders = True
        
                        my = myConn()
                        my["cursor"].execute(selectProject, { 'idp': BOOKDIRNAME })
                        rows = my["cursor"].fetchall()
                        
                        if (len(rows) > 0) and (rows[0][1] == 'FINISH' or rows[0][1] == 'PROCESS' or rows[0][1] == 'start' ):
                            noProcess = False
                        elif  (len(rows) > 0) and rows[0][1] == 'ABORT':
                            noProcess = True  
                        elif (len(rows) == 0):
                            noProcess = True
                        myConnClos(my["cursor"], my["cnx"])    
                            
                        if noProcess and bookMeta['uploaded']:
                            
                            if os.path.isdir( ORIG ):
                                logging.info("Start project {0} ".format(fname) )
                                print("Start project {0} ".format(fname) )
                                
                                
                                dataFile = ( BOOKDIRNAME, 'wait', 'wait', 'wait', 'wait','wait', 'wait', 'wait','start', now, bookMeta['target']  )
                                my = myConn()
                                my["cursor"].execute(add_project, dataFile)
                                idFiles = my["cursor"].lastrowid 
                                my["cnx"].commit()
                                myConnClos(my["cursor"], my["cnx"])
                                
                                if not moveFromOrigin:
                                    noPath = False
                                else:
                                    noPath = os.path.exists( TARGET )
                                
                                if not noPath:
                                    if moveFromOrigin:
                                        os.makedirs( TARGET )
                                        #print(ORIG,TARGET )
                                        copy_tree(ORIG, TARGET)
                                        isReady = verifyDir(ORIG, TARGET)
        
                                    else:
                                        isReady = True
                                    
                                    
                                    if isReady:
                                        update_sql = "UPDATE process SET get_files = 'OK' WHERE idProcess ={0}".format(idFiles)
                                        my = myConn()
                                        my["cursor"].execute(update_sql)
                                        my["cnx"].commit()
                                        myConnClos(my["cursor"], my["cnx"])
                                        
                                        if bookMeta['source'] == 'dslr':
                                            
                                            items = getCharsAndItems(TEMPROOT,fname)
                                            nFiles = items['pages']
                                            nCharts = items['chars']
                                            print("nItemsA")
                                            print(items)
    
                                            
                                            # Intenta detectar cartas
                                            if nCharts == 0:
                                                logging.info("Charts detect intent" )
                                                subprocess.call(['python','detect_charts_normalize.py', TARGET,REPORT ])
                                                items = getCharsAndItems(TEMPROOT,fname)
                                                nFiles = items['pages']
                                                nCharts = items['chars']
                                                
                                                print("nItemsB")
                                                print(items)
                                                
                                                if nCharts > 0:
                                                    logging.info("EXECUTE_ALL SAYS: Charts was found" )
                                                else:
                                                    logging.error("EXECUTE_ALL SAYS: Charts NOT found" )
                                                    updateMsgDB(BOOKDIRNAME, "_IMPOSSIBLE TO DETECT CHARTS" )
                                                    
                                            #cuenta archivos por si se duplican los raws
                                            newFilesRaw = countPreviousFiles(TARGET,'raw', 'cr2')
                                            #print("Raw files after Dectect Charts")
                                            #print(newFilesRaw)
                                                
                                            #nFilesRaw = countPreviousFiles(ORIG,'raw', 'cr2'
                                            #countFiles(TARGET, "raw",fname,'get_files',idFiles, items['total'] )
                                            logging.info("Project has {0} charts".format(nCharts) )
                                            logging.info("Project has {0} images".format(nFiles) )
                                            
                                            
                                            if processColor: 
                                                
                                                if nCharts > 0:                  
                                                    print("********** process_color.py*****************")
                                                    logging.info("********** process_color.py*****************" )
                                                    subprocess.call(['python','process_color.py', TARGET ])
                                                    countFiles(TARGET, "PROFILES",fname,'process_color',idFiles, nCharts)
                                                elif nCharts == 0 and bookMeta['source'] == 'dslr':
                                                    print("********** process_fake_color.py*****************")
                                                    logging.info("********** process_fake_color.py*****************" )
                                                    subprocess.call(['python','process_fake_color.py', TARGET ])     
                                            if processRaw:
                                                print("********** process_raw.py *****************")
                                                logging.info("********** process_raw.py *****************" )
                                                subprocess.call(['python','process_raw.py', TARGET, IMTEMP ])
                                                countFiles(TARGET, "TIFF",fname,'process_raw',idFiles, nFiles)
                                                countFiles(TARGET, "JPEG",fname,'process_raw',idFiles, nFiles)
                                                countFiles(TARGET, "TIFFP",fname,'process_raw',idFiles, nFiles)
                                        elif bookMeta['source'] == 'scanner':
                                            if processTiff:
                                                print("********** process_tiff.py *****************")
                                                logging.info("********** process_tiff.py *****************" )
                                                nFiles = countPreviousFiles(ORIG, None, 'TIF')
                                                subprocess.call(['python','process_tiff.py', TARGET,IMTEMP ])
                                                countFiles(TARGET, "TIFF",fname,'process_tiff',idFiles,nFiles)
                                                countFiles(TARGET, "JPEG",fname,'process_tiff',idFiles,nFiles)
                                                countFiles(TARGET, "TIFFP",fname,'process_tiff',idFiles,nFiles)
                                        
                                        if processPDF:    
                                            print("********** create_pdf.py*****************")
                                            logging.info("********** create_pdf.py *****************" )
                                            subprocess.call(['python','create_pdf.py', TARGET,IMTEMP ])
                                            countFiles(TARGET, "PDF",fname,'create_pdf',idFiles, 1)
                                        
                                        if processExport:    
                                            #exporta a la web
                                            print("********** export_project.py*****************")
                                            logging.info("********** export_project.py  *****************" )
                                            PATH = os.path.join(WEBSITE_REPO, BOOKDIRNAME)
                                            if folderExists(PATH,'export_project',idFiles):
                                                subprocess.call(['python','export_project.py', TARGET, WEBSITE_REPO, REPORT, USER_DB, PASS_DB, HOST_DB, DATABASE  ])
                                                countFiles(PATH, "TIFFP",BOOKDIRNAME,'export_project',idFiles,nFiles)
                                                countFiles(PATH, "JPEG",BOOKDIRNAME,'export_project',idFiles,nFiles)
                                                countFiles(PATH, "PDF",BOOKDIRNAME,'export_project',idFiles, 1)
                                        
                                        if processMoveFolders:
                                            #guarda los archivos en el archivo profundo
                                            print("********** move_folders.py*****************")
                                            logging.info("********** move_folders.py *****************" )
                                            PATH = os.path.join(TARGET_DEEP, BOOKDIRNAME)
                                            if folderExists(PATH,'move_folders',idFiles):
                                                subprocess.call(['python','move_folders.py', TARGET, TARGET_DEEP, REPORT ])
                                    
                                                countFiles(TARGET_DEEP, "TIFF",fname,'move_folders',idFiles,nFiles)
                                                countFiles(TARGET_DEEP, "PDF",fname,'move_folders',idFiles,1)
                                                
                                                if bookMeta['source'] == 'dslr':
                                                    #nFilesRaw = countPreviousFiles(ORIG,'raw', 'cr2')
                                                    nFilesRaw = newFilesRaw
                                                    countFiles(TARGET_DEEP, "raw",fname,'move_folders',idFiles,nFilesRaw)
                                                
                                        logging.info("Finish project {0}".format(fname) )
                                        print( "Finish project {0}".format(fname) )   
                                        alertByMail("Finish project {0}".format(fname))                      
                                        update_sql = "UPDATE process SET status='FINISH',notes='N/A' WHERE idProcess ={0}".format(idFiles)
                                        my = myConn()
                                        my["cursor"].execute(update_sql)
                                        my["cnx"].commit()
                                        myConnClos(my["cursor"], my["cnx"])
                                        logging.info("********** THE END !!! *****************")
                                    else:
                                        logging.error("Verify error at project {0}".format(fname) )
                                        update_sql = "UPDATE process SET status='ABORT',notes='Copy not match, directory of files not match' WHERE idProcess ={0}".format(idFiles)
                                        my = myConn()
                                        my["cursor"].execute(update_sql)
                                        my["cnx"].commit()
                                        myConnClos(my["cursor"], my["cnx"])
                                        print( "Verify error at project {0}".format(fname) )
                                       
                                else:
                                    logging.error("Target directory exists!!" ) 
                                    update_sql = "UPDATE process SET status='ABORT',notes='Target directory exists on copy project' WHERE idProcess ={0}".format(idFiles)
                                    my = myConn()
                                    my["cursor"].execute(update_sql)
                                    my["cnx"].commit()
                                    myConnClos(my["cursor"], my["cnx"])
                                    print("Target directory exists!!" )
                            else:
                                logging.error("Abort due XML ERROR" ) 
                                update_sql = "UPDATE process SET status='ABORT',notes='XML ERROR FORMAT' WHERE idProcess ={0}".format(idFiles)
                                my = myConn()
                                my["cursor"].execute(update_sql)
                                my["cnx"].commit()
                                myConnClos(my["cursor"], my["cnx"])
                                print("Abort due XML ERROR" )
                    else:
                        print("Imposible to update XML FILE" ) 
                        logging.error( "Imposible to update XML FILE {}".format(fname) )
                        alertByMail( "Imposible to update XML FILE {}".format(fname) )           
    #except IOError, e:
    #    print "Unable to copy file. %s" % e
        
    #except EnvironmentError, e:
    #    print "Unable to copy file. %s" % e
    #    print( "no se pudo copiar %s a %s \n" % (ORIG, TARGET))
                
    except subprocess.CalledProcessError as e:
        print e.output
        logging.error( e.output )
        alertByMail(e.output)
    
    except OSError as e:
        if e.errno != errno.EEXIST:
            logging.error( e )
            print(e)
            alertByMail(e)
            sys.exit(e.errno)

    except mysql.connector.Error as err:
        if err.errno == errorcode.ER_ACCESS_DENIED_ERROR:
            print("Something is wrong with your user name or password")
            logging.error("Something is wrong with your user name or password" )
        elif err.errno == errorcode.ER_BAD_DB_ERROR:
            print("Database does not exist")
            logging.error("Database does not exist" )
        else:
            print(err)
            logging.warn("Error at MySQL: {}".format(err) )
            alertByMail(err)
        
    except mysql.connector.IntegrityError as err:
        logging.warn("Error at MySQL: {}".format(err) )
        alertByMail(err)


