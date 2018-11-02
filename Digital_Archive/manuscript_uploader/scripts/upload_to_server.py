# upload script by Enrique Esteban
# Uses python-osc. To install run sudo pip install python-osc


import errno, os, sys, time,shlex,glob
import ftplib

from pythonosc import osc_message_builder
from pythonosc import udp_client



ftp_host = "192.168.2.201"
ftp_user = "factum"
ftp_pass = "1234asdf$"
file_path = '1/0001.TIF'

"""
print("Connect to server")
ftp = ftplib.FTP("192.168.2.201")
ftp.login("factum", "1234asdf$")

## Create input folder on the server
#ftp.cwd("/Scans/")
#ftp.mkd("NEW_SCANS")

print("Moving to input folder")
ftp.cwd("/Scans/NEW_SCANS")

##Directory listing
#data = []
#ftp.dir(data.append)
#for line in data:
#    print("-", line)

print("Sending file")
start = time.time()
file = open(file_path,'rb')                 
ftp.storbinary('STOR 0001_Oriental_Manuscript.TIF', file)    
file.close()
end = time.time()
elapsed = end - start
print("Ellapsed time", elapsed)

print("Close server")
ftp.quit()

"""



if __name__ == '__main__':

  
    dirname = os.path.dirname(__file__)
    stopFlag = os.path.join(dirname, 'stopUpload.txt')

    ftp_host = sys.argv[1]
    ftp_user = sys.argv[2]
    ftp_pass = sys.argv[3]
 
    process_stopped = False
    client = udp_client.SimpleUDPClient("127.0.0.1", 5005)
    for x in range(10):
        print("Uploading ",x)
        client.send_message("/uploaded", "file_name")
        if os.path.isfile(stopFlag):
            os.remove(stopFlag)
            client.send_message("/stopped", "stopped by user")
            process_stopped = True
            break    
        time.sleep(2)

    if not process_stopped :
        client.send_message("/end", "finish upload")
   


 


    





