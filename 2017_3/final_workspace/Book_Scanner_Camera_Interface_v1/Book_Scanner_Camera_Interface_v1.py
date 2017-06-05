import common
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
last_function = 0

function_finished_left = 0
function_finished_right = 0


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

    pending_image = False
    fileName = "default.cr2"

    if cameraId == 0:
        fileName = "izq.cr2"
    elif cameraId == 1:
        fileName = "der.cr2"

    camera_detected = True

    if port != "usb:,":
        command = 'gphoto2 --capture-tethered --port ' + port + ' --force-overwrite --filename ' + fileName
        print command
        lock.acquire()
        proc = subprocess.Popen(command,
                                shell=True,
                                stdin=subprocess.PIPE,
                                stdout=subprocess.PIPE
                                )
        lock.release()

        try:
            while True:

                # Exit the loop when flag stopSession is active
                if stopSession:
                    print("End of processs  ")
                    kill_gphoto_processes()
                    break

                # Read tether connection output and detect when the image is downloaded
                next_line = proc.stdout.readline()
                if not next_line:

                    # Download process finished, reconnect tethering
                    if not stopSession and camera_detected:
                        """
                        port = get_camera_port(cameraId)
                        if (port == "usb:," and camera_detected == True):
                            camera_detected = False
                            print("Error detecting camera " + str(cameraId))

                        if (port != "usb:,"):
                            if camera_detected == False:
                                print("Reconnecting to camera " + str(cameraId))
                            camera_detected = True
                        """
                        print("-----------------------------------Last function : " + str(last_function))
                        hasResults = False
                        if last_function == 1:
                            print("Threaded function 1")
                            set_camera_parameters(port, cameraId)
                            capture(port, cameraId)
                            hasResults = True
                        elif last_function == 2:
                            update_camera_state_into_DB(port, cameraId)
                            print("Threaded function 2")
                            hasResults = True
                        elif last_function == 3:
                            set_camera_parameters(port, cameraId)
                            print("Threaded function 3")
                            hasResults = True
                        elif last_function == 4:
                            common.system_test()
                            print("Threaded function 4")
                            lock.acquire()
                            newPort = get_camera_port(cameraId)
                            lock.release()
                            if newPort != "usb:,":
                                db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass,
                                                     db=database_name)
                                cursor = db.cursor(MySQLdb.cursors.DictCursor)
                                cursor.execute("UPDATE camerasettings SET active='1' WHERE type='TARGET' AND id_camera='" + str(cameraId) + "'")
                                result = cursor.fetchall()
                                db.close()
                            hasResults = True
                        elif last_function == 5:
                            print("Threaded function 5")
                            set_camera_parameters(port, cameraId)
                            capture_test(port, cameraId)
                            hasResults = True


                        if hasResults == True:
                            print("updating after results")
                            # Print log
                            common.print_logs()
                            common.log_to_ddbb()
                            hasResults = False
                            lock.acquire()
                            if( cameraId == 0):
                                function_finished_left = 1
                            else :
                                function_finished_right = 1
                            lock.release()


                        command = 'gphoto2 --capture-tethered --port ' + port + ' --force-overwrite --filename ' + fileName
                        print command
                        proc = subprocess.Popen(command,
                                                shell=True,
                                                stdin=subprocess.PIPE,
                                                stdout=subprocess.PIPE
                                                )
                    else:
                        proc.kill()
                        break

                else:
                    # Message Log

                    if cameraId == 0:
                        print("Camera 0 : " + next_line.rstrip())
                    elif cameraId==1:
                        print("Camera 1 : " + next_line.rstrip())


                    # A new file is downloaded: postrpocess starts
                    if "Saving file as " in next_line.rstrip():

                        pending_image = True
                        proc.kill()

                        # update communication table with the response (2: ok, -1 : bad)
                        if (cameraId == 0):
                            db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
                            cursor = db.cursor(MySQLdb.cursors.DictCursor)
                            cursor.execute(
                                "UPDATE communication SET response='1', function='1' WHERE communication_id=1")
                            result = cursor.fetchall()
                            db.close()


                        process_tethered(cameraId)

                        # update communication table with the response (2: ok, -1 : bad)
                        if(cameraId == 0) :
                            db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
                            cursor = db.cursor(MySQLdb.cursors.DictCursor)
                            cursor.execute("UPDATE communication SET response='2', function='1' WHERE communication_id=1")
                            result = cursor.fetchall()
                            db.close()


        except:
            # problem communicating with cameras, kill any pending gphoto process
            print traceback.format_exc()
            print("end of listening camera thread- killing gphoto subprocess ")
            kill_gphoto_processes()


