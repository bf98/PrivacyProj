FROM jenkins/jenkins:latest
USER root
RUN apt-get update && apt-get install -y maven iputils-ping
RUN chmod u+s /bin/ping
USER jenkins
