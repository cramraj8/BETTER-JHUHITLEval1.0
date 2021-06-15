
* we place JAR file in the /core/ folder. But if the page is not responsive, then we don't have to place the JAR file to do any IR tasks.

1. main.py - main python script (server-site script) that runs python-flask server & handle all GET & SET requests
2. templates/index.html - main index.html page that will be shown to the user
3. static/css/index-ui.css - the necessary CSS content for templates/index.html UI
4. static/js/index-ui.js - the main javascript file that does client-site functions for responsive page of templates/index.html UI
5. file-transfers/ - contains any input, output, or intermediate-files to the python scripts to invoke
6. core/required-files/ - contains auxiliary files necessary for the baisc-IR's JAR file