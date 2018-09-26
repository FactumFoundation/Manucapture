#!/usr/bin/python -tt
# -*- coding: utf-8 -*-
"""
Archivo para procesar raws con gestion del color y creacion de derivados jpeg y tiff
by jpereira :: www.jpereira.net :: info@jpereira.net
"""
import errno, os, sys, time,shlex,glob
import multiprocessing, subprocess
import xml.dom.minidom
import logging


SRGB_ICC = "RESOURCES/sRGB_Color_Space_Profile.icc"

#SRGB_ICC = "/Users/jpereira/Documents/LiClipse_Workspace/factum-daguestan/SCRIPTS/RESOURCES/sRGB_Color_Space_Profile.icc"

JPEG_RESIZE = "x370"
JPEG_QUALITY = 70

DCRAW_H = "" #crea imagenes a mitad de tamanho con el parametro -h, para hacer pruebas solamente!!!!

IMAGES_IN_XML = 0
ERROR_COUNTER = 0


def raw_process((rawname,data1,data2,data3,data4,data5,data6,data7,IMTEMP)):
        
    rawname =  os.path.join(PATH, rawname )
    total=None
    global ERROR_COUNTER
    
    if os.path.isfile(rawname) and os.access(rawname, os.R_OK):
    
        book_title = "-xmp-dc:Title='{0}'".format(data4)
        jsonmetadata = "[{{'title': '{0}'}},{{'code': '{1}'}},{{'pagenumber': '{2}'}},{{'pageposition': '{3}'}},{{'type': {4}'}}]".format(data4,data5,data1,data3,data2)
        book_data = "-xmp-dc:Description=\"{0}\"".format(jsonmetadata)
        
        jpgname = rawname.rsplit(".", 1)[0] + ".jpg"
        tiffname = rawname.rsplit(".", 1)[0] + ".tiff"
        
        p = jpgname.rsplit("/", 1)
        if len(p) > 1:
            jpgname = p[1]
            
        p = tiffname.rsplit("/", 1)
        if len(p) > 1:
            tiffname = p[1]
            
        jpgname = os.path.join(JPEG_DIR, jpgname)
        tiffPname = os.path.join(TIFFP_DIR, tiffname)
        #Se le saca la ruta a TIFF porque Exiftools gestion nombre y ruta por separado.
        #tiffname = os.path.join(TIFF_DIR, tiffname )
    
        start = time.time()
        
        
        if data7 != 0:
            b = " -b {0} ".format( data7 )
        else:
            b = ""
         
        rotate = ""    
        if data3 == "L":
            rotate = " -rotate 90 "
        elif data3 == "R":
            rotate = " -rotate -90 "
            
            
        if data6 != "":
            PROFILES_PATH = os.path.join(PATH, data6)
            profile = " -profile {0} ".format(PROFILES_PATH)
        else:
            profile = ""
            logging.info("Color charts, not available, project without color management!!" )

        dcraw = "dcraw -T -6 -v -o 0 -W -w {0} -g 2.2 0  {2} -c {1}".format(b, rawname,DCRAW_H)
        tiff = "convert - -define registry:temporary-path={2} {0} -compress lzw -depth 8 {1} tiff:-".format(profile, rotate,IMTEMP ) 
        tiffp = "convert - -define registry:temporary-path={2} -profile {0} -define tiff:tile-geometry=256x256 -compress jpeg 'ptif:{1}'".format(SRGB_ICC,tiffPname,IMTEMP)
        jpeg = "convert - -define registry:temporary-path={4} -profile {0} -resize {1} -quality {2} {3} ".format(SRGB_ICC , JPEG_RESIZE, JPEG_QUALITY, jpgname, IMTEMP)
        exiftool = "exiftool -TagsFromFile {0} '-all>all' {3} {4} -filename={2} - -o {1}".format(rawname,TIFF_DIR, tiffname, book_title,book_data ) 
        
        #Se lanza DCRAW
        d = subprocess.Popen(shlex.split(dcraw), stdout=subprocess.PIPE)
        
        #Se toma la salida de DCRAW y se lanza Convert si existen los perfiles ICC. Se da la salida en TIFF
        c = subprocess.Popen(shlex.split(tiff),stdin=d.stdout, stdout=subprocess.PIPE  )
        out2 = c.stdout.read()

        
        #Se prepara la ejecucion de Exiftool 
        #Exiftool solo copia los metadatos al TIFF no lo hace al JPEG
        f = subprocess.Popen(shlex.split(exiftool), stdin=subprocess.PIPE )
        
        #Se prepara la ejecucion de Convert con salida a TIFF Piramidal
        g = subprocess.Popen(shlex.split(tiffp), stdin=subprocess.PIPE)
        
        #Se prepara la ejecucion de Convert con salida a JPEG
        e = subprocess.Popen(shlex.split(jpeg), stdin=subprocess.PIPE)
    
        #Se le pasa a Exiftool la salida de Convert en TIFF
        f.communicate(input=out2)[0]
        #Se le pasa a Convert la salida de Convert en TIFF para hacer la conversion a JPEG
        e.communicate(input=out2)[0]
        #Se le pasa a Convert la salida de Convert en TIFF para hacer la conversion a TIFF Piramidal
        g.communicate(input=out2)[0]
       
        #Cierra las salidas de DCRAW y Convert
        d.stdout.close()
        
        c.stdout.close()
         
        os.waitpid(d.pid, 0)
        #os.waitpid(c.pid, 0)
        #os.waitpid(f.pid, 0)
        
        total = time.time() - start
        
        print("Finished", rawname, "in", "{0:.2f} seconds".format(total) )
        logging.info("Finished {0} in {1:.2f} seconds".format(os.path.basename(rawname), total) )
        check_files(jpgname)
        check_files( os.path.join(TIFF_DIR, tiffname ) )
    else:
        logging.error('RAW %s FILE DOES NOT EXISTS' % rawname )
        ERROR_COUNTER = ERROR_COUNTER + 1
        

    return total

