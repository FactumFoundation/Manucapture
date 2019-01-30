# Based on the processing server of Jose

# TODO:
#  #1 Type is old scanner is Item, not page
# 


import os, sys, filecmp, glob
import logging
import subprocess
from distutils.dir_util import copy_tree
import xml.dom.minidom
from datetime import datetime
import smtplib
import shutil
import time, errno
from time import gmtime, strftime


# Input params
moveFromOrigin = True
processColor = True
processRaw = True
processTiff = True
processPDF = True
processExport = True
processMoveFolders = True
ORIGPATH = "/home/factum/0_Book_Scans"
# Directorio para almacenar ciertos archvos de logs
REPORT = "/home/factum/0_Book_Scans/postprocessing/reports"
fname = "009"


def createFullNameProject(code, timeStamp):
    timemark = datetime.strptime('{:<011d}'.format(int(timeStamp)), '%Y%m%d%H%M%S').strftime('%Y%m%d%H%M%S')
    return "{0}_{1}".format(code, timemark)


def getCharsAndItems(ORIGPATH, fname):
    projectXML = os.path.join(ORIGPATH, fname, "project.xml")
    DOMTree = xml.dom.minidom.parse(projectXML)
    collection = DOMTree.documentElement
    items = collection.getElementsByTagName("item")
    bookcode = collection.getElementsByTagName('code')[0].childNodes[0].data
    timeStamp = collection.getElementsByTagName('timestamp')[0].childNodes[0].data
    fullBookName = createFullNameProject(bookcode, timeStamp)
    seen = set()
    i = 0
    j = 0
    h = 0
    for item in items:

        if item.getElementsByTagName('type')[0].childNodes[0].data == "chart":
            i = i + 1

        if item.getElementsByTagName('type')[0].childNodes[0].data == "noPage":
            h = h + 1

        # TODO #1 Changed page to Item to process old scans!!
        if item.getElementsByTagName('type')[0].childNodes[0].data == "Item":
            if item.getElementsByTagName('image_left')[0].childNodes:
                rawLeft = os.path.join(ORIGPATH, fname, item.getElementsByTagName('image_left')[0].childNodes[0].data)
                if os.path.isfile(rawLeft):
                    if rawLeft not in seen:
                        # print(item.getElementsByTagName('image_left')[0].childNodes[0].data)
                        seen.add(rawLeft)
                        j = j + 1
                    else:
                        itemCount = (j / 2) + 1
                        logging.error("_DUPLICATE_IMAGE at item {0} image_LEFT".format(itemCount))
                else:
                    itemCount = (j / 2) + 1
                    logging.error("_IMAGE_MISING_AT_FILESYSTEM at item {0} image_left".format(itemCount))
            else:
                itemCount = (j / 2) + 1
                logging.error("_IMAGE_MISING_AT_XML at item {0} image_left".format(itemCount))

            if item.getElementsByTagName('image_right')[0].childNodes:
                rawRight = os.path.join(ORIGPATH, fname, item.getElementsByTagName('image_right')[0].childNodes[0].data)
                if os.path.isfile(rawRight):
                    if rawRight not in seen:
                        seen.add(rawRight)
                        # print(item.getElementsByTagName('image_right')[0].childNodes[0].data)
                        j = j + 1
                    else:
                        itemCount = (j / 2) + 1
                        logging.error("_DUPLICATE_IMAGE at item {0} image_RIGHT".format(itemCount))
                else:
                    itemCount = (j / 2) + 1
                    logging.error("_IMAGE_MISING_AT_FILESYSTEM at item {0} image_right".format(itemCount))
            else:
                itemCount = (j / 2) + 1
                logging.error("_IMAGE_MISING_AT_XML at item {0} image_right".format(itemCount))
    o = {}
    o['chars'] = i * 2
    if o['chars'] == 0:
        logging.error("_NO_CHARS_FOUND")
    o['pages'] = j
    o['noPages'] = h * 2
    o['total'] = o['chars'] + o['pages'] + o['noPages']
    return o


