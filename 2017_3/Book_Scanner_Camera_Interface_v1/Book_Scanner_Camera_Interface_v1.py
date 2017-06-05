#import common
import logging
import time
import sys
import os
import subprocess
from datetime import datetime
from time import gmtime, strftime
import threading
import inspect
import gphoto2 as gp
import traceback
import MySQLdb
import random
from StringIO import StringIO
import xml.etree.cElementTree as ET
import string


# Database connection data
host_address = "localhost"
user_name = "root"
user_pass = "factum"
database_name = "BD2"

port_1 = "usb:,"
port_2 = "usb:,"

downloadLeft = False
downloadRight = False
stopSession = False

function = "0"
last_function = "0"

function_finished_left = 0
function_finished_right = 0

camera_0_thread_active = False
camera_1_thread_active = False

last_time_function = -1000
max_time_function = 15000


# use Python logging
logging.basicConfig(
    format='%(levelname)s: %(name)s: %(message)s', level=logging.WARNING)

def system_call(command):
    p = subprocess.Popen([command], stdout=subprocess.PIPE, shell=True)
    return p.stdout.read()


class Thread(threading.Thread):
    def __init__(self, t, *args):
        threading.Thread.__init__(self, target=t, args=args)
        self.start()


# Threaded function: Tethered communication + postprocess
def listen_camera(cameraId, lock, port):
    global downloadLeft
    global downloadRight
    global stopSession
    global last_function
    global function_finished_left
    global function_finished_right
    global camera_0_thread_active
    global camera_1_thread_active

    fileName = "default.cr2"

    if cameraId == 0:
        camera_0_thread_active = True
        fileName = "izq.cr2"
    elif cameraId == 1:
        camera_1_thread_active = True
        fileName = "der.cr2"

    camera_detected = True

    if port != "usb:,":
        command = 'gphoto2 --capture-tethered --port ' + port + ' --force-overwrite --filename ' + fileName
        proc = subprocess.Popen(command,
                                shell=True,
                                stdin=subprocess.PIPE,
                                stdout=subprocess.PIPE,
                                )

        connected = False



        try:
            while True:

                # Exit the loop when flag stopSession is active
                if stopSession:
                    lock.acquire()
                    system_log.info("End of processs  ")
                    lock.release()
                    kill_gphoto_processes()
                    break

                # Read tether connection output and detect when the image is downloaded
                next_line = proc.stdout.readline()
                if not next_line:

                    # Download process finished, reconnect tethering
                    if not stopSession and camera_detected:
                        hasResults = False
                        if last_function == 1:
                            capture(port, cameraId)
                            hasResults = True
                        elif last_function == 2:
                            update_camera_state_into_DB(port, cameraId)
                            hasResults = True
                        elif last_function == 3:
                            set_camera_parameters(port, cameraId)
                            hasResults = True
                        elif last_function == 4:
                            system_test(cameraId)
                            hasResults = True
                        elif last_function == 5:
                            capture_test(port, cameraId)
                            time.sleep(2)
                            hasResults = True


                        if hasResults == True:
                            hasResults = False
                            lock.acquire()
                            if( cameraId == 0):
                                function_finished_left = 1
                            else :
                                function_finished_right = 1
                            lock.release()


                        if connected :
                            command = 'gphoto2 --capture-tethered --port ' + port + ' --force-overwrite --filename ' + fileName
                            print command
                            proc = subprocess.Popen(command,
                                                    shell=True,
                                                    stdin=subprocess.PIPE,
                                                    stdout=subprocess.PIPE
                                                    )
                            connected = False
                        else:
                            if cameraId == 0:
                                camera_0_thread_active = False
                            elif cameraId == 1:
                                camera_1_thread_active = False
                            print ("Exit capture thread for camera " + str(cameraId))
                            proc.kill()
                            break
                    else:
                        if cameraId == 0:
                            camera_0_thread_active = False
                        elif cameraId == 1:
                            camera_1_thread_active = False
                        proc.kill()
                        break

                else:
                    if(next_line.rfind("LANG=C",0,len(next_line))!=-1):
                        if cameraId == 0:
                            camera_0_thread_active = False
                        elif cameraId == 1:
                            camera_1_thread_active = False
                        print ("Exit capture thread for camwera " + str(cameraId))
                        proc.kill()
                    else:
                        connected = True
                    # A new file is downloaded: postrpocess starts
                    if "der.cr2" in next_line.rstrip() or "izq.cr2" in next_line.rstrip():

                        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
                        cursor = db.cursor(MySQLdb.cursors.DictCursor)
                        cursor.execute("UPDATE communication SET function='6', response='1' WHERE communication_id=1")
                        result = cursor.fetchall()
                        db.close()

                        pending_image = True
                        proc.kill()
                        process_tethered(cameraId)

                        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
                        cursor = db.cursor(MySQLdb.cursors.DictCursor)
                        cursor.execute("UPDATE communication SET function='6', response='2' WHERE communication_id=1")
                        result = cursor.fetchall()
                        db.close()

                        # Send log to DDBB
                        lock.acquire()
                        if cameraId==0:
                            function_finished_left = 1
                        elif cameraId==1:
                            function_finished_right = 1
                        lock.release()

        except:
            # problem communicating with cameras, kill any pending gphoto process
            print traceback.format_exc()
            lock.acquire()
            system_log.info("end of listening camera thread- killing gphoto subprocess ")
            lock.release()
            kill_gphoto_processes()

