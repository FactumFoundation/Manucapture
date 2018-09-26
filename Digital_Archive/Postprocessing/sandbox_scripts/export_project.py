#!/usr/bin/python -tt
# -*- coding: utf-8 -*-
"""
Archivo para procesar exportar los archivos al repositorio de la web y poblar la BD
by jpereira :: www.jpereira.net :: info@jpereira.net
"""
import errno, os, sys, time, filecmp
from datetime import datetime
import xml.dom.minidom
import logging
import mysql.connector
from mysql.connector import errorcode
from distutils.dir_util import copy_tree
import shutil
import xml.etree.cElementTree as ET




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

def getXMLmeta(PATH):

    
    DOMTree = xml.dom.minidom.parse(PATH)
    collection = DOMTree.documentElement
    metaBook = {}
    
    meta = collection.getElementsByTagName("metadata")
    metaBook['bookname'] = meta[0].getElementsByTagName('name')[0].childNodes[0].data
    metaBook['bookcode'] = meta[0].getElementsByTagName('code')[0].childNodes[0].data
    metaBook['timestamp'] = meta[0].getElementsByTagName('timestamp')[0].childNodes[0].data
    metaBook['source'] = meta[0].getElementsByTagName('source')[0].childNodes[0].data
    if meta[0].getElementsByTagName('comment')[0].childNodes:
        metaBook['comment'] = meta[0].getElementsByTagName('comment')[0].childNodes[0].data
    else:
        metaBook['comment'] = ""
    
    target_directory = meta[0].getElementsByTagName('target_directory')[0].childNodes[0].data
    target_subdirectory = meta[0].getElementsByTagName('target_subdirectory')[0].childNodes[0].data
    metaBook['target'] = os.path.join(target_directory, target_subdirectory)
    
    logging.info("***** Start export project with code {0} ******".format(metaBook['bookcode'] ) )

    #project_counter = collection.getElementsByTagName("image_counter")[0]
    #IMAGES_IN_XML = project_counter.firstChild.data

    
    return metaBook


def parse_xml(PATH):

    tree = ET.parse( PATH )
    images = {}
    i = 0
    for elt in tree.findall('./items/item'):
       
        page_num = elt.find('page_num').text 
        tipo = elt.find('type').text 
        

        
        if tipo  == "page":
            
            image_right = elt.find('image_right').text 

            if not image_right: 
                image_right = "blank_page"  
            else:
                imgR = os.path.join(os.path.dirname(PATH), image_right )
                print( imgR )
                if not os.path.isfile(imgR): 
                    image_right = "blank_page" 
        
            images[i]={}
            images[i][0] = image_right
            images[i][1] = page_num
            images[i][2] = "R"
            i = i+1
            
            image_left = elt.find('image_left').text 
        
            if not image_left: 
                image_left = "blank_page" 
            else:
                imgL = os.path.join(os.path.dirname(PATH), image_left )
                if not os.path.isfile(imgL): 
                    image_left = "blank_page" 
                
            images[i]={}
            images[i][0] = image_left
            images[i][1] = page_num
            images[i][2] = "L"  
            i = i+1

    return images

