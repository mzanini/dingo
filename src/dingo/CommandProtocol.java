package dingo;

/**Contains all messages to permit communication among components
 * 
 * @author marco
 *	
 */
public class CommandProtocol
{
	public static final String MESSAGE_HI = "HI";
	public static final String MESSAGE_BYE = "BYE";
	public static final String MESSAGE_OK = "OK";
	public static final String MESSAGE_KO = "KO";
	public static final String MESSAGE_SERVER_SHUTDOWN = "SERVER_SHUTDOWN";
	public static final String MESSAGE_SERVER_STATUS = "SERVER_STATUS";
	//Marco
	public static final String MESSAGE_DIR_CONTENT = "DIR_CONTENT";
	public static final String MESSAGE_CONNECT_MACHINE = "CONNECT_MACHINE";
	public static final String MESSAGE_ADD_BOLT = "ADD_BOLT"; 
	public static final String MESSAGE_REMOVE_BOLT = "REMOVE_BOLT";
	public static final String MESSAGE_REMOVE_ALL_BOLTS = "REMOVE_ALL_BOLTS";
	
	public static final String MESSAGE_FILE_ADDED = "file_added";
	public static final String MESSAGE_FILE_DELETED = "file_deleted";
	public static final String MESSAGE_FILE_CHANGED = "file_changed";
	public static final String MESSAGE_DIRECTORY_ADDED = "directory_added";
	public static final String MESSAGE_DIRECTORY_DELETED = "directory_deleted";
	public static final String MESSAGE_DIRECTORY_CHANGED = "directory_changed";
	
	public static final String COMMAND_EXIT = "exit";
	public static final String COMMAND_VER = "ver";
	public static final String COMMAND_CONNECT = "connect";
	public static final String COMMAND_DISCONNECT = "disconnect";
	public static final String COMMAND_STATUS = "status";
	public static final String COMMAND_HELP = "help";
	public static final String COMMAND_SAY = "say";
	public static final String COMMAND_SERVER_SHUTDOWN = "server_shutdown";
	public static final String COMMAND_SERVER_STATUS = "server_status";
	//Marco
	public static final String COMMAND_DIR_CONTENT= "dir_content";
	public static final String COMMAND_CONNECT_MACHINE= "connect_machine";
	public static final String COMMAND_ADD_BOLT = "add_bolt"; 
	public static final String COMMAND_REMOVE_BOLT = "remove_bolt"; 
	public static final String COMMAND_REMOVE_ALL_BOLTS = "remove_all_bolts";
}