# Test system
def system_test(cameraId):
    newPort = get_camera_port(cameraId)
    if newPort != "usb:,":
        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass,
                             db=database_name)
        cursor = db.cursor(MySQLdb.cursors.DictCursor)
        cursor.execute("UPDATE camerasettings SET active='1' WHERE id_camera='" + str(cameraId) + "'")
        result = cursor.fetchall()
        db.close()
    else :
        lock.acquire()
        system_log.info("Could not get port for camera " + str(cameraId))
        lock.release()


# -------------------------------------------------------------------------------
# capture()
# -----------------------------------------------------------------------------
def capture(port, camera_id):
    debug = False

    if port == "usb:,":
        lock.acquire()
        system_log.error("ERROR 006: Serial number doesn't appears")
        lock.release()
        return -1

    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    cursor.execute("SELECT * FROM camerasettings WHERE type='CURRENT'")
    result = cursor.fetchall()
    db.close()

    ddbb_active = result[camera_id]["active"]

    if (not ddbb_active):
        return -1

    # Get current book data
    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    cursor.execute("SELECT * FROM book WHERE status='ACTIVE'")
    result = cursor.fetchall()
    db.close()
    root_path = result[0]["folder_path"]
    if camera_id == 0:
        page_num = result[0]["page_num_a"]  # + "_" + str(id_camera)
    else:
        page_num = result[0]["page_num_b"]  # + "_" + str(id_camera)
    id_book = result[0]["id_book"]
    image_unique_id = int(result[0]["image_unique_id_counter"])

    if debug:
        lock.acquire()
        system_log.info("INFO: Get current book data: DONE!")
        lock.release()

    timeCode = strftime("%Y%m%d%H%M%S", gmtime())

    # Create an unique file name, and timeCode for each image
    file_name = "_" + str(id_book) + "_" + page_num + "_"+ str(camera_id) +"_" + timeCode
    raw_hd_filename = root_path + "/" + file_name + ".cr2"
    full_file_name = root_path + "/" + file_name

    if debug:
        lock.acquire()
        system_log.info("INFO: Create an unique file name, and timestamp for each image: DONE!")
        lock.release()


    set_camera_parameters(port, camera_id)

    # Download last image and delete it from SD card
    lock.acquire()
    system_log.info("INFO: Capture and Download new image in : " + raw_hd_filename)
    lock.release()

    proc = subprocess.Popen(
        "gphoto2 --port " + port + " --capture-image-and-download --force-overwrite --filename " + raw_hd_filename,
        shell=True,
        stdout=subprocess.PIPE
        )
    while True:
        next_line = proc.stdout.readline()
        if not next_line:
            break

    if debug:
        lock.acquire()
        system_log.info("INFO: Capture and Download: DONE!")
        lock.release()

    post_process(file_name, full_file_name, raw_hd_filename, root_path, camera_id, page_num, id_book, image_unique_id)



