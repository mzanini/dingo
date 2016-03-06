package dingo.terminal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

import commons.FunnyThings;

import dingo.CommandProtocol;


public class DingoTerminal implements Runnable
{
	private static final String PROMPT = ">>> ";
	private static final String NAME = "Dingo Terminal";
	private static final String VERSION = "Version 0.01";
	
	private Socket socket;
	private PrintStream out;
 	private BufferedReader in;
 	private boolean connected = false;
	private String host;
	private int port;
	
	public static void main(String[] args)
	{
		FunnyThings.printDingo();
		
		DingoTerminal ht = new DingoTerminal();
		ht.run();
	}
	
	public void run()
	{
		System.out.println(DingoTerminal.NAME + " " + DingoTerminal.VERSION + "\n");
		BufferedReader sourceReader = new BufferedReader(new InputStreamReader(System.in));
	    String commandString = "";
	    try 
	    {
	    	boolean iterate = true;
	    	do 
	    	{
		        System.out.print(DingoTerminal.PROMPT);
	    		commandString = sourceReader.readLine();
	    		String[] tokens = commandString.split(" ");
	    		boolean recognizedCommand = false;
	    		
	    		if (tokens[0].equalsIgnoreCase(CommandProtocol.COMMAND_VER))
	    		{
	    			recognizedCommand = true;
	    			System.out.println(DingoTerminal.NAME + " " + DingoTerminal.VERSION + ".");
	    		}
	    		if (tokens[0].equalsIgnoreCase(CommandProtocol.COMMAND_EXIT))
	    		{
	    			recognizedCommand = true;
	    			iterate = false;
	    			System.out.println("Bye.\n");
	    		}
	    		if (tokens[0].equalsIgnoreCase(CommandProtocol.COMMAND_CONNECT))
	    		{
	    			recognizedCommand = true;
	    			this.handleCommandConnect(tokens);
	    		}
	    		if (tokens[0].equalsIgnoreCase(CommandProtocol.COMMAND_DISCONNECT))
	    		{
	    			recognizedCommand = true;
	    			this.handleCommandDisconnect();
	    		}
	    		if (tokens[0].equalsIgnoreCase(CommandProtocol.COMMAND_STATUS))
	    		{
	    			recognizedCommand = true;
	    			this.handleCommandStatus();
	    		}
	    		if (tokens[0].equalsIgnoreCase(CommandProtocol.COMMAND_HELP))
	    		{
	    			recognizedCommand = true;
	    			this.handleCommandHelp();
	    		}
	    		if (tokens[0].equalsIgnoreCase(CommandProtocol.COMMAND_SAY))
	    		{
	    			recognizedCommand = true;
	    			this.handleCommandSay(tokens);
	    		}
	    		if (tokens[0].equalsIgnoreCase(CommandProtocol.COMMAND_SERVER_SHUTDOWN))
	    		{
	    			recognizedCommand = true;
	    			this.handleCommandShutdown();
	    		}
	    		if (tokens[0].equalsIgnoreCase(CommandProtocol.COMMAND_SERVER_STATUS))
	    		{
	    			recognizedCommand = true;
	    			this.handleCommandServerStatus();
	    		}
	    		if (tokens[0].equalsIgnoreCase(CommandProtocol.COMMAND_DIR_CONTENT))
	    		{
	    			recognizedCommand = true;
	    			this.handleCommandDirContent();
	    		}
	    		if (tokens[0].equalsIgnoreCase(CommandProtocol.COMMAND_CONNECT_MACHINE))
	    		{
	    			recognizedCommand = true;
	    			this.handleCommandConnectMachine(tokens);
	    		}
	    		if (tokens[0].equalsIgnoreCase(CommandProtocol.COMMAND_ADD_BOLT))
	    		{
	    			recognizedCommand = true;
	    			this.handleCommandAddBolt(tokens);
	    		}
	    		if (tokens[0].equalsIgnoreCase(CommandProtocol.COMMAND_REMOVE_BOLT))
	    		{
    				recognizedCommand = true;
    				this.handleCommandRemoveBolt(tokens);
	    		} 
	    		if (tokens[0].equalsIgnoreCase(CommandProtocol.COMMAND_REMOVE_ALL_BOLTS))
	    		{
    				recognizedCommand = true;
    				this.handleCommandRemoveAllBolt(tokens);
	    		}
	    		if (!recognizedCommand) System.out.println("[DT] Unrecognized command."); 
		    } 
	    	while (iterate);
	    }
	    catch(IOException e) {e.printStackTrace();}
	}
	
