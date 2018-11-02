import os
import json
import ntpath
import xml.etree.ElementTree as ET
import datetime
from os import listdir
from os.path import isfile, join

"""
This script fix cyrilic chars in code project by a given code replacement
The resulting project needs to be updated later!
"""

def detect_non_ascii_in_project_name(url) :
    code = ntpath.basename(url)
    try:
        code.decode('ascii')
    except UnicodeEncodeError:
        print "it was not a ascii-encoded unicode string"
    else:
        print "Ok!!"

def detect_non_ascii_in_project(url, code_replacement) :
    PATH_TO_PROJECT_FILE = os.path.join(url, "project_bp.xml")
    et = ET.parse(PATH_TO_PROJECT_FILE)
    root = et.getroot()
    metadata = root.find("metadata")
    name_xml = metadata.find('name')
    try:
        name_xml.text.decode('ascii')
    except UnicodeEncodeError:
        print "Project Name has non ascii characters"
        name_xml.text = code_replacement
    code_xml = metadata.find('code')
    try:
        code_xml.text.decode('ascii')
    except UnicodeEncodeError:
        print "Project Code has non ascii characters"
        code_xml.text = code_replacement
    author_xml = metadata.find('author')
    try:
        author_xml.text.decode('ascii')
    except UnicodeEncodeError:
        print "Project Author has non ascii characters"
        author_xml.text = code_replacement
    items = root.find("items")
    for item in items:
        left_img = item.find("image_left")
        name_components = left_img.text.split('_')
        try:
            name_components[0].decode('ascii')
        except UnicodeEncodeError:
            new_name = code_replacement + "_" + name_components[1] + "_" + name_components[2]
            print "replacing " + left_img.text + " by  raw/" + new_name
            src_image_file = os.path.join(url, left_img.text)
            dst_image_file = os.path.join(url, "raw", new_name)
            os.rename(src_image_file,dst_image_file)
            left_img.text = os.path.join("raw", new_name)
        right_img = item.find("image_right")
        name_components = right_img.text.split('_')
        try:
            name_components[0].decode('ascii')
        except UnicodeEncodeError:
            new_name = code_replacement + "_" + name_components[1] + "_" + name_components[2]
            print "replacing " + right_img.text + " by raw/" + new_name
            src_image_file = os.path.join(url, right_img.text)
            dst_image_file = os.path.join(url, "raw", new_name)
            os.rename(src_image_file,dst_image_file)
            right_img.text = os.path.join("raw", new_name)
    project_path = os.path.join(url, "project.xml")
    print "Writting " + project_path
    et.write(project_path)