# -------------------------------------------------------------------------------
# capture()
# -----------------------------------------------------------------------------
def capture_test(port, camera_id):
    debug = False

    if debug:
        lock.acquire()
        system_log.info("INFO: Look for camera USB port (using its serial_number): DONE!")
        lock.release()

    if port == "usb:,":
        system_log.error("ERROR 006: Serial number doesn't appears")
        return -1

    lock.acquire()
    system_log.info("INFO: Camera port detected...[%s]", port)
    lock.release()

    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    cursor.execute("SELECT * FROM camerasettings WHERE type='CURRENT'")
    result = cursor.fetchall()
    db.close()

    ddbb_active = result[camera_id]["active"]

    if (not ddbb_active):
        return -1

    # Get current book data
    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    cursor.execute("SELECT * FROM tab_bd1t08_variables")
    result = cursor.fetchall()
    db.close()
    root_path = result[0]["ruta_test"]
    if camera_id == 0:
        file_name = result[0]["img_test_a"]
    else:
        file_name = result[0]["img_test_b"]

    raw_hd_filename = root_path + file_name + ".cr2"
    full_file_name = root_path + file_name

    if debug:
        lock.acquire()
        system_log.info("Create test url")
        lock.release()

    set_camera_parameters(port, camera_id)


    # Download last image and delete it from SD card
    lock.acquire()
    system_log.info("Capture and Download new image in : " + raw_hd_filename)
    print("capture test " + raw_hd_filename)
    lock.release()

    proc = subprocess.Popen(
        "gphoto2 --port " + port + " --capture-image-and-download --force-overwrite --filename " + raw_hd_filename,
        shell=True,
        stdout=subprocess.PIPE
        )
    while True:
        next_line = proc.stdout.readline()
        if not next_line:
            break

    if debug:
        lock.acquire()
        system_log.info("Capture and Download: DONE!")
        lock.release()

    system_call("rm " + full_file_name + ".jpg")
    command = "exiftool -b -PreviewImage -w .jpg " + raw_hd_filename
    system_call(command)
    system_call("convert -resize 9% " + full_file_name + ".jpg" + " " + full_file_name + ".jpg")

def process_tethered(camera_id):
    tethered_temp_download_path = os.path.dirname(os.path.realpath(__file__)) + "/"

    debug = False

    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    cursor.execute("SELECT * FROM camerasettings WHERE type='CURRENT'")
    result = cursor.fetchall()
    db.close()

    ddbb_active = result[camera_id]["active"]

    if (ddbb_active):
        lock.acquire()
        system_log.info("camera " + str(camera_id) + " triggered ")
        lock.release()
        # Get current book data
        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
        cursor = db.cursor(MySQLdb.cursors.DictCursor)
        cursor.execute("SELECT * FROM book WHERE status='ACTIVE'")
        result = cursor.fetchall()
        db.close()

        # Here we detect that there is an active proyect
        if(len(result)==0):
            lock.acquire()
            system_log.error("Log: There is not active project")
            lock.release()
            return -1

        root_path = result[0]["folder_path"]
        if (camera_id == 0):
            page_num = result[0]["page_num_a"]  # + "_" + str(id_camera)
            orig_path = tethered_temp_download_path + "izq.cr2"
        else:
            page_num = result[0]["page_num_b"]  # + "_" + str(id_camera)
            orig_path = tethered_temp_download_path + "der.cr2"

        id_book = result[0]["id_book"]
        image_unique_id = int(result[0]["image_unique_id_counter"])

        if debug:
            lock.acquire()
            system_log.info("Get current book data: DONE!")
            lock.release()

        # Create an unique file name, and timestamp for each image
        timeCode = strftime("%Y%m%d%H%M%S", gmtime())
        file_name = "_" + str(id_book) + "_" + page_num + "_"+ str(camera_id) +"_" + timeCode

        raw_hd_filename = root_path + "/" + file_name + ".cr2"
        full_file_name = root_path + "/" + file_name

        time.sleep(1)
        post_process_tethered(orig_path, file_name, full_file_name, raw_hd_filename, root_path, camera_id, page_num,
                              id_book, image_unique_id)

        # Update image_unique_id:  TODO check autoincrement
        image_unique_id = image_unique_id + 1

        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
        cursor = db.cursor(MySQLdb.cursors.DictCursor)
        query = "UPDATE book SET image_unique_id_counter=" + str(image_unique_id) + " WHERE status='ACTIVE'"
        cursor.execute(query)
        result = cursor.fetchall()
        db.close()

        if debug:
            lock.acquire()
            system_log.info("Update image_unique_id: DONE!")
            lock.release()