	private void handleCommandServerStatus()
	{
		if (!this.connected) System.out.println("Not connected.");
		else
		{
			this.sendMessage(CommandProtocol.MESSAGE_SERVER_STATUS);
			String s = "";
			do
			{
				do {s = this.receiveMessage();} while(s == null || s.equals(""));
				if ( !s.equalsIgnoreCase(CommandProtocol.MESSAGE_OK) ) System.out.println(s);
			}
			while ( !s.equalsIgnoreCase(CommandProtocol.MESSAGE_OK) );
		}
	}
	
	private void handleCommandShutdown()
	{
		if (!this.connected) System.out.println("Not connected.");
		else
		{
			try
			{
				this.sendMessage(CommandProtocol.MESSAGE_SERVER_SHUTDOWN);
				String s = "";
				do {s = this.receiveMessage();} while(!s.startsWith(CommandProtocol.MESSAGE_OK));
				this.in.close();
		 		this.out.close();
		 		this.socket.close();
		 		this.connected = false;
		 		System.out.println("Disconnected.");
			}
			catch(IOException e) {System.out.println("I/O Error.");}
		}
	}
	
	
	private void handleCommandSay(String[] args)
	{
		if (!this.connected) System.out.println("Not connected.");
		else
		{
			if (args.length == 2)
			{
				String message = args[1].replaceAll("\"", "");
				this.sendMessage(message);
			}
			else System.out.println("Invalid arguments.");
		}
	}
	
	private void handleCommandDisconnect()
	{
		if (!this.connected) System.out.println("Not connected.");
		else
		{
			try
	 		{
		 		this.sendMessage(CommandProtocol.MESSAGE_BYE);
		 		String s = "";
		 		do {s = this.receiveMessage();} while(!s.startsWith(CommandProtocol.MESSAGE_OK));
				this.in.close();
		 		this.out.close();
		 		this.socket.close();
		 		this.connected = false;
		 		System.out.println("Disconnected.");
	 		}
	 		catch(IOException e) {System.out.println("I/O Error.");}
		}
	}
	
	private void handleCommandHelp()
	{
		System.out.println("CONNECT host port         Connects to a specified host and port.");
		System.out.println("DISCONNECT       Disconnects an active connection.");
		System.out.println("SERVER_STATUS    Provides info on HyaenaServer status.");
		System.out.println("SERVER_SHUTDOWN  Terminates the server and closes the active connection.");
		System.out.println("SAY string             Sends to the server the specified string.");
		System.out.println("ADD_BOLT username@host[:port] remote_directory         Adds the specified Bolt machine to the cloud.");
		System.out.println("REMOVE_BOLT username@host     Removes the specified Bolt machine from the cloud.");
		System.out.println("REMOVE_ALL_BOLTS  Removes all remote machines from the cluster");
		System.out.println("STATUS           Provides info on " + DingoTerminal.NAME + " connection status.");
		System.out.println("VER              Prints " + DingoTerminal.NAME + " version.");
		System.out.println("EXIT             Quits " + DingoTerminal.NAME + ".");
	}
	
	private void handleCommandStatus()
	{
		if (this.connected) 
			System.out.println("Connected to [" + this.host + ":" + this.port + "]");
		else 
			System.out.println("Not connected.");
	}
	
	private void handleCommandConnect(String[] args)
	{
		if (args.length == 3)
		{
			this.host = args[1];
			this.port = 0;
			try
			{
				this.port = Integer.parseInt(args[2]);
				try
				{
					System.out.println("Connecting to " + this.host + ":" + this.port + "...");
					this.socket = new Socket(this.host, this.port);
					try
					{
						this.out = new PrintStream(this.socket.getOutputStream());
						this.out.flush();
						this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
						String s = "";
						//Waiting for MESSAGE HI from Server
						do {s = this.receiveMessage();} while(!s.startsWith(CommandProtocol.MESSAGE_HI));
						this.connected = true;
						System.out.println("Connected to [" + this.host + ":" + this.port + "]");
					}
					catch(NullPointerException e) {System.out.println("Connection timed out.");}
				}
				catch(UnknownHostException e) {System.out.println("Unable to reach host " + host);}
				catch(IOException e) {System.out.println("I/O Error.");}
			}
			catch(NumberFormatException e) {System.out.println("Invalid port number.");}
		}
		else 
			System.out.println("Invalid arguments.");
	}
	
	private void handleCommandDirContent()
	{
		if (!this.connected) 
			System.out.println("Not connected.");
		else {
			System.out.println("I am sending the message dir_content");
			this.sendMessage(CommandProtocol.COMMAND_DIR_CONTENT);
			String s = "";
			do
			{
				do {s = this.receiveMessage();} while(s == null || s.equals(""));
				if ( !s.equalsIgnoreCase(CommandProtocol.MESSAGE_OK) ) System.out.println(s);
			}
			while ( !s.equalsIgnoreCase(CommandProtocol.MESSAGE_OK) );
		}
	}
	
