# FROM jenkins/jenkins:latest
# USER root
# RUN apt-get update && apt-get install -y maven iputils-ping
# RUN chmod u+s /bin/ping
# USER jenkins

FROM jenkins/jenkins:latest

USER root

RUN apt-get update && \
    apt-get install -y maven iputils-ping wget unzip && \
    wget https://repo1.maven.org/maven2/com/github/spotbugs/spotbugs/4.9.8/spotbugs-4.9.8.zip && \
    unzip spotbugs-4.9.8.zip -d /opt/ && \
    rm spotbugs-4.9.8.zip

RUN chmod u+s /bin/ping

ENV PATH="/opt/spotbugs-4.9.8/bin:${PATH}"

USER jenkins

