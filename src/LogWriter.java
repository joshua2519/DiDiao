import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;


public class LogWriter{
	File logFile;
	
	public LogWriter(String filePath) throws IOException{
		logFile = new File(filePath);
		
	}
	
	public void writeLog(String msg) throws IOException{		
		FileWriter logWriter =new FileWriter(logFile,true);
		Date cal = Calendar.getInstance().getTime();		
		SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		String now = df.format(cal);
		logWriter.write(now+";"+msg+"\n");
		System.out.println(now+";"+msg);
		logWriter.flush();
		logWriter.close();
	}
	
	
	
	
}
