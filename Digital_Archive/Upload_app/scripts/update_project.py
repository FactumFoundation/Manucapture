#!/usr/bin/python -tt

"""
Update project.xml script
by eEsteban :: enrique.esteban@factumfoundation.org
"""

"""

1. Metadata modification

    <metadata>
        <name>654</name>
        <code>654</code>
        <comment />
        <author>654</author>
    </metadata>


    <metadata>
        <name>Book 1064</name>
        <code>1064</code>
        <comment />
        <author />
        <timestamp>2018112734</timestamp>
        <target_directory>1_Oriental_Manuscripts_Fund</target_directory>
        <target_subdirectory>F14</target_subdirectory>
        <uploaded>True</uploaded>
        <source>dslr</source>
    </metadata>

2. Modificacion de items:

XML original

<item>
    <image_left>raw/234_A_9.cr2</image_left>
    <image_right>raw/234_B_12.cr2</image_right>
    <page_num>1.0</page_num>
    <comment/>
    <type>chart</type>
</item>

xml Final

<item id="1">
    <image_left>raw/234_A_9.cr2</image_left>
    <image_left_md5>sdgfdgsdfgsfgsgdfsdfgsdg</image_left_md5>
    <icc_profile_left>PROFILES/234_A_9.icc</icc_profile_left>
    <process_exposure_factor_left>1.11</process_exposure_factor_left>
    <image_right>raw/234_B_9.cr2</image_right>
    <image_right_md5>sgsfdasdfasdfasdfs</image_right_md5>
    <icc_profile_right>PROFILES/234_B_9.icc</icc_profile_right>
    <process_exposure_factor_right>1.11</process_exposure_factor_right>
    <page_num>0</page_num>
    <comment />
    <type>chart</type>
</item>


"""

import xml.etree.ElementTree as ET
import os
import logging
import datetime
import ntpath
import json


