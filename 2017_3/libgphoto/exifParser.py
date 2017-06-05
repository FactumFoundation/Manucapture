import common

import string



import random
r = lambda: random.randint(0,255)
randomHEX = str('%02X%02X%02X' % (r(),r(),r()))
print(randomHEX)



def parseMetadataToXML(file_path):
    command = "exiftool -EXIF:All " + file_path
    #print command
    metadata = common.system_call(command)
    #print metadata
    lines = string.split(metadata, "\n")

    for i in range( 0, len(lines)-1 ):
        data = string.split(lines[i], ":")
        param_name = data[0].strip()
        param_value = data[1].strip()
        #print "param_name: "+ param_name
        #print "param_value: "+ param_value


        
    #print lines[0]
    
parseMetadataToXML("/home/factum/test.cr2")
    


    
