#!/usr/bin/env python
# -*- coding: utf-8 -*-

import MySQLdb
import common
import os
from time import gmtime, strftime
import time
import serial
import gphoto2 as gp
import xml.etree.cElementTree as ET
import string
import random

# Database connection data
host_address = "localhost"
user_name = "root"
user_pass = "factum"
database_name = "BD2"

# app settings
sd_root_path = "/store_00020001/DCIM/100EOS5D/"

#-------------------------------------------------------------------------------
# list_sd_files()
#-----------------------------------------------------------------------------
def list_sd_files(camera, context, path='/'):
    result = []
    # get files
    for name, value in camera.folder_list_files(path, context):
        result.append(os.path.join(path, name))
    # read folders
    folders = []
    for name, value in camera.folder_list_folders(path, context):
        folders.append(name)
    # recurse over subfolders
    for name in folders:
        result.extend(list_sd_files(camera, context, os.path.join(path, name)))
    return result

#-------------------------------------------------------------------------------
# capture()
#-----------------------------------------------------------------------------
def capture(camera_id):

    common.system_log.info("------------------------------------------------")
    common.system_log.info("INFO: Capture from camera %i",camera_id)
    
    # Get camera data from the ddbb
    try:
        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    except:
        common.system_log.error("ERROR 004 - Database connection fails. HOST:%s USER:%s PASS:%s :DB NAME%s",host_address,user_name,user_pass,database_name)
        return -1
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    cursor.execute("SELECT * FROM camerasettings WHERE type='CURRENT'")
    result = cursor.fetchall()
    db.close()

    ddbb_active = result[camera_id]["active"]
    ddbb_serial_number = result[camera_id]["serial_number"]
    ddbb_id_camera = result[camera_id]["id_camera"]
    
    common.system_log.info("INFO: App settings for this camera...")
    common.system_log.info("INFO:\t\tactive:\t\t"+str(ddbb_active))
    common.system_log.info("INFO:\t\tserial_number:\t"+ddbb_serial_number)

    # Check if we need to capture with this camera
    if ( ddbb_active == 0 ):
        common.system_log.info("INFO: Aborting capture...")
        return -1

    # Make a list of all available cameras
    context = gp.Context()    
    camera_list = []
    for name, addr in context.camera_autodetect():
        camera_list.append((name, addr))

    # Check if there are, at least, one camera detected...
    if not camera_list:
        common.system_log.error("ERROR 005: No cameras connected")
        return -1
    
    camera_list.sort(key=lambda x: x[0])
    common.system_log.info("INFO: Connected cameras list...")
    
    # Look for camera USB port (using its serial_number)
    current_camera = -1
    for cam in range(len(camera_list)):
        common.system_log.info("INFO:\t\t"+str(camera_list[cam]))
        #print("INFO:\t\t"+str(camera_list[cam]))
        # Init camera
        camera = gp.Camera()
        port_info_list = gp.PortInfoList()
        port_info_list.load()
        index = port_info_list.lookup_path(camera_list[cam][1])
        camera.set_port_info(port_info_list[index])

        # TODO Add security checking
        camera.init(context)
        time.sleep(1)
        
        # Get serial number
        config = gp.check_result(gp.gp_camera_get_config(camera, context))
        capture_target = gp.check_result(gp.gp_widget_get_child_by_name(config, 'eosserialnumber'))
        serial_number = gp.check_result(gp.gp_widget_get_value(capture_target))
        common.system_log.info("INFO:\t\tserial_number\t"+serial_number)
        # Store camera object for later use
        if ( serial_number == ddbb_serial_number ):
            current_camera = camera          
            break
        
        # Release camera
        gp.check_result(gp.gp_camera_exit(camera, context))
        camera.exit(context)

    # Security check 
    if current_camera ==-1:
        common.system_log.error("ERROR 006: Serial number doesn't appears")        
        return -1

    common.system_log.info("INFO: Camera port detected...[%s]",camera_list[cam][1])        

    # Update camera parameters from webapp
    if camera_id != "":
        filtro=" where id_camera = '"+str(camera_id) + "' and type='TARGET'" 
    else:
        filtro=""
    try:
        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
        cursor = db.cursor(MySQLdb.cursors.DictCursor)
        query = "SELECT * FROM camerasettings"+filtro
        cursor.execute(query)
        result = cursor.fetchall()
        db.close()

        config = gp.check_result(gp.gp_camera_get_config(current_camera, context))

        # Set values
        aperture_target = gp.check_result(gp.gp_widget_get_child_by_name(config, 'aperture'))
        aperture = result[0]["fnumber"]
        gp.check_result(gp.gp_widget_set_value( aperture_target, str(aperture)))

        shutter_speed_target = gp.check_result(gp.gp_widget_get_child_by_name(config, 'shutterspeed'))
        shutterspeed = result[0]["shutterspeed"]
        gp.check_result(gp.gp_widget_set_value( shutter_speed_target, str(shutterspeed)))

        iso_target = gp.check_result(gp.gp_widget_get_child_by_name(config, 'iso'))
        iso = result[0]["iso"]
        gp.check_result(gp.gp_widget_set_value( iso_target, str(iso)))
        
        # Update config
        gp.check_result(gp.gp_camera_set_config(camera, config, context))
        
        common.system_log.info("INFO: Current parameters uploaded to camera")
    except:
        common.system_log.error("ERROR 007: Error while setting current camera parameters")
        return -1
    #time.sleep(2)
    
    # Get a list with file names on SD card
    camera_files =  list_sd_files(current_camera,context)
    if not camera_files:
        common.system_log.error("ERROR 008: No files on SD card")
        return -1

    # Get current book data
    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    cursor.execute("SELECT * FROM book WHERE status='ACTIVE'")
    result = cursor.fetchall()
    db.close()
    root_path = result[0]["folder_path"]
    if camera_id == 0:
        page_num = result[0]["page_num_a"] # + "_" + str(id_camera)
    else:
        page_num = result[0]["page_num_b"] # + "_" + str(id_camera)
    id_book = result[0]["id_book"]
    image_unique_id = int (result[0]["image_unique_id_counter"])
    
    # Create an unique file name, and timestamp for each image
    timestamp = strftime("%Y%m%d%H%M%S", gmtime())
    file_name = "_" + str(id_book) + "_" + str(camera_id) + "_"+ page_num +"_" + timestamp
    raw_hd_filename = root_path + "/" + file_name  + ".cr2"
    full_file_name = root_path + "/" + file_name
    
    # Download last image and delete it from SD card
    sd_filepath = camera_files[len(camera_files)-1]
    folder, name = os.path.split(sd_filepath)
    camera_file = gp.check_result(gp.gp_camera_file_get(current_camera, folder, name, gp.GP_FILE_TYPE_NORMAL, context))
    common.system_log.info("INFO: Downloading capture..." + file_name + ".cr2")
    gp.check_result(gp.gp_file_save(camera_file, raw_hd_filename))
    #common.system_log.info("INFO: Deleting image on SD..." + sd_filepath)
    #gp.check_result(gp.gp_camera_file_delete(camera, folder, name, context))
                             
    # Generates a JPG thumbail from CR2 file
    command = "exiftool -b -PreviewImage -w .jpg -ext cr2 -r " + raw_hd_filename
    response = common.system_call(command)
    common.system_call("convert -resize 9% " + full_file_name + ".jpg" + " " + full_file_name + ".jpg")
     
    # Rotate images
    angle_str = ""
    if ( camera_id == 0 ):
        angle_str = "90 "
    else:
        angle_str = "270 "

    command = "exiftool -Orientation='Rotate " + angle_str + "' "+raw_hd_filename
    common.system_call(command)    
    command = "convert -rotate " + angle_str + full_file_name +".jpg" + " " + full_file_name + ".jpg"
    common.system_call(command)

    # Generate MD5 (only for CR2 files)
    try:
        response = common.system_call("md5sum " + raw_hd_filename )
        index = response.find(" ",0)
        md5sum = response[0:index]
    except:
        common.system_log.error("ERROR 009: Error creating MD5SUM - " + raw_hd_filename)

    # Compose id_label
    r = lambda: random.randint(0,255)
    randomHEX = str('%02X%02X%02X_' % (r(),r(),r()))
    id_label = randomHEX + str(id_book) + "_" + str(page_num) + "_" + str(image_unique_id) + "_" + str(timestamp)
    # Add current page to database
    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    query = "INSERT INTO image (id_cameraSetting,file_name,file_path,md5_checksum,time_stamp,id_page,id_book,unique_image_id_per_book,id_camera,iso,shutterspeed,fnumber) VALUES ("+str(1)+",'"+file_name+ ".cr2"+"','"+root_path+"','"+md5sum+"','"+timestamp+"','"+page_num+"',"+str(id_book)+",'"+id_label+"','"+str(camera_id)+"','"+"100"+"','"+"1/125"+"','"+"0.8f"+"')"
    result = cursor.execute(query)
    db.commit()
    db.close()

    # Update image_unique_id
    image_unique_id = image_unique_id +1
    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    query = "UPDATE book SET image_unique_id_counter="+str(image_unique_id)+" WHERE status='ACTIVE'"
    cursor.execute(query)
    result = cursor.fetchall()
    db.close()
    
    # Add rootpath,page_num and id_book to image metadata
    command = "exiftool -XMP-dc:Identifier='"+id_label+"' "+raw_hd_filename
    common.system_call(command)
    command = "exiftool -XMP-dc:Source='"+str(id_book)+"' "+raw_hd_filename
    common.system_call(command)
    command = "exiftool -XMP-dc:Description='"+page_num+"' "+raw_hd_filename
    common.system_call(command)
    command = "exiftool -XMP-dc:All "+raw_hd_filename
    #common.system_log.info("INFO: Metadata inside CR2 file:\n" + common.system_call(command) )

    # Create an XML file with all associated metadata
    root = ET.Element("page")
    doc = ET.SubElement(root, "descriptive_metada")

    ET.SubElement(doc, "file_name").text = file_name  + ".cr2"
    ET.SubElement(doc, "check_sum").text = md5sum
    ET.SubElement(doc, "book_id").text = str(id_book)
    ET.SubElement(doc, "page_num").text = page_num
    ET.SubElement(doc, "image_unique_id").text = id_label

    # Store EXIF metadata
    command = "exiftool -EXIF:All " + raw_hd_filename
    metadata = common.system_call(command)
    lines = string.split(metadata, "\n")
    doc = ET.SubElement(root, "technical_metadata")
        
    for i in range( 0, len(lines)-1 ):
        data = string.split(lines[i], ":")
        param_name = data[0].strip()
        param_value = data[1].strip()
        param_name = param_name.replace(" ", "")
        param_name = param_name.replace("/", "_")
        ET.SubElement(doc, param_name ).text = param_value
    
    tree = ET.ElementTree(root)
    tree.write( full_file_name+".xml" )

    # Release camera
    gp.check_result(gp.gp_camera_exit(current_camera, context))
    camera.exit(context)

    # Delete exiftool backup and sd images
    
    time.sleep(1)
    common.system_call("rm " + raw_hd_filename + "_original")
    common.system_call("rm " + full_file_name + ".jpg" + "_original")

    # For test purposes only
    
    time.sleep(1)
    common.system_call("rm " + raw_hd_filename)
    common.system_call("rm " + full_file_name + ".jpg")
    
    #print "delete: " + os.remove(sd_filepath)
    #print common.system_call("rm " + sd_filepath)
    #print sd_filepath

