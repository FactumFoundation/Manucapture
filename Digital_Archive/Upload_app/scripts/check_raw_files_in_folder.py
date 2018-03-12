#!/usr/bin/python -tt

"""
Update project.xml script
by eEsteban :: enrique.esteban@factumfoundation.org
"""

"""
checks that all the raw files in the project.xml exists in the RAW folder
"""

import xml.etree.ElementTree as ET
import os
import logging
import datetime
import ntpath
import json


def check_raws_in_folder(url):
    # Open original file and do backup
    PATH_TO_PROJECT =  url #.encode('ascii', 'ignore')
    PATH_TO_PROJECT_FILE =  os.path.join(PATH_TO_PROJECT, "project.xml")
    PROJECT_NAME = ntpath.basename(PATH_TO_PROJECT).encode('ascii', 'ignore')
    print "Opening " + PATH_TO_PROJECT_FILE
    if os.path.isfile(PATH_TO_PROJECT_FILE) and os.access(PATH_TO_PROJECT_FILE, os.R_OK):
        et = ET.parse(PATH_TO_PROJECT_FILE)
        root =  et.getroot()
        items = root.find("items")
        for item in items :
            left_raw_img = item.find("image_left").text
            right_raw_img = item.find("image_right").text
            PATH_TO_LEFT_RAW = os.path.join(PATH_TO_PROJECT,left_raw_img)
            PATH_TO_RIGHT_RAW = os.path.join(PATH_TO_PROJECT,right_raw_img)
            if not (os.path.isfile(PATH_TO_LEFT_RAW) and os.access(PATH_TO_LEFT_RAW, os.R_OK)) :
                print " ERROR: " + PATH_TO_LEFT_RAW + " not exists"
            if not (os.path.isfile(PATH_TO_RIGHT_RAW) and os.access(PATH_TO_RIGHT_RAW, os.R_OK)) :
                print " ERROR: " + PATH_TO_RIGHT_RAW + " not exists"










