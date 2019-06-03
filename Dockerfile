FROM openjdk:8

WORKDIR /app
COPY ./target/universal/imguruploads-1.0-SNAPSHOT/ /app
EXPOSE 9000
CMD ./bin/imguruploads