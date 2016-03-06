package communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.jcraft.jsch.UserInfo;

public class AccessInfo implements UserInfo{
	
	private String passwd;
	private String passphrase = "";
	

	public AccessInfo(String password){
		if (password == null) 
			this.passwd = "";
		else
			this.passwd = password;
			System.out.println("[SI] Password set");
	}
	
	@Override
	public String getPassword(){
		return this.passwd;
	}
	
	@Override
	public String getPassphrase(){
		return this.passphrase;
	}
	

	@Override
	public boolean promptPassphrase(String message) {
	  //Reset previous passphrase
	    passphrase = null;
	  
	    System.out.println(message);

		passphrase = readOneLine();
	
	    if (passphrase != null && passphrase.length() > 0) {
	      return true;
	    } else {
	      return false;
	    }
	}
	
	//Prompt user to input password
	  @Override
	public boolean promptPassword(String message) {
		  // Reset previous password
	    this.passwd = null;
	    
	    System.out.println(message);

	    this.passwd = readOneLine();
	    
	    if (passwd != null && passwd.length() > 0) {
	      return true;
	    } else {
	      return false;
	    }
	  }
	  
	  //As the user to connect to a non verified host
	  @Override
	  public boolean promptYesNo(String message) {

	    System.out.println(message);

	    String s = readOneLine();
	    if ("yes".equalsIgnoreCase(s)) {
	      return true;
	    }

	    return false;
	  }

	  // Show a framework message
	  @Override
	  public void showMessage(String message) {
	    System.out.println(message);
	  }
	  
	private String readOneLine() {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		try {
			return in.readLine();
		} catch (IOException e) {e.printStackTrace(); return null;}
	}
}
