FROM openjdk:11-jre
RUN mkdir app
ADD database-streamer.jar /app/database-streamer.jar
WORKDIR /app
ENTRYPOINT java -jar database-streamer.jar