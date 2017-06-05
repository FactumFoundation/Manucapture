#!/usr/bin/env python
# -*- coding: utf-8 -*-

import MySQLdb
import sys
import os, time
from time import sleep
from sys import exit
import subprocess
from time import gmtime, strftime


default_serial = 'usb:000,000'
cameras = [{'serial':'000000000000','port':default_serial},{'serial':'000000000000','port':default_serial}]



def init_cameras():
    db = MySQLdb.connect(host="localhost", user="root", passwd="factum", db="BD2")
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    filtro =" where type = 'CURRENT'"
    cursor.execute("SELECT * FROM cameraSettings"+filtro)
    result = cursor.fetchall()
    db.close()

    if result[0]['id_camera'] == 0 :
        cameras[0]['serial'] = result[0]['serial']
        cameras[1]['serial'] = result[1]['serial']
    else:
        cameras[0]['serial'] = result[1]['serial']
        cameras[1]['serial'] = result[0]['serial']    

    usb_ports = get_ports()
    if len(usb_ports)==0:
        print "init_cameras : no camera detected"
    else:
        serial = get_camera_serial(usb_ports[0])
        if serial==cameras[0]['serial']:
            cameras[0]['port']=usb_ports[0]
        elif serial==cameras[1]['serial']:
            cameras[1]['port']=usb_ports[0]
        if len(usb_ports)==1:
            print "init_cameras : only one camera detected"
        else:
            serial = get_camera_serial(usb_ports[1])
            if serial==cameras[0]['serial']:
                cameras[0]['port']=usb_ports[1]
            elif serial==cameras[1]['serial']:
                cameras[1]['port']=usb_ports[1]

    print cameras


def system_call(command):
    p = subprocess.Popen([command], stdout=subprocess.PIPE, shell=True)
    return p.stdout.read()

def get_ports():
    ports = []
    command = 'gphoto2 --auto-detect'
    response = system_call(command)
    index = response.find("usb:")
    ports.append(response[index:index+11])
    index_2 = response.find("usb:", index+4)
    if index_2 != -1:
        ports.append(response[index_2:index_2+11])
    return ports
    
def get_camera_serial(port):
    command = 'gphoto2 --port '+port+' --get-config /main/status/eosserialnumber'
    response = system_call(command)
    index = response.find("Current: ")
    return response[index+9:index+9+12]



def set_target_parameters(id_camera):
    error_code = -1

    if id_camera != "":
        filtro=" where id_camera = "+str(id_camera) + " and type='TARGET'" 
    else:
        filtro=""
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="factum", db="BD2")
        cursor = db.cursor(MySQLdb.cursors.DictCursor)
        cursor.execute("SELECT * FROM cameraSettings"+filtro)
        result = cursor.fetchall()
        db.close()
        param1 = '--set-config-value /main/capturesettings/aperture='+str(result[0]["fnumber"])
        param2 = '--set-config-value /main/capturesettings/shutterspeed='+str(result[0]["shutterspeed"])
        param3 = '--set-config-value /main/imgsettings/iso='+str(result[0]["iso"])
        command = 'gphoto2 '+param1+' '+param2+' '+param3+' --port '+ cameras[id_camera]['port']
        response = system_call(command)
        print response
    except:
        print "Error occured"
        sys.exit(1)

    return error_code

    
def shootCameras(id_camera):
    
    #get info from database
    db = MySQLdb.connect(host="localhost", user="root", passwd="factum", db="BD2")
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    cursor.execute("SELECT * FROM book WHERE status='ACTIVE'")
    result = cursor.fetchall()
    db.close()
    rootpath = result[0]["folder_path"]
    page_num = result[0]["page_num"] + "_" + str(id_camera)
    id_book = result[0]["id"]
    
    # take picture
    filename = str(id_camera) + "_" + strftime("%Y-%m-%d_%H-%M-%S", gmtime()) + ".cr2"
    full_filename = rootpath + "/" + filename 

    #Shoot from USB
    #command = 'gphoto2 --capture-image-and-download --filename ' + filename + ' --port ' + cameras[id_camera]['port']
    #system_call(command)
    
    #Shoot from microcontroler
    command = "gphoto2 -f='/store_00020001/DCIM/100EOS5D' -n --port="+cameras[id_camera]['port']
    response = system_call(command)
    command = "gphoto2 --get-file=" + str(int(response[59:])) + " --filename=" + full_filename + " --port=" + cameras[id_camera]['port']
    response = system_call(command)
    print response
    
    #md5 and timestamp calculation
    md5sum = system_call("md5sum " + full_filename)[0:32]

    #timestamp
    timestamp = time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(os.path.getctime(full_filename)))    

    #add page to database
    db = MySQLdb.connect(host="localhost", user="root", passwd="factum", db="BD2")
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    query = "INSERT INTO images (id_cameraSetting,file_name,file_path,md5_checksum,time_stamp,id_page,id_book) VALUES ("+str(1)+",'"+filename+"','"+rootpath+"','"+md5sum+"','"+timestamp+"','"+page_num+"',"+str(id_book)+")"
    print query
    #result = cursor.execute(query,(str(1),filename,rootpath,md5sum,timestamp,page_num,str(id_book)))
    result = cursor.execute(query)
    db.commit()
    db.close()
    print result


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


def update_cameras_state(id_camera):
    error_code = -1 # no error
    properties = get_camera_properties(cameras[id_camera]['port'])
    db = MySQLdb.connect(host="localhost", user="root", passwd="factum", db="BD2")
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    filtro = "WHERE serial='"+ cameras[id_camera]['serial'] + "' and type='CURRENT'" 
    cursor.execute("""
       UPDATE cameraSettings
       SET iso=%s, shutterspeed=%s, fnumber=%s, serial=%s
       """ + filtro, (properties["iso"], properties["shutterspeed"], properties["fnumber"], cameras[id_camera]['serial']))

    db.commit()
    db.close()
    return error_code

    
#initialization - common to all scripts
init_cameras()


# update settings in db from cameras
if cameras[0]['port']!=default_serial: # check port initialization
    print "update camera 0"
    update_cameras_state(0)
if cameras[1]['port']!=default_serial: # check port initialization
    print "update camera 1"
    update_cameras_state(1)


"""
# config settings in cameras from db targets
if cameras[0]['port']!=default_serial: # check port initialization
    print "configure camera 0"
    set_target_parameters(0)
if cameras[1]['port']!=default_serial: # check port initialization
    print "configure camera 1"
    set_target_parameters(1)
"""

"""
#shoot cameras
if cameras[0]['port']!=default_serial: # check port initialization
    print "configure camera 0"
    shootCameras(0)
if cameras[1]['port']!=default_serial: # check port initialization
    print "configure camera 1"
    shootCameras(1)
"""