def update_xml(url, chart_A, chart_B):
    # Open original file and do backup
    PATH_TO_PROJECT =  url #.encode('ascii', 'ignore')
    PATH_TO_PROJECT_FILE =  os.path.join(PATH_TO_PROJECT, "project_bp.xml")
    PROJECT_NAME = ntpath.basename(PATH_TO_PROJECT).encode('ascii', 'ignore')
    print "Opening " + PATH_TO_PROJECT_FILE + " " + chart_A + " " + chart_B
    print os.path.isfile(PATH_TO_PROJECT_FILE)
    print os.access(PATH_TO_PROJECT_FILE, os.R_OK)
    if os.path.isfile(PATH_TO_PROJECT_FILE) and os.access(PATH_TO_PROJECT_FILE, os.R_OK):
        print "Process " + PATH_TO_PROJECT_FILE + " " + chart_A + " " + chart_B
        PATH_TO_NEW_PROJECT_FILE = os.path.join(PATH_TO_PROJECT, "project.xml")
        et = ET.parse(PATH_TO_PROJECT_FILE)
        PATH_TO_CHART_A = os.path.join("raw", chart_A)
        PATH_TO_CHART_B = os.path.join("raw", chart_B)
        root =  et.getroot()
        metadata = root.find("metadata")
        name_xml = metadata.find('name')
        code_xml = metadata.find('code')
        name_xml.text = "Book " + ntpath.basename(PATH_TO_PROJECT)
        timestamp_xml = ET.SubElement(metadata, 'timestamp')
        current_date = datetime.datetime.now()
        timestamp = str(current_date.year) + str(current_date.month) + str(current_date.day) + str(current_date.hour) + str(
            current_date.minute) + str(current_date.second)
        timestamp_xml.text = timestamp
        target_directory_xml = ET.SubElement(metadata, 'target_directory')
        target_directory_xml.text = "1_Oriental_Manuscripts_Fund"
        target_subdirectory_xml = ET.SubElement(metadata, 'target_subdirectory')
        target_subdirectory_xml.text = "F14"
        uploaded_xml = ET.SubElement(metadata, 'uploaded')
        uploaded_xml.text = "True"
        source_xml = ET.SubElement(metadata,'source')
        source_xml.text = "dslr"
        items = root.find("items")
        #chart_counter = 0
        chart_counter = 1  # This is for start counting at 0. Assuming first page is for Book cover
        chart_item_A = None
        chart_item_B = None
        chart_item_A_to_remove = ""
        chart_item_B_to_remove = ""
        print "parse items"
        for item in items :
            page_num = float(item.find("page_num").text)
            print item.find("image_right").text + " " + PATH_TO_CHART_B
            if item.find("image_left").text == PATH_TO_CHART_A or item.find("image_right").text == PATH_TO_CHART_B:
                if chart_item_A == None and item.find("image_left").text == PATH_TO_CHART_A:
                    print "Chart A " + item.find("image_left").text + " at " + str(page_num)
                    chart_item_A = item
                    chart_item_A_to_remove = item.find("image_right").text
                if chart_item_B == None and item.find("image_right").text == PATH_TO_CHART_B:
                    print "Chart B " + item.find("image_right").text + " at " + str(page_num)
                    chart_item_B = item
                    chart_item_B_to_remove = item.find("image_left").text
                chart_counter += 1
                page_num = 0
            else :
                item.find("type").text = 'page'
                page_num -= chart_counter
                if page_num - int(page_num) == 0 :
                    item.attrib['id'] = str(int(page_num+1))
                else :
                    item.attrib['id'] = str(page_num+1)
            item.find("page_num").text = str(page_num)
            ET.SubElement(item, 'image_left_md5')
            ET.SubElement(item, 'icc_profile_left')
            ET.SubElement(item, 'process_exposure_factor_left')
            ET.SubElement(item, 'image_right_md5')
            ET.SubElement(item, 'icc_profile_right')
            ET.SubElement(item, 'process_exposure_factor_right')
        if chart_item_A != None and chart_item_B != None :
            #charts_item = ET.SubElement(items,'item')
            charts_item = ET.Element('item')
            charts_item.attrib["id"] = "0"
            image_left = ET.SubElement(charts_item, 'image_left')
            image_left.text = PATH_TO_CHART_A
            ET.SubElement(charts_item, 'image_left_md5')
            ET.SubElement(charts_item, 'icc_profile_left')
            ET.SubElement(charts_item, 'process_exposure_factor_left')
            image_right = ET.SubElement(charts_item, 'image_right')
            image_right.text = PATH_TO_CHART_B
            ET.SubElement(charts_item, 'image_right_md5')
            ET.SubElement(charts_item, 'icc_profile_right')
            ET.SubElement(charts_item, 'process_exposure_factor_right')
            page_num_xml = ET.SubElement(charts_item, 'page_num')
            page_num_xml.text = "0"
            ET.SubElement(charts_item, 'comment')
            type = ET.SubElement(charts_item, 'type')
            type.text = "chart"
            items.insert(0,charts_item)
            try :
                items.remove(chart_item_A)
                items.remove(chart_item_B)
                logging.info("Proyect {0} processed successfully. Images to remove --> {1} {2} ".format(PROJECT_NAME,chart_item_A_to_remove,chart_item_B_to_remove))
                images_to_remove = [chart_item_A_to_remove,chart_item_B_to_remove]
                with open(os.path.join(url, 'items_to_remove.json'), 'w') as outfile:
                    json.dump(images_to_remove, outfile)
            except Exception as e:
                print(e)
                logging.error("Proyect {0} failed when remove items".format(PROJECT_NAME))
        else :
            logging.error("Proyect {0} failed, charts not found ".format(PROJECT_NAME))
        # Write back to file
        print "Writting " + PATH_TO_NEW_PROJECT_FILE
        et.write(PATH_TO_NEW_PROJECT_FILE)











