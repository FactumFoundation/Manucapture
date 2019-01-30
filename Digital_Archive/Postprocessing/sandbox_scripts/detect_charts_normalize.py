#!/usr/bin/python -tt
# -*- coding: utf-8 -*-
"""
Archivo para crear perfiles ICC y parametrizar revelado raw
by jpereira :: www.jpereira.net :: info@jpereira.net
"""
import errno, os, sys ,shlex,math,glob, ntpath,time
from itertools import islice
import subprocess
import xml.etree.cElementTree as ET
from shutil import copyfile
import logging

from PIL import Image
import numpy as np
import scipy.cluster
from numpy import double

CHT = "RESOURCES/ColorChecker.cht" 
CIE = "RESOURCES/ColorChecker.cie"


def kmeansClusters(filename):
    NUM_CLUSTERS = 5
    im = Image.open(filename) 
    im = im.resize((150, 150))
    ar = np.asarray(im)
    shape = ar.shape
    ar = ar.reshape(scipy.product(shape[:2]), shape[2]).astype(float)
    #print 'localiza clusters'
    codes, dist = scipy.cluster.vq.kmeans(ar, NUM_CLUSTERS)
    #calculo la desviación entre los colores de cada cluster y los clusteres en si para cuanto
    # menos variación más probable que sea un fondo negro.
    devClus = []
    i = 0
    for c in codes:
        devClus.append(np.std(c) )
        i = i+1
    desv =  np.std( devClus )
    """
    #para sacar el color medio por si hace falta
    vecs, dist = scipy.cluster.vq.vq(ar, codes)        
    counts, bins = scipy.histogram(vecs, len(codes))    
    
    index_max = scipy.argmax(counts)                    
    peak = codes[index_max]
    colour = ''.join(chr(int(c)) for c in peak).encode('hex')
    
    luma = 0.2126*peak[0] + 0.7152*peak[1] + 0.0722*peak[2]
    print 'most frequent is %s (#%s)' % (peak, colour)
    print(luma)
    """
    return desv

def call_dcraw(rawname):
    dcraw = "dcraw -T -v -o 0 -W -w -g 2.2 0 -h {0}".format(rawname)
    subprocess.check_call(shlex.split(dcraw) )
    
def backup(PATH):
    projectXML = os.path.join(PATH,"project.xml")
    projectXMLBackUp = os.path.join(PATH,"project.xml.backup")
    copyfile(projectXML, projectXMLBackUp )
    if os.path.isfile(projectXMLBackUp): 
        s = True
    else:
        s = False
    return s
    
    
def call_scanin(tiffName): 
           
    
    scanin = "scanin -v -a {0} {1} {2}".format( tiffName, CHT, CIE )
    #Cooprof
    #print( scanin )
    s = subprocess.Popen(shlex.split(scanin), stdout=subprocess.PIPE )
    cadena = "Writing output values to file" 
    cadena2 = "Robust mean angle"
    PATH = os.path.dirname(TIFFILE)
    FILENAME = os.path.basename(TIFFILE)
    NAMENOEXT = os.path.splitext(FILENAME)[0]
    o = False
    while True:
        line = s.stdout.readline()
        if line != b'':
            if line.startswith(cadena2 ) :
                meanAngle = double(line[19:28])
                print("Mean Angle = " + line[19:28])
            if line.startswith(cadena ) and meanAngle > -1 :
                print("CHART EXISTS")
                #o = NAMENOEXT+".cr2"
                o = True
            else:
                o = False
        else:
            break
        
    ti3 = os.path.join(PATH, NAMENOEXT+".ti3" ) 
    if os.path.isfile(ti3):
        os.remove( ti3 )
        
    return o


def getImgFromXML(PATH):
    
    tree = ET.parse( os.path.join(PATH, "project.xml" )  )
        
    o = {}
    i = 0
    #for elt in tree.iter():
    for elt in tree.findall('./items/item'):
        
        if i < 2:
            
            o[i] = {}
            o[i][0] =  elt.find('image_left').text
            o[i][1] =  elt.find('image_right').text       
            i = i+1
            
        
    return o


def verificaResultados(lst):
    o = 0
    for i in range(len(lst)):  
        if lst[i][1] == "chart" and lst[i][2] == "image":
            #print "chart" 
            o = 0
        elif lst[i][1] == "chart" and lst[i][2] == "noImage":
            print "error en logica, carta como no image" 
            logging.error("Error en logica, carta como no image" )
            o = 1   
        
        if lst[i][1] == "noChart" and lst[i][2] == "noImage":
            #print "noChart"
            o = o + 0
        elif  lst[i][1] == "noChart" and lst[i][2] == "image":
            print "se ha detectado una posible imagen que no es carta"
            logging.error("Se ha detectado una posible imagen que no es carta" )
            o = o + 0
    if o > 0:
        s = False
    elif o == 0:
        s = True
        
    return s