# -------------------------------------------------------------------------------
# capture()
# -----------------------------------------------------------------------------
def capture(port, camera_id):
    debug = True

    if debug:
        print("Look for camera USB port (using its serial_number): DONE!")

    if port == "usb:,":
        print("ERROR 006: Serial number doesn't appears")
        return -1

    print("INFO: Camera port detected...[%s]", port)

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
        print("Get current book data: DONE!")

    # Create an unique file name, and timestamp for each image
    timestamp = strftime("%Y%m%d%H%M%S", gmtime())
    file_name = "_" + str(id_book) + "_" + str(camera_id) + "_" + page_num + "_" + timestamp
    raw_hd_filename = root_path + "/" + file_name + ".cr2"
    full_file_name = root_path + "/" + file_name

    if debug:
        print("Create an unique file name, and timestamp for each image: DONE!")

    # Download last image and delete it from SD card
    print("Capture and Download new image in : " + raw_hd_filename)

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
        print("Capture and Download: DONE!")

    post_process(file_name, full_file_name, raw_hd_filename, root_path, camera_id, page_num, id_book, image_unique_id,
                 timestamp)



# -------------------------------------------------------------------------------
# capture()
# -----------------------------------------------------------------------------
def capture_test(port, camera_id):
    debug = True

    if debug:
        print("Look for camera USB port (using its serial_number): DONE!")

    if port == "usb:,":
        print("ERROR 006: Serial number doesn't appears")
        return -1

    print("INFO: Camera port detected...[%s]", port)

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
        file_name = "img_test_a"
    else:
        file_name = "img_test_b"

    # Create an unique file name, and timestamp for each image
    raw_hd_filename = root_path + file_name + ".cr2"
    full_file_name = root_path + file_name

    if debug:
        print("Create test url")

    # Download last image and delete it from SD card
    print("Capture and Download new image in : " + raw_hd_filename)

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
        print("Capture and Download: DONE!")

    command = "exiftool -b -PreviewImage -w .jpg " + raw_hd_filename
    system_call(command)
    system_call("convert -resize 9% " + full_file_name + ".jpg" + " " + full_file_name + ".jpg")






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
    debug = True

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

        if debug:
            print(result)

        param1 = '--set-config-value /main/capturesettings/aperture=' + str(result[0]["fnumber"])
        param2 = '--set-config-value /main/capturesettings/shutterspeed=' + str(result[0]["shutterspeed"])
        param3 = '--set-config-value /main/imgsettings/iso=' + str(result[0]["iso"])
        command = 'gphoto2 ' + param1 + ' ' + param2 + ' ' + param3 + ' --port ' + port
        response = system_call(command)
        print response

        common.system_log.info("INFO: Current parameters uploaded to camera")
    except:
        print ("problema")
        print traceback.format_exc()
        common.system_log.error("ERROR 007: Error while setting current camera parameters")

        return -1
    # time.sleep(2)

    if debug:
        print("Update camera parameters from webapp: DONE!")


def get_camera_serial(port):
    command = 'gphoto2 --port ' + port + ' --get-config /main/status/eosserialnumber'
    print(command)
    response = system_call(command)
    print(response)
    index = response.find("Current: ")
    return response[index + 9:index + 9 + 12]


def get_camera_port(camera_id):
    # Get camera data from the ddbb
    debug = True

    try:
        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    except:
        common.system_log.error("ERROR 004 - Database connection fails. HOST:%s USER:%s PASS:%s :DB NAME%s",
                                host_address, user_name, user_pass, database_name)
        return "usb:,"
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    cursor.execute("SELECT * FROM camerasettings WHERE type='TARGET'")
    result = cursor.fetchall()
    db.close()

    ddbb_serial_number = result[camera_id]["serial_number"]
    ddbb_id_camera = result[camera_id]["id_camera"]

    common.system_log.info("INFO: App settings for this camera...")
    common.system_log.info("INFO:\t\tserial_number:\t" + ddbb_serial_number)

    if debug:
        print("Get camera data from the ddbb: DONE!")

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
                print ("discovered port : " + port + " name : " + name)
                camera_list.append((name, port))

    if debug:
        print ("Make a list of all available cameras: DONE!")

    print(camera_list)

    # Check if there are, at least, one camera detected...
    if not camera_list:
        common.system_log.error("ERROR 005: No cameras connected")
        return "usb:,"

    camera_list.sort(key=lambda x: x[0])
    common.system_log.info("INFO: Connected cameras list...")

    # Look for camera USB port (using its serial_number)
    current_camera = -1
    for cam in range(len(camera_list)):
        common.system_log.info("INFO:\t\t" + str(camera_list[cam]))
        # print("INFO:\t\t"+str(camera_list[cam]))
        # Get serial number
        serial_number = get_camera_serial(camera_list[cam][1])
        common.system_log.info("INFO:\t\tserial_number\t" + serial_number)
        # Store camera object for later use
        if (serial_number == ddbb_serial_number):
            # Release camera
            return camera_list[cam][1]

    return "usb:,"


