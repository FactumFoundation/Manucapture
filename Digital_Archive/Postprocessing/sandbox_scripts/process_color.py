#!/usr/bin/python -tt
# -*- coding: utf-8 -*-
"""
Archivo para crear perfiles ICC y parametrizar revelado raw
by jpereira :: www.jpereira.net :: info@jpereira.net
"""
import errno, os, sys ,shlex,math,glob, ntpath
from itertools import islice
import subprocess
import xml.etree.cElementTree as ET
import xml.dom.minidom
from shutil import copyfile
import logging
from _elementtree import tostring
import sys  

CHT = "RESOURCES/ColorChecker.cht" 
CIE = "RESOURCES/ColorChecker.cie"


#CHT = "/Users/jpereira/Documents/LiClipse_Workspace/factum-daguestan/SCRIPTS/RESOURCES/ColorChecker.cht" 
#CIE = "/Users/jpereira/Documents/LiClipse_Workspace/factum-daguestan/SCRIPTS/RESOURCES/ColorChecker.cie"

 

def raw_process(rawname, position, id_char):
        #print(rawname)
        #print(position)
        
        ti3_no_ext = rawname.rsplit(".", 1)[0]
        tiffname = rawname.rsplit(".", 1)[0] + ".tiff"
        ti3 = rawname.rsplit(".", 1)[0] + ".ti3"
        icc = rawname.rsplit(".", 1)[0] + ".icc"

        dEV = None
        call_dcraw(rawname,dEV)
        
        dEV =  Delta_EV(rawname)
        if abs(dEV) > 0.02:
            call_dcraw(rawname,dEV)
            
            dEV =  Delta_EV(rawname)
            logging.info("Corrected exposure error level {0} EV at camera {1}".format( dEV, position ) ) 
        if dEV > 1:
            print("Exposure error too high, could not be corrected: {0} EV at camera {1}".format( dEV, position ))
            logging.error("Exposure error too high, could not be corrected: {0} EV at camera {1}".format( dEV, position ) )    
        collprof(ti3_no_ext)
        
           
        if os.path.isfile(icc) and os.path.getsize( icc ) > 0 :
            tag = ""
            exp = ""
            if position == 0:
                tag = "icc_profile_right"
                exp =  "process_exposure_factor_right"
            elif position == 1:
                tag = "icc_profile_left"
                exp =  "process_exposure_factor_left"
            
            edit_xml( os.path.dirname( os.path.dirname(rawname) ), "PROFILES/"+ntpath.basename(  rawname.rsplit(".", 1)[0] + ".icc" ) ,tag,id_char ) 
            edit_xml( os.path.dirname( os.path.dirname(rawname) ), str(  dEV + 1 + 0.1) ,exp, id_char ) 

            print("Everything is fine the profile has been created".format( rawname.rsplit(".", 1)[0] + ".icc" ) )
            logging.info("Everything is fine the profile {0} has been created for camera position {1} page {2}".format( rawname.rsplit(".", 1)[0] + ".icc", position, id_char ) )
            
            os.remove(rawname)
            os.remove(ti3)
            os.remove(tiffname)
            
            
            
def collprof(ti3_no_ext):
    """
    Llama a collprof para generar los perfiles ICC
    """

    maker = ' -A "Canon" ' 
    model = ' -M "Canon EOS 5D Mark II" ' 
    desc =  ' -D "'+ os.path.basename( ti3_no_ext )+'" ' 
    autor = ' -C "Jose Pereira (info@jpereira.net)" ' 
    extras = maker + model + desc + autor


    coolprof = "colprof -v -qm -al -uc -U1.06 {1} {0} ".format(ti3_no_ext, extras)
    #Cooprof
    s = subprocess.Popen(shlex.split(coolprof), stdout=subprocess.PIPE )
    cadena = "Profile check complete" 

    while True:
        line = s.stdout.readline()
        if line != b'':
            if line.startswith(cadena ) :
                arr = line.split()
                print("Delta-e peak err {0}".format( arr[6]) )
                print("Delta-e avg err {0}".format( arr[10]) )
                logging.info("Delta-e peak err {0} and Delta-e avg err {1} at the creation of the ICC profile".format( arr[6], arr[10]  ) )         
                if os.path.isfile(ti3_no_ext+".icc") :
                    logging.info("ICC Profile {0} exists".format( ti3_no_ext+".icc"  ) )         
                else:  
                    logging.error("ICC Profile {0} NO exists".format( ti3_no_ext+".icc"  ) )
        
        else:
            break
        
