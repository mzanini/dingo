package dingo.server.filesystem;
import java.nio.file.Path;


public class UpdateEvent {
	private Path file;
	private String type;
	
	public UpdateEvent(Path file, String type){
		this.file = file;
		this.type = type;
	}
	
	public Path getFile() {
		return file;
	}

	public String getType() {
		return type;
	}

}