def kill_gphoto_processes():
    pAux = subprocess.Popen('ps -aux | grep "gphoto2"',
                            shell=True,
                            stdout=subprocess.PIPE
                            )
    while True:
        next_line = pAux.stdout.readline()
        if not next_line:
            break
        else:
            if "--capture-tethered " in next_line:
                # print("killing process:    " + next_line)
                index1 = next_line.find("  ")
                index2 = next_line.find("  ", index1 + 4)
                try:
                    pid = int(next_line[index1 + 3:index2])
                    os.kill(pid, 2)
                except:
                    print traceback.format_exc()
                    print("Problem killing gphoto process")


def process_tethered(camera_id):
    tethered_temp_download_path = "/home/factum/PycharmProjects/Book_Scanner_Camera_Interface_v1/"
    debug = True

    db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    cursor.execute("SELECT * FROM camerasettings WHERE type='CURRENT'")
    result = cursor.fetchall()
    db.close()

    ddbb_active = result[camera_id]["active"]

    if (ddbb_active):

        print("camera " + str(camera_id) + " triggered ")
        # Get current book data
        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
        cursor = db.cursor(MySQLdb.cursors.DictCursor)
        cursor.execute("SELECT * FROM book WHERE status='ACTIVE'")
        result = cursor.fetchall()
        db.close()

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
            print("Get current book data: DONE!")

        # Create an unique file name, and timestamp for each image
        timestamp = strftime("%Y%m%d%H%M%S", gmtime())
        file_name = "_" + str(id_book) + "_" + str(camera_id) + "_"+ page_num +"_" + timestamp

        raw_hd_filename = root_path + "/" + file_name + ".cr2"
        full_file_name = root_path + "/" + file_name

        time.sleep(1)
        post_process_tethered(orig_path, file_name, full_file_name, raw_hd_filename, root_path, camera_id, page_num,
                              id_book, image_unique_id, timestamp)

        # Update image_unique_id:  TODO check autoincrement
        print("Before ID : " + str(image_unique_id))
        image_unique_id = image_unique_id + 1
        print("After ID : " + str(image_unique_id))

        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
        cursor = db.cursor(MySQLdb.cursors.DictCursor)
        query = "UPDATE book SET image_unique_id_counter=" + str(image_unique_id) + " WHERE status='ACTIVE'"
        cursor.execute(query)
        result = cursor.fetchall()
        db.close()

        if debug:
            print("Update image_unique_id: DONE!")

        """
        pg_num = int(page_num[:(len(page_num)-1)])
        pg_index = page_num[(len(page_num)-1):]
        pg_num = pg_num + 1;
        new_page_num = str(pg_num) + pg_index

        print("Page number : " + str(pg_num) + " Page index : " + pg_index)
        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
        cursor = db.cursor(MySQLdb.cursors.DictCursor)
        if camera_id == 0 :
            query = "UPDATE book SET page_num_a='"+str(new_page_num)+"' WHERE status='ACTIVE'"
            print(query)
        elif camera_id == 1 :
            query = "UPDATE book SET page_num_b='"+str(new_page_num)+"' WHERE status='ACTIVE'"
            print(query)
        cursor.execute(query)
        result = cursor.fetchall()
        db.close()

        """


def post_process_tethered(orig_path, file_name, full_file_name, raw_hd_filename, root_path, camera_id, page_num,
                          id_book, image_unique_id, timestamp):

    command = "mv " + orig_path + " " + raw_hd_filename
    print(command)
    system_call(command)

    time.sleep(1)
    post_process(file_name, full_file_name, raw_hd_filename, root_path, camera_id, page_num, id_book, image_unique_id,
                 timestamp)


