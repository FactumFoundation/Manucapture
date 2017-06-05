#!/usr/bin/env python
# -*- coding: utf-8 -*-

import MySQLdb
import sys
import subprocess
import os
import inspect

workspace_folder = "/home/factum/"

#-------------------------------------------------------------------------------
# Global vars
#-------------------------------------------------------------------------------

# Cameras data
cameras_info = [{'serial_number':'000000000000','port':'usb:000,000'},{'serial_number':'000000000000','port':'usb:000,000'}]

# CANON specific
# TODO: check with other camera
serial_number_length = 12

# Database connection data
host_address = "localhost"
user_name = "root"
user_pass = "factum"
database_name = "BD2"

#-------------------------------------------------------------------------------
# Utils
#-------------------------------------------------------------------------------

def system_call(command):
    p = subprocess.Popen([command], stdout=subprocess.PIPE, shell=True)
    return p.stdout.read()

#-------------------------------------------------------------------------------
# Get camera (USB) ports
#-------------------------------------------------------------------------------
def get_camera_USB_ports():

    # Get connected cameras and USB port for each one
    ports = []
    command = 'gphoto2 --auto-detect'
    response = system_call(command)
    index = response.find("usb:")

    # usb_length (11 in this case) is the number of chars that define an USB port
    usb_length = 11
    port_a = response[index:index+usb_length]   #first port
    index = response.find("usb:",index+1)
    port_b = response[index:index+usb_length]   #second port

    ports.append(port_a)
    ports.append(port_b)
    
    return ports
#-------------------------------------------------------------------------------
# Get camera serial number
#-------------------------------------------------------------------------------
def get_camera_serial_number( port ):
    
    command = 'gphoto2 --port '+str(port)+' --get-config /main/status/eosserialnumber'
    response = system_call(command)
    
    # Response example:
    #
    # Label: Serial Number
    # Type: TEXT
    # Current: 093022000603
    index = response.find("Current: ") + 9
    serial_number = response[index:index+serial_number_length]

    # TODO: add security checking
    return serial_number

#-------------------------------------------------------------------------------
# Get camera settings from database
#-------------------------------------------------------------------------------
def get_camera_settings_from_database( id_camera ):

    camera_settings = {}

    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    filtro =" where type = 'CURRENT'"
    sentencia="SELECT * FROM camerasettings"+filtro
    cursor.execute(sentencia)
    result = cursor.fetchall()
    db.close()

    # TODO: add security checking
    print result
    
#-------------------------------------------------------------------------------
# Get camera parameters from camera
#-------------------------------------------------------------------------------
def get_camera_settings_from_camera (id_camera ):
    camera_settings = {}
    command = 'gphoto2 --port' 
    response = system_call(command)

    # TODO: add security checking
    return camera_settings

#-------------------------------------------------------------------------------
# Shoot from USB
#-------------------------------------------------------------------------------
def shootCamera( id_camera ):

    fileName = "/home/factum/Documentos/tests/test_2.cr2"
    command = 'gphoto2--capture-image-and-download --filename ' + fileName + ' --port ' + cameras_info[id_camera]['port']
    system_call(command)

    # TODO: add security checking
    

#-------------------------------------------------------------------------------
# main()
#-------------------------------------------------------------------------------

ports = get_camera_USB_ports()
print ports
# Get cameras info (USB port and serial number)
# TODO: check with database
cameras_info[0]['port'] = ports[0]
cameras_info[1]['port'] = ports[1]

if cameras_info[0]['port'] != "":
    cameras_info[0]['serial_number'] = get_camera_serial_number(cameras_info[0]['port'])
if cameras_info[1]['port'] != "":
    cameras_info[1]['serial_number'] = get_camera_serial_number(cameras_info[1]['port'])

#print get_camera_settings_from_camera(0)

# Get script location
#script_path = os.path.dirname(os.path.abspath(__file__))

#image_name = "test_" + 
#image_full_name = "/home/factum/Documentos" + 
#log_full_name = 
#command = "gphoto2 --capture-image-and-download --debug --force-overwrite --debug-logfile=" + script_path + "/xx.log"


#print "camera 0 - take picture! | PORT:" + cameras_info[0]['port']
#file_name_1 = workspace_folder + "img_1.cr2"
#command = 'gphoto2 --capture-image-and-download --filename ' + file_name_1 + ' --port ' + cameras_info[0]['port']
#system_call(command)

#print "camera 1 - take picture! | PORT:" + cameras_info[1]['port']
#file_name_2 = workspace_folder + "img_2.cr2"
#command = 'gphoto2 --capture-image-and-download --filename ' + file_name_2 + ' --port ' + cameras_info[1]['port']
#system_call(command)
#print file_name_2

# Common paths:
#
# /home/factum/Documentos/prototipo-digitalizacion
# /home/factum/Documentos




def get_camera_properties(port):
    properties = {}
    #iso
    command = 'gphoto2 --port '+port+' --get-config /main/imgsettings/iso'
    response = system_call(command)
    index = response.find("Current: ")
    index_end = response.find("Choice: ")
    properties["iso"] = response[index+9:index_end-1]
    #shutterspeed
    command = 'gphoto2 --port '+port+' --get-config /main/capturesettings/shutterspeed'
    response = system_call(command)
    index = response.find("Current: ")
    index_end = response.find("Choice: ")
    properties["shutterspeed"] = response[index+9:index_end-1]
    #fnumber
    command = 'gphoto2 --port '+port+' --get-config /main/capturesettings/aperture'
    response = system_call(command)
    index = response.find("Current: ")
    index_end = response.find("Choice: ")
    properties["fnumber"] = response[index+9:index_end-1]
    return properties

#print get_camera_properties(cameras_info[1]['port'])
shootCamera(1)

