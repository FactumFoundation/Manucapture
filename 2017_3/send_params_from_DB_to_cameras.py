#!/usr/bin/env python
# -*- coding: utf-8 -*-

import MySQLdb
import common
import serial
import gphoto2 as gp
from time import gmtime, strftime
import time

# Database connection data
host_address = "localhost"
user_name = "root"
user_pass = "factum"
database_name = "BD2"

debug = True
#-------------------------------------------------------------------------------
# set_camera_parameters()
#-----------------------------------------------------------------------------
def set_camera_parameters(camera_id):

    if debug:
        print ("------------------------------------------------")
        print ("Start to settings camera parameters for camera %s:", camera_id)

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

    if debug:
        print "Get camera data from the ddbb: DONE!"
        
    # Check if we need to capture with this camera
    if ( ddbb_active == 0 ):
        common.system_log.info("INFO: Aborting capture...")
        return -1
    
    if debug:
        print "Check if we need to capture with this camera: DONE!"
        
    # Make a list of all available cameras
    context = gp.Context()   
    camera_list = []
    for name, addr in context.camera_autodetect():
        camera_list.append((name, addr))

    if debug:
        print "Make a list of all available cameras: DONE!"
        
    # Check if there are, at least, one camera detected...
    if not camera_list:
        common.system_log.error("ERROR 005: No cameras connected")
        return -1
    
    camera_list.sort(key=lambda x: x[0])
    common.system_log.info("INFO: Connected cameras list...")

    if debug:
        print "Check if there are, at least, one camera detected: DONE!"
        
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

    if debug:
        print "Look for camera USB port (using its serial_number): DONE!"
        
    # Security check 
    if current_camera ==-1:
        common.system_log.error("ERROR 006: Serial number doesn't appears")        
        return -1

    common.system_log.info("INFO: Camera port detected...[%s]",camera_list[cam][1])
    
    # Update camera parameters from webapp
    if camera_id != "":
        filtro=" where id_camera = '"+str(camera_id) + "' and type='CURRENT'" 
    else:
        filtro=""
    try:
        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
        cursor = db.cursor(MySQLdb.cursors.DictCursor)
        query = "SELECT * FROM camerasettings"+filtro
        cursor.execute(query)
        result = cursor.fetchall()
        db.close()

        if debug:
            print result

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
    
    if debug:
        print "Update camera parameters from webapp: DONE!"
        

set_camera_parameters(0)
set_camera_parameters(1)
