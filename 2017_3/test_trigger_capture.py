import common
from time import gmtime, strftime
import time
import serial

# Database connection data
host_address = "localhost"
user_name = "root"
user_pass = "factum"
database_name = "BD2"
sd_root_path = "/store_00020001/DCIM/100EOS5D/"

# Init cameras
common.system_log.info("INFO: Initializing cameras")
response = common.init_camera_data()
print "CAMERA DATA [0]: "+ str(common.camera_data[0])
print "CAMERA DATA [1]: "+ str(common.camera_data[1])

# Capture images using external trigger
serialFromArduino = serial.Serial("/dev/ttyACM0",9600)
serialFromArduino.flush()
serialFromArduino.write('a')
time.sleep(8)

def get_image( id_camera ):
    # Get image names to download
    print "\nLISTING FILES..."
    command = "gphoto2 --list-files --port=" + common.camera_data[id_camera]['usb_port']
    print "\nGPHOTO COMMAND..." + command
    response = common.system_call(command)
    print "\nGPHOTO SAYS..." + response
    response_lines = response.split("\n")
    raw_sd_filename = response_lines[len(response_lines)-4]
    index_0 = raw_sd_filename.find(" ", 7)
    raw_sd_filename = raw_sd_filename[7:index_0]
    raw_sd_filename = sd_root_path + raw_sd_filename
    print "\nSD FILE NAME..." + raw_sd_filename

    # Create unique name
    timestamp = strftime("%Y%m%d%H%M%S", gmtime())
    file_name = "_"+ timestamp
    raw_file_name = file_name + ".cr2"
    raw_full_filename = "/home/factum/Book_Scanner_Master/Libro_de_prueba/" + raw_file_name
    print "\nHD FILE NAME..." + raw_full_filename

    # Download last image captured
    command = "gphoto2 --get-file=" + raw_sd_filename + " --filename=" + raw_full_filename + " --port=" + common.camera_data[id_camera]['usb_port']
    print "\nDOWNLOADING IMAGE... " + raw_sd_filename
    response = common.system_call(command)
    print "\nGPHOTO SAYS..." + response
    
get_image(0)
get_image(1)


