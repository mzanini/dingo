package communication;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Vector;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;


/**Client for ssh communication, if the thread is running it means we established a session
 * @author marco
 */
public class SshClient implements CommunicationClient{
	private Session session;
	
	private String user;
	private String host;
	private int port;
	//Stores user's password
	private UserInfo ui;
	
	private InputStream in;
	private OutputStream out;
	
	/**Create a new SshClient, in order to connect to a remote host and perform ssh and sftp operations
	 * @param usr Username of the user for the connection
	 * @param hst IP address of the remote machine
	 * @param prt  Port to connect to
	 * @param usrInf Object containing user's password
	 * @param in *testing
	 * @param out *testing
	 * @throws JSchException
	 */
	public SshClient(String usr, String hst, int prt, UserInfo usrInf, InputStream in, OutputStream out){
		
		this.user = usr;
		this.host = hst;
		this.port = prt;
		this.ui = usrInf;
		//Connects input and output streams of the machine to the CommandInterface, only for testing purposes
		this.in = in;
		this.out = out;
	}
	
	public void run() {
		System.out.println("[SshClient] Starting...");
		
		System.out.println("[SshClient] Initializing client and waiting until the session is connected.");
		this.initialize();
		
		synchronized(this){
			this.notifyAll();
		}
	}
	
	/**
	 * Initializes JSch framework and connects the session. Waits until the session connects.
	 */
	private synchronized boolean initialize(){
		//Initializes the framework
		JSch jsch = new JSch();
		
		System.out.println("[SSHClient] Creating session...");
		try {
			this.session = jsch.getSession(this.user, this.host, this.port);
		} catch (JSchException e1) {
			e1.printStackTrace();
			return false;
		}
		
		//Set the UserInfo class to get user's passwd, connect input and output streams
		session.setUserInfo(ui);
		System.out.println("[SSHClient] User's password is: " + ui.getPassword());
		session.setPassword(ui.getPassword());
		session.setInputStream(this.in);
		session.setOutputStream(this.out);	
		
		//Sets properties of the connection
		Properties config = new Properties();
        config.setProperty("StrictHostKeyChecking", "no");
        session.setConfig(config);
        
        //Connects the session with a certain timeout
        return this.connectSession(10000, 2);
	}
	
	/**
	 * Creates the Channel, depending on the type
	 */
	private Channel createChannel(String type) throws JSchException{
		if(type.contentEquals("shell")){			
			ChannelShell channel = (ChannelShell) session.openChannel(type);
			return channel;
		} 
		else if(type.contentEquals("sftp")) {
			ChannelSftp channel = (ChannelSftp) session.openChannel(type);
			return channel;
		} 
		else if(type.contentEquals("exec")) {  
			ChannelExec channel = (ChannelExec) session.openChannel(type);
			return channel;
		} else System.out.println("[SSHClient] Channel type not recognized! Return null...");
		
		return null;
	}
	
	/**
	 * Connects the session
	 * 
	 * @param timeout For the connection
	 * @param tries before giving up
	 * @return true if the session is connected
	 */
	private synchronized boolean connectSession(int timeout, int tries){
		if (session.isConnected()) 
			return true;
		
		for(int i=0; i<=tries; i++){
			//Connects the session with a certain timeout
	        try {
				session.connect(timeout);
			} catch (JSchException e) {
					e.printStackTrace();
				}
	        
	        //Wait 1 second for the session to connect
	        if(!session.isConnected()) {
	        	try {
	        		System.out.println("[SSHClient] Waiting connection...");
	        		Thread.sleep(1000);
				} catch (InterruptedException e) {
					System.out.println("[SSHClient] Interrupted.");
					e.printStackTrace();
				} 
	        } else {
	        	System.out.println("[SSHClient] Connected!");
	        	return true;
	        }
		}    
		
		System.out.println("[SSHClient] Unable to connect the session, sorry.");
		return false;
	}
	
