-----------------------------------------

Copyright 2012 Yahoo! Inc. Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by 
applicable law or agreed to in writing, software distributed under the License 
is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
either express or implied. See the License for the specific language governing 
permissions and limitations under the License. See accompanying LICENSE file.

------------ Version 1 NOTES ---------------
- Inspired from BigTable paper[1] description about  Bentley and McIlroy’s long string compression scheme[2].

[1] - http://static.googleusercontent.com/media/research.google.com/en/us/archive/bigtable-osdi06.pdf
[2] - http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.11.8470&rep=rep1&type=pdf


- JITed version maximum speed
	Encoding - 35MB/s
	Decoding - 330MB/s
 The speeds reported above are due to tests with following parameters
	a) 3Gz dual core iMac (8GB RAM) with 1GB allotted to the VM(JDK 1.8 preview).
	b) Block length = 32
	c) UTF-8 text file sizes 150KB and 7MB.

 The speed changes when block length changes(smaller block lengths decrease the speed, larger block length decreases compression).
- There is scope for speed improvement which is the target for next version as this version the target was to produce correct enc/dec.

- There is a slight twist to the original algorithm in the implementation which is when repeated string occur together then still they are chained and represented using <n,m> notation but for the decoder to decipher this properly for such cases the encoding is <n,m|> instead.

Related Bugs in the open:

HBASE  - https://issues.apache.org/jira/browse/HBASE-2655
Hadoop - https://issues.apache.org/jira/browse/HADOOP-5793
