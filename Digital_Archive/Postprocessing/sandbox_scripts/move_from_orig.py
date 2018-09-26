#!/usr/bin/python -tt
# -*- coding: utf-8 -*-
"""
Archivo para procesar raws con gestion del color y creacion de derivados jpeg y tiff
by jpereira :: www.jpereira.net :: info@jpereira.net
"""
import errno, os, sys, time, filecmp
from datetime import datetime
import logging
from distutils.dir_util import copy_tree
import shutil

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



def moveFiles(PATH1,PATH2):

    for fname in os.listdir( PATH1 ):
        
        pt_orig = os.path.join(PATH1, fname)
        pt_target = os.path.join(PATH2, fname)
        
        if os.path.isdir( pt_orig ):
            logging.info("Start project {0} ".format(fname) )
            if not os.path.exists( pt_target ):
                os.makedirs( pt_target )
                copy_tree(pt_orig, pt_target)
                isReady = verifyDir(pt_orig, pt_target)
            else:
                logging.error("Target directory exists!!" ) 
                
        logging.info("Finish project {0} ".format(fname) )    
    return isReady    


if __name__ == '__main__':

    PATH1 = sys.argv[1]
    PATH2 = sys.argv[2] 
    PATH3 = sys.argv[3]               
        
    LOGFILE = os.path.join(PATH3, "move-orig-report.txt" )
    logging.basicConfig(filename= LOGFILE ,
                    level=logging.DEBUG,
                    format='%(asctime)s %(name)s %(levelname)s %(message)s',
                    datefmt='%d/%m/%Y %H:%M:%S',
                    )    
    

    moveFiles(PATH1,PATH2)


    try:
        flags = os.O_CREAT | os.O_EXCL | os.O_WRONLY
        file_handle = os.open( LOGFILE , flags)
        
        
    except OSError as e:
        if e.errno != errno.EEXIST:
            logging.error( e )
            print(e)
            sys.exit(e.errno)

