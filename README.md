This source is a derivative work of the Independent JPEG Group (IJG) decompression librray, the c-source was made available under the terms contained in the file ijg_license.txt

the project supports the full 8-, 10-, 12-, 16-bit lossy and lossless codecs

Due to the nature of C and Java, the following changes were made when porting the code:
* pointers were replaced with indexing
* function pointers replaced with switch statements
* structs became objects objects, including abstract classes and inheritance
* error handling became Java exception based, though error messages were maintained
* virtual memory, tracing and progress monitoring were removed

The author recommends cloning into IntelliJ as a maven project, and executing the maven build cycle. Main.java is intended to demonstrate how to invoke the library. 
