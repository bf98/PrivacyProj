@ECHO OFF
docker exec my_jenkins cat /var/jenkins_home/secrets/initialAdminPassword
PAUSE