def check_files(PATH):
    
    if os.path.isfile(PATH) and os.access(PATH, os.R_OK):
        size = os.path.getsize( PATH ) / 1024
        file_name = os.path.basename(PATH)
        logging.info("{0} file exist, with size {1} Kb".format(file_name, size) )
    else:   
        logging.error("{0} file does NOT exist".format(file) )

def get_cms(PATH):
    
    DOMTree = xml.dom.minidom.parse(PATH)
    collection = DOMTree.documentElement
    
    cms = {}
    cm = collection.getElementsByTagName("cms")
    icc_profile_right = cm[0].getElementsByTagName('icc_profile_right')[0]
    cms[0] = icc_profile_right.childNodes[0].data
    icc_profile_left = cm[0].getElementsByTagName('icc_profile_left')[0]
    cms[1] = icc_profile_left.childNodes[0].data
    process_exposure_factor_r = cm[0].getElementsByTagName('process_exposure_factor_r')[0]
    cms[2] = float( process_exposure_factor_r.childNodes[0].data )
    process_exposure_factor_l = cm[0].getElementsByTagName('process_exposure_factor_l')[0]
    cms[3] = float( process_exposure_factor_l.childNodes[0].data )
    
    logging.info("Load profiles: {0}, {1}, and exposure param {2}".format(cms[0],cms[1], cms[2]) )

    return cms


def parse_xml(PATH):
    
    global IMAGES_IN_XML 
    
    DOMTree = xml.dom.minidom.parse(PATH)
    collection = DOMTree.documentElement
        
    meta = collection.getElementsByTagName("metadata")
    bookname = meta[0].getElementsByTagName('name')[0]
    bookcode = meta[0].getElementsByTagName('code')[0]
    
    #project_counter = collection.getElementsByTagName("image_counter")[0]
    #IMAGES_IN_XML = project_counter.firstChild.data
    
    logging.info("***** Start project with code {0}  ******".format(bookcode.childNodes[0].data) )
    
    items = collection.getElementsByTagName("item")
    
    images = {}
    i = 0
    for item in items:
       
        #page_num = item.getElementsByTagName('page_num')[0]
        tipo = item.getElementsByTagName('type')[0]
        #image_righta = item.getElementsByTagName('image_right')[0]
        #image_left = item.getElementsByTagName('image_left')[0]
        
        icc_right = item.getElementsByTagName('icc_profile_right')[0]
        icc_left = item.getElementsByTagName('icc_profile_left')[0]
        
        exp_right = item.getElementsByTagName('process_exposure_factor_right')[0]
        exp_left = item.getElementsByTagName('process_exposure_factor_left')[0]
        
        if tipo.childNodes[0].data  == "page":
        
            images[i]={}   
            if item.getElementsByTagName('image_right')[0].childNodes:
                images[i][0] = item.getElementsByTagName('image_right')[0].childNodes[0].data
            else:
                images[i][0] = 0
                itemCount = (i/2)+1
                logging.error("_ERROR_IMAGE_MISING_AT_XML at item {0} image_right {1}".format(itemCount, bookcode.childNodes[0].data ) )    
            images[i][1] = item.getElementsByTagName('page_num')[0].childNodes[0].data
            images[i][2] = tipo.childNodes[0].data 
            images[i][3] = "R"
            images[i][4] = bookname.childNodes[0].data
            images[i][5] = bookcode.childNodes[0].data  
            images[i][6] = icc_right.childNodes[0].data if icc_right.childNodes.length > 0 else '' #perfil derecho
            images[i][7] = exp_right.childNodes[0].data if icc_right.childNodes.length > 0 else 0 #factor de exposicion right
    
            i = i+1
            images[i]={}
            if item.getElementsByTagName('image_left')[0].childNodes:
                images[i][0] = item.getElementsByTagName('image_left')[0].childNodes[0].data
            else:
                images[i][0] = 0
                itemCount = (i/2)+1
                logging.error("_ERROR_IMAGE_MISING_AT_XML at item {0} image_left {1}".format(itemCount, bookcode.childNodes[0].data ) )    
            images[i][1] = item.getElementsByTagName('page_num')[0].childNodes[0].data
            images[i][2] = tipo.childNodes[0].data 
            images[i][3] = "L"
            images[i][4] = bookname.childNodes[0].data
            images[i][5] = bookcode.childNodes[0].data   
            images[i][6] = icc_left.childNodes[0].data if icc_left.childNodes.length > 0 else '' #perfil izquierdo
            images[i][7] = exp_left.childNodes[0].data if icc_right.childNodes.length > 0 else 0 #factor de exposicion left
            
            i = i+1

    IMAGES_IN_XML = len(images)
    return images