def post_process_tethered(orig_path, file_name, full_file_name, raw_hd_filename, root_path, camera_id, page_num,
                          id_book, image_unique_id):

    command = "mv " + orig_path + " " + raw_hd_filename
    system_call(command)
    time.sleep(1)
    post_process(file_name, full_file_name, raw_hd_filename, root_path, camera_id, page_num, id_book, image_unique_id)


def post_process(file_name, full_file_name, raw_hd_filename, root_path, camera_id, page_num, id_book, image_unique_id):

    debug = False
    command = "exiftool -b -PreviewImage -w .jpg " + raw_hd_filename
    response = system_call(command)
    system_call("convert -resize 9% " + full_file_name + ".jpg" + " " + full_file_name + ".jpg")

    if debug:
        lock.acquire()
        system_log.info("Generates a JPG thumbail from CR2 file: DONE!")
        lock.release()

    # Rotate images
    angle_str = ""
    if (camera_id == 0):
        angle_str = "90 "
    else:
        angle_str = "270 "

    command = "exiftool -Orientation='Rotate " + angle_str + "' " + raw_hd_filename
    system_call(command)
    command = "convert -rotate " + angle_str + full_file_name + ".jpg" + " " + full_file_name + ".jpg"
    system_call(command)

    if debug:
        lock.acquire()
        system_log.info("Rotate images: DONE!")
        lock.release()

    creationSeconds = os.path.getmtime(raw_hd_filename)
    timestampString =  time.ctime(creationSeconds)
    timestampStruct = time.strptime(timestampString, "%a %b %d %H:%M:%S %Y")
    timestamp = strftime("%Y%m%d%H%M%S", timestampStruct)

    # Compose id_label
    r = lambda: random.randint(0, 255)
    randomHEX = str('%02X%02X%02X_' % (r(), r(), r()))
    id_label = randomHEX + str(id_book) + "_" + str(page_num) + "_" + str(image_unique_id) + "_" + str(timestamp)

    if debug:
        lock.acquire()
        system_log.info("Compose id_label: DONE!")
        lock.release()

    # Add rootpath,page_num and id_book to image metadata
    command = "exiftool -XMP-dc:Identifier='" + id_label + "' " + raw_hd_filename
    system_call(command)
    command = "exiftool -XMP-dc:Source='" + str(id_book) + "' " + raw_hd_filename
    system_call(command)
    command = "exiftool -XMP-dc:Description='" + page_num + "' " + raw_hd_filename
    system_call(command)
    command = "exiftool -XMP-dc:All " + raw_hd_filename
    # system_log.info("INFO: Metadata inside CR2 file:\n" + system_call(command) )

    if debug:
        lock.acquire()
        system_log.info("Add rootpath,page_num and id_book to image metadata: DONE!")
        lock.release()

    # Generate MD5 (only for CR2 files)
    try:
        command = "md5sum " + raw_hd_filename
        response = system_call(command)
        index = response.find(" ", 0)
        md5sum = response[0:index]
    except:
        lock.acquire()
        system_log.error("ERROR 009: Error creating MD5SUM - " + raw_hd_filename)
        lock.release()

    if debug:
        lock.acquire()
        system_log.info("Generate MD5 (only for CR2 files): DONE!")
        lock.release()


    # Create an XML file with all associated metadata
    root = ET.Element("page")
    doc = ET.SubElement(root, "descriptive_metada")

    ET.SubElement(doc, "file_name").text = file_name + ".cr2"
    ET.SubElement(doc, "check_sum").text = md5sum
    ET.SubElement(doc, "book_id").text = str(id_book)
    ET.SubElement(doc, "page_num").text = page_num
    ET.SubElement(doc, "image_unique_id").text = id_label

    if debug:
        lock.acquire()
        system_log.info("Create an XML file with all associated metadata: DONE!")
        lock.release()

    # Store EXIF metadata
    command = "exiftool -EXIF:All " + raw_hd_filename
    metadata = system_call(command)
    lines = string.split(metadata, "\n")
    doc = ET.SubElement(root, "technical_metadata")

    for i in range(0, len(lines) - 1):
        data = string.split(lines[i], ":")
        param_name = data[0].strip()
        param_value = data[1].strip()
        param_name = param_name.replace(" ", "")
        param_name = param_name.replace("/", "_")
        ET.SubElement(doc, param_name).text = param_value

    tree = ET.ElementTree(root)
    tree.write(full_file_name + ".xml")

    if debug:
        lock.acquire()
        system_log.info("Store EXIF metadata: DONE!")
        lock.release()

    # Delete exiftool backup and sd images
    time.sleep(1)
    system_call("rm " + raw_hd_filename + "_original")

    if debug:
        lock.acquire()
        system_log.info("Delete exiftool backup and sd image: DONE!")
        lock.release()

    # Add current page to database
    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    query = "INSERT INTO image (id_cameraSetting,file_name,file_path,md5_checksum,time_stamp,id_page,id_book,unique_image_id_per_book,id_camera,iso,shutterspeed,fnumber) VALUES (" + str(
        1) + ",'" + file_name + ".cr2" + "','" + root_path + "','" + md5sum + "','" + timestamp + "','" + page_num + "'," + str(
        id_book) + ",'" + id_label + "','" + str(camera_id) + "','" + "100" + "','" + "1/125" + "','" + "0.8f" + "')"
    result = cursor.execute(query)
    db.commit()
    db.close()

    if debug:
        lock.acquire()
        system_log.info("Add current page to database: DONE!")
        lock.release()




