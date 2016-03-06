package commons;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class FunnyThings
{	
	/**
	 * Funny print of Dingo ASCII ART!
	 */
	public static void printDingo ()
	{
		FunnyThings.printAsciiArt( "dingo_ascii.txt" );
	}
	
	
	/**
	 * Funny print of a specific animal :)
	 * @param filename
	 */
	private static void printAsciiArt ( String filename )
	{
		String fullPath = System.getProperty("user.dir") + File.separator + "etc" + File.separator + filename;
		
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(new File(fullPath)));
			String line = null;
			while ( (line = br.readLine()) != null )
			{
				System.out.println( line );
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			//do nothing!
			//se non fa, pazienza.
		}
	}
	
}
