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

ssh_client = paramiko.SSHClient()
ssh_client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh_client.connect(hostname=os.environ["SERVER_HOST"],username=os.environ["SERVER_USER"],password=os.environ["SERVER_PASS"])
ssh_client.exec_command("killall java")
ssh_client.exec_command("cd /root/web")
ssh_client.exec_command("java -jar server.jar > server.log 2>&1 &")