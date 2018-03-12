import xml.etree.ElementTree as ET
import os
import logging
import datetime
import ntpath
import json


def create_scanner_XML(url) :
    code = ntpath.basename(url)
    project_xml = ET.Element('project')
    metadata_xml = ET.SubElement(project_xml, 'metadata')
    name_xml = ET.SubElement(metadata_xml, 'name')
    name_xml.text = "Book " + code
    code_xml = ET.SubElement(metadata_xml, 'code')
    code_xml.text = code
    timestamp_xml = ET.SubElement(metadata_xml, 'timestamp')
    current_date = datetime.datetime.now()
    timestamp = str(current_date.year) + str(current_date.month) + str(current_date.day) + str(current_date.hour) + str(
        current_date.minute) + str(current_date.second)
    timestamp_xml.text = timestamp
    timestamp_xml.text = timestamp
    target_directory_xml = ET.SubElement(metadata_xml, 'target_directory')
    target_directory_xml.text = "1_Oriental_Manuscripts_Fund"
    target_subdirectory_xml = ET.SubElement(metadata_xml, 'target_subdirectory')
    target_subdirectory_xml.text = "F14"
    uploaded_xml = ET.SubElement(metadata_xml, 'uploaded')
    uploaded_xml.text = "True"
    source_xml = ET.SubElement(metadata_xml, 'source')
    source_xml.text = "scanner"
    ET.SubElement(metadata_xml, 'comment')
    ET.SubElement(metadata_xml, 'author')
    tree = ET.ElementTree(project_xml)
    tree.write(open(os.path.join(url,"project.xml"),'w'),xml_declaration=True, encoding='utf-8', method="xml")



