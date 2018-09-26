#!/usr/bin/python -tt
# -*- coding: utf-8 -*-
"""
Archivo para crear perfiles ICC y parametrizar revelado raw
by jpereira :: www.jpereira.net :: info@jpereira.net
"""
import errno, os
import xml.etree.cElementTree as ET
from shutil import copyfile
import logging
import sys  

GENERIC_ICC = "RESOURCES/generic_icc_profile.icc" 

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


def update_xml_project(PATH):
    
    tree = ET.parse( PATH  )
    root =  tree.getroot() 
    
    j = 0
    
    for elt in root.findall('./items/'):    
        
        if elt.find('icc_profile_right') is None:
            profile_right = ET.Element("icc_profile_right")
            profile_right.text = "PROFILES/generic_icc_profile.icc"
            elt.insert(2, profile_right)
        else:
            elt.find('icc_profile_right').text = "PROFILES/generic_icc_profile.icc"
            
        if elt.find('icc_profile_left') is None:
            icc_profile_left = ET.Element("icc_profile_left")
            icc_profile_left.text = "PROFILES/generic_icc_profile.icc"
            elt.insert(3, icc_profile_left)
        else:
            elt.find('icc_profile_left').text = "PROFILES/generic_icc_profile.icc"

        if elt.find('process_exposure_factor_left') is None:
            process_exposure_factor_left = ET.Element("process_exposure_factor_left")
            process_exposure_factor_left.text = "1"
            elt.insert(4, process_exposure_factor_left)
        else:
            elt.find('process_exposure_factor_left').text = "1"
            
        if elt.find('process_exposure_factor_right') is None:
            process_exposure_factor_right = ET.Element("process_exposure_factor_right")
            process_exposure_factor_right.text = "1"
            elt.insert(5, process_exposure_factor_right)
        else:
            elt.find('process_exposure_factor_right').text = "1"
        
        elt.find('type').text = "page"   
        elt.find('page_num').text = "{}.0".format(j) 
        j = j + 1
        
    indent(root)                      
    tree.write( PATH, encoding="utf-8", xml_declaration=True )
    
    """
    tree = ET.parse( PATH )
    root =  tree.getroot()
    
    i = 0
    j = 0
    for elt in tree.iter():
    
        if elt.tag == "item":
            elt.set('id', str(i) )
            i = i + 1
        
        if elt.tag == "icc_profile_left":
            elt.text = "PROFILES/generic_icc_profile.icc"

        if elt.tag == "icc_profile_right":
            elt.text = "PROFILES/generic_icc_profile.icc"
            
        if elt.tag == "process_exposure_factor_right":
            elt.text = "1"
            
        if elt.tag == "process_exposure_factor_left":
            elt.text = "1"
            
        #otra version
        
            profile_left = ET.Element("icc_profile_left")
            profile_left.text = "PROFILES/generic_icc_profile.icc"
            elt.insert(0, profile_left)
            
            profile_right = ET.Element("icc_profile_right")
            profile_right.text = "PROFILES/generic_icc_profile.icc"
            elt.insert(1, profile_right)
            
            exposure_factor_right = ET.Element("process_exposure_factor_right")
            exposure_factor_right.text = "1"
            elt.insert(2, exposure_factor_right)
    
            exposure_factor_left = ET.Element("process_exposure_factor_left")
            exposure_factor_left.text = "1"
            elt.insert(3, exposure_factor_left)
            

        if elt.tag == "type":
            elt.text = "page"
                
        if elt.tag == "page_num":
            elt.text = "{}.0".format(j)
            j = j + 1
    
    indent(root)                      
    tree.write( PATH, encoding="utf-8", xml_declaration=True )
    """
    
if __name__ == '__main__':

    try:      
        
        PATH = sys.argv[1] #ruta al directorio raiz  del proyecto          

        if not os.path.exists( os.path.join(PATH,"PROFILES") ):
            os.makedirs( os.path.join(PATH,"PROFILES" ))
        
        copyfile(GENERIC_ICC, os.path.join(PATH,"PROFILES","generic_icc_profile.icc" ) )
        
        if os.path.isfile(os.path.join(PATH,"PROFILES","generic_icc_profile.icc" ) ):
        
            projectXML = os.path.join(PATH,"project.xml")
            projectXMLBackUp = os.path.join(PATH,"project.xml.backup")
            
            copyfile(projectXML, projectXMLBackUp )
            
            if os.path.isfile(projectXMLBackUp): 
                update_xml_project(projectXML)
            else:
                print("no xml copia")

            
            
        
    except OSError as e:
        if e.errno != errno.EEXIST:
            logging.error( e )
            print(e)
            sys.exit(e.errno)