def check_xml(PATH):
    ERROR = 0
    if os.path.isfile(PATH):
        try:
            DOMTree = xml.dom.minidom.parse(PATH)
            collection = DOMTree.documentElement
        except:
            logging.error("CORRUPTED XML path: {0}".format(PATH))
            return False
        meta = collection.getElementsByTagName("metadata")
        if (len(meta[0].getElementsByTagName("code")) == 0):
            ERROR = ERROR + 1
        elif (len(meta[0].getElementsByTagName("name")) == 0):
            ERROR = ERROR + 1
        elif (len(meta[0].getElementsByTagName("timestamp")) == 0):
            ERROR = ERROR + 1
        elif (len(meta[0].getElementsByTagName("target_directory")) == 0):
            ERROR = ERROR + 1
        elif (len(meta[0].getElementsByTagName("target_subdirectory")) == 0):
            ERROR = ERROR + 1
        elif (len(meta[0].getElementsByTagName("source")) == 0):
            ERROR = ERROR + 1
        if (ERROR == 0):
            # print("_no_error")
            return True
        else:
            # print("_ERROR_")
            logging.error("XML ERROR NOT TAGs path: {0}".format(PATH))
            return False
    else:
        # print("_NO_FILE_")
        logging.error("XML ERROR NOT project.xm FILE path: {0}".format(PATH))
        return False

def str_to_bool(s):
    if s == 'True':
        return True
    elif s == 'False':
        return False
    else:
        raise ValueError

def parse_xml(PATH):
    DOMTree = xml.dom.minidom.parse(PATH)
    collection = DOMTree.documentElement
    metaInfo = {}
    meta = collection.getElementsByTagName("metadata")
    print(meta[0].getElementsByTagName('name')[0].childNodes)


    if len(meta[0].getElementsByTagName('name')[0].childNodes)==0 :
        metaInfo['name'] = ""
    else :
        metaInfo['name'] = meta[0].getElementsByTagName('name')[0].childNodes[0].data

    # bookname.childNodes[0].data
    metaInfo['code'] = meta[0].getElementsByTagName('code')[0].childNodes[0].data
    metaInfo['timestamp'] = meta[0].getElementsByTagName('timestamp')[0].childNodes[0].data

    target_directory = ""
    if len(meta[0].getElementsByTagName('target_directory')[0].childNodes)!=0:
        target_directory = meta[0].getElementsByTagName('target_directory')[0].childNodes[0].data

    target_subdirectory = ""
    if (len(meta[0].getElementsByTagName('target_subdirectory')[0].childNodes)!=0):
        target_subdirectory = meta[0].getElementsByTagName('target_subdirectory')[0].childNodes[0].data

    metaInfo['target'] = os.path.join(target_directory, target_subdirectory)

    metaInfo['uploaded'] = "False"
    if len(meta[0].getElementsByTagName('uploaded')[0].childNodes) != 0 :
        metaInfo['uploaded'] = str_to_bool(meta[0].getElementsByTagName('uploaded')[0].childNodes[0].data)

    metaInfo['source'] = "dslr"
    if len(meta[0].getElementsByTagName('source')[0].childNodes)!=0 :
        metaInfo['source'] = meta[0].getElementsByTagName('source')[0].childNodes[0].data

    return metaInfo


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
        noIguales = " ".join(noIguales)
        logging.error("Files that do not match: {0}".format(noIguales))

    if NenDerechaSolo > 0:
        output += 1
        enDerechaSolo = " ".join(enDerechaSolo)
        logging.error("Files only on the right directory:  {0}".format(enDerechaSolo))

    if NenIzquierdaSolo > 0:
        output += 1
        enIzquierdaSolo = " ".join(enIzquierdaSolo)
        logging.error("Files only on the left directory:  {0}".format(enIzquierdaSolo))

    if output > 0:
        std = False
        logging.error("There are errors in copying")
        print("There are errors in copying")
    else:
        std = True
        print("Directories exported without errors")
        logging.info("Directories exported without errors")

    return std


def countPreviousFiles(PATH, FOLDER, EXT):
    if FOLDER is not None:
        PATH = os.path.join(PATH, FOLDER)
    return len(glob.glob1(PATH, "*." + EXT))