def recuentaCartas(lst):
    imgs = {}
    imgs[1] = 0
    imgs[0] = 0

    for i in range(len(lst)):  
        if lst[i][1] == "chart" and lst[i][2] == "image":
            imgs[0] = imgs[0] + 1
        
        if lst[i][1] == "noChart" and lst[i][2] == "noImage":
            imgs[1] = imgs[1] + 1 
            
    if imgs[0] == 2:
        print("tienes dos cartas para procesar")
        logging.info("DETECT CHARTS SAYDS: PROJECT HAS TWO CHARTS" )
    if imgs[0] == 1:
        print("tiene una carta para procesar")
        logging.info("DETECT CHARTS SAYDS: PROJECT HAS ONE CHART" )
    if imgs[1] == 2:
        print("tiene dos imagenes sin informacion")
        logging.info("DETECT CHARTS SAYDS: PROJECT HAS TWO NO-PAGES" )
    if imgs[1] == 1:
        print("tiene una imagen sin información")  
        logging.info("DETECT CHARTS SAYDS: PROJECT HAS ONE NO-PAGES" )
            
    if imgs[0] == 0:
        print("no de han detectado cartas")
        logging.error("DETECT CHARTS SAYDS: IT IS IMPOSSIBLE TO FIND CHARS" )
    if imgs[1] == 0: 
        print("no hay imágenes sin informacion")  
        logging.error("DETECT CHARTS SAYDS: NO-PAGES NOT FOUND" ) 
            
    if imgs[0] > 2:
        print("Hay más de dos cartas")
        logging.error("DETECT CHARTS SAYDS: MORE TWO CHARTS!" ) 
        sys.exit("Hay más de dos cartas")
    if imgs[1] > 2: 
        print("Hay más de dos imágenes sin fondo")  
        logging.error("DETECT CHARTS SAYDS: MORE TWO NO-IMAGES!" )  
        sys.exit("Hay más de dos imágenes sin fondo")
            
    #if (imgs[0] == 2 and imgs[1] == 1) or  (imgs[1] == 2 and imgs[0] == 1) or (imgs[1] > 2 and imgs[0] < 2 )  or (imgs[0] > 2 and imgs[1] < 2 ):
        #print("No hay el mismo número de imagens de cartas que sin información")
        #logging.error("DETECT CHARTS SAYDS: There is not the same number of images of CHARTS that without NO-PAGES" ) 
        #sys.exit( "DETECT CHARTS SAYDS: There is not the same number of images of CHARTS that without NO-PAGES" )
        

    return imgs


def duplicaArchivos(PATH, arch):
    orig = os.path.join(PATH,"raw", arch)
    new = os.path.join(PATH,"raw", "duplicate_"+arch )
    copyfile(orig, new )
    if os.path.isfile(new): 
        s = True
    else:
        s = False
    return s

