from tkinter import *
from tkinter.ttk import *
import pysftp
import ftplib

class Example(Frame):

    ftp_host = "192.168.2.201"
    ftp_user = "factum"
    ftp_pass = "1234asdf$"

    progressBar = None
    log = None

    def __init__(self):
        super().__init__()
        self.initUI()

    def perform_upload(self):
        print("*************Connecting to FTP server*************")
        ftp = ftplib.FTP("192.168.2.201")
        ftp.login("factum", "1234asdf$")
        data = []
        ftp.dir(data.append)
        ftp.quit()
        for line in data:
            print("-", line)
            
        """
        cnopts = pysftp.CnOpts()
        cnopts.hostkeys = None   
        with pysftp.Connection(self.ftp_host, port=21, username=self.ftp_user, password=self.ftp_pass, cnopts=cnopts) as sftp:
            #sftp.put(local_path, remote_path)
            print(sftp.listdir())

        """

    def open_settings(self):
        print("Open settings window to configure connection")
        
    def initUI(self):
        self.style = Style()
        self.style.theme_use("default")
        self.master.title("Manuscripts Uploader")
        self.pack(fill=BOTH, expand=1)
        self.centerWindow()
        self.initGUI()
 
    def centerWindow(self):
        appWidth = 350
        appHeight = 450
        screenWidth = self.master.winfo_screenwidth()
        screenHeight = self.master.winfo_screenheight()
        #x = (screenWidth - appWidth)/2
        #y = (screenHeight - appHeight)/2
        x = 0
        y = 0
        self.master.geometry('%dx%d+%d+%d' % (appWidth, appHeight, x, y))

    def initGUI(self):
        quitButton = Button(self, text="Upload", command=self.perform_upload)
        quitButton.place(x=0, y=0)
        quitButton.pack(side=TOP, pady=10)

        settingsButton = Button(self, text="Settings", command=self.open_settings)
        settingsButton.place(x=0, y=0)
        settingsButton.pack(side=TOP, pady=10)

        self.progressbar=Progressbar(self,orient="horizontal",length=300,mode="determinate")
        self.progressbar.pack(side=TOP, pady=10)
    
        self.log = Text(x=50, y=100, wrap=WORD)
        self.log.insert(INSERT, "27/09/2018 - 12:27:20 : Start upload of manuscript 001\n")
        self.log.insert(INSERT, "27/09/2018 - 12:40:20 : End of upload of manuscript 001\n")
        self.log.insert(END, "Ending session")
        self.log.pack()
            
        scroll = Scrollbar(command=self.log.yview)
        scroll.pack(side=RIGHT, fill=Y)
        self.log.config(yscrollcommand=scroll.set)

    def progress(currentValue):
        progressbar["value"]=currentValue


    def get_foldername(prompt):
        root = Tk()
        root.withdraw()  # get rid of annoying little window
        my_path = filedialog.askdirectory(initialdir="/Users/" + getuser(), title=prompt) + "/"
        return my_path

def main():
    root = Tk()
    app = Example()
    root.mainloop()  
if __name__ == '__main__':
    main()   