# Update CURRENT field on DDBB with current properties for each camera
def update_camera_state_into_DB(port, id_camera):
    properties = {}

    # iso
    command = 'gphoto2 --port ' + port + ' --get-config /main/imgsettings/iso'
    response = system_call(command)
    index = response.find("Current: ")
    index_end = response.find("Choice: ")
    properties["iso"] = response[index + 9:index_end - 1]

    # shutterspeed
    command = 'gphoto2 --port ' + port + ' --get-config /main/capturesettings/shutterspeed'
    response = system_call(command)
    index = response.find("Current: ")
    index_end = response.find("Choice: ")
    properties["shutterspeed"] = response[index + 9:index_end - 1]

    # fnumber
    command = 'gphoto2 --port ' + port + ' --get-config /main/capturesettings/aperture'
    response = system_call(command)
    index = response.find("Current: ")
    index_end = response.find("Choice: ")
    properties["fnumber"] = response[index + 9:index_end - 1]

    system_log.info("INFO: Update camera settings from camera to DDBB")
    system_log.info("INFO: Properties set to..." + "ISO:" + properties["iso"] + " SHUTTER SPEED: " + properties[
        "shutterspeed"] + " FNUMBER: " + properties["fnumber"])

    error_code = -1  # no error

    db = MySQLdb.connect(host="localhost", user="root", passwd="factum", db="BD2")
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    filtro = "WHERE id_camera='" + str(id_camera) + "' and type='TARGET'"
    cursor.execute("""
       UPDATE camerasettings
       SET iso=%s, shutterspeed=%s, fnumber=%s, active='1'
       """ + filtro, (properties["iso"], properties["shutterspeed"], properties["fnumber"]))

    # TODO: Anhadir 1 a campo active si la camara esta conectada
    result = db.commit()
    # common.system_log.info("INFO: DDBB result..." + result)

    db.close()

    # system_info.info( "update_camera_state_into_db() - starts ")
    return error_code


# -------------------------------------------------------------------------------
# set_camera_parameters()
# -----------------------------------------------------------------------------
def set_camera_parameters(port, camera_id):
    debug = False

    # Update camera parameters from webapp
    if camera_id != "":
        filtro = " where id_camera = '" + str(camera_id) + "' and type='CURRENT'"
    else:
        filtro = ""
    try:
        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
        cursor = db.cursor(MySQLdb.cursors.DictCursor)
        query = "SELECT * FROM camerasettings" + filtro
        cursor.execute(query)
        result = cursor.fetchall()
        db.close()

        param1 = '--set-config-value /main/capturesettings/aperture=' + str(result[0]["fnumber"])
        param2 = '--set-config-value /main/capturesettings/shutterspeed=' + str(result[0]["shutterspeed"])
        param3 = '--set-config-value /main/imgsettings/iso=' + str(result[0]["iso"])
        command = 'gphoto2 ' + param1 + ' ' + param2 + ' ' + param3 + ' --port ' + port
        response = system_call(command)

        system_log.info("INFO: Current parameters uploaded to camera")
    except:
        print traceback.format_exc()
        system_log.error("ERROR 007: Error while setting current camera parameters")

        return -1
    # time.sleep(2)

    if debug:
        system_log.info("Update camera parameters from webapp: DONE!")