def countFiles(PATH, FOLDER, PROJECT, TOOL, nFILES):
    PATH = os.path.join(PATH, FOLDER)
    NUMBERFILES = 0
    if os.path.isdir(PATH):
        NUMBERFILES = len([name for name in os.listdir(PATH) if
                           not name.startswith(".") and os.path.isfile(os.path.join(PATH, name))])

        if NUMBERFILES == nFILES or (TOOL == 'process_color' and NUMBERFILES > 0):
            logging.info("Folder {2}/{0} has {1} files ({3})".format(FOLDER, NUMBERFILES, PROJECT, PATH))
        else:
            errTXT = "Folder {1}/{0} NUMBER OF FILES NOT MATCH. ORIG {3} TARGET {4}  ({2})".format(FOLDER, PROJECT,
                                                                                                   PATH, nFILES,
                                                                                                   NUMBERFILES)
            logging.error(errTXT)
            sys.exit(errTXT)
    else:
        errTXT = "Folder {1}/{0} NO EXISTS ({2})".format(FOLDER, PROJECT, PATH)
        logging.error(errTXT)
        sys.exit(errTXT)


print("Lets process this manuscript!! :) ")
projectXML = os.path.join(ORIGPATH, fname, "project.xml")


# Repositorio para el archivo profundo o definitivo
TARGETROOT = os.path.join(ORIGPATH, fname, "EXPORT_ARCHIVE/")
# Repositorio de la web
WEBSITEREPO = os.path.join(ORIGPATH, fname, "EXPORT_ARCHIVE/")
# Temporal files folder
TEMPROOT = os.path.join(ORIGPATH, "TEMP/IMG_TEMP/")
# Directorio temporal para IM
IMTEMP = os.path.join(ORIGPATH,"TEMP/IMK_TEMP/")

# Primer movimiento: de directorio origen a carpeta temporal
ORIG = os.path.join(ORIGPATH, fname)
TARGET = os.path.join(TEMPROOT, fname)


isValid = check_xml(projectXML)
if not isValid:
    print("some fields missed, updating project!")
    subprocess.call(['python', 'update_xml_head.py', projectXML, REPORT])
    isValid = check_xml(projectXML)
if isValid :
    bookMeta = parse_xml(projectXML)
    BOOKDIRNAME = createFullNameProject(bookMeta['code'], bookMeta['timestamp'])
    print("target folder name : " +  BOOKDIRNAME)
    print("Start project {0} ".format(fname))

    # TODO #2
    #
    try :
        #os.mkdir(TARGET)
        os.makedirs(TARGET)
        # print(ORIG,TARGET )
        copy_tree(ORIG, TARGET)
    except:
        print("Temporal directory", TARGET, "already exists")

    try:
        os.makedirs(IMTEMP)
    except:
        print("Temporal directory", IMTEMP, "already exists")

    isReady = verifyDir(ORIG, TARGET)
    if isReady :
        if bookMeta['source'] == 'dslr':
            logging.info("Charts detect intent")
            subprocess.call(['python', 'detect_charts_normalize.py', TARGET, REPORT])
            items = getCharsAndItems(TEMPROOT, fname)
            nFiles = items['pages']
            nCharts = items['chars']
            print("nItemsB")
            print(items)
            if nCharts > 0:
                logging.info("EXECUTE_ALL SAYS: Charts was found")
            else:
                logging.error("EXECUTE_ALL SAYS: Charts NOT found")

            # cuenta archivos por si se duplican los raws
            newFilesRaw = countPreviousFiles(TARGET, 'raw', 'cr2')
            # print("Raw files after Dectect Charts")
            # print(newFilesRaw)

            # nFilesRaw = countPreviousFiles(ORIG,'raw', 'cr2'
            # countFiles(TARGET, "raw",fname,'get_files',idFiles, items['total'] )
            logging.info("Project has {0} charts".format(nCharts))
            logging.info("Project has {0} images".format(nFiles))

            if processRaw:
                print("********** process_raw.py *****************")
                logging.info("********** process_raw.py *****************")
                subprocess.call(['python', 'process_raw.py', TARGET, IMTEMP])
                countFiles(TARGET, "JPEG", fname, 'process_raw', nFiles)



