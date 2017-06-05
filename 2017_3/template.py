#!/usr/bin/env python
# -*- coding: utf-8 -*-

import common
import MySQLdb

# Database connection data
host_address = "localhost"
user_name = "root"
user_pass = "factum"
database_name = "BD2"

# Camera data
camera_data = [{'serial_number':'000000000000','usb_port':'usb:XXX,XXX'},{'serial_number':'000000000000','usb_port':'usb:XXX,XXX'}]

#-------------------------------------------------------------------------------
# Init cameras
#-------------------------------------------------------------------------------
def init_camera_data():

    # Check how many cameras are connected (must be two)
    # and USB port for each one
    command = 'gphoto2 --auto-detect'
    result = common.system_call(command)
    num_of_cameras = len(result.split('\n')) - 3

    # Security checking, return an error if there are less than two cameras
    if num_of_cameras == 0:
        print "ERROR 001 - No se detecta ninguna cámara"
        
    elif num_of_cameras == 1:
        print "ERROR 002 - Sólo se detecta una cámara"

    # Now we need to identify A and B cameras by serial numbers and USB ports
    # Then, we store these data into camera_data
    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    filtro =" where type = 'CURRENT'"
    sentencia="SELECT * FROM camerasettings"+filtro
    cursor.execute(sentencia)
    result = cursor.fetchall()
    db.close()
      
    # Store serial numbers into camera_data
    if result[0]['id_camera'] == 0 :
        camera_data[0]['serial_number'] = result[0]['serial_number']
        camera_data[1]['serial_number'] = result[1]['serial_number']
    else:
        camera_data[0]['serial_number'] = result[1]['serial_number']
        camera_data[1]['serial_number'] = result[0]['serial_number']    

    # Store USB ports into camera_data
    usb_ports = []
    usb_ports = common.get_camera_USB_ports()
    
    serial_number = common.get_camera_serial_number(usb_ports[0])
    if serial_number == camera_data[0]['serial_number']:
        camera_data[0]['usb_port']=usb_ports[0]
    elif serial_number == camera_data[1]['serial_number']:
        camera_data[1]['usb_port']=usb_ports[0]

    serial_number = common.get_camera_serial_number(usb_ports[1])
    if serial_number==camera_data[0]['serial_number']:
        camera_data[0]['usb_port']=usb_ports[1]
    elif serial_number==camera_data[1]['serial_number']:
        camera_data[1]['usb_port']=usb_ports[1]

    # TODO: Check security / issues
    # print camera_data
#-------------------------------------------------------------------------------
# main()
#-------------------------------------------------------------------------------
init_camera_data()
#update_camera_state_into_DB( 0 )
#update_camera_state_into_DB( 1 )

fileName = "/home/factum/Documentos/tests/test_2.cr2"
command = 'gphoto2 --capture-image-and-download --filename ' + fileName + ' --port ' + camera_data[0]['usb_port']
print common.system_call(command)