if __name__ == '__main__':
        
    try:
        
        start = time.time()
    
        PATH = sys.argv[1]
        IMTEMP = sys.argv[2]
        
        JPEG_DIR = os.path.join(PATH, "JPEG")
        TIFF_DIR = os.path.join(PATH, "TIFF")
        TIFFP_DIR = os.path.join(PATH, "TIFFP")
        
        if not os.path.isdir( JPEG_DIR ):
            os.mkdir(JPEG_DIR)
        if not os.path.isdir( TIFF_DIR ):
            os.mkdir(TIFF_DIR)
        if not os.path.isdir( TIFFP_DIR ):
            os.mkdir(TIFFP_DIR)
        
        LOGFILE = os.path.join(PATH, "jobfile.txt" )
        
        logging.root.handlers = []
        logging.basicConfig(filename=LOGFILE,
                            filemode='a',
                            format='%(asctime)s %(name)s %(levelname)s %(message)s',
                            datefmt= '%d/%m/%Y %H:%M:%S',
                            level=logging.DEBUG)
    
        PATH_TO_PROJECT =  os.path.join(PATH, "project.xml")
        #PATH_TO_CMS =  os.path.join(sys.argv[1],"PROFILES", "cms.xml")
        print(PATH_TO_PROJECT)
        
        if os.path.isfile(PATH_TO_PROJECT) and os.access(PATH_TO_PROJECT, os.R_OK):
            rawimages = parse_xml(PATH_TO_PROJECT)
                
             
            imgpath = [] #ruta al raw
            impath = [] #path para el temporal de IM
            data1 = [] #page_num
            data2 = [] #type
            data3 = [] #position
            data4 = [] #title
            data5 = [] #code
            data6 = [] #perfil ICC
            data7 = [] #factor exposicion
                
            rawimages_len = len(rawimages)
            for i in range(0, rawimages_len):
                if rawimages[i][0] != 0:
                    imgpath.append(rawimages[i][0])   
                    impath.append(IMTEMP) 
                    data1.append(rawimages[i][1])
                    data2.append(rawimages[i][2])
                    data3.append(rawimages[i][3])
                    data4.append(rawimages[i][4].encode('utf-8') )
                    data5.append(rawimages[i][5])
                    data6.append(rawimages[i][6])
                    data7.append(rawimages[i][7])
                else:
                    itemCount = (i/2)+1
                    logging.error("_ERROR_FILE {} MISSING".format(itemCount) )
                
        else:   
            logging.error("Project file {0} does NOT EXIST".format(PATH_TO_PROJECT) )
            
    except subprocess.CalledProcessError as e:
        print e.output
        logging.error( e.output )
    
    except OSError as e:
        if e.errno != errno.EEXIST:
            logging.error( e )
            print(e)
            sys.exit(e.errno)

    # at the beginning, dcraw(1) does something cpu non-intensive
    # let the kernel scheduler take care of that
    units = multiprocessing.cpu_count() + 2
    pool = multiprocessing.Pool(units)

    times = pool.map(raw_process,zip(imgpath,data1,data2,data3,data4,data5,data6,data7, impath) )

    total = time.time() - start
    sumtimes = sum(times)
    avg = sumtimes/len(times)
    speedup = sumtimes/total
    logging.info("**** RESUME ******")
    logging.info("RAW files declared in project file {0}".format(IMAGES_IN_XML) )
    raw_counter = len( glob.glob1( os.path.join(PATH, "raw") ,"*.cr2"))
    logging.info("RAW files in directory {0}".format(raw_counter) )
    if raw_counter != IMAGES_IN_XML:
        logging.error( "RAW declared files do not match with RAW files found" )
    logging.info("JPEG files created {0}".format(len( glob.glob1(JPEG_DIR,"*.jpg"))) )
    logging.info("TIFF files created {0}".format(len( glob.glob1(TIFF_DIR,"*.tiff"))) )
    if ERROR_COUNTER > 0:
        logging.error("THERE ARE {0} RAW FILES MISSING!!!".format(ERROR_COUNTER ) )
    logging.info( "***** Finish project with average {0:.2f} sec, total {1:.2f} sec, relative speedup {2:.2f}x *****".format(avg, total, speedup) )
    print("Average {0:.2f} sec, total {1:.2f} sec, relative speedup {2:.2f}x".format(avg, total, speedup))
