FROM ubuntu:14.04
FROM openjdk:8u131

RUN \
  apt-get clean && apt-get update && apt-get install -y --no-install-recommends software-properties-common \
  && apt-get update 

RUN echo Asia/Kolkata |  tee /etc/timezone &&  dpkg-reconfigure --frontend noninteractive tzdata

EXPOSE 8080
EXPOSE 8081

VOLUME /var/log/primer

ADD config/docker/config.yml config.yml
ADD target/primer*.jar primer-service.jar

CMD sh -c "sleep 15; java -jar -XX:+${GC_ALGO-UseG1GC} -Xms${JAVA_PROCESS_MIN_HEAP-1g} -Xmx${JAVA_PROCESS_MAX_HEAP-1g} primer-service.jar server config.yml"
