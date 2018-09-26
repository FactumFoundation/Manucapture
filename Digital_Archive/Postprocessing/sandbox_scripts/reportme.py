#!/usr/bin/python -tt
# -*- coding: utf-8 -*-
"""
Archivo para procesar raws con gestion del color y creacion de derivados jpeg y tiff
by jpereira :: www.jpereira.net :: info@jpereira.net
"""
import os, sys,time, errno, glob
import logging
import xml.dom.minidom


def parse_xml(PATH):
        
    DOMTree = xml.dom.minidom.parse(PATH)
    collection = DOMTree.documentElement
    images = {}
    meta = collection.getElementsByTagName("metadata")
    images['1_name'] = meta[0].getElementsByTagName('name')[0].childNodes[0].data
    #bookname.childNodes[0].data
    images['2_code'] = meta[0].getElementsByTagName('code')[0].childNodes[0].data
    images['3_timestamp']  = meta[0].getElementsByTagName('timestamp')[0].childNodes[0].data
    
    images['4_target_dir'] = meta[0].getElementsByTagName('target_directory')[0].childNodes[0].data
    images['5_target_subdir'] = meta[0].getElementsByTagName('target_subdirectory')[0].childNodes[0].data
    
    images['6_source'] = meta[0].getElementsByTagName('source')[0].childNodes[0].data
    
    items = collection.getElementsByTagName("item")
    print("longitud de items {}".format( (len(items) * 2) ))
    images['7_image']={}
    i = 0
    for item in items:
        images['7_image'][i]={}
        if item.getElementsByTagName('image_right')[0].childNodes:
        	images['7_image'][i][0] = item.getElementsByTagName('image_right')[0].childNodes[0].data
        else:
        	images['7_image'][i][0] = 0
        	itemCount = (i/2)+1
        	print( itemCount)
        	logging.error("ERROR AT ITEM COUNT {} AT PROJECT {}".format(itemCount,images['2_code'] ) )
        images['7_image'][i][1] = item.getElementsByTagName('type')[0].childNodes[0].data

        i = i+1
        images['7_image'][i]={}
        if item.getElementsByTagName('image_left')[0].childNodes:
        	images['7_image'][i][0] = item.getElementsByTagName('image_left')[0].childNodes[0].data
        else:
        	images['7_image'][i][0] = 0
        	itemCount = (i/2)+1
        	print( itemCount)
        	logging.error("ERROR AT ITEM COUNT {} AT PROJECT {}".format(itemCount,images['2_code'] ) )
        images['7_image'][i][1] = item.getElementsByTagName('type')[0].childNodes[0].data

        i = i+1

    images['8_XMLimgCount'] = i
    return images

if __name__ == '__main__':
    
   

    try:
        PATH = sys.argv[1]
        XMLPATH = os.path.join(PATH, "project.xml" )
        XML = parse_xml(XMLPATH)
        i = 0
        
        date = time.strftime("%Y%m%d")         
        logging.root.handlers = []
        logging.basicConfig(filename=os.path.join("/home/user/scripts/reports", "report-"+date+".txt" ),
                            filemode='a',
                            format='%(asctime)s %(name)s %(levelname)s %(message)s',
                            datefmt= '%d/%m/%Y %H:%M:%S',
                            level=logging.DEBUG)  
        j = 0
        for key, value in sorted(XML.items()):
            
            if type(value) is dict:
                for key2, value2 in value.items():
                    if value2[0] != 0:
                    	ROUTE = os.path.join(PATH, value2[0] )
                    	if os.path.isfile( ROUTE ) and os.access(ROUTE, os.R_OK):
                        	logging.info("File {} exists with type {}".format(ROUTE,value2[1] ) )
                        	print( "File {} exists with type {}".format(ROUTE,value2[1] ) )
                        	j = j +1
                    	else:
                        	logging.info("File {} NO exists with type {}".format(ROUTE,value2[1] ) )
                        	print("File {} NO exists with type {}".format(ROUTE,value2[1] ) )
                    else:
                    	print("_NOFILE_AT_XML_")
                    	logging.info("_NOFILE_AT_XML_" )
            else:
                print("{} : {}".format(key, value) ) 
                logging.info("{} : {}".format(key, value) )
                
            print("9_XMLrawLoop {}".format(j) )
            logging.info("9_XMLrawLoop {}".format(j) )
            
            lst = len(glob.glob1(os.path.join(PATH, "raw" ),"*.cr2"))
            print( "10_cr2CounterFilesystem {}".format( lst )  )
            
            
            
            
 
                


        
      

    
    except OSError as e:
        if e.errno != errno.EEXIST:
            logging.error( e )
            print(e)
            sys.exit(e.errno)

