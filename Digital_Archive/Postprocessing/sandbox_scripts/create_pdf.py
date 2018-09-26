#!/usr/bin/python -tt
# -*- coding: utf-8 -*-
"""
Archivo para crear PDF a partir de los TIFF
by jpereira :: www.jpereira.net :: info@jpereira.net
"""
import glob,sys,errno
import logging
import subprocess, shlex
import os.path
from os.path import basename
import xml.dom.minidom
import xml.etree.cElementTree as ET


RESOURCES = "/home/user/scripts/RESOURCES"
BLANK_PAGE = os.path.join( RESOURCES,"blank_page.tif")


def getSource(PATH):
        
    DOMTree = xml.dom.minidom.parse(PATH)
    collection = DOMTree.documentElement
    meta = collection.getElementsByTagName("metadata")
    source = meta[0].getElementsByTagName('source')[0].childNodes[0].data
    
    return source

def getFiles(PATH):

    ruta = os.path.join(PATH, "*.tiff")
    fileList = sorted( glob.glob(ruta))
    fileString = " ".join(fileList )
    return fileString


def createPDF(STRINGofFILES, OUT, projectName,IMTEMP):
    
    OUT = os.path.join(OUT, projectName+".pdf")
    PDF = "convert -monitor -limit memory 2GiB -limit map 4GiB -define registry:temporary-path={2} {0} -resize x1000 -compress jpeg -quality 75 {1}".format(STRINGofFILES, OUT, IMTEMP ) 
    p = subprocess.Popen( shlex.split(PDF), stdout=subprocess.PIPE )
    out = p.communicate()
    print(out)
    
"""
def getFilesFromXML(projectXML,TIFF_DIR):
    
    DOMTree = xml.dom.minidom.parse(projectXML)
    collection = DOMTree.documentElement
        
    items = collection.getElementsByTagName("item")
        
    images = []
    
    for item in items:
       
        tipo = item.getElementsByTagName('type')[0]
        image_righta = item.getElementsByTagName('image_right')[0]
        image_left = item.getElementsByTagName('image_left')[0]
        
        if tipo.childNodes[0].data  == "page":
            if image_righta.childNodes:
                fNameA = basename( os.path.splitext( image_righta.childNodes[0].data )[0])          
                images.append( os.path.join( TIFF_DIR,fNameA+".tiff") )
            if image_left.childNodes: 
                fNameB = basename( os.path.splitext( image_left.childNodes[0].data  )[0])
                images.append(  os.path.join( TIFF_DIR,fNameB+".tiff")   )
            
    fileString = " ".join(images )
    return fileString
"""

def getFilesFromXML(projectXML,TIFF_DIR):
    
    tree = ET.parse( projectXML )
    images = []
    for elt in tree.findall('./items/item'):
        
        if elt.tag == "item":  
            tipo = elt.find('type').text  
         
            if tipo == "page":
                image_left  = elt.find('image_left').text    
                image_right = elt.find("image_right").text


                if image_right:
                    fNameA = basename( os.path.splitext( image_right )[0])  
                    imgR = os.path.join( TIFF_DIR,fNameA+".tiff")
                    if os.path.isfile(imgR):        
                        images.append( imgR )
                    else:
                        images.append( BLANK_PAGE )
                    
                else:
                    images.append( BLANK_PAGE )   
                 
                
                if image_left:
                    fNameB = basename( os.path.splitext( image_left  )[0])
                    imgL = os.path.join( TIFF_DIR,fNameB+".tiff")
                    if os.path.isfile(imgL):
                        images.append(imgL)
                    else:
                        images.append( BLANK_PAGE )
                      
                else:
                    images.append( BLANK_PAGE )  
            
    fileString = " ".join(images )
    return fileString

if __name__ == '__main__':
        
    try:
    
        PATH = sys.argv[1]
        IMTEMP = sys.argv[2]
        
        PDF_DIR = os.path.join(PATH, "PDF")
        TIFF_DIR = os.path.join(PATH, "TIFF")
        
        projectName = os.path.basename(os.path.normpath( PATH ))

        
        if not os.path.exists(PDF_DIR):
            os.makedirs(PDF_DIR)
        
        LOGFILE = os.path.join(PATH, "jobfile.txt" )
        
        logging.root.handlers = []
        logging.basicConfig(filename=LOGFILE,
                            filemode='a',
                            format='%(asctime)s %(name)s %(levelname)s %(message)s',
                            datefmt= '%d/%m/%Y %H:%M:%S',
                            level=logging.DEBUG)

        
        projectXML = os.path.join(PATH, "project.xml")
        
        src = getSource(projectXML)
        
        if src == "dslr":
            fileList = getFilesFromXML(projectXML,TIFF_DIR)
            
        elif src == "scanner":
            fileList = getFiles(TIFF_DIR)
        
        print(fileList)
        
        createPDF(fileList, PDF_DIR, projectName, IMTEMP)
        
        PDFPATH = os.path.join(PDF_DIR, projectName+".pdf" )
        
        if os.path.isfile(PDFPATH) :
            size = os.path.getsize(PDFPATH) / 1024 
            logging.info("PDF file exists ({0}) with size {1} Kb".format( PDFPATH, size) )
            print("PDF file exists ({0}) with size {1} Kb".format( PDFPATH, size) )
        else:
            logging.error("PDF file NOT exists error to create!!!" )
            print("PDF file NOT exists error to create!!!" )            
            
    except subprocess.CalledProcessError as e:
        print e.output
        logging.error( e.output )      
        
    except OSError as e:
        if e.errno != errno.EEXIST:
            logging.error( e )
            print(e)
            sys.exit(e.errno)

