import os
import paramiko

transport = paramiko.Transport((os.environ["SERVER_HOST"], 22))
transport.connect(username=os.environ["SERVER_USER"], password=os.environ["SERVER_PASS"])
sftp = paramiko.SFTPClient.from_transport(transport)
try:
    sftp.mkdir("/root/web")
except IOError:
    pass
sftp.put("build/libs/" + os.listdir("build/libs/")[0], "/root/web/server.jar")
sftp.close()