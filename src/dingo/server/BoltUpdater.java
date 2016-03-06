package dingo.server;

import java.util.concurrent.BlockingQueue;

import dingo.CommandProtocol;
import dingo.server.filesystem.UpdateEvent;

/** Thread in charge of looking for updates on a Queue and propagate them to the cloud
 * @author marco
 *
 */
public class BoltUpdater implements Runnable{
	private BlockingQueue<UpdateEvent> queue;
	private BoltsManager manager;
	
	/** Creates a new BoltUpdater, associating it with a Manager and Queue
	 * @param man BoltManager to associate the Updater with
	 * @param q Queue to look for updates
	 */
	public BoltUpdater(BoltsManager man, BlockingQueue<UpdateEvent> q){
		this.manager = man;
		this.queue = q;
		
		System.out.println("[Updater] Creating Updater... ");
	}
	
	public void run() {
		System.out.println("[Updater] Starting....");
		this.waitForChanges();
	}
	
	/** Wait for updates, blocks if no updates are available
	 * 
	 */
	private void waitForChanges(){
		while(true){
			try {
				//Try to get an UpdateEvent, blocks if no updates are available 
				UpdateEvent item = this.queue.take();
				
				synchronized(this){
					for( BoltInstance cloudItem: this.manager.getCloud() ){
						if(item.getType().equalsIgnoreCase(CommandProtocol.MESSAGE_DIRECTORY_ADDED)){
							System.out.println("[Updater] Directory " + item.getFile() + " added, propagating change...");
							cloudItem.directoryAdded(item.getFile(), this.manager.getLocalBaseDir() );
						} else if(item.getType().equalsIgnoreCase(CommandProtocol.MESSAGE_FILE_ADDED)){
							//I'm not considering files added in the archive directory
							if( item.getFile().getParent().compareTo(this.manager.getLocalArchive().getParent()) != 0){
								System.out.println("[Updater] File " + item.getFile() + " added, propagating change...");
								cloudItem.fileAdded(item.getFile(), this.manager.getLocalBaseDir() );
							}
						}else if(item.getType().equalsIgnoreCase(CommandProtocol.MESSAGE_DIRECTORY_DELETED)){
							System.out.println("[Updater] Directory " + item.getFile() + " deleted, propagating change...");
							cloudItem.directoryDeleted(item.getFile(), this.manager.getLocalBaseDir() );
						} else if(item.getType().equalsIgnoreCase(CommandProtocol.MESSAGE_FILE_DELETED)){
							//Ignore deleting if it's the archive
							if (item.getFile().compareTo(this.manager.getLocalArchive()) == 0){
								System.out.println("[Updater] Archive " + item.getFile() + "deleted, but doing nothing!");
							} else {
								System.out.println("[Updater] File " + item.getFile() + " deleted, propagating change...");
								cloudItem.fileDeleted(item.getFile(), this.manager.getLocalBaseDir() );
							}
						} else if(item.getType().equalsIgnoreCase(CommandProtocol.MESSAGE_DIRECTORY_CHANGED)){
							//System.out.println("[Farmer] Directory " + item.getFile() + " modified, propagating change...");
						} else if(item.getType().equalsIgnoreCase(CommandProtocol.MESSAGE_FILE_CHANGED)){
							if (item.getFile().compareTo(this.manager.getLocalArchive()) == 0){
								System.out.println("[Updater] Archive " + item.getFile() + "modified propagating change...");
								cloudItem.archiveChanged(item.getFile());
							} else {
								System.out.println("[Updater] File " + item.getFile() + " modified, propagating change...");
								cloudItem.fileChanged(item.getFile(), this.manager.getLocalBaseDir() );
							}
						}
					}
				} //End synchronized
					
			} catch (InterruptedException e) {
				System.out.println("[Updater] Interrupted!");
				e.printStackTrace();
			}
			
		} //End while
		
	}

}
