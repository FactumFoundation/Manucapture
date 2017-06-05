#!/usr/bin/env python
# -*- coding: utf-8 -*-

import MySQLdb
import common

import os
from time import gmtime, strftime
import time
import serial

# Database connection data
host_address = "localhost"
user_name = "root"
user_pass = "factum"
database_name = "BD2"

# app settings
image_unique_id = -1

sd_root_path = "/store_00020001/DCIM/100EOS5D/"
microcontroller_serial_port = "/dev/ttyACM1"

def shootCamera(id_camera):
    # Get active camera data
    try:
        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    except:
        common.system_log.error("ERROR 004 - Database connection fails. HOST:%s USER:%s PASS:%s :DB NAME%s",host_address,user_name,user_pass,database_name)
        return -1;

    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    cursor.execute("SELECT * FROM book WHERE status='ACTIVE'")
    result = cursor.fetchall()
    db.close()
    
    root_path = result[0]["folder_path"]
    if id_camera == 0:
        page_num = result[0]["page_num_a"] # + "_" + str(id_camera)
    else:
        page_num = result[0]["page_num_b"] # + "_" + str(id_camera)
    id_book = result[0]["id_book"]   
    
    # Check in DDBB, is this camera active?
    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)

    cursor.execute("SELECT * FROM camerasettings WHERE type='TARGET'")
    result = cursor.fetchall()
    db.close()
    
    if id_camera == 0:
        is_camera_on = result[0]["active"]
    else:
        is_camera_on = result[1]["active"]

    # common.system_log.info("INFO: Camera id:%i is_camera_on in ddbb %i",1,is_camera_on)
 
    # Security check and ERROR
    if is_camera_on == 1:
        common.system_log.info("INFO: Camera with id[%i] is active on application",id_camera)
    else:
        common.system_log.error("ERROR 002 - Camera with id: %i not connected", id_camera)

    # Create an unique file name, and timestamp for each image
    # TODO: revisar diferencia horario. probable locales del sistema / python
    timestamp = strftime("%Y%m%d%H%M%S", gmtime())
    file_name = "_" + str(id_book) + "_" + str(id_camera) + "_"+ page_num +"_" + timestamp
    raw_file_name = file_name + ".cr2"
    raw_full_filename = root_path + "/" + raw_file_name

    # Creates an unique id for this page
    # TODO: Añadir a book una propiedad para guardar numeros incrementales por libro
    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    cursor.execute("SELECT * FROM py_app_settings WHERE ID=1")
    result = cursor.fetchall()
    db.close()
    image_unique_id = int (result[0]["image_unique_id"])
    image_unique_id = image_unique_id +1
    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    query = "UPDATE py_app_settings SET image_unique_id="+str(image_unique_id)+" WHERE ID=1"
    cursor.execute(query)
    result = cursor.fetchall()
    db.close()

    # TODO: check what happens while setting camera parameters on camera[0]
    if id_camera != "":
        filtro=" where id_camera = "+str(id_camera) + " and type='TARGET'" 
    else:
        filtro=""
    try:
        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
        cursor = db.cursor(MySQLdb.cursors.DictCursor)
        query = "SELECT * FROM camerasettings"+filtro
        cursor.execute(query)
        result = cursor.fetchall()
        db.close()
        param1 = '--set-config-value /main/capturesettings/aperture='+str(result[0]["fnumber"])
        param2 = '--set-config-value /main/capturesettings/shutterspeed='+str(result[0]["shutterspeed"])
        param3 = '--set-config-value /main/imgsettings/iso='+str(result[0]["iso"])

        command = 'gphoto2 '+param1+' '+param2+' '+param3+' --port ' + common.camera_data[id_camera]['usb_port']
        response = common.system_call(command)
        common.system_log.info("INFO: Current parameters uploaded to camera")        
    except:
        common.system_log.error("ERROR 010: Error while setting current camera parameters")
        #return -1
    
    # Store images on SDCARD not RAM
    #command = "gphoto2 --set-config capturetarget=1"

    # Get image names on sdcard
    command = "gphoto2 --list-files --port=" + common.camera_data[id_camera]['usb_port']
    response = common.system_call(command)
    response_lines = response.split("\n")
    raw_sd_filename = response_lines[len(response_lines)-4]
    index = raw_sd_filename.find(" ", 7)
    raw_sd_filename = raw_sd_filename[7:index]
    raw_sd_filename = sd_root_path + raw_sd_filename
    jpg_sd_filename = response_lines[len(response_lines)-3]
    index = jpg_sd_filename.find(" ", 7)
    jpg_sd_filename = jpg_sd_filename[7:index]
    jpg_sd_filename = sd_root_path + jpg_sd_filename

    # Create an unique file name for each image
    # TODO: revisar diferencia horario. probable locales del sistema / python
    timestamp = strftime("%Y%m%d%H%M%S", gmtime())
    file_name = "_" + str(id_book) + "_" + str(id_camera) + "_"+ page_num +"_" + timestamp
    full_file_name = root_path + "/" + file_name
    raw_file_name = file_name + ".cr2"
    jpg_file_name = file_name + ".jpg"   
    raw_full_filename = root_path + "/" + raw_file_name
    jpg_full_filename = root_path + "/" + jpg_file_name
    
    # Get both CR2 and JPG files from SD card to HD
    command = "gphoto2 --get-file=" + raw_sd_filename+ " --filename=" + raw_full_filename + " --port=" + common.camera_data[id_camera]['usb_port']
    response = common.system_call(command)
    common.system_log.info("INFO: CR2 file copied to ..." + raw_full_filename)

    # Generates a JPG thumbail from CR2 file
    command = "exiftool -b -PreviewImage -w .jpg -ext cr2 -r " + raw_full_filename
    response = common.system_call(command)
    common.system_call("convert -resize 9% " + full_file_name + ".jpg" + " " + full_file_name + ".jpg")
 
    command = "exiftool -XMP-dc:All "+raw_full_filename
    response =  common.system_call(command) 
    common.system_log.error("INFO: Metadata added: \n" + response ) 

    # Rotate images
    angle_str = ""
    if ( id_camera == 0 ):
        angle_str = "90 "
    else:
        angle_str = "270 "

    command = "exiftool -Orientation='Rotate " + angle_str + "' "+raw_full_filename
    common.system_call(command)     
    command = "convert -rotate " + angle_str + full_file_name +".jpg" + " " + full_file_name + ".jpg"
    common.system_call(command)

    # Add image metadata
    command = "exiftool -XMP-dc:Identifier='"+str(image_unique_id)+"' "+raw_full_filename
    common.system_call(command)
    command = "exiftool -XMP-dc:Source='"+str(id_book)+"' "+raw_full_filename
    common.system_call(command)
    command = "exiftool -XMP-dc:Description='"+page_num+"' "+raw_full_filename
    common.system_call(command)
    
    # Get MD5sum for this file to store into DDBB
    try:
        response = common.system_call("md5sum " + raw_full_filename )
        index = response.find(" ",0)
        md5sum = response[0:index]
    except:
        common.system_log.error("ERROR 009: Error while creating MD5SUM for file..." + raw_full_filename)

    # Add current page to database
    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    query = "INSERT INTO image (id_cameraSetting,file_name,file_path,md5_checksum,time_stamp,id_page,id_book,id_camera,iso,shutterspeed,fnumber) VALUES ("+str(1)+",'"+file_name+ ".cr2"+"','"+root_path+"','"+md5sum+"','"+timestamp+"','"+page_num+"',"+str(id_book)+",'"+str(id_camera)+"','"+"100"+"','"+"1/125"+"','"+"0.8f"+"')"
    result = cursor.execute(query)
    db.commit()
    db.close()

    # Delete exiftool backup images
    common.system_call("rm " + raw_full_filename + "_original")
    common.system_call("rm " + jpg_full_filename + "_original")   

