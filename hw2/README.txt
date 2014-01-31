Christopher Mersman
ASG2 README



Part 1:
	Before you do any testing or running, make sure you are within the build class after doing the 'ant' command

	To run part 1 you start by creating a register node:
		java cs455.cdn.node.Registry <registry_port> <refresh_interval>

		registry_port is the port you want the registry's serverSocketChannel listeningon
		refresh_interval is the amount of time (in milliseconds) between each edge weight updates after the cdn has been established.

	an example would be
		java cs455.cdn.node.Registry 2004 5000

		This sets up a Registry on port 2004 that will refresh every 5 seconds


	The next thing you have to do is create messaging nodes:
		java cs455.cdn.node.MessagingNode <node_id> <reg_address> <reg_port> <thread_count>

		node_id is the unique identification for that node. (must be unique)
		reg_address is the address of the registry
		reg_port is the port of the registry
		thread_count is the amount of threads initialized (but this only for part 2)

	an example would be
		java cs455.cdn.node.MessagingNode A 129.82.28.124 2004 5

		This will set a messaging node with the id of "A" register to the address 129.82.28.124:2004 with 5 threads.


	With this you can register as many nodes as you want, but it you want the connections to be sufficient for when you run setup-cdn then I would stick with 10 nodes with 4 connections. As far as I know, everything works for part 1 of the assignment.

	The only thing that I did not account for part one is that when sending a file, you have a path within the file name. The file must be in the same directory as the directory cs455.







Part 2:
	To run part 2 you start by creating a Node without a registry for it to register to.
	
	I was a little confused on how you guys wanted us to do this so this is how I've implemented it.
	You run a MessagingNode the same as you would before but for it to go into Part 2 mode, there must NOT be a registry for it to register to.

	As for the Client node:
		java cs455.cdn.node.Client <node_addr> <node_port> <amt>

		node_addr is the node address
		node_port is the node port
		amt is the amout of messages per second for the specific Client node.

	an example would be
		java cs455.cdn.node.Client 129.22.44.128 35000 2

		This will set up a Client to send 2 random byte[4096] every second to 129.22.44.128:35000

	The big problem with part 2 is when you have different clients set up with different packet speeds, then sometimes you will loose packets all over. 

	As for the output of this (which can be confusing) but a client will send and if the same thing that it sent and the hash it recieves is successfully deleted from the LinkedList, then the Client prints a simple Success or a simple nope (for a false). As for the MessagingNode, it prints out everytime a new task is taken in from the thread pool to a thread and shows the amount of threads that are working.








Conclusion:

	This was a very difficult assignment and with all that was going one during the time alloted, I felt that we could of used a little extension. I know that the department frowns on homework over a break, but I would have been glad to do it over break, especially because I really enjoy this class. But with all the projects, tests, papers, and life that was going on outside of 455 (or even inside 455) I feel that I could of done a lot more if I had a little more time. I would also say that I did not sluff off on this assignment at all. Whenever I had time I took too it to get stuff done. (Not to be dramatic or anything but) I even stayed over night in the lab to get a good ten hours in extra to see if I could make a dent in it.
