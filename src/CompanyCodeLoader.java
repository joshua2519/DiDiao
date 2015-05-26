import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;


//截取 上市/上櫃公司代號

public class CompanyCodeLoader {

	public static void main(String[] args) {
		
		//File file = new File("bsdata/listcompany.csv");//上市
		File file = new File("bsdata/listcompanyOTC.csv");//上櫃
		//File jsonFile=new File("E:\\MonthlyWeather.json");
		try{
			PrintWriter out = new PrintWriter(new FileWriter(file,false));
			
			StringBuilder sb= new StringBuilder(1024);
			
			//Document doc = Jsoup.connect("http://isin.twse.com.tw/isin/C_public.jsp?strMode=2").get();//上市
			Document doc = Jsoup.connect("http://isin.twse.com.tw/isin/C_public.jsp?strMode=4").get(); //上櫃
			Elements list;
			list = doc.getElementsByTag("table").get(1).children().get(0).children();
			sb.append("有價證券代號,名稱,國際證券辨識號碼,上市日,市場別,產業別,CFICode,備註");
			sb.append("\n");
			int count=0;
			for(int i=1;i<list.size();i++){
				if(list.get(i).children().size()<7)
					continue;
				Elements tdlist= list.get(i).children();
				String[] str= tdlist.get(0).text().split(" ");
				if(str.length<2){
					str= tdlist.get(0).text().split("　");
				}
				//System.out.println(tdlist.get(0).text().replace(" ", "/").replace("　", "/"));
				//System.out.println(tdlist.get(0).text().trim().length());
				String code=str[0];				
				String name=str[1].replace("　", "");
				
				String isin= tdlist.get(1).text();
				String listDate=tdlist.get(2).text();
				String type= tdlist.get(3).text();
				String indus= tdlist.get(4).text();
				String CFI= tdlist.get(5).text();
				String note= tdlist.get(6).text();
				sb.append(code+",").append(name+",").append(isin+",").append(listDate+",").append(type+",").append(indus+",").append(CFI+",").append(note);
				sb.append("\n");
				count+=1;
			}
			
			out.println(sb);
			out.close();
			System.out.println("輸出完成:"+count);

		}catch(IOException e){
			e.printStackTrace();
		}
		
	}

}