def post_process(file_name, full_file_name, raw_hd_filename, root_path, camera_id, page_num, id_book, image_unique_id,
                 timestamp):
    debug = True
    command = "exiftool -b -PreviewImage -w .jpg " + raw_hd_filename
    response = system_call(command)
    system_call("convert -resize 9% " + full_file_name + ".jpg" + " " + full_file_name + ".jpg")

    if debug:
        print("Generates a JPG thumbail from CR2 file: DONE!")

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
        print("Rotate images: DONE!")

    # Generate MD5 (only for CR2 files)
    try:
        response = system_call("md5sum " + raw_hd_filename)
        index = response.find(" ", 0)
        md5sum = response[0:index]
    except:
        print("ERROR 009: Error creating MD5SUM - " + raw_hd_filename)

    if debug:
        print ("Generate MD5 (only for CR2 files): DONE!")

    # Compose id_label
    r = lambda: random.randint(0, 255)
    randomHEX = str('%02X%02X%02X_' % (r(), r(), r()))
    id_label = randomHEX + str(id_book) + "_" + str(page_num) + "_" + str(image_unique_id) + "_" + str(timestamp)

    if debug:
        print("Compose id_label: DONE!")

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
        print("Add rootpath,page_num and id_book to image metadata: DONE!")

    # Create an XML file with all associated metadata
    root = ET.Element("page")
    doc = ET.SubElement(root, "descriptive_metada")

    ET.SubElement(doc, "file_name").text = file_name + ".cr2"
    ET.SubElement(doc, "check_sum").text = md5sum
    ET.SubElement(doc, "book_id").text = str(id_book)
    ET.SubElement(doc, "page_num").text = page_num
    ET.SubElement(doc, "image_unique_id").text = id_label

    if debug:
        print("Create an XML file with all associated metadata: DONE!")

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
        print("Store EXIF metadata: DONE!")

    # Delete exiftool backup and sd images
    time.sleep(1)
    system_call("rm " + raw_hd_filename + "_original")
    system_call("rm " + full_file_name + ".jpg" + "_original")

    if debug:
        print("Delete exiftool backup and sd image: DONE!")

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
        print("Add current page to database: DONE!")

    # Send log to DDBB
    common.log_to_ddbb()


###############################################################################
#
#  Start of the program
#
###############################################################################

kill_gphoto_processes()
time.sleep(0.1)
response = common.system_call("sudo killall gvfs-gphoto2-volume-monitor")
time.sleep(0.1)

lock = threading.Lock()
# use Python logging
logging.basicConfig(
    format='%(levelname)s: %(name)s: %(message)s', level=logging.WARNING)


port_1 = get_camera_port(0)

while port_1 == "usb:,":
    if stopSession:
        break
    else:
        print("*********************************** Error opening port")
        time.sleep(1)
        lock.acquire()
        port_1 = get_camera_port(0)
        lock.release()
        camera_detected = True

port_2 = get_camera_port(1)

while port_2 == "usb:,":
    if stopSession:
        break
    else:
        print("*********************************** Error opening port")
        time.sleep(1)
        lock.acquire()
        port_2 = get_camera_port(1)
        lock.release()
        camera_detected = True


listen = Thread(listen_camera, 0, lock, port_1)
print("*****************************************************************************************")
time.sleep(1)

listen_2 = Thread(listen_camera, 1, lock, port_2)
print("*****************************************************************************************")

last_function = int(function)

try:
    while True:
        # Infinite loop

        # Check if we need to execute some function
        db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
        cursor = db.cursor(MySQLdb.cursors.DictCursor)
        cursor.execute("SELECT * FROM communication WHERE communication_id=1")
        result = cursor.fetchall()
        db.close()

        function = result[0]["function"]
        response = result[0]["response"]

        # 1. Capture from web app
        if function != "0" and response == "0":
            db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
            cursor = db.cursor(MySQLdb.cursors.DictCursor)
            cursor.execute("UPDATE communication SET response='1' WHERE communication_id=1")
            result = cursor.fetchall()
            print("function " + str(function))
            last_function = int(function)
            kill_gphoto_processes()


        if function_finished_left == 1 and  function_finished_right == 1:
            lock.acquire()
            function_finished_left = 0
            function_finished_right = 0
            lock.release()
            db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
            cursor = db.cursor(MySQLdb.cursors.DictCursor)
            cursor.execute("UPDATE communication SET response='2' WHERE communication_id=1")
            result = cursor.fetchall()
            db.close()


        time.sleep(1)  # time for other threads to set variables again

        # End of infinite loop

except:
    print traceback.format_exc()
    print("finishing program - killing gphoto subprocess ")
    lock.acquire()
    stopSession = True
    lock.release()
    time.sleep(1)
    kill_gphoto_processes()




