package dingo.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import dingo.CommandProtocol;
import dingo.server.DingoServer;


/**
 * Handles a single connection with a specific terminal/Dingo.
 * This server is started by {@link CommandInterfaceWrapper}  when he receives 
 * a new connection request from a terminal/Dingo.
 * @author Leonardo
 *
 */
public class CommandInterface implements Runnable
{
	private DingoServer hs;
	private Socket clientSocket;
	private CommandInterfaceWrapper ciw;
	private boolean connected;
	
	private PrintStream output;
	private BufferedReader input; 
	
	public CommandInterface(DingoServer cm, Socket clientSocket, CommandInterfaceWrapper ciw) throws Exception
	{
		this.clientSocket = clientSocket;
		this.hs = cm;
		this.ciw = ciw;
		
		this.output = new PrintStream(clientSocket.getOutputStream());
		this.input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		this.output.println(CommandProtocol.MESSAGE_HI);
		
		this.connected = true;
	}
	
	public void run()
	{
		try
		{
			while(this.connected)
			{
				String commandString = "";
				do{commandString = this.input.readLine();} while(commandString.equals(""));
				String[] tokens = commandString.split(" ");
				boolean recognizedCommand = false;
				
				if (tokens[0].equalsIgnoreCase(CommandProtocol.MESSAGE_BYE))
				{
					recognizedCommand = true;
					this.handleCommandBye();
				}
				if (tokens[0].equalsIgnoreCase(CommandProtocol.MESSAGE_SERVER_SHUTDOWN))
				{
					recognizedCommand = true;
					this.handleCommandShutdown();
				}
				if (tokens[0].equalsIgnoreCase(CommandProtocol.MESSAGE_SERVER_STATUS))
				{
					recognizedCommand = true;
					this.handleCommandServerStatus();
				}
				
				if (tokens[0].equalsIgnoreCase(CommandProtocol.MESSAGE_ADD_BOLT)){
					recognizedCommand = true;
					/** Get user, host and port
			           */
			          String user = tokens[1].substring(0, tokens[1].indexOf('@'));

			          String host = null;
			          
			          //Default SSH port is 22
			          int port = 22;

			          if (tokens[1].contains(":")) {
			            host = tokens[1].substring(tokens[1].indexOf('@') + 1, tokens[1].indexOf(":"));
			            port = Integer.parseInt(tokens[1].substring(tokens[1].indexOf(':') + 1));
			          } else {
			            host = tokens[1].substring(tokens[1].indexOf('@') + 1);
			          }
			          
			          System.out.println("[CI] user:" + user + " host:" + host + " port:" + port);
			          
			          String path = tokens[2];
			          
			    	  this.handleCommandAddBolt(user, host, port, path);
				}
				if (tokens[0].equalsIgnoreCase(CommandProtocol.MESSAGE_REMOVE_BOLT))
				{
					recognizedCommand = true;
					/** Get user, host and port
			           */
			          String user = tokens[1].substring(0, tokens[1].indexOf('@'));

			          String host = null;
			          			          
			        //Default SSH port is 22
			          int port = 22;

			          if (tokens[1].contains(":")) {
			            host = tokens[1].substring(tokens[1].indexOf('@') + 1, tokens[1].indexOf(":"));
			            port = Integer.parseInt(tokens[1].substring(tokens[1].indexOf(':') + 1));
			          } else {
			            host = tokens[1].substring(tokens[1].indexOf('@') + 1);
			          }
			          
			          System.out.println("[CI] user:" + user + " host:" + host + " port:" + port);
			          
			    	  this.handleCommandRemoveBolt(user, host, port);
				}
				if (tokens[0].equalsIgnoreCase(CommandProtocol.MESSAGE_REMOVE_ALL_BOLTS))
				{
					recognizedCommand = true;
					this.handleCommandRemoveAllBolts();
				}
				if (!recognizedCommand) System.out.println("[CI] Command not recognized, received: " + commandString);
			}
			
			this.output.close();
			this.input.close();
			this.clientSocket.close();
			System.out.println("Socket closed");
		}
		catch(IOException e) {e.printStackTrace();} catch (JSchException e) {e.printStackTrace();} 
		catch (SftpException e) {e.printStackTrace();}
	}

	protected void shutdown()
	{
		this.connected = false;
	}
	
	private void handleCommandServerStatus()
	{
		this.output.println(this.hs.getStatus());
		this.output.println(CommandProtocol.MESSAGE_OK);
	}
	
	private void handleCommandBye()
	{
		this.connected = false;
		this.output.println(CommandProtocol.MESSAGE_OK);
		this.ciw.removeInterface ( this );
	}
	
	private void handleCommandShutdown()
	{
		synchronized (this.hs)
		{
			this.hs.notify();
			this.hs.shutdown();	
		}
		this.handleCommandBye();
	}
	
	private void handleCommandAddBolt(String user, String host, int port, String path) throws IOException, JSchException, SftpException{
		this.output.println("[CI] I am about to call method addBolt of the Server");
		try {
			if(!this.hs.addBolt(user, host, port, path, this.clientSocket.getInputStream(), this.clientSocket.getOutputStream()))
				{
				System.out.println("[CI] Something went wrong!");
				this.output.println(CommandProtocol.MESSAGE_KO);
				}
		} catch (InstantiationException e) {e.printStackTrace();}
		this.output.println(CommandProtocol.MESSAGE_OK);
	}
	
	private void handleCommandRemoveBolt(String user, String host, int port) throws IOException, JSchException, SftpException{
		this.output.println("[CI] I am about to call method removeBolt of the Server");
		if(!this.hs.removeBolt(user, host, port)) {
			System.out.println("[CI] Something went wrong!");
			this.output.println(CommandProtocol.MESSAGE_KO);
		}
		this.output.println(CommandProtocol.MESSAGE_OK);
	}
	
	private void handleCommandRemoveAllBolts() {
		this.output.println("[CI] I am about to call method removeAllBolts of the Server");
		if(!this.hs.removeAllBolts()) {
			System.out.println("[CI] Something went wrong!");
			this.output.println(CommandProtocol.MESSAGE_KO);
		}
		this.output.println(CommandProtocol.MESSAGE_OK);
	}

	private String getRemoteAddress ()
	{
		return this.clientSocket.getRemoteSocketAddress().toString();
	}
	
	protected String getRemoteIP ()
	{
		return this.getRemoteAddress().substring(1, this.getRemoteAddress().indexOf(':'));
	}
	
	protected int getRemotePort ()
	{
		return Integer.parseInt(this.getRemoteAddress().substring(this.getRemoteAddress().indexOf(':') + 1, this.getRemoteAddress().length()));
	}
	
}