def editarXML(PATH, lst):
    
    nCartas = recuentaCartas(lst)
    #print("numero de cartas "+str(nCartas) )
    
    if backup(PATH):
        if nCartas[0] == 1:
            add_new_xml_item(PATH) 
    
        tree = ET.parse( os.path.join(PATH, "project.xml" )  )
        
        root =  tree.getroot()
        
        t =  root.findall('./items/item')              
     
        for i in range(len(lst)):
            
            #print(lst[i][1]+" "+lst[i][2]+" "+lst[i][3])
            
            if nCartas[1] == 2:
                if lst[i][1] == 'noChart' and lst[i][2] == 'noImage' and lst[i][3] == 'left':
                    t[0].find('image_left').text = "raw/"+lst[i][0]
                    #print("ok1")
                    
                    
                elif lst[i][1] == 'noChart' and lst[i][2] == 'noImage' and lst[i][3] == 'right':
                    t[0].find('image_right').text = "raw/"+lst[i][0]
                    #print("ok2")
                    
                t[0].find('type').text = 'noPage'
                #print("1 nopage")
                
            elif nCartas[1] == 1:
                if lst[i][1] == 'noChart' and lst[i][2] == 'noImage':
                    t[0].find('image_left').text = "raw/"+lst[i][0]
                    if duplicaArchivos(PATH, lst[i][0] ):
                        t[0].find('image_right').text = "raw/duplicate_"+lst[i][0]
                        #print("ok6")
                t[0].find('type').text = 'noPage' 
                #print("2 nopage")   
                
            else: 
                t[0].find('type').text = 'page'
                #print("3 page")   
                
            
            
            if nCartas[0] == 2:
             
                if lst[i][1] == 'chart' and lst[i][2] == 'image' and lst[i][3] == 'left':
                    t[1].find('image_left').text = "raw/"+lst[i][0]
                    #print("ok3")
                    
                elif lst[i][1] == 'chart' and lst[i][2] == 'image' and lst[i][3] == 'right':
                    t[1].find('image_right').text = "raw/"+lst[i][0]
                    #print("ok4")
                    
                t[1].find('type').text = 'chart'
                #print("4 chart")
                
            elif nCartas[0] == 1:
                
                if lst[i][1] == 'chart' and lst[i][2] == 'image':
                    
                    t[1].find('image_left').text = "raw/"+lst[i][0]
                    if duplicaArchivos(PATH, lst[i][0] ):
                        t[1].find('image_right').text = "raw/duplicate_"+lst[i][0]
                        #print("ok5")
                        
                    
                t[1].find('type').text = 'chart'
                #print("5 chart")
            else: 
                t[0].find('type').text = 'page' 
                #print("6 page")
                    
                    
        tree.write( os.path.join(PATH, "project.xml" ), encoding="utf-8", xml_declaration=True )
        return True
    else:
        print("Impossible to create the project.xml backup")
        logging.error("Impossible to create the project.xml backup" )
        return False

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

def add_new_xml_item(PATH):
    
    tree = ET.parse( os.path.join(PATH, "project.xml" ) )
    
    root =  tree.getroot()
    items = root.find("items")
        
    it_root = ET.Element('item')
    ET.SubElement(it_root, 'image_left')
    ET.SubElement(it_root, 'image_right')
    ET.SubElement(it_root, 'icc_profile_left')
    ET.SubElement(it_root, 'icc_profile_right')
    ET.SubElement(it_root, 'process_exposure_factor_left')   
    ET.SubElement(it_root, 'process_exposure_factor_right') 
    ET.SubElement(it_root, 'page_num') 
    ET.SubElement(it_root, 'type')  

    items.insert(0,it_root )
    indent(root)    
    #ET.dump(root)

    tree.write(  os.path.join(PATH, "project.xml" ), encoding="utf-8", xml_declaration=True )
    
    
def uptate_all_xml(PATH):
    
    
    #print("update xml go!")
    """
    tree = ET.parse( os.path.join(PATH, "project.xml" ) )
    i = 0
    j = 0

    for elt in tree.iter():
    
        if elt.tag == "item":

            elt.set('id', str(i) )
            i = i + 1
            
            profile_left = ET.Element("icc_profile_left")
            profile_left.text = ""
            elt.insert(0, profile_left)
            
            profile_right = ET.Element("icc_profile_right")
            profile_right.text = ""
            elt.insert(1, profile_right)
            
            exposure_factor_right = ET.Element("process_exposure_factor_right")
            exposure_factor_right.text = "0"
            elt.insert(2, exposure_factor_right)
    
            exposure_factor_left = ET.Element("process_exposure_factor_left")
            exposure_factor_left.text = "0"
            elt.insert(3, exposure_factor_left)
            
        if elt.tag == "type":
            #print(elt.text)
            if ( str(elt.text) != "chart") and (str(elt.text) != "noPage"):
                elt.text = "page"
                #print("cambia to "+elt.text) 

        if elt.tag == "page_num":
            if i > 1:
                elt.text = "{}.0".format(j)
                #print(elt.text)
                j = j + 1
            else:
                elt.text = "0.0"
                
        tree.write( os.path.join(PATH, "project.xml" ), encoding="utf-8", xml_declaration=True )
                          
        #tree.write( os.path.join(PATH, "project.xml" ), encoding="utf-8", xml_declaration=True, pretty_print=True )
        """
   
    tree = ET.parse( os.path.join(PATH, "project.xml" ) )
    root =  tree.getroot()
    
    i = 0
    j = 0    
    for elt in tree.iter():
    
        if elt.tag == "item":
    
            elt.set('id', str(i) )
            i = i + 1
            
            profile_left = ET.Element("icc_profile_left")
            profile_left.text = ""
            elt.insert(0, profile_left)
            
            profile_right = ET.Element("icc_profile_right")
            profile_right.text = ""
            elt.insert(1, profile_right)
            
            exposure_factor_right = ET.Element("process_exposure_factor_right")
            exposure_factor_right.text = "1"
            elt.insert(2, exposure_factor_right)
    
            exposure_factor_left = ET.Element("process_exposure_factor_left")
            exposure_factor_left.text = "1"
            elt.insert(3, exposure_factor_left)
            
        if elt.tag == "type":
            print(elt.text)
            if ( str(elt.text) != "chart") and (str(elt.text) != "noPage"):
                elt.text = "page"
                print("cambia"+elt.text) 

        if elt.tag == "page_num":
            if i > 1:
                elt.text = "{}.0".format(j)
                #print(elt.text)
                j = j + 1
            else:
                elt.text = "0.0"
    indent(root)    
    #ET.dump(root)
    tree.write(  os.path.join(PATH, "project.xml" ), encoding="utf-8", xml_declaration=True )

