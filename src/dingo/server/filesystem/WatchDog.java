package dingo.server.filesystem;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
//import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Properties;

import static java.nio.file.StandardWatchEventKinds.*;

import dingo.CommandProtocol;

/**
 * Recursively watches a directory for changes, notifies the server when a change happens.
 * @author Marco
 *
 */
public class WatchDog implements Runnable{
	
	private boolean run;
	private WatchService watcher;
	private Path localDir;
	private Path localArch;
	private boolean trace;
	private HashMap<WatchKey,Path> keys;
	private HashSet<Path> directories;
	
	private BlockingQueue<UpdateEvent> queue;
	
	/** Creates a new WatchDog, it will put updates in the queue
	 * @param q BlockingQueue to put updates in
	 * @param localDir Path of the local directory to look for changes in 
	 * @param localArch Path of the archive file, will look for changes in this directory too
	 * @throws InstantiationException
	 */
	public WatchDog(BlockingQueue<UpdateEvent> q, Path localDir, Path localArch) throws InstantiationException 
	{
		this.localDir = localDir;
		this.localArch = localArch;
		System.out.println("[WD] The base directory is: " + this.localDir + " and local archive: " + this.localArch);
		this.keys = new HashMap<WatchKey, Path>();
		this.directories = new HashSet<Path>();
		
		this.queue = q;
	}
	
	public void run() {
		System.out.println("[WD] Starting...");
		this.run = true;
		
		try {
			this.watchChanges();
		} catch (InterruptedException e1) {
			System.out.println("[WD] Interrupted");
			e1.printStackTrace();
		}
		
	}
	