	/**
	 * Associates stdin and stdout based on os type
	 */
	private boolean connectInOut(String osType, Channel channel, InputStream in, OutputStream out){
		System.out.println("[SSHClient] Changing Input and Output stream...");
		
		if(osType.equalsIgnoreCase("Linux")) {
			channel.setInputStream(in); //System.in
			channel.setOutputStream(out); //System.out
			return true;
		} else if (osType.equalsIgnoreCase("Windows")) {
			channel.setInputStream(new FilterInputStream(System.in) { 
				public int read(byte[] b, int off, int len) throws IOException { 
					return in.read(b, off, (len > 1024 ? 1024 : len)); } 
				}
			);
			return true;
		} else{
			System.out.println("[SSHClient] OS not recognized: returning false.");
			return false;
		}
	}
	
	/**
	 * Sends a file using sftp
	 * 
	 * @param source absolute source Path 
	 * @param destination absolute destination Path (it's a String)
	 * @return true if the file is correctly copied
	 * @throws JSchException
	 * @throws SftpException
	 */
	public boolean sendFile(Path source, String destination){
		if(!this.checkSessionConnection()) 
			return false;
		
		if(destination.toString().isEmpty() || source.toString().isEmpty()){
			System.out.println("[SSHClient] Source or directory are empty string, returning false.");
			return false;
		}
		
		ChannelSftp channel;
		try {
			channel = (ChannelSftp)createChannel("sftp");
			
			System.out.println("[SSHClient] Connecting Sftp channel...");
			channel.connect(3000);
			
			System.out.println("[SSHClient] Copying file: " + source + " to: " + destination);
			channel.put(source.toString(), destination);
			
			channel.disconnect();
		} catch (JSchException | SftpException e1) {
			e1.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/** Creates a directory using sftp
	 * @param destination Absolute Path of the directory to create
	 * @return true if the directory has been created successfully
	 * @throws JSchException
	 * @throws SftpException
	 */
	public boolean createDir(String destination){
		if(!this.checkSessionConnection()) return false;
		
		if(destination.toString().isEmpty() ){
			System.out.println("[SSHClient] Source or directory are empty string, returning false.");
			return false;
		}

		try {
			ChannelSftp channel = (ChannelSftp)createChannel("sftp");
			System.out.println("[SSHClient] Connecting Sftp channel...");
			channel.connect(3000);
			
			System.out.println("[SSHClient] Creating directory: " + destination);
			channel.mkdir(destination.toString());
			
			channel.disconnect();
		} catch (SftpException | JSchException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	
	/**Cancels a file on the remote machine
	 * 
	 * @param fileToCancel path of the file to remove
	 * @return true if the file is correctly removed
	 * @throws JSchException
	 * @throws SftpException
	 */
	//TODO: Implement a mechanism to control if the file is correctly deleted
	public boolean cancelFile(String fileToCancel){
		if(!this.checkSessionConnection()) 
			return false;
		
		ChannelSftp channel;
		try {
			channel = (ChannelSftp)createChannel("sftp");
			
			System.out.println("[SSHClient] Connecting Sftp channel...");
			channel.connect(3000);
			
			System.out.println("[SSHClient] Deleting File: " + fileToCancel);
			channel.rm(fileToCancel.toString());
			
			System.out.println("[SSHClient] Disconnecting Sftp channel...");
			channel.disconnect();
		} catch (JSchException | SftpException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**Cancels a directory or file on the remote machine, whatever it contains! It's pretty dangerous since it cancels everything without regards.
	 * 
	 * @param dirToEmpty path of the directory to remove
	 * @param cancelMainDir set this to false if you want to cancel only sub-directories and files
	 * @return true if the directory is correctly removed
	 * @throws JSchException
	 * @throws SftpException
	 */
	//TODO: Implement a mechanism to control if the directory is correctly deleted
	public boolean emptyDir(String dirToEmpty, boolean cancelMainDir){
		if(!this.checkSessionConnection()) 
			return false;
		
		ChannelSftp channel;
		try {
			channel = (ChannelSftp)createChannel("sftp");
			System.out.println("[SSHClient] Connecting Sftp channel...");
			channel.connect(3000);
			
			SftpATTRS dirAttr = channel.lstat(dirToEmpty);
			if(!dirAttr.isDir()){
				System.out.println("[SSHClient] " + dirToEmpty + " is a file, cancelling...");
				channel.rm(dirToEmpty);
			} else {
				Vector files = channel.ls(dirToEmpty);
				if(files!=null){
				      for(int ii=0; ii<files.size(); ii++){
//					out.println(vv.elementAt(ii).toString());
			                Object obj=files.elementAt(ii);
			                if(obj instanceof com.jcraft.jsch.ChannelSftp.LsEntry && !((com.jcraft.jsch.ChannelSftp.LsEntry)obj).getFilename().equalsIgnoreCase("..")  
			                		&& !((com.jcraft.jsch.ChannelSftp.LsEntry)obj).getFilename().equalsIgnoreCase(".")){
			                  System.out.println("[SSHClient] Found sub-directory:" + ((com.jcraft.jsch.ChannelSftp.LsEntry)obj).getFilename());      
			                  //Cancel all files in this directory
			                  this.emptyDir( dirToEmpty +"/" +((com.jcraft.jsch.ChannelSftp.LsEntry)obj).getFilename(), true);
			                 }
				      }
				}
				
				if(cancelMainDir) {
					System.out.println("[SSHClient] Deleting empty directory: " + dirToEmpty);
					channel.rmdir(dirToEmpty.toString());
				}
			}
			
			System.out.println("[SSHClient] Disconnecting Sftp channel...");
			channel.disconnect();
		} catch (JSchException | SftpException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * Executes a command
	 * 
	 * @param command To execute
	 * @return true if the command is executed correctly (exitStatus==0)
	 * @throws JSchException
	 * @throws IOException
	 */
	public boolean executeCommand(String command){
		if(!this.checkSessionConnection()) return false;
		
		ChannelExec channel;
		try {
			channel = (ChannelExec) createChannel("exec");
			System.out.println("[SSHClient] Executing command: " + command);
			channel.setCommand(command + "\n");
			
			channel.setInputStream(null);
			channel.setErrStream(System.err);
			
			//connectInOut("linux", channel, System.in, System.out);
			// get I/O streams for remote scp
	        OutputStream out=channel.getOutputStream();
		    
		    //channel.setOutputStream(System.out);
		    
		    InputStream in = channel.getInputStream();
		    
		    channel.connect(300);
		    
		    byte[] tmp=new byte[1024];
			while(true){
			    while(in.available()>0){
			      int i=in.read(tmp, 0, 1024);
			      if(i<0)break;
			      System.out.print(new String(tmp, 0, i));
			    }
			    if(channel.isClosed()){
			      break;
			    }
			    try{Thread.sleep(1000);}catch(Exception ee){ ee.printStackTrace(); }
		    }
			
			int exitStatus = channel.getExitStatus();
		    System.out.println("exit-status: "+ exitStatus);
		    
		    channel.disconnect();
		    System.out.println("[SSHClient] Exec channel disconnected");
		    
		    if(exitStatus == 0){
		    	return true;
		    } else 
		    	return false;
		} catch (JSchException | IOException e) {
			e.printStackTrace();
			return false;
		}
		
	}
	
	/**
	 * Controls if the session is connected, if not it tries to connect (1 try)
	 * 
	 * @return true if the session is connected
	 */
	private boolean checkSessionConnection(){
		if(!this.session.isConnected()){
			System.out.println("[SSHClient] Session not connected, trying another time...");
			this.connectSession(10000, 5); 
			if(!this.session.isConnected()) return false;
		}
		
		return true;
	}
	
	//Method to stop the thread
    public void shutdown() {
    	System.out.println("[SSHClient] Shutting down...");
    	this.session.disconnect();
    	
    	synchronized (this){
			this.notifyAll();
		}
    }
	
}
