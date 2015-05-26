import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import cn.easyproject.easyocr.*;

import org.opencv.core.*;
import org.opencv.imgproc.*;
import org.opencv.highgui.*;

/**
 * @author Joshua
 *
 */
/**
 * @author Joshua
 *
 */
public class bsMenu {	
	
	//decode captcha image
	static String SolveCaptcha(String file,String imgFolder,String OutImgFolder){
		 int erosion_size = 1;	     
	     Mat erodElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new  Size(2*erosion_size +1, 2*erosion_size+1));
	     Mat origImage = Highgui.imread( imgFolder+file);
	     Mat erosion= new Mat();
	     Mat outImage = new Mat();
		 Imgproc.erode(origImage, erosion, erodElement);
		 Imgproc.medianBlur(erosion, outImage, 3);
		 Highgui.imwrite(OutImgFolder+file, outImage);
		 EasyOCR ocr=new EasyOCR();
		 return ocr.discernAndAutoCleanImage(OutImgFolder+file,ImageType.CAPTCHA_WHITE_CHAR).replaceAll("(?i)[^a-zA-Z0-9\u4E00-\u9FA5]", "");
			 
	}
	
	//save image from URL
	static void saveImage(String imageUrl, String destinationFile) throws IOException {
		URL url = new URL(imageUrl);
		InputStream is = url.openStream();
		OutputStream os = new FileOutputStream(destinationFile);

		byte[] b = new byte[2048];
		int length;

		while ((length = is.read(b)) != -1) {
			os.write(b, 0, length);
		}

		is.close();
		os.close();
	}
	
	//write input stream into outputstream
	static void dump(InputStream src, OutputStream dest) throws IOException{
		try(InputStream input=src;OutputStream output=dest){
			byte[] data= new byte[1024];
			int length;
			while((length=input.read(data))!=-1){
				output.write(data,0, length);
			}
		}
	}
	
	/**
	 download data from http://bsr.twse.com.tw/bshtm/
     * @param listedCompany A file path of list company csv file.
     * @param imgFolder A folder path of image folder for captcha images.
     * @param OutImgFolder A folder path of image folder for pre-processed captcha images.
     * @param csvFolder A folder path of CSV files.
     * @param logFile A file path for log. 
	 * @throws Exception 

     */
	static void bsMenuDownloader(String listedCompany,String imgFolder,String OutImgFolder,String csvFolder) throws Exception{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		//read listed company
		FileReader ir = new FileReader(listedCompany);
		String str;	
		List<String> listCom=new ArrayList<String>();
		BufferedReader listComReade = new BufferedReader(ir);
		while((str=listComReade.readLine())!=null){
			String[] record = str.split(",");
			listCom.add(record[0]);
		}
		listComReade.close();
		LogWriter logger;
		//log file output
		logger = new LogWriter(csvFolder+"/log.txt");
		
		for(String id:listCom){
			String bsrURL="http://bsr.twse.com.tw/bshtm/";
			String  targetPage="bsMenu.aspx";
			String downloadPage="bsContent.aspx";
			//post data
			String VIEWSTATE ="";
			String EVENTVALIDATION="";
			String RadioButton_Normal="RadioButton_Normal";
			String TextBox_Stkno=id;
			String CaptchaControl1="";
			String btnOK="查詢";
			
			
			CloseableHttpClient httpclient = HttpClients.createDefault();
			boolean pass=false;
			
			File outCSVFile=new File(csvFolder+id+".csv");
			//check file exists or id is empty
			if(outCSVFile.exists() || id.trim().length()==0){
				//System.out.println(id.length());
				continue;
			}
				
			//Failures
			int capFail=0;

			do{
				//get page info
				HttpGet httpGet = new HttpGet(bsrURL+targetPage);
				CloseableHttpResponse initRes = httpclient.execute(httpGet);
				HttpEntity initResEntity = initRes.getEntity();
				org.jsoup.nodes.Document  initResDoc = Jsoup.parse(EntityUtils.toString(initResEntity));
				VIEWSTATE=initResDoc.getElementById("__VIEWSTATE").val();
				EVENTVALIDATION=initResDoc.getElementById("__EVENTVALIDATION").val();
				Elements imgList= initResDoc.getElementsByTag("img");
				Element imgCaptcha = imgList.get(1);
				String imgURL = bsrURL+imgCaptcha.attr("src");
				String imgid=imgCaptcha.attr("src").substring(imgCaptcha.attr("src").indexOf("guid=")+5);
				capFail+=1;
				try{
					saveImage(imgURL,imgFolder+imgid+".jpg");
				}catch(IOException e){
					Thread.sleep(5000);
					continue;
				}					
				CaptchaControl1=SolveCaptcha(imgid+".jpg",imgFolder,OutImgFolder);
					//System.out.println(imgid+".jpg");
					//System.out.println(CaptchaControl1);
						
				HttpUriRequest queryPost = RequestBuilder.post()
		                    .setUri(new URI(bsrURL+targetPage))
		                    .addParameter("__VIEWSTATE", VIEWSTATE)
		                    .addParameter("__EVENTVALIDATION", EVENTVALIDATION)
		                    .addParameter("RadioButton_Normal", RadioButton_Normal)
		                    .addParameter("TextBox_Stkno", TextBox_Stkno)
		                    .addParameter("CaptchaControl1", CaptchaControl1)
		                    .addParameter("btnOK", btnOK)
		                    .build();
				Thread.sleep(1000);
				CloseableHttpResponse queryPostRes = httpclient.execute(queryPost);
				HttpEntity queryPostEntity = queryPostRes.getEntity();
					 //System.out.println(EntityUtils.toString(entity2));
				org.jsoup.nodes.Document queryPostEntityDoc= Jsoup.parse(EntityUtils.toString(queryPostEntity));
				Element csvlink = queryPostEntityDoc.getElementById("HyperLink_DownloadCSV");
					 //System.out.println(csvlink.html());
				 //if label error msg show no data break the loop
				String errMsg= queryPostEntityDoc.getElementById("Label_ErrorMsg").text();
				 if(errMsg.equals("查無資料")){
					 logger.writeLog(TextBox_Stkno+";"+errMsg+";驗證次數:"+capFail);
					 capFail=0;
					 PrintWriter emptyFile = new PrintWriter(outCSVFile);
					 emptyFile.write(errMsg);
					 emptyFile.close();
					 break;
				 }
				 //check if the CSV link exist
				 if(csvlink != null){
					 pass=true;
					 HttpGet CSVGet = new HttpGet(bsrURL+downloadPage);
					 Thread.sleep(1000);
					 CloseableHttpResponse CSVRes = httpclient.execute(CSVGet);
					 HttpEntity CSVentity = CSVRes.getEntity();
					 //System.out.println(EntityUtils.toString(CSVentity));
					 BufferedInputStream CSVIS = new BufferedInputStream(CSVentity.getContent());
					 FileOutputStream CSVFOS = new FileOutputStream(outCSVFile);
					 //write csvis into csvfos
					 dump(CSVIS,CSVFOS);
					 CSVFOS.close();
					 CSVIS.close();
					 logger.writeLog(TextBox_Stkno+";輸出成功;驗證次數:"+capFail);
					 capFail=0;
				 }
					 
			}while(!pass);
			
			httpclient.close();		
			
			Thread.sleep(Math.round(Math.random()*3000));
		}// end of for loop
	}
	
	
	public static void main(String[] args) {
		
		//驗證圖片儲存位置
		String  imgFolder="C:/Temp/captcha/";
		//處理後驗證圖片儲存位置
		String  OutImgFolder="C:/Temp/outs/";
		//上市公司列表
		String listedCompany="C:/Users/Joshua/Google 雲端硬碟/BIGDATA/ZB101上課資料分享區/上市日報/listcompany.csv";
		//csv檔儲存位置
		String csvFolder="C:/Users/Joshua/Google 雲端硬碟/BIGDATA/ZB101上課資料分享區/上市日報/20150526/";
		//log file
		//String logFile="bsdata/log.csv";
		try{
			bsMenuDownloader(listedCompany,imgFolder,OutImgFolder,csvFolder);
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

}