	private void handleCommandConnectMachine(String[] args){
		if (!this.connected) 
			System.out.println("[DT] Not connected.");
		else {
			if (args.length == 3)
			{	
				String message = args[0] + " " + args[1] + " " + args[2];
				System.out.println("[DT] I am sending the message connect_machine");
				this.sendMessage(message);
			}
			else 
				System.out.println("[DT] Invalid arguments.");
			
			String s = "";
			do
			{
				do {s = this.receiveMessage();} while(s == null || s.equals(""));
				if ( !s.equalsIgnoreCase(CommandProtocol.MESSAGE_OK) ) System.out.println(s);
			}
			while ( !s.equalsIgnoreCase(CommandProtocol.MESSAGE_OK) );
		}
	}
	
	private void handleCommandAddBolt(String[] args){
		if (!this.connected) 
			System.out.println("[DT] Not connected.");
		else {
			if (args.length == 3) {	
				System.out.println("[DT] I am sending the message add_c3po");
				String message = args[0] + " " + args[1] + " " + args[2];
				this.sendMessage(message);
				String s;
				do{ s = this.receiveMessage(); } 
					while(!s.equalsIgnoreCase(CommandProtocol.MESSAGE_OK) && !s.equalsIgnoreCase(CommandProtocol.MESSAGE_KO));
				if ( s.equalsIgnoreCase(CommandProtocol.MESSAGE_OK) )
					System.out.println ( "[DT] Added instance of Bolt." );
				else if ( s.equalsIgnoreCase(CommandProtocol.MESSAGE_KO) )
					System.out.println ( "[DT] Failure during the addition of the Bolt instances." );
			} else 
				System.out.println("[DT] Usage: add_c3po user@host[:port] remote_directory_to_create");
		}
	}
	
	private void handleCommandRemoveBolt(String[] args){
		if (!this.connected) 
			System.out.println("[DT] Not connected.");
		else {
			if (args.length == 2) {	
				System.out.println("[DT] I am sending the message remove_c3po");
				String message = args[0] + " " + args[1];
				this.sendMessage(message);
				String s;
				do{ s = this.receiveMessage(); } 
					while(!s.equalsIgnoreCase(CommandProtocol.MESSAGE_OK) && !s.equalsIgnoreCase(CommandProtocol.MESSAGE_KO));
				if ( s.equalsIgnoreCase(CommandProtocol.MESSAGE_OK) )
					System.out.println ( "[DT] Bolt instance removed." );
				else if ( s.equalsIgnoreCase(CommandProtocol.MESSAGE_KO) )
					System.out.println ( "[DT] Failure during the removal of the Bolt instance." );
			}
			else 
				System.out.println("[DT] Usage: remove_c3po user@host[:port]");
		}
	}
	
	
	private void handleCommandRemoveAllBolt(String[] args){
		if (!this.connected) 
			System.out.println("[DT] Not connected.");
		else {
			if (args.length == 1) {	
				System.out.println("[DT] I am sending the message remove_all_c3po");
				String message = args[0];
				this.sendMessage(message);
				String s;
				do{ s = this.receiveMessage(); } 
					while(!s.equalsIgnoreCase(CommandProtocol.MESSAGE_OK) && !s.equalsIgnoreCase(CommandProtocol.MESSAGE_KO));
				if ( s.equalsIgnoreCase(CommandProtocol.MESSAGE_OK) )
					System.out.println ( "[DT] All Bolt instances removed." );
				else if ( s.equalsIgnoreCase(CommandProtocol.MESSAGE_KO) )
					System.out.println ( "[DT] Failure during the removal of all Bolt instances." );
			}
			else 
				System.out.println("[DT] Usage: remove_all_c3po");
		}
	}
	
	//Tries to receive a message every 100 milliseconds
	private String receiveMessage()
 	{
 		while(true){
			try {
				if(this.in.ready()) return this.in.readLine();
			} catch (IOException e) { e.printStackTrace(); System.out.println("[DT] IOException, returning null string."); return " ";}
			
			try{Thread.sleep(100);}catch(Exception ee){ee.printStackTrace(); System.out.println("[DT] Waiting for a response, next try in 0,1 seconds.");}
		}
 	}
	
	private boolean sendMessage(String msg)
	{
		this.out.println(msg);
		out.flush();
		return true;
	}
	
}