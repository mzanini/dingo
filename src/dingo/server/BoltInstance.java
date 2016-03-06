package dingo.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;

import communication.CommunicationClient;
import communication.SshClient;


/** Denotes a remote instance of Bolt, provides methods to control the instance and propagate file changes
 * @author marco
 *
 */
public class BoltInstance {
	
	//Connection Data
	private String host;
	private String user;
	private int port;
	private UserInfo ui;
	private CommunicationClient cc;
	
	//Bolt Data
	private String remoteDir;
	//TODO Implement in the future:
	private boolean running = false;
	private boolean updated = false;
	
	//Testing
	private InputStream in;
	private OutputStream out;
	
	/** Creates a new BoltInstance, filling the appropriate variables in order to initialize it. Attention: this only creates the instance locally,
	 * to create it remotely you have to call initialize(Path archive)
	 * 
	 * @param user Remote user for accessing the machine
	 * @param host Address of the machine (normally ip)
	 * @param port for connection
	 * @param ui stores user's password
	 * @param baseDir Base directory on the remote machine
	 * @param in Input stream
	 * @param out Output stream
	 */
	public BoltInstance(String user, String host, int port, UserInfo ui, String remoteDir, InputStream in, OutputStream out){
		this.user = user;
		this.host = host;
		this.port = port;
		this.ui = ui;
		if(remoteDir.endsWith("/")) {
			this.remoteDir = remoteDir;
		} else if(!remoteDir.endsWith("/")){
			this.remoteDir = remoteDir + "/";
		}
		
		this.in = in;
		this.out = out;
	}
	
	/**Initializes this instance, copying the compressed archive, extracting, compiling and executing.
	 * Deletes the compressed archive at the end.
	 * 
	 * @param archive Absolute path of the compressed archive (starting from root /) 
	 * @return false if the initialization fails
	 * @throws IOException archive file doesn't exist
	 * @throws JSchException 
	 * @throws SftpException 
	 */
	public boolean initialize(Path archive){
		//Check if the name of the archive is correct
		if(!archive.toString().endsWith(".tar.gz")){
			System.out.println("[BoltInstance] Wrong archive name! Return false.");
			return false;
		}
		
		//Connect to the remote machine
		System.out.println("[BoltInstance] Creating SshClient...");
		this.cc = new SshClient(this.user, this.host, this.port, this.ui, this.in, this.out);
		
		System.out.println("[BoltInstance] Connecting to the remote machine...");
		System.out.println("[BoltInstance] Starting Ssh...");
		Thread ccT =new Thread(this.cc);
		ccT.start();
		synchronized(ccT){
			try {
				ccT.wait();
			} catch (InterruptedException e) {
				System.out.println("[BoltInstance] CommunicationClient interrupted! ");
				e.printStackTrace();
				return false;
			}
		}
		
		//Send compressed file
		System.out.println("[BoltInstance] Copying compressed file: " + archive);
		this.cc.sendFile(archive, this.remoteDir + archive.getFileName());
		
		//Extract file and wait for completion
		System.out.println("[BoltInstance] Extracting file: " + this.remoteDir +  archive.getFileName());
		if(archive.getFileName().toString().endsWith(".tar.gz")){
			if( !this.cc.executeCommand("tar -zxvf " + this.remoteDir + archive.getFileName() + " -C " + this.remoteDir)){
				System.out.println("[BoltInstance] Unable to extract the File.");
				return false;
			} 
		} else {
			System.out.println("[BoltInstance] Unable to recognize archive type.");
		}
		
		//Cancel compressed file;
		System.out.println("[BoltInstance] Deleting compressed file: " + this.remoteDir + archive.getFileName());
		if(!this.cc.cancelFile(this.remoteDir +  archive.getFileName()) ){
			System.out.println("[BoltInstance] Unable to cancel the File.");
			return false;
		}
		
		System.out.println("[BoltInstance] Instance initialized correctly! Returning true.");
		return true;
	}
	
	/** Stop the instance and cancel all files
	 * @return true if the instance has been correctly destroyed
	 */
	public boolean destroy(){
		System.out.println("[BoltInstance] Destroying!");
		//Stop the instance
		//this.stop();
		
		if(this.cc.emptyDir(this.remoteDir, false))
			return true;
		else 
			return false;
	}
	
