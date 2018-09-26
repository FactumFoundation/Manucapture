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
JPEG_RESIZE = "x370"
JPEG_QUALITY = 70

DCRAW_H = "" #crea imagenes a mitad de tamanho con el parametro -h, para hacer pruebas solamente!!!!

IMAGES_IN_XML = 0
ERROR_COUNTER = 0


def tiff_process((PATH,IMTEMP)):
        
    #tiffname =  os.path.join(PATH, tiffname )
    total=None
    global ERROR_COUNTER
    print(PATH)
    if os.path.isfile(PATH) and os.access(PATH, os.R_OK):
    
        jpgname = PATH.rsplit(".", 1)[0] + ".jpg"
        tiffname = PATH.rsplit(".", 1)[0] + ".tiff"
        
        p = jpgname.rsplit("/", 1)
        if len(p) > 1:
            jpgname = p[1]
            
        p = tiffname.rsplit("/", 1)
        if len(p) > 1:
            tiffname = p[1]
            
        jpgname = os.path.join(JPEG_DIR, jpgname)
        tiffPname = os.path.join(TIFFP_DIR, tiffname)
        tiffnameSave = os.path.join(TIFF_DIR, tiffname)
    
        start = time.time()
        
        tiff = "convert -define registry:temporary-path={2} {0} -compress lzw -depth 8 tiff:{1}".format(PATH, tiffnameSave,IMTEMP ) 
        tiffp = "convert -define registry:temporary-path={2} {0} -define tiff:tile-geometry=256x256 -compress jpeg 'ptif:{1}'".format(PATH, tiffPname,IMTEMP)
        jpeg = "convert -define registry:temporary-path={4} {0} -resize {1} -quality {2} {3} ".format(PATH,JPEG_RESIZE, JPEG_QUALITY, jpgname,IMTEMP)
        
        #Se toma la salida de DCRAW y se lanza Convert. Se da la salida en TIFF
        c = subprocess.Popen(shlex.split(tiff), stdout=subprocess.PIPE  )
        out2 = c.stdout.read()
        
        #Se prepara la ejecucion de Convert con salida a TIFF Piramidal
        g = subprocess.Popen(shlex.split(tiffp), stdin=subprocess.PIPE)
        
        #Se prepara la ejecucion de Convert con salida a JPEG
        e = subprocess.Popen(shlex.split(jpeg), stdin=subprocess.PIPE)

        #Se le pasa a Convert la salida de Convert en TIFF para hacer la conversion a JPEG
        e.communicate(input=out2)[0]
        #Se le pasa a Convert la salida de Convert en TIFF para hacer la conversion a TIFF Piramidal
        g.communicate(input=out2)[0]
       

        c.stdout.close()
        #------
         
        
        total = time.time() - start
        
        print("Finished", tiffname, "in", "{0:.2f} seconds".format(total) )
        logging.info("Finished {0} in {1:.2f} seconds".format(os.path.basename(tiffname), total) )
        #check_files(jpgname)
        check_files( os.path.join(PATH, tiffnameSave ) )
    else:
        logging.error('Raw %s file does not exist' % tiffname )
        ERROR_COUNTER = ERROR_COUNTER + 1   
        
    return total

def check_files(PATH):
    
    if os.path.isfile(PATH) and os.access(PATH, os.R_OK):
        size = os.path.getsize( PATH ) / 1024
        file_name = os.path.basename(PATH)
        logging.info("{0} file exist, with size {1} Kb".format(file_name, size) )
    else:   
        logging.error("{0} file does NOT exist".format(file) )


if __name__ == '__main__':
        
    try:
        
        start = time.time()
    
        PATH = sys.argv[1]
        IMTEMP = sys.argv[2]
        
        JPEG_DIR = os.path.join(PATH, "JPEG")
        TIFF_DIR = os.path.join(PATH, "TIFF")
        TIFFP_DIR = os.path.join(PATH, "TIFFP")
        
        os.mkdir(JPEG_DIR)
        os.mkdir(TIFF_DIR)
        os.mkdir(TIFFP_DIR)
        
        LOGFILE = os.path.join(PATH, "job.txt" )
        
        logging.root.handlers = []
        logging.basicConfig(filename=LOGFILE,
                            filemode='a',
                            format='%(asctime)s %(name)s %(levelname)s %(message)s',
                            datefmt= '%d/%m/%Y %H:%M:%S',
                            level=logging.DEBUG)
    
        
        tiffimages = []
        imtemp = []
    
        for fname in os.listdir( PATH ):
            if os.path.isfile and os.path.splitext(fname)[1] == ".TIF":
                tiffimages.append( os.path.join(PATH, fname) )
                imtemp.append(IMTEMP) 

        #tiffimages = parse_dir(PATH)  
        

             

   
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

    #times = pool.map(tiff_process,tiffimages )
    times = pool.map(tiff_process,zip(tiffimages,imtemp) )

    total = time.time() - start
    sumtimes = sum(times)
    avg = sumtimes/len(times)
    speedup = sumtimes/total
    logging.info("**** RESUME ******")
    totalOrigTiffFiles = len(tiffimages)
    totalTiffProcess =  len([name for name in os.listdir(TIFF_DIR) if os.path.isfile(os.path.join(TIFF_DIR, name))])
   
    logging.info("TIFF files in directory {0}".format(totalOrigTiffFiles) )
    logging.info("TIFF PROCESS files in directory {0}".format(totalTiffProcess) )
    
     
    if totalOrigTiffFiles == totalTiffProcess:
        logging.info("Remove original TIFF" )
        for f in tiffimages:
            os.remove(f)
    else:            
        logging.error("NOT ALL TIFF FILES WAS PROCESED" )

    logging.info( "***** Finish project with average {0:.2f} sec, total {1:.2f} sec, relative speedup {2:.2f}x *****".format(avg, total, speedup) )
    print("Average {0:.2f} sec, total {1:.2f} sec, relative speedup {2:.2f}x".format(avg, total, speedup))