#-------------------------------------------------------------------------------
# capture_both()
#-------------------------------------------------------------------------------
def capture_both():
    # Get which cameras should be triggered
    try:
        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    except:
        common.system_log.error("ERROR 004: Database connection fails. HOST:%s USER:%s PASS:%s :DB NAME%s",host_address,user_name,user_pass,database_name)

    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    cursor.execute("SELECT * FROM camerasettings WHERE type='CURRENT'")
    result = cursor.fetchall()
    db.close()
    ddbb_active_left = result[0]["active"]
    ddbb_active_right = result[1]["active"]

    if ddbb_active_left == 1 and ddbb_active_right == 1:
        serialFromArduino.write('b')
        time.sleep(3)
        capture(0)
        capture(1)
    elif ddbb_active_left == 1 and ddbb_active_right == 0:
        serialFromArduino.write('l')
        time.sleep(3)
        capture(0)
    elif ddbb_active_left == 0 and ddbb_active_right == 1:
        serialFromArduino.write('r')
        time.sleep(3)
        capture(1)
        
#-------------------------------------------------------------------------------
# main()
#-------------------------------------------------------------------------------

# Unmount SDs
response = common.system_call("sudo killall gvfs-gphoto2-volume-monitor")
                              
# Arduino connection
microcontroller_serial_port = "/dev/ttyACM"
success = 0
for i in range(0, 9):
    try:
        serial_port = microcontroller_serial_port + str(i)
        common.system_call("sudo chmod 666 " + serial_port)
        serialFromArduino = serial.Serial(serial_port,9600)
        serialFromArduino.flush()
        common.system_log.info("INFO: Connected to serial port: %s", serial_port)
        success = 1
        break
    except:
        common.system_log.info("INFO: Failed to connect to serial port: %s", serial_port)
# Wait for the arduino (each time we open a serial connection the arduino resets)
time.sleep(2)

# Capture from cameras
if success==0:
    common.system_log.error("ERROR 001: Failed to connect to microcontroller")
else:
    for i in range(0, 1000):
        common.system_log.info("-----------------------------------------")        
        common.system_log.info("INFO: %s", i)       
        capture_both()
        common.print_logs()
        
serialFromArduino.close()

# Print log

common.log_to_ddbb()