	/** Updates the instance by stopping the execution, cancelling files and compiling again
	 * @param archive New archive containing Bolt installation files
	 * @return true if the instance has been correctly updated
	 */
	private boolean update(Path archive){
		if(!this.destroy()){
			System.out.println("[BoltInstance] Failed to destroy instance, returning false!");
			return false;
		}
		
		//Initialize again
		System.out.println("[BoltInstance] Initializing again..");
		if(this.initialize(archive)){
			return true;
		} else {
			return false;
		}
	}
	
	/** Stops Bolt execution 
	 * @return true if the execution has been stopped successfully
	 */
	private boolean stop(){	
		return true;
	}

	
	/** Updates the instance with another archive file
	 * @param archive Absolute Path of the archive file (starting from root /)
	 * @return true if the instance has been updated correctly
	 */
	public boolean archiveChanged(Path archive){
		if (this.update(archive)){
			System.out.println("[BoltInstance] Correctly updated, returning true.");
			return true;
		} else {
			System.out.println("[BoltInstance] Something in the update went wrong, returning false.");
			return false;
		}
		
	}
	
	/** Sends a new file
	 * @param file Absolute path of the file to be sent (starting from root /)
	 * @param baseDir Base directory of the file in order to calculate the remote Path
	 * @return true if the file has been successfully added
	 */
	public boolean fileAdded(Path file, Path baseDir) {
		//Calculate remote directory
		String remotePath = this.remoteDir + baseDir.relativize(file).toString();
		//Send the new file
		System.out.println("[BoltInstance] Copying new file: " + file + " in " + remotePath);
		this.cc.sendFile(file, remotePath);
		
		return true;
	}

	/** Creates a new empty directory (remotely)
	 * @param file Absolute Path of the directory (starting from root /)
	 * @param baseDir Base directory of the directory, necessary in order to calculate the remote Path
	 * @return true if the directory has been successfully added
	 */
	public boolean directoryAdded(Path file, Path baseDir) {
		//Calculate remote directory
		String remotePath = this.remoteDir + baseDir.relativize(file).toString();
		//Send the new file
		System.out.println("[BoltInstance] Creating directory: " + file + " in " + remotePath);
		this.cc.createDir(remotePath);
		
		return true;
	} 
	
	/** Deletes a file
	 * @param file Absolute local path of the file to be deleted (starting from root /)
	 * @param baseDir Base directory of the file in order to calculate the remote Path
	 * @return true if the file has been correctly deleted
	 */
	public boolean fileDeleted(Path file, Path baseDir) {
		//Calculate remote directory
		String remotePath = this.remoteDir + baseDir.relativize(file).toString();
		//Cancel the remote file
		System.out.println("[BoltInstance] Deleting file: " + file);
		this.cc.cancelFile(remotePath);
		return true;
	}
	
	/** Deletes a directory
	 * @param file Absolute local Path of the directory to be deleted
	 * @param baseDir Base directory of the directory in order to calculate the remote Path
	 * @return true if the directory has been successfully deleted
	 */
	public boolean directoryDeleted(Path file, Path baseDir) {
		//Calculate remote directory
		String remotePath = this.remoteDir + baseDir.relativize(file).toString();
		//Cancel the remote file
		System.out.println("[BoltInstance] Deleting directory: " + file);
		this.cc.emptyDir(remotePath, true);
		return true;
	}
	
	/** Deletes old version of the file and copies the new version
	 * @param fileName Absolute local Path of the new file
	 * @param baseDir Base Directory of the file in order to calculate the remote Path
	 * @return true if the file has been correctly updated
	 */
	public boolean fileChanged(Path fileName, Path baseDir) {
		//Cancel previous File
		String remotePath = this.remoteDir + baseDir.relativize(fileName).toString();
		
		System.out.println("[BoltInstance] Deleting file:" + remotePath);
		this.cc.cancelFile(remotePath);
		
		//Copy new File
		System.out.println("[BoltInstance] Copying file:" + fileName +" in " + remotePath);
		this.cc.sendFile(fileName, remotePath);
		return true;
	}
	
	/**
	 * @return current remote directory 
	 */
	public String getBaseDir() {
		return remoteDir;
	}
	
	//TODO not functional now
	public void outdate(){
		this.updated = false;
	}
	
	/*May be implemented later
	 * public void changeBaseDir(Path baseDir) {
		this.baseDir = baseDir;
	}*/

	public String getUser(){
		return this.user;
	}
	
	public String getHost(){
		return this.host;
	}
	
	public int getPort(){
		return this.port;
	}

	public boolean isRunning(){
		return running;
	}
	
	public boolean isUpdated(){
		return updated;
	}

}
