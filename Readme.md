Requirements:
* docker and sbt should be installed on the machine

Build Steps:
* Download the project
* Open command line terminal and execute following commands one by one.
    ```
    1. cd /path/to/imguruploads
    2. sbt universal:packageZipTarball
    3. cd imguruploads/target/universal/
    4. tar zxvf imguruploads-{version}-SNAPSHOT.tgz
    5. cd ../../
    6. docker build -t {image_name} ./
    ```
    
To run the application execute:
```
 docker run -d --name {container_name} -p 9000:9000 {image_name}
```

To upload image use following api:
```
Method: POST
Host: http://localhost:9000/v1/images/upload
Header: 'Content-Type: application/json'
Body: 
    {
       "query" : "query ImagesUpload($urls: [String!]!) {jobId(urls:$urls)}",
       "variables": {
         "urls": [
           "https://homepages.cae.wisc.edu/~ece533/images/airplane.png", 
           "https://homepages.cae.wisc.edu/~ece533/images/arctichare.png",
           "https://homepages.cae.wisc.edu/~ece533/images/baboon.png"
         ]
       }
    }
```
    
To check the application logs, execute:
* `docker logs {container_name}`

To stop the application, execute:
* `docker stop {container_name}`
* `docker rm {container_name}`