if __name__ == '__main__':
                
    start = time.time()
    
    #TARGET, WEBSITEREPO, REPORT, USER_DB, PASS_DB, HOST_DB, DATABASE
    PATH  = sys.argv[1]
    ROOT_REPO_PATH = sys.argv[2]
    PATH2 = sys.argv[3]
    
    
    USER_DB = sys.argv[4]
    PASS_DB = sys.argv[5]
    HOST_DB = sys.argv[6]
    DATABASE = sys.argv[7]
    
        
    project = os.path.basename(os.path.normpath( PATH ))
        
    logging.info("START PROJECT {0} ".format(project) )
    

    date = time.strftime("%Y%m%d")
    
    LOGFILE = os.path.join(PATH2, "move-website-report-"+date+".txt" )

    logging.root.handlers = []
    logging.basicConfig(filename=LOGFILE,
                        filemode='a',
                        format='%(asctime)s %(name)s %(levelname)s %(message)s',
                        datefmt= '%d/%m/%Y %H:%M:%S',
                        level=logging.DEBUG)
    
    PATH_TO_PROJECT =  os.path.join(sys.argv[1], "project.xml")        
        
    add_meta = ("INSERT INTO objects "
                       "(codObject, nameProject, dateProject, status,target, source, comment ) "
                       "VALUES (%s, %s, %s, %s, %s, %s, %s)")
    add_files = ("INSERT INTO files "
                      "(idObject, fileName, thumbnail, page, position) "
                      "VALUES (%s, %s, %s, %s, %s)")
    logging.info("Start {0} {1} {2}".format(PATH,PATH2,ROOT_REPO_PATH ))
        
    try:        
        
    
        meta = getXMLmeta(PATH_TO_PROJECT)  
        
        dateprocess = datetime.strptime('{:<011d}'.format( int(meta['timestamp'] ) ), '%Y%m%d%H%M%S')

        dataMeta = ( meta['bookcode'],  meta['bookname'], dateprocess,1, meta['target'],meta['source'], meta['comment']  )
            
        timemark = datetime.strptime('{:<011d}'.format( int(meta['timestamp'] ) ), '%Y%m%d%H%M%S').strftime('%Y%m%d%H%M%S')
        #timemark = datetime.strptime(meta['timestamp'], '%Y%m%d%H%M%S').strftime('%Y%m%d%H%M%S')
        
        BOOKDIRNAME =  "{0}_{1}".format( meta['bookcode'], timemark )
        REPO_DIR = os.path.join(ROOT_REPO_PATH,BOOKDIRNAME )
        REPO_DIR_JPEG = os.path.join(REPO_DIR,"JPEG" )
        REPO_DIR_TIFFP = os.path.join(REPO_DIR,"TIFFP" )
        REPO_DIR_PDF = os.path.join(REPO_DIR,"PDF" )
            
        JPEG_DIR = os.path.join(PATH, "JPEG")
        TIFFP_DIR = os.path.join(PATH, "TIFFP")
        PDF_DIR = os.path.join(PATH, "PDF")
            
        if not os.path.exists(REPO_DIR):
            os.makedirs(REPO_DIR)
            
        cnx = mysql.connector.connect(user=USER_DB, password=PASS_DB, host=HOST_DB, database=DATABASE)
        cursor = cnx.cursor()
            
        affected_count = cursor.execute(add_meta, dataMeta)
        idObject = cursor.lastrowid 
        logging.warn("{0}".format( affected_count ) )
        logging.info("Insert object record with id {0}".format(idObject))
              
            
        #esto solo debe ser para los raws 
        if meta['source'] == 'dslr':  
            rawimages = parse_xml(PATH_TO_PROJECT)    
            rawimages_len = len(rawimages)
        elif meta['source'] == 'scanner':
            rawimages = {}
            i = 0
            lst = os.listdir(os.path.join(PATH, "TIFF"))
            lst.sort()
            for f in lst:
                if f.endswith(".tiff"):
                    rawimages[i] = f
                    i = i+1
                    
            rawimages_len = len(rawimages)
            
        for i in range(0, rawimages_len):
            if meta['source'] == 'dslr':
                name = rawimages[i][0].rsplit(".", 1)[0]
                page = rawimages[i][1]
                position = rawimages[i][2]
            elif meta['source'] == 'scanner':
                name = rawimages[i].rsplit(".", 1)[0]
                page = int(name)
                position = ""
                
            p = name.rsplit("/", 1)
            if len(p) > 1:
                name = p[1]
                    
            tiffname = name + ".tiff"
            jpegname = name + ".jpg"
                    
            dataFile = ( idObject,tiffname,jpegname, page, position  )
            affected_count2 = cursor.execute(add_files, dataFile)
            idFiles = cursor.lastrowid 
            logging.warn("{0}".format( affected_count2 ) )
            logging.info("Insert files record with id {0}".format(idFiles))
                
            
        if not os.path.exists(REPO_DIR_JPEG):
            os.makedirs(REPO_DIR_JPEG)
                
        if not os.path.exists(REPO_DIR_TIFFP):
            os.makedirs(REPO_DIR_TIFFP)
                
        if not os.path.exists(REPO_DIR_PDF):
            os.makedirs(REPO_DIR_PDF)
            
        copy_tree(TIFFP_DIR, REPO_DIR_TIFFP)
        copy_tree(JPEG_DIR, REPO_DIR_JPEG)
        copy_tree(PDF_DIR, REPO_DIR_PDF)
            
        TIFFPready = verifyDir(TIFFP_DIR, REPO_DIR_TIFFP)
        JPEGready = verifyDir(JPEG_DIR, REPO_DIR_JPEG)
            
        if TIFFPready:
            shutil.rmtree(TIFFP_DIR)
                
        if JPEGready:
            shutil.rmtree(JPEG_DIR)
                
        cnx.commit()
        cursor.close()
        cnx.close()
            

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
        
    except mysql.connector.IntegrityError as err:
        logging.warn("Error at MySQL: {}".format(err) )
    
    except OSError as e:
        if e.errno != errno.EEXIST:
            logging.error( e )
            print(e)
            sys.exit(e.errno)
    