def call_dcraw(rawname,dEV):
    """
    Llama a DCRAW
    """
    
    if dEV != None:
        if dEV < 0:
            dEV = abs(dEV)
        else:
            dEV = dEV * -1
    #Se introduce un factor de correccion de 0.1
        b = "-b {0}".format( dEV + 1 + 0.1)
    else:
        b = ""

    dcraw = "dcraw -T -6 -v -o 0 -W -w {1} -g 2.2 0 -h {0}".format(rawname,b)
    subprocess.check_call(shlex.split(dcraw) )
     
     
def Delta_EV(rawname):
        """
        Genera los factores de exposicion
        """
        
        tiffname = rawname.rsplit(".", 1)[0] + ".tiff"
        scanin = "scanin -v -a {0} {1} {2}".format( tiffname, CHT, CIE )
        subprocess.check_call(shlex.split(scanin) )
        
        with open(rawname.rsplit(".", 1)[0] + ".ti3", 'r') as f:
            lines = f.readlines()
        cadena="D02"
        for member in lines:
            if member.startswith(cadena ) :
                lista = member.split()
                r = (float(lista[4])/100)*255
                g = (float(lista[5])/100)*255
                b = (float(lista[6])/100)*255
                
        Yp = 0.2126*r + 0.7152*g + 0.0722*b  
        print(round(Yp,0) )     
        Yo = 200        
        #EV = ( Ln(Y') - Ln(Y)*2.2) / Ln(2)
        EV = ( (math.log1p(Yp) - math.log1p(Yo))*2.2) / math.log1p(2)
        
        logging.info("Exposure error level {0} EV".format( round(EV,2) ) ) 
        print( "Exposure error level {0} EV".format( round(EV,2) ) ) 
        
        return round(EV,2)


def edit_xml(PATH, value, tag, id_char):
    """ 
    Edita el archivo project.xml con los datos
    de gestion del color
    """
   
    XML_PATH = os.path.join(PATH, "project.xml")
    tree = ET.parse( XML_PATH )
    path = ".//item[@id='{0}']/{1}".format(id_char,tag)
    #print(path)
    #subelem = tree.find(path)
    #if subelem != None:
    #    list.append(subelem.text)  
    #else:
        #list.append(False)
        
    tree.find(path).text = value
    tree.write( XML_PATH, encoding="utf-8", xml_declaration=True )  
    
def move_raws(images, PATH):
    """ 
    Copia los archivos raw correspondientes a las
    cartas de color para su procesamiento al directorio PROFILES
    """

    if not os.path.exists( os.path.join(PATH,"PROFILES") ):
        os.makedirs( os.path.join(PATH,"PROFILES" ))
    
    for x in images:
        for y in islice(images[x], 0,2):
            Orig = os.path.join(PATH, images[x][y])
            Dest = os.path.join(PATH,"PROFILES", images[x][y].split("/",1)[1] )
            copyfile(Orig, Dest)
    
        
def get_charts(PATH):
    """ 
    Obtiene los registros correspondientes con 
    las cartas de color
    """
    
    PATH_TO_XML =  os.path.join(PATH, "project.xml")
    DOMTree = xml.dom.minidom.parse(PATH_TO_XML)
    collection = DOMTree.documentElement

    items = collection.getElementsByTagName("item")

    images = {}
    i = 0
    for item in items:
        tipo = item.getElementsByTagName('type')[0]
        image_right = item.getElementsByTagName('image_right')[0]
        image_left = item.getElementsByTagName('image_left')[0]
        id = item.getAttribute('id')
        
        if tipo.childNodes[0].data == "chart":
            images[i]={}
            images[i][0] = image_right.childNodes[0].data
            images[i][1] = image_left.childNodes[0].data
            images[i][2] = item.getAttribute('id')
            i = i+1
               
    return images   