	/** Recursively register directories and enter an infinite loop to watch for changes
	 * @throws InterruptedException
	 */
	private void watchChanges() throws InterruptedException{
		System.out.println("[WD] Watching directory " + this.localDir + " and " + this.localArch.getParent() + " for changes");
		this.watcher = null;
		// 1) Create a WatchService for the file system
		System.out.println("[WD] Initializing watcher...");
		try {
			this.watcher = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {e.printStackTrace();}
		
		if(watcher==null){
			System.out.println("[WD] Ops something went wrong: watcher is null!");
			return;
		}
		
		try {
			this.registerAll(this.localDir);
			this.register(this.localArch.getParent());
		} catch (IOException e) {e.printStackTrace();}
		
		//Enable trace after initial registration
		this.trace = true;
		
		// 3) Infinite loop to wait for incoming events. When an event occurs, the key is signaled and 
		// placed into the Watcher's queue.
		while (this.run) {
			// 4) Retrieve the key from the watcher's queue
			//wait for key to be signaled
		    WatchKey key;
		    try {
		    	key = watcher.take();
		    } catch (InterruptedException x) {return;}
		    
		    // Retrieves the directory associated with this key
		    Path dir = keys.get(key);
            if (dir == null) {
                System.out.println("WatchKey not recognized!!");
                continue;
            }
            
            // 5) Retrieve each pending event for the key
		    for (WatchEvent<?> event: key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();
				//The fileName is the context of the event
				WatchEvent<Path> ev = cast(event);
				Path filename = ev.context();
	
				if (kind == ENTRY_CREATE && filename!=null) {
					if(Files.isDirectory( dir.resolve(filename) )){
						//System.out.println("[WD] New directory created:" + filename.toAbsolutePath());
						synchronized(this){
							try {
								this.registerAll( dir.resolve(filename) );
							} catch (IOException e) { e.printStackTrace(); }
						}
						this.queue.put(new UpdateEvent(dir.resolve(filename), CommandProtocol.MESSAGE_DIRECTORY_ADDED));
						//this.hs.fileChanged( dir.resolve(filename) , CommandProtocol.MESSAGE_DIRECTORY_ADDED);
					}
					else {
						//System.out.println("[WD] New file created:" + filename.toAbsolutePath());
						this.queue.put(new UpdateEvent(dir.resolve(filename), CommandProtocol.MESSAGE_FILE_ADDED));
						//this.hs.fileChanged( dir.resolve(filename), CommandProtocol.MESSAGE_FILE_ADDED);
					}					
				    continue;
				} else 
				if (kind == ENTRY_DELETE && filename!=null) {
					if( this.checkIsDirectory(dir, filename) ){
						//System.out.println("[WD] Directory deleted:" + filename.toAbsolutePath());
						this.queue.put(new UpdateEvent(dir.resolve(filename), CommandProtocol.MESSAGE_DIRECTORY_DELETED));
						//this.hs.fileChanged( dir.resolve(filename), CommandProtocol.MESSAGE_DIRECTORY_DELETED);
					}
					else { 
						//System.out.println("[WD] File deleted:" + filename.toAbsolutePath());
						this.queue.put(new UpdateEvent(dir.resolve(filename), CommandProtocol.MESSAGE_FILE_DELETED));
						//this.hs.fileChanged( dir.resolve(filename), CommandProtocol.MESSAGE_FILE_DELETED);
					}
				    continue;
				} else
				if (kind == ENTRY_MODIFY && filename!=null) {
					if(Files.isDirectory( dir.resolve(filename) )){
						//System.out.println("[WD] Directory modified:" + filename.toAbsolutePath());
						//this.hs.fileChanged( dir.resolve(filename), CommandProtocol.MESSAGE_DIRECTORY_CHANGED);
						this.queue.put(new UpdateEvent(dir.resolve(filename), CommandProtocol.MESSAGE_DIRECTORY_CHANGED));
					}
					else { 
						//System.out.println("[WD] File modified:" + filename.toAbsolutePath() );
						this.queue.put(new UpdateEvent(dir.resolve(filename), CommandProtocol.MESSAGE_FILE_CHANGED));
						//this.hs.fileChanged( dir.resolve(filename), CommandProtocol.MESSAGE_FILE_CHANGED);
					}
				    continue;
				} else
				if (kind == OVERFLOW) {
					System.out.println("[WD] Ops, maybe I lost some events!");
				    continue;
				}
				
		    }
	
		    // 6) Reset the key -- this step is critical if you want to receive
		    //further watch events. If the key is no longer valid, the directory
		    //is inaccessible so exit the loop.
		    boolean valid = key.reset();
		    if (!valid) {
                keys.remove(key);
                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
		    
		}//End while, can exit only if this.run=false
		
		System.out.println("[WD] Shutting down.");
	}
	
	/** Check if the second argument is a Directory
	 * 
	 * @param dir
	 * @param filename
	 * @return
	 */
	private boolean checkIsDirectory(Path dir, Path filename){
		System.out.println("[WD] Controlling if " + dir.resolve(filename) + " is a directory...");
		Iterator<Path> itr = this.directories.iterator();
		boolean isDirectory = false;
		while(itr.hasNext()){
			Path item = itr.next();
			if( item.compareTo(dir.resolve(filename))==0 ){
				System.out.println("[WD] Deleted file is a directory!");
				isDirectory=true;
			}
		}
		return isDirectory;
	}
	
    /**Register the given directory, and all its sub-directories, with the
     * WatchService.
     * 
     * @param start Root path to start from
     * @throws IOException
     */
    private synchronized void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
    	SimpleFileVisitor<Path> directoryRegister = new SimpleFileVisitor<Path>() {
    		@Override
    		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attr) throws IOException {
    			System.out.println("[WD] Calling register...");
    			register(dir); 
    			return FileVisitResult.CONTINUE; 
    		}
    	};
    	
    	System.out.println("[WD] Calling walkFileTree...");
    	Files.walkFileTree(start, directoryRegister); 
    }

    /**Register the given directory with the WatchService
     * 
     * @param dir Absolute Path of the directory to register
     * @throws IOException
     */
    private synchronized void register(Path dir) throws IOException {
    	if(watcher==null) System.out.println("[WD] Ops something went wrong: watcher is null!");
        
    	// 2) For each directory that you want to be monitored, register it with the watcher. You receive a WatchKey
    	// instance for each directory that you register.
    	WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    	this.directories.add(dir);
    	
        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                System.out.println("[WD] Registering new directory: " + dir);
            } else {
                if (!dir.equals(prev)) {
                    System.out.println("Update registration: from " + prev + " to " + dir);
                }
            }
        }
        
        System.out.println("[WD] Registering directory: " + dir);
        
        keys.put(key, dir);
    }
    
	@SuppressWarnings("unchecked")
	private static WatchEvent<Path> cast(WatchEvent<?> event) {
	    return (WatchEvent<Path>)event;
	}
	
	//Method to stop the thread
    public void shutdown() {
    	this.run = false;
    	
    	synchronized (this){
			this.notifyAll();
		}
    }
    
}