#-------------------------------------------------------------------------------
# main()
#-------------------------------------------------------------------------------
common.system_log.info("INFO: Initializing cameras")
value = common.init_camera_data()

# Unmount SDs and ...?
response = common.system_call("sudo killall gvfs-gphoto2-volume-monitor")
response = common.system_call("sudo killall PTPCamera")

# TODO: Detectar puerto arduino y comprobar si hay que cerrar el puerto
# TODO: Enviar trigger a una u otra cámara
serialFromArduino = serial.Serial(microcontroller_serial_port,9600)
serialFromArduino.flush()
serialFromArduino.write('a')
time.sleep(4)

if value != -1:
    common.system_log.info("INFO: Cameras initialized!")
    value = shootCamera(0)
    if value != -1:
        shootCamera(1)


# Print log
common.print_logs()
common.log_to_ddbb()

# TODO: Possible speed improvement...
# gphoto command --capture-tethered doesn't work...NO!!
# gphoto command --wait-event-and-download doesn't works...
# Using eonremoterelease option....
    
#common.system_call("gphoto2 --get-config eosremoterelease");
#common.system_call("gphoto2 --set-config eosremoterelease=2 --wait-event=1s")
#common.system_call("gphoto2 --set-config eosremoterelease=4")
#common.system_call("gphoto2 --set-config eosremoterelease=Immediate --wait-event=2s")
#gphoto2 --set-config eosremoterelease=4 --wait-event=4s --set-config eosremoterelease=2 --wait-event=4s --set-config eosremoterelease=3 --wait-event=4s --set-config eosremoterelease=1 --wait-event=4s
#command = "gphoto2 --capture-image --port=" + camera_data[id_camera]['usb_port'] + " --wait-event-and-download"
#print command
#print common.system_call( command )

# TODO:
# 3. Comprobar metadatos
# 4. Comprobar flujo de la app con una sóla cámara
# 5. Probar qué pasa con la desconexión de las cámaras
# 6. comprobar log de errores
# Cambiar código arduino para disparar una u otra cámara

    

