#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import subprocess
import MySQLdb
import hashlib
import logging
from StringIO import StringIO
import os

# Camera data (SERIAL_NUM<->USB_PORT)
default_usb_port = 'usb:XXX,XXX'
camera_data = [{'serial_number': '000000000000', 'usb_port': default_usb_port},
               {'serial_number': '000000000000', 'usb_port': default_usb_port}]

# -------------------------------------------------------------------------------
# Logger
# -------------------------------------------------------------------------------
system_log_stream = StringIO()
system_log_handler = logging.StreamHandler(system_log_stream)
formatter = logging.Formatter('%(asctime)s | %(message)s',
                              '%d-%b-%Y | %H:%M:%S')
system_log_handler.setFormatter(formatter)
system_log = logging.getLogger('mylogger')
system_log.addHandler(system_log_handler)

# Log level
system_log.setLevel(logging.INFO)
for system_log_handler in system_log.handlers:
    system_log.removeHandler(system_log_handler)
system_log.addHandler(system_log_handler)
system_log.info("INFO: Session starts")


def print_logs():
    system_log_handler.flush()
    print system_log_stream.getvalue()


def log_to_ddbb():
    system_log_handler.flush()
    log_result = system_log_stream.getvalue()

    # Insert log entry into DDBB
    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)

    query = "INSERT INTO py_system_log (content) VALUES (" + '"' + log_result + '"' + ")"
    # print "DB query: " + query
    result = cursor.execute(query)
    db.commit()
    db.close()


# -------------------------------------------------------------------------------
# Utils
# -------------------------------------------------------------------------------
def system_call(command):
    p = subprocess.Popen([command], stdout=subprocess.PIPE, shell=True)
    return p.stdout.read()


# Returns MD5
def create_md5_checksum(file_path):
    with open(file_path, "rb") as fh:
        m = hashlib.md5()
        while True:
            data = fh.read(4096)
            if not data:
                break
            m.update(data)
            return m.hexdigest()


def create_md5_checksum_base64(file_path):
    hash = hashlib.md5()
    with open(file_path) as f:
        for chunk in iter(lambda: f.read(4096), ""):
            hash.update(chunk)
    return hash.hexdigest().encode('base64').strip()


# -------------------------------------------------------------------------------
# Get camera USB ports
# -------------------------------------------------------------------------------
def get_camera_USB_ports():
    # Get connected cameras and USB port for each one
    ports = []
    command = 'gphoto2 --auto-detect'
    response = system_call(command)
    index = response.find("usb:")

    # usb_length (11 in this case) is the number of chars that define an USB port
    usb_port_char_length = 11
    port_a = response[index:index + usb_port_char_length]  # first port
    index = response.find("usb:", index + 1)
    port_b = response[index:index + usb_port_char_length]  # second port

    ports.append(port_a)
    ports.append(port_b)

    return ports


# -------------------------------------------------------------------------------
# Get camera serial number
# -------------------------------------------------------------------------------
def get_camera_serial_number(port):
    command = 'gphoto2 --port ' + str(port) + ' --get-config /main/status/eosserialnumber'
    # print "comand: "+ command
    response = system_call(command)
    # print "get_camera_serial_number() :  " + response

    # Response example:
    #
    # Label: Serial Number
    # Type: TEXT
    # Current: 093022000603
    index = response.find("Current: ") + 9
    serial_number_char_length = 12
    serial_number = response[index:index + serial_number_char_length]

    # TODO: add security checking
    if index == -1:
        print "ERROR 003 - Error al pedir el serial number de la cámara"
    return serial_number


# Database connection data
host_address = "localhost"
user_name = "root"
user_pass = "factum"
database_name = "BD2"

# Camera data (SERIAL<->USB_PORT)
default_usb_port = 'usb:XXX,XXX'
camera_data = [{'serial_number': '000000000000', 'usb_port': default_usb_port},
               {'serial_number': '000000000000', 'usb_port': default_usb_port}]


def print_sep():
    system_log.info("\n----------------------------------------------------------\n")


