Refer for gradle plugin doc:

https://gatling.io/docs/current/extensions/gradle_plugin/ 

- To run the simulation use command
``` ./gradlew clean gatlingRun -DHOST=https://uat01.socashapp.io ```

- To run the simulation with number of users and duration use command line

``` ./gradlew clean gatlingRun -DHOST=https://uat01.socashapp.io -DUsers=2 -DDuration=20 ```