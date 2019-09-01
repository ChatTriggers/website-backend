import os
import paramiko

transport = paramiko.Transport((os.environ["SERVER_HOST"], 22))
transport.connect(username=os.environ["SERVER_USER"], password=os.environ["SERVER_PASS"])
sftp = paramiko.SFTPClient.from_transport(transport)
sftp.mkdir("/root/web", ignore_existing=True)
sftp.put("build/libs/" + os.listdir("build/libs/")[0], "/root/web/server.jar")
sftp.close()