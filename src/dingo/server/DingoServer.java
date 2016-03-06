package dingo.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import commons.FunnyThings;

import dingo.Settings;


/** Server, responsible of processing commands and call other components
 * @author marco
 *
 */
public class DingoServer{
	
	private Properties settings;
  
    private CommandInterfaceWrapper ci;

	private ArrayList<BoltsManager> cloudMan = new ArrayList<BoltsManager>();	
	
	/**
	 * This is the main server
	 * 
	 * @throws InstantiationException
	 */
	public DingoServer() throws InstantiationException{
		System.out.println("[Dingo] Starting server...");
		long start = System.currentTimeMillis();
		
		//Loading properties files
		this.settings = new Properties();
		try {
			this.settings.load(new FileInputStream(Settings.DINGO_PROPERTIES));
		} catch (FileNotFoundException e) { 
			System.out.println("[Dingo] Settings File not found");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

				//Initializing main components
		this.ci = new CommandInterfaceWrapper(this);
		new Thread(this.ci).start();

		long end = System.currentTimeMillis();
		System.out.println("[Dingo] Command Interface Port: " + this.ci.getServerPort());
		System.out.println("[Dingo] Server started in " + (end - start) + " ms.");
	}	
	
	/**
	 * Takes care of closing all the components connected with the server
	 * @return 
	 */
	public boolean shutdown()
	{		
		this.ci.shutdown();

		if(!this.removeAllBolts()) {
			System.out.println("[Dingo] Removing of some machine failed, returning false.");
			return false;
		}
		
		System.out.println("[Dingo] Shutting down.");
		return true;
	}
	
	/**
	 * @return Status of every connected component
	 */
	public String getStatus()
	{
		String status = "Status:\n";
		
		Vector<String> temp = new Vector<String>();
		
		// CommandInterface part
		temp = this.ci.getStatus();
		status = status + "- CommandInterface Status [n. = " + temp.size() + "]\n";
		for ( String s : temp )
		{
			status = status + "\t- " + s + "\n";
		}
		temp.clear();
		status = status + "\n";
		
		return status;
	}
	
	protected Properties getSettings() {return this.settings;}
	
	//Extends the cloud
	public boolean addBolt(String user, String host, int port, String remotePath, InputStream in, OutputStream out) throws JSchException, SftpException, IOException, InstantiationException{
		
		if( this.cloudMan.isEmpty() ){
			BoltsManager item = new BoltsManager(FileSystems.getDefault().getPath(this.settings.getProperty("BASE_DIR")), FileSystems.getDefault().getPath(this.settings.getProperty("Bolt_ARCHIVE")) );
			this.cloudMan.add(item);
			return item.addComputer(user, host, port, remotePath, in, out);
		} else {
			//TODO insert code to get the correct Manager
			return this.cloudMan.get(0).addComputer(user, host, port, remotePath, in, out);
		}
		
	}
	
	//Restrict the cloud
	public boolean removeBolt(String user, String host, int port){
		
		if( this.cloudMan.isEmpty() ){
			System.out.println("[Dingo] No Bolt instances left!");
			return false;
		} else {
			//TODO insert code to get the correct Manager
			return this.cloudMan.get(0).removeComputer(user, host, port);
		}
		
	}
	
	//Removes all remote machines
	public boolean removeAllBolts(){
		
		if( this.cloudMan.isEmpty() ){
			System.out.println("[Dingo] No Bolt instances left!");
			return true;
		} else {
			//TODO insert code to get the correct Manager
			return this.cloudMan.get(0).shutdown();
		}	
	}
		
	public static void main(String[] args) throws Exception 
	{
		FunnyThings.printDingo();
		new DingoServer();
	}
}
