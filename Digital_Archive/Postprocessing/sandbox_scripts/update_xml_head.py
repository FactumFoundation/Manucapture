#!/usr/bin/python -tt
# -*- coding: utf-8 -*-
"""
Archivo para modificar cabeceras del XML
by jpereira :: www.jpereira.net :: info@jpereira.net
"""
import errno, os, time
import xml.etree.cElementTree as ET
import logging
import datetime
import ntpath
import sys 
from shutil import copyfile
import unicodedata
import os.path, datetime



#PATH = "/Users/jpereira/Documents/LiClipse_Workspace/factum-daguestan/555"
#REPORT = "/Volumes/Macintosh_HD_DATA/REPORTS/";

def modification_date(filename):
    t = os.path.getmtime(filename)
    return datetime.datetime.fromtimestamp(t) 


def backup(PATH):
    projectXML = PATH
    projectXMLBackUp = os.path.join(os.path.dirname( PATH ),"project.xml.backup_meta")
            
    copyfile(projectXML, projectXMLBackUp )
    
    if os.path.isfile(projectXMLBackUp): 
        s = True
        logging.info("BACKUP PROJECT.XML FILE {}".format(PATH) )
    else:
        s = False
        
    return s


def indent(elem, level=0):
    i = "\n" + level*"  "
    j = "\n" + (level-1)*"  "
    if len(elem):
        if not elem.text or not elem.text.strip():
            elem.text = i + "  "
        if not elem.tail or not elem.tail.strip():
            elem.tail = i
        for subelem in elem:
            indent(subelem, level+1)
        if not elem.tail or not elem.tail.strip():
            elem.tail = j
    else:
        if level and (not elem.tail or not elem.tail.strip()):
            elem.tail = j
    return elem 

def upadate_xml_head(PATH,d):
    
    
    
    logging.info("Date of digitization {}".format(d) )
    logging.info("UPDATE XML HEAD {}".format(PATH) )
    et = ET.parse( PATH )
    root =  et.getroot()

    metadata = root.find("metadata")
    name_xml = metadata.find('name')
    code_xml = metadata.find('code')
    
    if name_xml.text != None:
        name = name_xml.text
    else:
        name = ""

    name_xml.text = "Book " + str(code_xml.text) +" "+ name
    timestamp_xml = ET.SubElement(metadata, 'timestamp')
    #current_date = datetime.datetime.now()
    #timestamp = str(current_date.year) + str(current_date.month) + str(current_date.day) + str(current_date.hour) + str(
    #        current_date.minute) + str(current_date.second)
            
    #timestamp = "{:4d}{:02d}{:02d}{:02d}{:02d}{:02d}".format(current_date.year, current_date.month, current_date.day, current_date.hour, current_date.minute, current_date.second)
    timestamp = "{:4d}{:02d}{:02d}{:02d}{:02d}{:02d}".format(d.year, d.month, d.day, d.hour, d.minute, d.second)

    timestamp_xml.text = timestamp
    target_directory_xml = ET.SubElement(metadata, 'target_directory')
    target_directory_xml.text = "1_Oriental_Manuscripts_Fund"
    target_subdirectory_xml = ET.SubElement(metadata, 'target_subdirectory')
    target_subdirectory_xml.text = "F14"
    uploaded_xml = ET.SubElement(metadata, 'uploaded')
    uploaded_xml.text = "True"
    source_xml = ET.SubElement(metadata,'source')
    source_xml.text = "dslr"
    
    indent(root)
    
    et.write( PATH , encoding="utf-8", xml_declaration=True )
    

if __name__ == '__main__':
    
            
    try:
        
        PATH = sys.argv[1]
        REPORT = sys.argv[2]
        
        date = time.strftime("%Y%m%d") 
        LOGFILE = os.path.join(REPORT, "process-report-"+date+".txt" )
    
        logging.root.handlers = []
        logging.basicConfig(filename=LOGFILE,
                            filemode='a',
                            format='%(asctime)s %(name)s %(levelname)s %(message)s',
                            datefmt= '%d/%m/%Y %H:%M:%S',
                            level=logging.DEBUG)

        d = modification_date( PATH )
        s = backup(PATH)
        if s:
            upadate_xml_head(PATH,d)

    
    except OSError as e:
        if e.errno != errno.EEXIST:
            logging.error( e )
            print(e)
            sys.exit(e.errno)