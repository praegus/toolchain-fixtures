[![Maven Central](https://img.shields.io/maven-central/v/nl.praegus/toolchain-fixtures.svg?maxAge=21600)](https://mvnrepository.com/artifact/nl.praegus/toolchain-fixtures)
# Toolchain-fixtures
A set of test fixtures for use with FitNesse (combined with [hsac-fitnesse-fixtures](https://github.com/fhoeben/hsac-fitnesse-fixtures))

## Fixtures
### Pause test fixture
Intended as a library used for debugging a test object while running FitNesse Scripts.
Pauses the test and prompts the user to continue or abort.

### CSV File Fixture
An extension to HSAC's File fixture to work with CSV files. Allowing to treat these files as tables and get rows as key/value pairs in a hashmap.

### GMail OAuth fixture
A fixture to interact with the GMail API. Can be used to fetch, filter, thrash, delete and send email.
Also allows for extraction of i.e. activation links or the complete message as plain text or HTML.

### Image Compare Fixture
A fixture to compare images and display difference heatmaps.
Uses Galen Framework's compare library to allow for accepted differences/tolerance

### PDF Compare fixture
A Fixture to compare PDF files and display difference maps.
Uses RedSix's compare library to be able to exclude certain area's from comparison (useful to exclude page numbers, dates, etc) and to be able to define a comparison tolerance.

### SFTP Fixture
A fixture to interact with a SFTP server. Upload/download/poll/delete/chmod are supported

### Toolchain File fixture
An extension to HSAC's file fixture that allows polling for files and find files that match a regex pattern.

### XHR Test
A fixture that creates a browsermob proxy to use with BrowserTest and allows to get HAR data from the session after the browsertest has completed.

### Mendix HTTP Test
A fixture to easily interact with the /xas endpoint of Mendix web applications. Allowing for browserless data retrieval from Mendix apps.

### Mendix Browser Test
A set of helper methods for BrowserTest that make interacting with Mendix Apps easier.

### Praegus Browser Test
A set of methods for BrowserTest with added functionality for loaders, blur after sending text, stop test on exceptions and improving browser resize.

### TOTP fixture
A fixture the generates Time-based One Time Password, just like Google Authenticator does. 
Based on [WStranges GoogleAuth code](https://github.com/wstrange/GoogleAuth)

### Excel File Fixture
A fixture to read/write String data from/to xls files.

### Calculator
A fixture that calculates the result of mathematical expression (input as String)
Returns whole numbers by default, precision can be added to the constructor. 

### Mock server
A Fixture that allows advanced mocking, proxying and forwarding for HTTP(S)

## Other extra's

### Console log listener
A Junit Listener for HSAC projects that outputs the test results as wiki tables in ansi color to the console.
###### Example:
![Console output example](https://raw.githubusercontent.com/tcnh/binstore/master/console.PNG)

### Toolchain Test Runner
A version of HsacFitnesseRunner that sets nodebug (to stop testing when a pause command is executed) and uses the console log listener.