def get_camera_serial(port):
    command = 'gphoto2 --port ' + port + ' --get-config /main/status/eosserialnumber'
    response = system_call(command)
    print("Get Camera Serial response port " + port + " : " + response)
    index = response.find("Current: ")
    return response[index + 9:index + 9 + 12]


def get_camera_port(camera_id):
    # Get camera data from the ddbb
    debug = False

    try:
        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    except:
        system_log.error("ERROR 004 - Database connection fails. HOST:%s USER:%s PASS:%s :DB NAME%s",
                                host_address, user_name, user_pass, database_name)
        return "usb:,"
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    cursor.execute("SELECT * FROM camerasettings WHERE (type='TARGET' AND id_camera='"+ str(camera_id) +"')")
    result = cursor.fetchall()
    db.close()

    ddbb_serial_number = result[0]["serial_number"]

    #
    # system_log.info("INFO: App settings for this camera...")
    # system_log.info("INFO:\t\tserial_number:\t" + ddbb_serial_number)
    #
    # if debug:
    #     system_log.info("Get camera data from the ddbb: DONE!")

    # Make a list of all available cameras

    pAux = subprocess.Popen('gphoto2 --auto-detect',
                            shell=True,
                            stdout=subprocess.PIPE
                            )

    camera_list = []
    while True:
        next_line = pAux.stdout.readline()
        if not next_line:
            break
        else:
            if "usb:" in next_line:
                index = next_line.find("   ")
                name = next_line[0:index]
                index = next_line.find("usb:")
                port = next_line[index:(index + 11)]
                camera_list.append((name, port))

    # if debug:
    #     system_log.info("Make a list of all available cameras: DONE!")
    #     system_log.info(camera_list)

    # Check if there are, at least, one camera detected...
    if not camera_list:
        lock.acquire()
        system_log.error("ERROR 005: No cameras connected")
        lock.release()
        return "usb:,"

    for cam in range(len(camera_list)):
        command = 'gphoto2 --port ' + camera_list[cam][1] + ' --reset'
        response = system_call(command)
        print(response)


    if len(camera_list) != 2 :
        lock.acquire()
        system_log.error("ERROR 006: Only one camera connected")
        lock.release()
        return "usb:,"

    camera_list.sort(key=lambda x: x[0])


    # Look for camera USB port (using its serial_number)
    current_camera = -1
    lock.acquire()
    try :
        for cam in range(len(camera_list)):
            # print("INFO:\t\t"+str(camera_list[cam]))
            # Get serial number
            serial_number = get_camera_serial(camera_list[cam][1])
            # Store camera object for later use
            if (serial_number == ddbb_serial_number):
                db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
                cursor = db.cursor(MySQLdb.cursors.DictCursor)
                command = "UPDATE camerasettings SET serial_number='" + str(serial_number) + "' WHERE (id_camera='" + str(camera_id) + "' AND type='CURRENT')"
                cursor.execute(command)
                result = cursor.fetchall()
                db.close()


                system_log.info("Get Camera " + str(camera_id) + " Port INFO:\t\t" + str(camera_list[cam][0]) + " detected with serial number " + serial_number)
                lock.release()

                # Release camera
                return camera_list[cam][1]
    except:
        print("Problem getting port for camera " + str(camera_id))

    lock.release()
    return "usb:,"


def kill_gphoto_processes():
    pAux = subprocess.Popen("ps aux | grep -ie gphoto | awk '{print $2}' | xargs kill -9",
                            shell=True,
                            stdout=subprocess.PIPE
                            )
    while True:
        next_line = pAux.stdout.readline()
        if not next_line:
            break
        else:
            print('Killing gphoto process ... ' + next_line)


def reset_log():
    # -------------------------------------------------------------------------------
    # Logger
    # -------------------------------------------------------------------------------
    global system_log
    global system_log_handler
    global system_log_stream

    lock.acquire()
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
    lock.release()

def log_to_ddbb():
    lock.acquire()
    log_result = system_log_stream.getvalue()
    system_log_handler.flush()
    lock.release()
    # Insert log entry into DDBB
    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)

    query = "INSERT INTO py_system_log (content) VALUES (" + '"' + log_result + '"' + ")"
    result = cursor.execute(query)
    db.commit()
    db.close()

###############################################################################
#
#  Start of the program
#
###############################################################################