def removeValidPages(lst):
    '''
    Elimina páginas validas, si solo hay una carta entre los os primeros items. 
    Se parte que la pagina valida esta en item 2. 
    Si estuviese de primera se entenderia que no existen cartas en procesos anteriores
    '''
    
    tamanho = len(lst) -1
    pos = {}
    pareja = 0
  
    j = 0
    for i in range(len(lst)): 
        if lst[i][1] == "noChart" and lst[i][2] == "image":
            pos = i
            anterior = i - 1
            posterior = i + 1
            j = j +1
            
    if pos > 2:
        if pos == tamanho: 
            pareja = anterior
            
        if pos == tamanho - 1: 
            pareja = posterior
            
        if pareja > 0:
            del lst[ pos ]
            del lst[ pareja ]
    
    return lst            
            
                    
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
        
        
        
        RAWPATH = os.path.join(PATH, "raw" ) 
        lst = getImgFromXML(PATH)      
        print(lst)
        #lst = os.listdir(RAWPATH)
        
        #full_list = [os.path.join(RAWPATH,i) for i in lst]
        #time_sorted_list = sorted(full_list, key=os.path.getmtime)
        #lst.sort()      
        
        i = 0 
        j = 0
        K = 0
        chart = {}
        tiffData = {}
        for i in range(len(lst)):
            #print( os.path.basename( lst[i][0] ))
            #print(os.path.basename( lst[i][1] ))
            
            for j in range(len(lst[i])):
                
                if lst[i][j] != None:
                    print( os.path.basename( lst[i][j] ))
                    fname =  os.path.basename( lst[i][j] )
                    (name,ext) = os.path.splitext(fname)
                    #print(fname)
                    RAWFILE = os.path.join(PATH, "raw", fname ) 
                    
                    if os.path.isfile(RAWFILE):
                        
                        call_dcraw(RAWFILE)
                        tiff = "{}.tiff".format(name)
                        TIFFILE = os.path.join(PATH, "raw", tiff ) 
                        
                        if os.path.isfile( TIFFILE ):
                            tiffData[K] = {}
                            NAMENOEXT = os.path.splitext( os.path.basename(TIFFILE) )[0]
                            tiffData[K][0] = NAMENOEXT+".cr2"
                            carta = call_scanin(TIFFILE)
                            if carta:
                                #chart[j] = carta
                                tiffData[K][1] = "chart"
                                #j = j + 1
                            else:
                                tiffData[K][1] = "noChart"  
                            
                            kmeans = kmeansClusters(TIFFILE)
                            print("kmenas"+str(kmeans) )
                            
                            if kmeans < 1:    
                                tiffData[K][2] = "noImage"
                            else:
                                tiffData[K][2] = "image"
                            
        
                            if NAMENOEXT.find("A") > 0:
                                tiffData[K][3] = "left"
                            
                            elif NAMENOEXT.find("B") > 0: 
                                tiffData[K][3] = "right"
                            
                            os.remove(TIFFILE)
                            K = K + 1    
                        #print(tiffData)
                    #i = i + 1
        if verificaResultados(tiffData):
            cartas = recuentaCartas(tiffData)  
            #if cartas[0] == 2:
            
            tiffData = removeValidPages(tiffData)
            s = editarXML(PATH, tiffData)
            if s:
                uptate_all_xml(PATH)
        print(tiffData)
       
        
    except OSError as e:
        if e.errno != errno.EEXIST:
            logging.error( e )
            print(e)
            sys.exit(e.errno)