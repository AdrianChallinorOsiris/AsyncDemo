# asyncDemo
A demonstration of asynchronous code vs streams. 

What I did was write a Spring Boot program to first compute the size of all the files in a directory. I did 
this twice, once using a stream and allowing that to go asynchonous internally. 

The second part is much more complex. I used the same directory, but now I have asynchronous processing and multiple threads. Thread 1 
is for all the files in the directory. Then there are multiple additional threads, one for each directory found.  Once 
all the threads have been created (they are called "Futures" because they will return some time in the future) you have 
to wait until they are all complete, so the main programme thread will sleep until all threads have completed. 


On my system, it took 2.2 seconds working synchronously, but virtually no time at all for the threaded, asynchronous 
time. Now, this is NOT scientific, because my Linux system and its high speed NVMe SSDs will cache alot of this information. 


But I am sure you can work out how to fit your code in to the methods I have used. I deliberately made all my 
directory processing in to methods. You can replace all my directory walking nonsense with your proper code. 


