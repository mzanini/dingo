package dingo.server;


import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;
import java.util.Vector;

import dingo.Settings;
import dingo.server.DingoServer;


/**
 * This component is the main server for an interface of type {@link CommandInterface}
 * The server remains listening for new connections on the port defined in the settings file.
 * Every time he receives a new requesst, it instantiates a new thread for a new {@link CommandInterface}
 * that will work as a new server dedicated to that specific connection.
 * Once the thread is created, it starts listening for new connections.
 * @author Leonardo
 *
 */
public class CommandInterfaceWrapper implements Runnable
{
	private DingoServer hs;
	private Properties settings;
	private ServerSocket serverSocket;
	private Socket clientSocket;
	private int serverPort;
	private boolean listen = true;
	
	private Vector<CommandInterface> ciList;
	
	
	public CommandInterfaceWrapper(DingoServer cm) throws InstantiationException
	{
		this.settings = new Properties();
		try
		{
			this.settings.load(new FileInputStream(Settings.DINGO_PROPERTIES));
		}
		catch(IOException e) {throw new InstantiationException("Unable to read properties file.");}		
		try
		{	
			this.serverPort = Integer.parseInt(this.settings.getProperty("COMMAND_INTERFACE_PORT"));
		}
		catch(NumberFormatException e) {throw new InstantiationException("Unable to read server port from properties file.");}
		try
		{
			this.serverSocket = new ServerSocket(this.serverPort);
		}
		catch(IOException e) {throw new InstantiationException("Unable to listen on port " + this.serverPort);}
		
		this.hs = cm;
		this.ciList = new Vector<CommandInterface>();
	}
	
	public void run()
	{
		try
		{
			while(this.listen)
			{
				boolean timedOut = false;
				try
				{
					this.serverSocket.setSoTimeout(10000);
					this.clientSocket = this.serverSocket.accept();
				}
				catch(SocketTimeoutException e) {timedOut = true;} 
				
				if (!timedOut)
				{
					// connection successful
					synchronized ( this.ciList )
					{
						CommandInterface ci = new CommandInterface(hs, clientSocket, this);
						Thread t = new Thread(ci);
						t.start();
						ciList.add(ci);	
					}
				}
			}
		}
		catch(Exception e) {e.printStackTrace();}
	}
	
	
	
	public void shutdown()
	{
		this.listen = false;

		synchronized ( this.ciList )
		{
			for ( CommandInterface ci : ciList )
	    	{
				ci.shutdown();
	    	}
			ciList.clear();	
		}
	}
	
	
	protected void removeInterface ( CommandInterface ci )
	{
		synchronized ( this.ciList )
		{
			this.ciList.remove( ci );
		}
	}
	
	public int getServerPort ()
	{
		return this.serverPort;
	}
	
	
	public Vector<String> getStatus()
	{
		Vector<String> result = new Vector<String>();
		
		synchronized ( this.ciList )
		{
			for ( CommandInterface ci : this.ciList )
			{
				result.add(ci.getRemoteIP() + ":" + ci.getRemotePort());
			}
		}
		
		return result;
	}
	
}