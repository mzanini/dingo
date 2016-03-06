package communication;

import java.nio.file.Path;

/** Interface for a communication client, provides interfaces for methods to operate on a remote machine
 * @author marco
 *
 */
public interface CommunicationClient extends Runnable{
	
	/** Sends a file
	 * @param source Absolute Path of the file
	 * @param destination Absolute Path of the destination
	 * @return true if the file has been correctly sent
	 */
	public boolean sendFile(Path source, String destination);
	
	/** Cancels a file
	 * @param fileToCancel Absolute Path (with extension) of the file to cancel
	 * @return true if the file has been correctly cancelled
	 */
	public boolean cancelFile(String fileToCancel);
	
	/** Creates a directory
	 * @param destination Absolute Path of the directory to create
	 * @return true if the directory has been correctly deleted
	 */
	public boolean createDir(String destination);
	
	/** Cancels all contained files in a directory, can cancel also the main directory
	 * @param dirToCancel Absolute Path of the main directory where files to cancel are located
	 * @param cancelMainDir Put this to true if you also want to cancel the main directory
	 * @return true if the files have been cancelled correctly
	 */
	public boolean emptyDir(String dirToEmpty, boolean cancelMainDir);
	
	/** Executes a remote command
	 * @param command Command to execute
	 * @return true if the command has been executed correctly
	 */
	public boolean executeCommand(String command);
	
	/** Shuts down the CommunicationClient, performing the necessary operations in order to properly close the connection
	 * @return
	 */
	public void shutdown();
	
}
