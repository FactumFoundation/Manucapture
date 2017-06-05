#!/usr/bin/env python
# -*- coding: utf-8 -*-

import MySQLdb
import common

# Database connection data
host_address = "localhost"
user_name = "root"
user_pass = "factum"
database_name = "BD2"

# Camera data (SERIAL<->USB_PORT)
default_usb_port = 'usb:XXX,XXX'
camera_data = [{'serial_number':'000000000000','usb_port':default_usb_port},{'serial_number':'000000000000','usb_port':default_usb_port}]

# Init cameras
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

# Get camera properties from gphoto
def get_camera_properties( usb_port ):
    
    properties = {}
    
    #iso
    command = 'gphoto2 --port '+ usb_port +' --get-config /main/imgsettings/iso'
    response = common.system_call(command)
    index = response.find("Current: ")
    index_end = response.find("Choice: ")
    properties["iso"] = response[index+9:index_end-1]
    
    #shutterspeed
    command = 'gphoto2 --port '+ usb_port +' --get-config /main/capturesettings/shutterspeed'
    response = common.system_call(command)
    index = response.find("Current: ")
    index_end = response.find("Choice: ")
    properties["shutterspeed"] = response[index+9:index_end-1]
    
    #fnumber
    command = 'gphoto2 --port '+ usb_port +' --get-config /main/capturesettings/aperture'
    response = common.system_call(command)
    index = response.find("Current: ")
    index_end = response.find("Choice: ")
    properties["fnumber"] = response[index+9:index_end-1]
    
    return properties

# Update CURRENT field on DDBB with current properties for each camera
def update_camera_state_into_DB(id_camera):
    error_code = -1 # no error
    properties = get_camera_properties(camera_data[id_camera]['usb_port'])
    print properties
    
    db = MySQLdb.connect(host="localhost", user="root", passwd="factum", db="BD2")
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    filtro = "WHERE serial_number='"+ camera_data[id_camera]['serial_number'] + "' and type='CURRENT'" 
    cursor.execute("""
       UPDATE camerasettings
       SET iso=%s, shutterspeed=%s, fnumber=%s, serial_number=%s
       """ + filtro, (properties["iso"], properties["shutterspeed"], properties["fnumber"], camera_data[id_camera]['serial_number']))

    db.commit()
    db.close()
    return error_code

#-------------------------------------------------------------------------------
# main()
#-------------------------------------------------------------------------------
init_camera_data()
update_camera_state_into_DB( 0 )
update_camera_state_into_DB( 1 )


