import os
import pyrebase
import firebase_admin
from firebase_admin import db
from firebase_admin import storage as admin_storage
import calendar
import time
from subprocess import Popen
import sched, time


#Initialize Static Data
PRINTER_ID = "50"
IMAGE_DOWNLOAD_PATH = 'C:/Users/Yousef/Desktop/GoSelfie/'
FIREBASE_ADMIN_CREDENTIALS_PATH = "C:/Users/Yousef/Desktop/key.json"
images = []
OKGREEN = '\033[92m'
WARNING = '\033[93m'
OKCYAN = '\033[96m'
ENDC = '\033[0m'
debug = False
timerCheck = False

s = sched.scheduler(time.time, time.sleep)

#Initialize Firebase

print(OKGREEN + "Starting firebase initialization..." + ENDC)

#pyrebase library
config = {
  "apiKey": "AIzaSyCfeXGXggp3XU_6XYWioh7rWyPd1iIQfXE",
  "authDomain": "goselfie-54e9b.firebaseapp.com",
  "databaseURL": "https://goselfie-54e9b-default-rtdb.firebaseio.com",
  "projectId": "goselfie-54e9b",
  "storageBucket": "goselfie-54e9b.appspot.com",
  "messagingSenderId": "932163176389",
  "appId": "1:932163176389:web:efd039d1e28d8dbd236dac",
  "measurementId": "G-R4NEZSTMDF"
}
firebase = pyrebase.initialize_app(config)
storage = firebase.storage()

#firebase_admin library
cred_obj = firebase_admin.credentials.Certificate(FIREBASE_ADMIN_CREDENTIALS_PATH)
databaseURL = 'https://goselfie-54e9b-default-rtdb.firebaseio.com'
default_app = firebase_admin.initialize_app(cred_obj, {'databaseURL':databaseURL})
bucket = admin_storage.bucket("goselfie-54e9b.appspot.com")
localRef = db.reference("/")
dataRef = db.reference("printer"+PRINTER_ID+"Data")
requestRef = db.reference("printer"+PRINTER_ID+"Requests")
print(OKGREEN + "Firebase initialized successfully!" + ENDC)


def listener(event):
    if debug == True:
        print(event.event_type)  # can be 'put' or 'patch'
        print(event.path)  # relative to the reference, it seems
        print(event.data)  # new data at /reference/event.path. None if deleted
    string_path = event.path
    string_data_update = event.data
    if string_path == "/printer"+PRINTER_ID+"Requests":
        #Check if the updated string is the "Requests" string to start printing requests
        if str(requestRef.get()) != "done":
            print(str(WARNING) + "Starting "+ str(requestRef.get()) + " Requests" + ENDC) 
            imagesArray2 = str(db.reference("printer"+PRINTER_ID+"Data").get()).split(",") 
            whileLoopText = requestRef.get()
            i = 0 
            if len(imagesArray2) >= int(requestRef.get()) :
                while whileLoopText != "done":
                    imagesArray2 = str(db.reference("printer"+PRINTER_ID+"Data").get()).split(",") 
                    if debug == True:
                            print("imagesArray: "+str(dataRef.get()))
                    if len(imagesArray2[0]) > 3: 
                        if debug == True:
                            print("imagesArray2Size: "+str(len(imagesArray2)) + " I: "+str(i))
                        if len(imagesArray2) == 1:
                            startPrint(imagesArray2[0],1,i)
                        else :
                            startPrint(imagesArray2[0],0,i)
                        whileLoopText = requestRef.get()
                        i+=1
                        imagesArray2 = str(db.reference("printer"+PRINTER_ID+"Data").get()).split(",")  
                    else:
                        print("There's an OverFlow Fixed")
                        localRef.update({"printer"+str(PRINTER_ID)+"Requests":"done"})
                        whileLoopText = requestRef.get()           
            else:
                localRef.update({"printer"+str(PRINTER_ID)+"Requests":str(len(imagesArray2))})
                print("Unknowwn else called: "+str(db.reference("printer"+PRINTER_ID+"Data").get())+" e: "+str(db.reference("printer"+PRINTER_ID+"Requests").get()))
                
            
                    
       
firebase_admin.db.reference('/').listen(listener)
print(OKGREEN + "Firebase Listener initialized, Debug is set to:" + str(debug)+ ENDC)
##Check if folder is not created
dirname = os.path.dirname(IMAGE_DOWNLOAD_PATH)
if not os.path.exists(dirname): 
    os.makedirs(dirname)
    print(WARNING + "Download Folder is created" + ENDC)


