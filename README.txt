COSA-IDA : Comprehensive Static Analyzer For Android Application (COSA with IDA Pro)

Author : Sangwon Lee (sangwon.lee@usc.edu, itools@gmail.com)

COSA-IDA is a static analyzer that detects system calls, JNI calls and memory usage pattern without actual execution.

Instead, it unpacks an Android Pakcage file and disassembles Dalvik exeuction(DEX) and NDK binary codes then it creates a call graph and traces native method calls.

For tracing native method calls, it uses virtual memory space and interprets each disasembled ARM instruction.

The analyzer emulates more than 100 system calls and JNI method calls. 

Specially, it tracks memory usage pattern for global variables, so that the memory will be uploaded for computation offloading. 

Moreover, the collected information will be used to detect malware and other security purpose. 

To compile it, you need addtional java packages including Soot framework and IDA pro. 

Plase, check the sample out file.