def update_xml_project(PATH, chars):
    """ 
    Acturaliza todos los registros del project.xml con 
    con la informacion de la gestion del color
    """
    
    PATH_TO_XML =  os.path.join(PATH, "project.xml")
    DOMTree = xml.dom.minidom.parse(PATH_TO_XML)
    collection = DOMTree.documentElement
    
    items = collection.getElementsByTagName("item")
    fin = 0
    ultimo = items[-1].getAttribute('id') 
    
    for x in chars:
         
        for item in items:      
            id_page = int( item.getAttribute('id') )
            #tipo = item.getElementsByTagName('type')[0]
            if chars.has_key(x+1):
                fin =   chars[ x+1 ][4]
            else:
                fin = int(ultimo)+1 
            #print("if {0} > {1} and {0} < {2}".format(id_page,img[x][4], fin  ) )
            if ( id_page > int( chars[x][4]) ) and (id_page < int(fin) ):
                edit_xml(PATH, chars[x][1], "icc_profile_left", id_page)
                edit_xml(PATH, chars[x][0], "icc_profile_right", id_page)
                edit_xml(PATH, chars[x][2], "process_exposure_factor_right", id_page)
                edit_xml(PATH, chars[x][3], "process_exposure_factor_left", id_page)
                logging.info("Edit project.xml field with id {0}".format( id_page ) ) 
                print( "Edit project.xml field with id {0}".format( id_page ) )


def get_full_charts(PATH):
    """ 
    Obtiene informacion sobre las rutas a 
    los perfiles ICC y factores de exposicion
    """
    PATH_TO_XML =  os.path.join(PATH, "project.xml")
    DOMTree = xml.dom.minidom.parse(PATH_TO_XML)
    collection = DOMTree.documentElement
    items = collection.getElementsByTagName("item")
    images = {}
    i = 0
    for item in items:
        tipo = item.getElementsByTagName('type')[0]
        icc_profile_right = item.getElementsByTagName('icc_profile_right')[0]
        icc_profile_left = item.getElementsByTagName('icc_profile_left')[0]
        process_exposure_factor_right = item.getElementsByTagName('process_exposure_factor_right')[0]
        process_exposure_factor_left = item.getElementsByTagName('process_exposure_factor_left')[0]
        #id = item.getAttribute('id')
        if tipo.childNodes[0].data == "chart":
            images[i]={}
            images[i][0] = icc_profile_right.childNodes[0].data
            images[i][1] = icc_profile_left.childNodes[0].data
            images[i][2] = process_exposure_factor_right.childNodes[0].data
            images[i][3] = process_exposure_factor_left.childNodes[0].data
            images[i][4] = item.getAttribute('id')
            i = i+1
        
    return images




if __name__ == '__main__':

    try:
        
        PATH = sys.argv[1] #ruta al directorio raiz  del proyecto
        
        ICC_DIR = os.path.join(PATH, "PROFILES")
        os.mkdir(ICC_DIR)
        
        LOGFILE = os.path.join(PATH, "jobcm.txt" )
        logging.root.handlers = []
        logging.basicConfig(filename=LOGFILE,
                            filemode='a',
                            format='%(asctime)s %(name)s %(levelname)s %(message)s',
                            datefmt= '%d/%m/%Y %H:%M:%S',
                            level=logging.DEBUG) 
        
        images = get_charts(PATH)
        move_raws(images, PATH) 
    
        for x in images:
            for y in islice(images[x], 0, 2):
                #print( images[x][y].split("/")[1] )
                raw_process( os.path.join(PATH, "PROFILES", images[x][y].split("/")[1]), y,images[x][2] )
                #i=i+1
                
        """
        Actualiza el xml completo
        """
        chars = get_full_charts(PATH)
        update_xml_project(PATH, chars)
    

            
    except subprocess.CalledProcessError as e:
        print e.output
        logging.error( e.output )
        
    except OSError as e:
        if e.errno != errno.EEXIST:
            logging.error( e )
            print(e)
            sys.exit(e.errno)

