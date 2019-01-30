import logging
import subprocess
from distutils.dir_util import copy_tree
import xml.dom.minidom
from datetime import datetime
import xml.etree.cElementTree as ET
import errno, os, sys ,shlex,math,glob, ntpath,time
from PIL import Image
import numpy as np
import scipy.cluster
from numpy import double

CHT = "/home/user/git/bookScanner/Digital_Archive/Postprocessing/sandbox_scripts/RESOURCES/ColorChecker.cht"
CIE = "/home/user/git/bookScanner/Digital_Archive/Postprocessing/sandbox_scripts/RESOURCES/ColorChecker.cie"


"""

Image check

"""

"""

Image analysis to detect no page and color chart

"""

def call_dcraw(rawname):
    dcraw = "dcraw -T -v -o 0 -W -w -g 2.2 0 -h {0}".format(rawname)
    subprocess.check_call(shlex.split(dcraw) )


def call_scanin(tiffName):
    scanin = "scanin -v -a {0} {1} {2}".format(tiffName, CHT, CIE)
    s = subprocess.Popen(shlex.split(scanin), stdout=subprocess.PIPE)
    cadena = "Writing output values to file"
    cadena2 = "Robust mean angle"
    PATH = os.path.dirname(tiffName)
    FILENAME = os.path.basename(tiffName)
    NAMENOEXT = os.path.splitext(FILENAME)[0]
    o = False
    while True:
        line = s.stdout.readline()
        if line != b'':
            if line.startswith(cadena2):
                meanAngle = double(line[19:28])
                print("Mean Angle = " + line[19:28])
            if line.startswith(cadena) and meanAngle > -1:
                print("CHART EXISTS")
                o = True
            else:
                o = False
        else:
            break
    ti3 = os.path.join(PATH, NAMENOEXT + ".ti3")
    if os.path.isfile(ti3):
        os.remove(ti3)
    return o


#Detección de página en negro
def kmeansClusters(filename):
    NUM_CLUSTERS = 5
    im = Image.open(filename)
    im = im.resize((150, 150))
    ar = np.asarray(im)
    shape = ar.shape
    ar = ar.reshape(scipy.product(shape[:2]), shape[2]).astype(float)
    # print 'localiza clusters'
    codes, dist = scipy.cluster.vq.kmeans(ar, NUM_CLUSTERS)
    # calculo la desviación entre los colores de cada cluster y los clusteres en si para cuanto
    # menos variación más probable que sea un fondo negro.
    devClus = []
    i = 0
    for c in codes:
        devClus.append(np.std(c))
        i = i + 1
    desv = np.std(devClus)
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


def getImageType(image_path):
    type = "image"
    if os.path.isfile(image_path):
        call_dcraw(image_path)
        name = os.path.splitext(image_path)[0]
        TIFFILE = "{}.tiff".format(name)
        if os.path.isfile(TIFFILE):
            kmeans = kmeansClusters(TIFFILE)
            print("kmenas" + str(kmeans))
            if kmeans < 1:
                type = "noImage"
            else:
                carta = call_scanin(TIFFILE)
                if carta:
                    type = "chart"
                else:
                    type = "image"
            os.remove(TIFFILE)
    return type

# for the bug in the new version
def getImagePath(PATH, relative_Image_URL):
    image_path = PATH + relative_Image_URL
    if not os.path.isfile(image_path):
        image_path = os.path.join(PATH, relative_Image_URL)
    return image_path


def check_images(PATH):
    RAW_PATH = os.path.join(PATH,"raw")
    PROJECT_PATH = os.path.join(PATH,"project.xml")
    if os.path.isfile(PROJECT_PATH) and os.path.isdir(RAW_PATH):
        #print("Factum project detected : {0}".format(PATH))
        imgArr = getImgFromXML(PROJECT_PATH)
        if len(imgArr)==0:
            logging.error("XML ERROR NO IMAGES IN FILE path: {0}".format(PATH))
        else:
            #print("Images in project {0}".format(PATH))
            numOfItems = len(imgArr)
            itemIndex = 0;
            for img in imgArr:
                if imgArr[img][0] == None and imgArr[img][1] == None :
                    print("MISSING : Empty item, missing the two images ")
                elif imgArr[img][0]==None :
                    print("MISSING : Empty image, Relative image : ", os.path.join(PATH, imgArr[img][1]))
                elif imgArr[img][1]==None :
                    print("MISSING : Empty image, Relative image : ", os.path.join(PATH, imgArr[img][0]))
                else:
                    image_path = getImagePath(PATH,imgArr[img][0])
                    print("checking", image_path)
                    if not os.path.isfile(image_path) :
                        print("MISSING : ", PATH + imgArr[img][0]," Relative image : ", os.path.join(PATH, imgArr[img][1]))
                    else:
                        if itemIndex < 2 or itemIndex > (numOfItems - 3):
                            print("image type",getImageType(image_path))
                    image_path = getImagePath(PATH, imgArr[img][1])
                    print("checking", image_path)
                    if not os.path.isfile(image_path):
                        print("MISSING : ", PATH + imgArr[img][1]," Relative image : ", os.path.join(PATH, imgArr[img][0]))
                    else:
                        if itemIndex < 2 or itemIndex > (numOfItems - 3):
                            print("image type",getImageType(image_path))
                itemIndex = itemIndex + 1



