# github-org-utility-webapp
A webapp that provides several utility functions for managing a github organization
=======


Requires Java 1.8, and Maven (`mvn` command)

To deploy

One time steps:

1.  *One time step*: Register a Github OAuth application at
    <https://github.com/settings/applications/new> to obtain
    values for GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET
   
2.  *One time step*:
    Copy `env.sh.SAMPLE` to `env.sh`, and edit `env.sh`
    to provide correct values
    for GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET.  For APPLICATION_SALT,
    enter a long random string of letters and digits (20 or more).
    For GITHUB_CALLBACK_URL, enter `http://localhost:4567/callback`.
    Substitute the actual hostname/port on the server on which you are
    running,
    if appropriate.   Note that `env.sh` is ignored in the `.gitignore`
    for the repo, because these values should be kept secret.

3.  *Each time you are running*: Use the following command to load the
    environment variables into your process:

    ```
    . env.sh
    ```

4.  *To compile code* each time you make changes, run:

    ```
    mvn package
    ```

5.  *To start server at command line*, after `mvn package`, use:

    ```
    java -jar target/github-org-utility-webapp-1.0.jar
    ```

The command `mvn clean` can be used if needed (equivalent of `make clean`
or `ant clean`) to remove temporary files and reset the project to get a clean
compile.

# References:

Some helpful references can be found here:

* https://github.com/pac4j/spark-pac4j
* https://github.com/pac4j/spark-pac4j-demo
* https://github.com/pconrad-webapps/sparkjava-pac4j-github-oauth-demo 
* https://github.com/kohsuke/github-api