def startPrint(imageName,finalRun,requestNumber):
    if finalRun == 1:
        print(OKGREEN + "########Request number "+str(requestNumber)+" Started, It's the final request" + ENDC)
    else :
        print(OKGREEN + "########Request number "+str(requestNumber)+" Started, It's not the final request" + ENDC)                 

    #Start Image Download
    print(OKGREEN +"Current Image Handeld: "+imageName+ ENDC)
    current_GMT = time.gmtime()
    downloadedImageName = str(calendar.timegm(current_GMT))
    storage.child("toPrint/"+imageName).download(IMAGE_DOWNLOAD_PATH,IMAGE_DOWNLOAD_PATH+downloadedImageName+".jpg")
    print(OKCYAN + "Image Downloaded successfully, ImageName: "+downloadedImageName+".jpg"+ " Download Path: " +IMAGE_DOWNLOAD_PATH+downloadedImageName+".jpg"+ ENDC)     
    
    #Start Image Delete from server
    blob = bucket.blob("toPrint/"+imageName)
    if blob.exists():
        try:
            blob.delete()
            print(WARNING + "Server Image Deleted successfully"+ ENDC)
        except:
            print("An exception occurred while deleting file from firebase")
    else:
        print(WARNING + "Server Image Wasent Deleted,File does not exist"+ ENDC)

    #Start running print commands   
    if debug == True:
            commands = ['echo Debug is turned on this command is just for testing']
    else:
        commands = ['/usr/bin/convert '+IMAGE_DOWNLOAD_PATH+downloadedImageName+'.jpg -quality 80 -gravity center -crop 3:2 '+IMAGE_DOWNLOAD_PATH+downloadedImageName+'_2.jpg && /usr/bin/lp -o raw -o media=Custom.106x152mm -o fit-to-page '+IMAGE_DOWNLOAD_PATH+downloadedImageName+'_2.jpg']
    processes = [Popen(cmd,shell=True) for cmd in commands]
    for p in processes: 
        p.wait()


    if os.path.exists(IMAGE_DOWNLOAD_PATH+downloadedImageName+".jpg"):
        os.remove(IMAGE_DOWNLOAD_PATH+downloadedImageName+".jpg")
        print(WARNING + "Local Image Deleted successfully"+ ENDC)


    #Update Firebase List
    localRef = db.reference("/")
    imagesRef = db.reference("printer"+PRINTER_ID+"Data")
    imagesStr = str(imagesRef.get())
    if debug == True:
        print(OKGREEN + "Downloaded Image List: "+imagesStr+ ENDC) 
    imagesStr = imagesStr.replace(","+imageName, "")
    imagesStr = imagesStr.replace(imageName+",", "")
    imagesStr = imagesStr.replace(imageName, "")
    if debug == True:
        print(OKGREEN + "Uploaded Image List: "+imagesStr+ ENDC) 
    localRef.update({"printer"+str(PRINTER_ID)+"Data":imagesStr})
    print(OKGREEN + "Printer Firebase Images List Updated" + ENDC)

    #Update request number in DB
    imagesArrayLen = len(str(db.reference("printer"+PRINTER_ID+"Data").get()).split(","))
    if imagesArrayLen > 1:
        localRef.update({"printer"+str(PRINTER_ID)+"Requests":str(imagesArrayLen)}) 
        print(OKGREEN + "Printer Firebase Request Number Updated to "+str(imagesArrayLen) + ENDC) 
    elif imagesArrayLen == 1:
        if str(db.reference("printer"+PRINTER_ID+"Data").get()) == "":
            localRef.update({"printer"+str(PRINTER_ID)+"Requests":"done"})
            print(OKGREEN + "Printer Firebase Request Number Updated to Done" + ENDC)
        else:
            localRef.update({"printer"+str(PRINTER_ID)+"Requests":"1"})
            print(OKGREEN + "Printer Firebase Request Number Updated to Done" + ENDC)

    print(WARNING + "Request number "+str(requestNumber)+" Ended" + ENDC)  

def checkForRequests():
    print("checkForRequests Called")
    if str(requestRef.get()) != "done":
            print(str(WARNING) + "Starting "+ str(requestRef.get()) + " Requests" + ENDC) 
            imagesArray2 = str(db.reference("printer"+PRINTER_ID+"Data").get()).split(",") 
            whileLoopText = requestRef.get()
            i = 0 
            if len(imagesArray2) >= int(requestRef.get()) :
                while whileLoopText != "done":
                    imagesArray2 = str(db.reference("printer"+PRINTER_ID+"Data").get()).split(",") 
                    if debug == True:
                            print("imagesArray: "+str(dataRef.get()))
                    if len(imagesArray2[0]) > 3: 
                        if debug == True:
                            print("imagesArray2Size: "+str(len(imagesArray2)) + " I: "+str(i))
                        if len(imagesArray2) == 1:
                            startPrint(imagesArray2[0],1,i)
                        else :
                            startPrint(imagesArray2[0],0,i)
                        whileLoopText = requestRef.get()
                        i+=1
                        imagesArray2 = str(db.reference("printer"+PRINTER_ID+"Data").get()).split(",")  
                    else:
                        print("There's an OverFlow Fixed")
                        localRef.update({"printer"+str(PRINTER_ID)+"Requests":"done"})
                        whileLoopText = requestRef.get()           
            else:
                localRef.update({"printer"+str(PRINTER_ID)+"Requests":str(len(imagesArray2))})
                print("Unknowwn else called: "+str(db.reference("printer"+PRINTER_ID+"Data").get())+" e: "+str(db.reference("printer"+PRINTER_ID+"Requests").get()))
    if(timerCheck == True):
        s.enter(60, 1, checkForRequests, ())                

#Setup timer
if(timerCheck == True):
    s.enter(60, 1, checkForRequests, ())
    s.run()
else:
    ##Check Old Requests
    checkForRequests()

      
