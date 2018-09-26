#!/usr/bin/python -tt
# -*- coding: utf-8 -*-
"""
Archivo para mover los archivos al archivo profundo
by jpereira :: www.jpereira.net :: info@jpereira.net
"""
import errno, os, sys, filecmp, time
import logging
from distutils.dir_util import copy_tree
import shutil
from shutil import copytree, ignore_patterns

def verifyDir(PATH1, PATH2):
    
        comparison = filecmp.dircmp(PATH1, PATH2, ignore=['Thumbs.db', 'thumbnails', '.@__thumb','_project.xml'])
        noIguales = comparison.diff_files
        enDerechaSolo = comparison.right_only
        enIzquierdaSolo = comparison.left_only
        #print(comparison)
        
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
    
    isValid = False
    if not os.path.exists(PATH2):
        #os.makedirs(PATH2)
        #copy_tree(PATH1, PATH2)
        copytree(PATH1, PATH2, ignore=ignore_patterns('Thumbs.db', 'thumbnails', '.@__thumb','_project.xml'))

        isValid = verifyDir(PATH1, PATH2)  
        if isValid:
            shutil.rmtree(PATH1)
        else:
            logging.error("There are errors, directory ({0}) will not deleted!!".format( PATH1 ) ) 
            print("There are errors, directory ({0}) will not deleted!!".format( PATH1 ))
            
    else:
        logging.error("Target directory ({0}) exists!!".format( PATH2 ) ) 
        print("Target directory ({0}) exists!!".format( PATH2 ))
                        
      
    return isValid    


if __name__ == '__main__':

    try:
    
        PATH1 = sys.argv[1] #directorio de inicio
        PATH2 = sys.argv[2] #directorio de destino
        PATH3 = "/home/user/scripts/reports"             
        
        date = time.strftime("%Y%m%d")
        
        LOGFILE = os.path.join(PATH3, "move-report-recursive-"+date+".txt" )
        logging.root.handlers = []
        logging.basicConfig(filename=LOGFILE,
                            filemode='a',
                            format='%(asctime)s %(name)s %(levelname)s %(message)s',
                            datefmt= '%d/%m/%Y %H:%M:%S',
                            level=logging.DEBUG)  
        
        project = os.path.basename(os.path.normpath( PATH1 ) )
        logging.info("Start project {0} ".format(project) )
        
        lst = os.listdir(PATH1)
        lst.sort()      
        
        NOFOLLOW = ["1_Oriental_Manuscripts_Fund"]
          
        for fname in lst:
            
            if not fname.startswith('.')  and fname not in NOFOLLOW :
        
        
                out = moveFiles(os.path.join(PATH1, fname ), os.path.join(PATH2, fname ))
        
                if out:
                    print( "Move project {0} finish without errors".format(fname) )
                    logging.info("Move project {0} finish without errors".format(fname) )
                else:
                    print("Move project {0} finish WITH errors".format(fname) )
                    logging.error("Move project {0} finish WITH errors".format(fname) )
        
               
    except OSError as e:
        if e.errno != errno.EEXIST:
            logging.error( e )
            print(e)
            sys.exit(e.errno)

