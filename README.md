## Synopsis
Dingo allows to install and execute remotely on one or more machines a software of choice. 

After the software has been installed, Dingo will keep all the machines in the computer cluster synchronized, propagating file changes.
This allows the user to distribute computation and take advantage of the computational power of multiple machines. 

Dingo uses SSH-2 to encrypt the data transmitted over the network.

##Terminology
Dingo is made of two main components: the **Server** and the **Terminal**.

The **Server** runs only on one machine in the cluster. Changes made to this machine will be automatically propagated to the other computers in the cluster. 

The only way to send commands to the **Server** is by using the **Terminal**. The **Terminal** is a command-line applicatoin that allows a user to connect to the **Server** and perform operations by sending syntactically correct commands.
Examples of these operations are adding or removing a new computer to the computing cluster. 

In the context of Dingo, the software that is ran on the computers in the cluster is called **Bolt**.

##Configuration
One settings file is available and allows to configure Dingo, it is placed under settings/dingo.properties.

> COMMAND_INTERFACE_PORT

Port on which a terminal can connect to send commands.

> BASE_DIR

Base directory on the server to look for changes, the path starts from root (/). Dingo looks for changes recursively.

> BOLT_ARCHIVE

Path to the archive file used to install Bolt on other machines, path starts from root (/).

##Software Requirements
**Java 1.7 or greater** The project needs a Java version >= 7 because the thread that is responsible of detecting file changes depends on WatchService API, that has been introduced in this version.

[Jsch 1.48](http://www.jcraft.com/jsch/) needed for Ssh communication.

**SSH Server** SSH-2 is needed. Every SSH Server should be fine, I used [openSSH](http://www.openssh.com/).

At the moment Dingo supports only machines running a GNU/Linux operating system.

##Scenarios
A machine in the cluster is identified by:
- IP address
- Port number
- Username

The whole application has been developed in order to achieve high performance with a high number of machines in the cluster. 
Therefore, the application is highly multi-threaded.

Typing

    help 
shows available commands.

###Connection to the Server
To send commands, the user should use the **Terminal**. **Terminal** has to connect to the **Server** before sending commands. 
The connection port is specified in Dingo configuration file. Typing

    connect IP_address port_number

connects to the **Server**. Dingo creates a specific thread for every communication channel, therefore supporting multiple **Terminals** concurrently. 

###Adding a machine to the cloud
In order to add a new machine, the user has to specify:
- UserName on the remote machine
- IP address
- port (optional, default is 22)
- remote directory where **Bolt** should be installed

and execute the command:

    add_bolt username@host[:port] remote_directory

If password is necessary to access the remote machine, it will be asked.

After the machine is added to the computing cluster, it will be kept updated by with the **Server** machine.

###Removing a machine
Command

    remove_bolt username@host

Stops execution of **Bolt** and removes all files.

###Removing all machines from the cluster
    remove_all_bolts

It's the same as above, with the only exception that all machines are removed.

###Shutting down the server
    server_shutdown

This commands shuts down the entire **Server**, taking care of removing all running **Bolt** instances.