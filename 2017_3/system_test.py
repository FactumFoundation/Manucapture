#!/usr/bin/env python
# -*- coding: utf-8 -*-

import MySQLdb
import common
import serial

# Database connection data
host_address = "localhost"
user_name = "root"
user_pass = "factum"
database_name = "BD2"

common.system_test()

print "----------------------------------------"
print "Detecting arduino..."
# Arduino connection
microcontroller_serial_port = "/dev/ttyACM"
success = 0

for i in range(0, 9):
    try:
        serial_port = microcontroller_serial_port + str(i)
        common.system_call("sudo chmod 666 " + serial_port)
        serialFromArduino = serial.Serial(serial_port,9600)
        serialFromArduino.flush()
        print ("Connected to serial port: " + serial_port)
        success = 1
        serialFromArduino.flushInput()
        serialFromArduino.flushOutput()      
        break
    except:
        print("")
        #print("Failed to connect to serial port "+ serial_port)

if success == 1:
    print "OK: Arduino connection established!"
else:
    print "ERROR: Arduino doesn't appear to be connected"