kill_gphoto_processes()
time.sleep(0.1)
response = system_call("sudo killall gvfs-gphoto2-volume-monitor")
time.sleep(0.1)

lock = threading.Lock()

reset_log()

system_log.info("INFO: Session starts")

port_1 = get_camera_port(0)
if port_1 == "usb:,":
    while port_1 == "usb:,":
        if stopSession:
            break
        else:
            port_1 = get_camera_port(0)

port_2 = get_camera_port(1)
if port_2 == "usb:,":
    while port_2 == "usb:,":
        if stopSession:
            break
        else:
            port_2 = get_camera_port(1)


listen = Thread(listen_camera, 0, lock, port_1)
time.sleep(1)
listen_2 = Thread(listen_camera, 1, lock, port_2)

function = 0
last_function = function
response = 0

db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
cursor = db.cursor(MySQLdb.cursors.DictCursor)
cursor.execute("UPDATE communication SET response='0', function='0' WHERE communication_id=1")
result = cursor.fetchall()

# Initial log
log_to_ddbb()
reset_log()

processing = False

try:
    while True:
        # Infinite loop

        # Check if we need to execute some function
        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
        cursor = db.cursor(MySQLdb.cursors.DictCursor)
        cursor.execute("SELECT * FROM communication WHERE communication_id=1")
        result = cursor.fetchall()
        db.close()

        last_function = function
        function = result[0]["function"]
        response = result[0]["response"]

        # Camera configuration check initial steps
        if function == "4" and response == "0":
            auto_detect_response = system_call("gphoto2 --auto-detect")
            parts = auto_detect_response.split("\n")
            print(parts)

            nlines = len(parts)

            if nlines == 5:
                lock.acquire()
                system_log.info("Cameras Configuration Check INFO: Two cameras are connected")
                lock.release()
            else:
                lock.acquire()
                system_log.error(
                    "Cameras Configuration Check ERROR: There is (at least) one camera that can't be reached")
                lock.release()



            # Detectar numero de serie
            try:
                db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
                db.close()
                lock.acquire()
                system_log.info("Cameras Configuration Check INFO: DDBB connection established!")
                lock.release()

            except:
                print traceback.format_exc()
                lock.acquire()
                system_log.error("Cameras Configuration Check ERROR: Database connection fails")
                lock.release()

        # 1. Capture from web app
        if function != "0" and response == "0" and not processing:
            db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
            cursor = db.cursor(MySQLdb.cursors.DictCursor)
            cursor.execute("UPDATE communication SET response='0' WHERE communication_id=1")
            result = cursor.fetchall()
            last_function = int(function)
            kill_gphoto_processes()
            processing = True
            last_time_function = int(round(time.time() * 1000))


        if function_finished_left == 1 and  function_finished_right == 1:
            print("Process finished !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1")
            lock.acquire()
            function_finished_left = 0
            function_finished_right = 0
            lock.release()
            db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
            cursor = db.cursor(MySQLdb.cursors.DictCursor)
            cursor.execute("UPDATE communication SET response='2' WHERE communication_id=1")
            result = cursor.fetchall()
            db.close()
            # Print log
            log_to_ddbb()
            reset_log()
            processing = False

        if not camera_0_thread_active:
            port_1 = get_camera_port(0)
            if port_1 != "usb:,":
                listen = Thread(listen_camera, 0, lock, port_1)

        if not camera_1_thread_active:
            port_2 = get_camera_port(1)
            if port_2 != "usb:,":
                listen = Thread(listen_camera, 1, lock, port_2)

        """""
        millis = int(round(time.time() * 1000))
        if(millis - last_time_function > max_time_function and processing):
            lock.acquire()
            function_finished_left = 0
            function_finished_right = 0
            lock.release()
            db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
            cursor = db.cursor(MySQLdb.cursors.DictCursor)
            cursor.execute("UPDATE communication SET response='2' WHERE communication_id=1")
            result = cursor.fetchall()
            db.close()
            # Print log
            log_to_ddbb()
            reset_log()
            processing = False
        """

        time.sleep(1)  # time for other threads to set variables again

        # End of infinite loop

except:
    print traceback.format_exc()
    lock.acquire()
    system_log.info("finishing program - killing gphoto subprocess ")
    lock.release()
    lock.acquire()
    stopSession = True
    lock.release()
    time.sleep(1)
    kill_gphoto_processes()




