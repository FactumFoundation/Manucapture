import errno, os, sys, time,shlex,glob
import ftplib

ftp_host = "192.168.2.201"
ftp_user = "factum"
ftp_pass = "1234asdf$"
file_path = '1/0001.TIF'

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

    print(sys.argv[1],sys.argv[2])

"""
    
