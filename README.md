Multicast-Routing-Protocol-using-OSPF
=====================================

Simulated a Link-state Multicast Routing Protocol with multiple senders and receivers using socket programming and multithreading in Java.


The topology file will look like this (By the way, the topology file is never changed
during a simulation)
0 4
4 0
4 5
5 4
5 9
9 5
0 8
8 0
0 3
3 5
5 3
3 9
1 3
3 1


Execute command like these:
>controller &
>node 4 &
>node 5 &
>node 6 &
>node 9 receiver 0 &
>node 0 sender “this is node 0 multicast message” &
