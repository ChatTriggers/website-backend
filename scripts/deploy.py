import os
import paramiko
import sys

if os.environ["TRAVIS_REPO_SLUG"] != "ChatTriggers/website-backend" or os.environ["TRAVIS_PULL_REQUEST"] != "false" or os.environ["TRAVIS_BRANCH"] != "master":
    sys.exit()

transport = paramiko.Transport((os.environ["SERVER_HOST"], 22))
transport.connect(username=os.environ["SERVER_USER"], password=os.environ["SERVER_PASS"])
sftp = paramiko.SFTPClient.from_transport(transport)

sftp.put("build/libs/" + os.listdir("build/libs/")[0], "/root/web/server.jar")
sftp.close()

ssh_client = paramiko.SSHClient()
ssh_client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh_client.connect(hostname=os.environ["SERVER_HOST"],username=os.environ["SERVER_USER"],password=os.environ["SERVER_PASS"])

stdin, stdout, stderr = ssh_client.exec_command("/root/web/restart.sh")  # Non-blocking call
stdout.channel.recv_exit_status()

ssh_client.close()