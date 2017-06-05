#!/usr/bin/env python
# -*- coding: utf-8 -*-

import MySQLdb
import common

# Database connection data
host_address = "localhost"
user_name = "root"
user_pass = "factum"
database_name = "BD2"

#-------------------------------------------------------------------------------
# main()
#-------------------------------------------------------------------------------
common.system_log.info("INFO: update_camera_params_to_DB script starts")
common.system_log.info("INFO: Initializing cameras")
common.init_camera_data()
common.update_camera_state_into_DB( 0 )
common.update_camera_state_into_DB( 1 )

# Print log
common.print_logs()