def getImgFromXML(PATH):
    try:
        tree = ET.parse(PATH)
        o = {}
        i = 0
        # for elt in tree.iter():
        for elt in tree.findall('./items/item'):
            o[i] = {}
            o[i][0] = elt.find('image_left').text
            o[i][1] = elt.find('image_right').text
            i = i + 1
        return o
    except:
        logging.error("CORRUPTED XML path: {0}".format(PATH))
        return {}


def check_img_in_all_folders():
    for folder in next(os.walk('.'))[1]:
        check_images(folder)


"""

Folder structure check

"""

def check_folder_structure(PATH):
    RAW_PATH = os.path.join(PATH,"raw")
    PROJECT_PATH = os.path.join(PATH,"project.xml")
    hasDSLR = False
    if os.path.isfile(PROJECT_PATH) and os.path.isdir(RAW_PATH):
        hasDSLR = True
    elif os.path.isdir(RAW_PATH):
        hasDSLR = True
        print("Missing project.xml in : {0}".format(PATH))
    hasTIF = False
    for file in next(os.walk(PATH))[2]:
        if os.path.splitext(file)[1] == ".TIF" or os.path.splitext(file)[1] == ".tif":
            hasTIF = True
            break
        elif os.path.splitext(file)[1] == ".cr2":
            print("cr2 are not in raw folder in : {0}".format(PATH))
            break
    if hasTIF and hasDSLR:
        print("Mixed project detected : {0}".format(PATH))
    elif hasTIF:
        print("Scanner project detected : {0}".format(PATH))
    elif hasDSLR:
        print("Factum project detected : {0}".format(PATH))
        check_images(PATH)
    elif not hasTIF and not hasDSLR:
        print("Unknown file structure at: {0}".format(PATH))


def check_scan_folder_structure():
    for folder in next(os.walk('.'))[1]:
        check_folder_structure(folder)


def check_scan_folder_structure_recursive():
    for root, dirs, files in os.walk('.'):
        check_folder_structure(root)


"""



"""


def check_xml(PATH):
    PATH = os.path.join(PATH,"project.xml")
    ERROR = 0
    if os.path.isfile(PATH):
        try:
            DOMTree = xml.dom.minidom.parse(PATH)
            collection = DOMTree.documentElement
        except:
            logging.error("CORRUPTED XML path: {0}".format(PATH))
            return False
        meta = collection.getElementsByTagName("metadata")
        if (len(meta[0].getElementsByTagName("code")) == 0):
            ERROR = 1
        elif (len(meta[0].getElementsByTagName("name")) == 0):
            ERROR = 2
        elif (len(meta[0].getElementsByTagName("timestamp")) == 0):
            ERROR = 3
        elif (len(meta[0].getElementsByTagName("target_directory")) == 0):
            ERROR = 4
        elif (len(meta[0].getElementsByTagName("target_subdirectory")) == 0):
            ERROR = 5
        elif (len(meta[0].getElementsByTagName("source")) == 0):
            ERROR = 6
        if (ERROR == 0):
            # print("_no_error")
            return True
        else:
            # print("_ERROR_")
            logging.error("XML ERROR NOT TAGs path: {0}, error code : {1}".format(PATH,ERROR))
            return False
    else:
        # print("_NO_FILE_")
        logging.error("XML ERROR NOT project.xml FILE path: {0}".format(PATH))
        return False

def check_xml_in_all_folders():
    for folder in next(os.walk('.'))[1]:
        if not check_xml(folder):
            print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", folder)