# Init cameras
def init_camera_data():
    # How many cameras should be connected?
    try:
        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    except:
        common.system_log.error("ERROR 004 - Database connection fails. HOST:%s USER:%s PASS:%s :DB NAME%s",
                                host_address, user_name, user_pass, database_name)
        return

    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    cursor.execute("SELECT * FROM camerasettings WHERE type='TARGET'")
    result = cursor.fetchall()
    db.close()

    # Check how many cameras are connected
    # and USB port for each one
    command = 'gphoto2 --auto-detect'
    result = system_call(command)

    print_sep()
    system_log.info("INFO:  Gphoto2 --auto-detect:\n\n" + result)
    num_of_cameras = len(result.split('\n')) - 3

    system_log.info("INFO: Number of cameras")

    # TODO: ver cuantas camaras en bbdd y sacar error si no hay las
    # mismas conectadas:
    # print "num_of_cameras: "+str(num_of_cameras)

    # Security checking, return an error if there are less than two cameras
    if num_of_cameras == 0:
        system_log.error("ERROR xxx - No se detecta ninguna cámara")
        return -1

    elif num_of_cameras == 1:
        system_log.error("ERROR xxx - Sólo se detecta una cámara")
        return -1

    # Now we need to identify A and B cameras by serial numbers and USB ports
    # Then, we store these data into camera_data
    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)

    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    filtro = " where type = 'CURRENT'"
    sentencia = "SELECT * FROM camerasettings" + filtro
    cursor.execute(sentencia)
    result = cursor.fetchall()
    db.close()

    # TODO: Add security check

    # Store serial numbers into camera_data
    if result[0]['id_camera'] == 0:
        camera_data[0]['serial_number'] = result[0]['serial_number']
        camera_data[1]['serial_number'] = result[1]['serial_number']
    else:
        camera_data[0]['serial_number'] = result[1]['serial_number']
        camera_data[1]['serial_number'] = result[0]['serial_number']

        # Store USB ports into camera_data
    usb_ports = []
    usb_ports = get_camera_USB_ports()
    serial_number = get_camera_serial_number(usb_ports[0])
    if serial_number == camera_data[0]['serial_number']:
        camera_data[0]['usb_port'] = usb_ports[0]
    elif serial_number == camera_data[1]['serial_number']:
        camera_data[1]['usb_port'] = usb_ports[0]

    serial_number = get_camera_serial_number(usb_ports[1])
    if serial_number == camera_data[0]['serial_number']:
        camera_data[0]['usb_port'] = usb_ports[1]
    elif serial_number == camera_data[1]['serial_number']:
        camera_data[1]['usb_port'] = usb_ports[1]

    system_log.info("INFO: Camera matching...")
    system_log.info("INFO: " + str(camera_data[0]))
    system_log.info("INFO: " + str(camera_data[1]))


# -------------------------------------------------------------------------------
# Get camera properties from gphoto
# -------------------------------------------------------------------------------
def get_camera_properties(usb_port):
    properties = {}

    # iso
    command = 'gphoto2 --port ' + usb_port + ' --get-config /main/imgsettings/iso'
    response = system_call(command)
    index = response.find("Current: ")
    index_end = response.find("Choice: ")
    properties["iso"] = response[index + 9:index_end - 1]

    # shutterspeed
    command = 'gphoto2 --port ' + usb_port + ' --get-config /main/capturesettings/shutterspeed'
    response = system_call(command)
    index = response.find("Current: ")
    index_end = response.find("Choice: ")
    properties["shutterspeed"] = response[index + 9:index_end - 1]

    # fnumber
    command = 'gphoto2 --port ' + usb_port + ' --get-config /main/capturesettings/aperture'
    response = system_call(command)
    index = response.find("Current: ")
    index_end = response.find("Choice: ")
    properties["fnumber"] = response[index + 9:index_end - 1]

    system_log.info("INFO: Update camera settings from camera to DDBB")
    system_log.info("INFO: Properties set to..." + "ISO:" + properties["iso"] + " SHUTTER SPEED: " + properties[
        "shutterspeed"] + " FNUMBER: " + properties["fnumber"])
    return properties


# Update CURRENT field on DDBB with current properties for each camera
def update_camera_state_into_DB(id_camera):
    error_code = -1  # no error

    # system_info.info( "update_camera_state_into_db() - starts ")
    # print camera_data
    # print ""
    # print ""
    properties = get_camera_properties(camera_data[id_camera]['usb_port'])

    db = MySQLdb.connect(host="localhost", user="root", passwd="factum", db="BD2")
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    filtro = "WHERE serial_number='" + camera_data[id_camera]['serial_number'] + "' and type='TARGET'"
    cursor.execute("""
       UPDATE camerasettings
       SET iso=%s, shutterspeed=%s, fnumber=%s, serial_number=%s, active='1'
       """ + filtro, (
    properties["iso"], properties["shutterspeed"], properties["fnumber"], camera_data[id_camera]['serial_number']))

    # TODO: Añadir 1 a campo active si la cámara está conectada
    result = db.commit()
    # common.system_log.info("INFO: DDBB result..." + result)

    db.close()

    # system_info.info( "update_camera_state_into_db() - starts ")
    return error_code


# Test system
def system_test():
    print "----------------------------------------"
    print "Detecting connected cameras..."

    response = system_call("gphoto2 --auto-detect")

    nlines = response.count('\n')
    nlines = nlines - 2

    if nlines == 2:
        print "OK: Both cameras are connected"
    else:
        print "ERROR: There is (at least) one camera that can't be reached"

    # Detectar numero de serie

    print "----------------------------------------"
    print "Detecting DDBB connection..."

    # Database connection data
    host_address = "localhost"
    user_name = "root"
    user_pass = "factum"
    database_name = "BD2"

    try:
        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
        print "OK: DDBB connection established!"
        db.close()
    except:
        print "Database connection fails"


# -------------------------------------------------------------------------------
# list_sd_files()
# -----------------------------------------------------------------------------
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
