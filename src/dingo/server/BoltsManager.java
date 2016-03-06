package dingo.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import communication.GAccessInfo;
import dingo.server.filesystem.UpdateEvent;
import dingo.server.filesystem.WatchDog;

/** Responsible of keeping remote instances associated with a specific version of Bolt running and
 *  updated.
 * 
 * @author marco
 *
 */
public class BoltsManager {
	private String versionNumber = "0.1";
	private Path localBaseDir;
	private Path localArchive;
	
	private WatchDog wd;
	private BoltUpdater updater;
	
	private HashSet<BoltInstance> cloud = new HashSet<BoltInstance>();
	
	
	/** Creates a new manager, given a specific local directory and archive file
	 * @param baseDir local Path of the current Bolt Instance, has to be absolute (start from root)
	 * @param archive  local Path of the current Bolt archive, has to be absolute
	 */
	public BoltsManager(Path baseDir, Path archive){
		System.out.println("[Manager] Initializing manager, local directory: " + baseDir + ", archive: " + archive );
		this.localBaseDir = baseDir;
		this.localArchive = archive;
	}
	
	/** Adds a computer to the cloud, takes care of copying and decompressing the archive into the remote directory, 
	 *  cancel the archive, compile and see if everything works fine, adding the computer to the cloud and keeping it 
	 *  updated with this version.
	 * @param user to access the computer with
	 * @param host ip address of the computer
	 * @param port used for communication
	 * @param path Remote Path where to install Bolt
	 * @param in *testing*
	 * @param out *testing*
	 * @return true if the computer has been successfully initialized and added to the cloud. 
	 */
	public synchronized boolean addComputer(String user, String host, int port, String path, InputStream in, OutputStream out){
		
		GAccessInfo gui = new GAccessInfo();
		
		BoltInstance item = new BoltInstance(user, host, port, gui, path, in, out);
		
		if( item.initialize(this.localArchive) ){
			System.out.println("[Manager] Bolt instance initialized successfully!");
			this.cloud.add(item);
			
			// If it's the first computer, initialize WatchDog
			if(this.cloud.size() == 1) {
				//Create the Queue
				BlockingQueue<UpdateEvent> q = new LinkedBlockingQueue<UpdateEvent>();
				
				//Starting updater Thread
				System.out.println("[Manager] Starting Updater...");
				this.updater = new BoltUpdater(this, q);
				new Thread(this.updater).start();
				
				//Starting WatchDog Thread
				try {
					this.wd = new WatchDog(q, this.localBaseDir, this.localArchive);
					System.out.println("[Manager] Starting Directory WatchDog...");
					new Thread(this.wd).start();
				} catch (InstantiationException e) {
					System.out.println("[Manager] Error instantiating WatchDog!");
					e.printStackTrace();
					}
			}
			
			System.out.println("[Manager] Returning true to the terminal....	");
			return true;
		} else {
			System.out.println("[Manager] Failed to initialize Bolt instance.");
			return false;
		}
	}
	
	/** Removes a computer from the cloud. Takes care of stopping the execution 
	 * @param user
	 * @param host
	 * @param port
	 * @return
	 */
	public synchronized boolean removeComputer(String user, String host, int port){
		for(BoltInstance item: this.cloud){
			//Check if there is an instance to cancel
			if(item.getUser().compareTo(user)==0 && item.getHost().compareTo(host)==0 && item.getPort()==port){
				if(item.destroy()==true){
					System.out.println("[Dingo] Bolt instance removed successfully!");
					this.cloud.remove(item);
					return true;
				} else {
					System.out.println("[Dingo] Wasn't able to remove the instance!");
					return false;
				}
			} else {
				System.out.println("[Dingo] Bolt instance to remove not found, trying with another item in the cloud!");
			}
		}
		
		System.out.println("[Dingo] Bolt instance to remove not found!");
		return false;
	}
	
	/** Shuts down the entire manager, removing all the computers from the cloud
	 * @return true if the manager has been correctly shutted down
	 */
	public boolean shutdown(){
		for(BoltInstance item: this.cloud){
			if(item.destroy()==true){
				System.out.println("[Dingo] Bolt instance removed successfully!");
				return true;
			} else {
				System.out.println("[Dingo] Wasn't able to remove the instance!");
				return false;
			}
		}
		
		System.out.println("[Dingo] All Bolt instances successfully removed!");
		return true;
	}
	
	/**
	 * @return the Set of Bolt Instances currently in the cloud
	 */
	protected HashSet<BoltInstance> getCloud(){
		return this.cloud;
	}
	
	/**
	 * @return current version number of this manager
	 */
	protected String getVersionNumber() {
		return this.versionNumber;
	}

	/**
	 * @return current local directory of this manager
	 */
	protected Path getLocalBaseDir() {
		return this.localBaseDir;
	}
	
	/**
	 * @return current local archive of this manager
	 */
	protected Path getLocalArchive(){
		return this.localArchive;
	}
	
}
