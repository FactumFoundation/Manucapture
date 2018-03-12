"""
reconstruct project.xml script
by eEsteban :: enrique.esteban@factumfoundation.org
"""

"""
This script analize the RAW folder and tryes to regenerate project.xml given:
- Project URL
- Color chart A file name
- Color chart B file name

La estructura del project.xml final es:

<project>
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
    <items>
        <item id="0">
            <image_left>raw/1064_A_6.cr2</image_left>
            <image_left_md5 />
            <icc_profile_left />
            <process_exposure_factor_left />
            <image_right>raw/_B_2.cr2</image_right>
            <image_right_md5 />
            <icc_profile_right />
            <process_exposure_factor_right />
            <page_num>0</page_num>
            <comment />
            <type>chart</type>
        </item>
        <item id="1">
            <image_left>raw/1064_A_7.cr2</image_left>
            <image_right>raw/1064_B_8.cr2</image_right>
            <page_num>0.0</page_num>
            <image_left_md5 />
            <icc_profile_left />
            <process_exposure_factor_left />
            <image_right_md5 />
            <icc_profile_right />
            <process_exposure_factor_right />
            <comment />
            <type>page</type>
        </item>
        
        ...
        
    </items>
    <image_counter>622</image_counter>
</project>
  
"""

import os
import json
import ntpath
import xml.etree.ElementTree as ET
import datetime
from os import listdir
from os.path import isfile, join

def getKey(item):
    base_name = os.path.splitext(item)[0]
    name_components = base_name.split('_')
    image_counter = name_components[len(name_components)-1]
    return int(image_counter)

def list_RAW_files(url) :
    raw_folder = os.path.join(url,"raw/")
    onlyRawFiles = [f for f in listdir(raw_folder) if isfile(join(raw_folder, f)) and os.path.splitext(f)[1]==".cr2"]
    sortedRawFiles = sorted(onlyRawFiles, key=getKey)
    first_element = ""
    last_file_name = ""
    page_pairs = []
    errors = False
    for f in sortedRawFiles :
        base_name = os.path.splitext(f)[0]
        name_components = base_name.split('_')
        if first_element == "" :
            first_element = name_components[len(name_components)-2]
            last_file_name = f
        elif first_element == "A" :
            if name_components[len(name_components)-2] == "A" :
                print "TWO A pages and no B pages " + f + " " + last_file_name
                errors = True
            else :
                page_pairs.append([last_file_name,f])
            first_element = ""
            last_file_name = f
        elif first_element == "B" :
            if name_components[len(name_components)-2] == "B" :
                print "TWO B pages and no A pages " + f + " " + last_file_name
                errors = True
            else :
                page_pairs.append([f,last_file_name])
            first_element = ""
            last_file_name = f
    if errors :
        return None
    else :
        return page_pairs

def reconstruct_project_XML(url, color_chart_A, color_chart_B) :
    code = ntpath.basename(url)
    project_xml = ET.Element('project')
    ## Metadata ##
    metadata_xml = ET.SubElement(project_xml, 'metadata')
    name_xml = ET.SubElement(metadata_xml, 'name')
    name_xml.text = "Book " + code
    code_xml = ET.SubElement(metadata_xml, 'code')
    code_xml.text = code
    timestamp_xml = ET.SubElement(metadata_xml, 'timestamp')
    current_date = datetime.datetime.now()
    timestamp = str(current_date.year) + str(current_date.month) + str(current_date.hour) + str(current_date.minute) + str(current_date.second)
    timestamp_xml.text = timestamp
    target_directory_xml = ET.SubElement(metadata_xml, 'target_directory')
    target_directory_xml.text = "1_Oriental_Manuscripts_Fund"
    target_subdirectory_xml = ET.SubElement(metadata_xml, 'target_subdirectory')
    target_subdirectory_xml.text = "F14"
    uploaded_xml = ET.SubElement(metadata_xml, 'uploaded')
    uploaded_xml.text = "True"
    source_xml = ET.SubElement(metadata_xml, 'source')
    source_xml.text = "dslr"
    ET.SubElement(metadata_xml, 'comment')
    ET.SubElement(metadata_xml, 'author')
    ## Items ###
    items = ET.SubElement(project_xml, 'items')
    page_pairs = list_RAW_files(url)
    if page_pairs == None :
        return
    else :
        print page_pairs
        images_to_remove = []
        ## If color_chart_A && color_chart_B
        if color_chart_A != "" and color_chart_B != "" :
            for item in page_pairs :
                if item[0] == color_chart_A :
                    images_to_remove.append(item[1])
                    page_pairs.remove(item)
                    break
            for item in page_pairs:
                if item[1] == color_chart_B:
                    images_to_remove.append(item[0])
                    page_pairs.remove(item)
                    break
            with open(os.path.join(url,'items_to_remove.json'), 'w') as outfile:
                json.dump(images_to_remove, outfile)
            # charts_item = ET.SubElement(items,'item')
            charts_item = ET.Element('item')
            charts_item.attrib["id"] = "0"
            image_left = ET.SubElement(charts_item, 'image_left')
            image_left.text = color_chart_A
            ET.SubElement(charts_item, 'image_left_md5')
            ET.SubElement(charts_item, 'icc_profile_left')
            ET.SubElement(charts_item, 'process_exposure_factor_left')
            image_right = ET.SubElement(charts_item, 'image_right')
            image_right.text = color_chart_B
            ET.SubElement(charts_item, 'image_right_md5')
            ET.SubElement(charts_item, 'icc_profile_right')
            ET.SubElement(charts_item, 'process_exposure_factor_right')
            page_num_xml = ET.SubElement(charts_item, 'page_num')
            page_num_xml.text = "0"
            ET.SubElement(charts_item, 'comment')
            type = ET.SubElement(charts_item, 'type')
            type.text = "chart"
            items.insert(0, charts_item)
            item_id = 1
            page_number = 0.0
            item_index = 1
            for item in page_pairs :
                item_xml = ET.Element('item')
                item_xml.attrib["id"] = str(item_id)
                item_id = item_id + 1
                image_left = ET.SubElement(item_xml, 'image_left')
                image_left.text = item[0]
                ET.SubElement(item_xml, 'image_left_md5')
                ET.SubElement(item_xml, 'icc_profile_left')
                ET.SubElement(item_xml, 'process_exposure_factor_left')
                image_right = ET.SubElement(item_xml, 'image_right')
                image_right.text = item[1]
                ET.SubElement(item_xml, 'image_right_md5')
                ET.SubElement(item_xml, 'icc_profile_right')
                ET.SubElement(item_xml, 'process_exposure_factor_right')
                page_num_xml = ET.SubElement(item_xml, 'page_num')
                page_num_xml.text = str(page_number)
                page_number = page_number + 1
                ET.SubElement(item_xml, 'comment')
                type = ET.SubElement(item_xml, 'type')
                type.text = "page"
                items.insert(item_index, item_xml)
                item_index = item_index + 1
            image_counter = ET.SubElement(project_xml, 'image_counter')
            image_counter.text = str(int(item_index * 2))
            print "number of items " + image_counter.text
            ## If not: Create project with no color charts item
            # TODO: Que hacer en caso de solo un color chart?
            ## Write project.xml ##
            tree = ET.ElementTree(project_xml)
            tree.write(open(os.path.join(url,"project.xml"),'w'),xml_declaration=True, encoding='utf-8', method="xml")





