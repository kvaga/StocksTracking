import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import ru.kvaga.investments.stocks.StockItem;
import ru.kvaga.investments.stocks.StocksTrackingException;
import ru.kvaga.investments.stocks.StocksTrackingException.GetContentOFSiteException;
import ru.kvaga.investments.stocks.StocksTrackingException.GetCurrentPriceOfStockException.Common;
import ru.kvaga.investments.stocks.StocksTrackingException.GetCurrentPriceOfStockException.ParsingResponseException;
import ru.kvaga.investments.stocks.StocksTrackingException.StoreDataException;
import ru.kvaga.telegram.sendmessage.TelegramSendMessage;

public class Exec {

	private static ru.kvaga.telegram.sendmessage.TelegramSendMessage telegramSendMessage=null;
	
	// Specify a configuration file with token and channelName information
	private static String envFilePath;
	static String token;
	static String channelName;
	private static String[] consoleArguments;
	
	public static void main(String[] args) throws Exception {
		String dataFileName="data/StocksTracking.csv";
		String configFileName="conf/TelegramSendMessage.env";
		String URL_TEXT_MOEXX="https://iss.moex.com/iss/engines/stock/markets/shares/boards/TQBR/securities.xml?iss.meta=off&iss.only=securities&securities.columns=SECID,PREVADMITTEDQUOTE";
//		String URL_TEXT_TINKOFF="https://www.tinkoff.ru/invest/stocks/"+stockShortName+"/";;
		String URL_TEXT_TINKOFF="https://www.tinkoff.ru/invest/stocks/%s/";

		ArrayList<StockItem> actualStockItems = new ArrayList<StockItem>();
		try {
			// Init
			getParameters(configFileName);
			telegramSendMessage = new ru.kvaga.telegram.sendmessage.TelegramSendMessage(token, channelName, TelegramSendMessage.PARSE_MODE_HTML, TelegramSendMessage.WEB_PAGE_PREVIEW_DISABLE);
			
			// Work
			for(StockItem si: getListOfStocks(new File(dataFileName))) {
				StockItem actualStockItem = new StockItem();
				double currentPrice = 0;
				actualStockItem.setName(si.getName());
				actualStockItem.setTraceablePrice(si.getTraceablePrice());
				if(!si.getName().startsWith("#")) {
					String url=String.format(URL_TEXT_TINKOFF, si.getName());
					System.out.println("Url is ready: " + url);
					String response=getContentOfSite(si.getName(), url);
					System.out.println("The response received");
					String fullName=getFullNameOfStock(response, si.getName(), url);
					System.out.println("Full name received: " + fullName);
					currentPrice = getCurrentPriceOfStock(si.getName(), response, url);
					System.out.println("Current price received: " + currentPrice);
					actualStockItem.setLastPrice(currentPrice);

					if(si.getTraceablePrice() > currentPrice) {
						telegramSendMessage.sendMessage(
								"Stock: <a href='https://tinkoff.ru/invest/stocks/"+si.getName()+"/'>"+si.getName()+"</a> " + fullName
								+ TelegramSendMessage.LINEBREAK
								+ "Tracking Price: " + si.getTraceablePrice()
								+ TelegramSendMessage.LINEBREAK 
								+ "Last Price: " + si.getLastPrice()
								+ TelegramSendMessage.LINEBREAK
								+ "Current Price: " + currentPrice
								+ TelegramSendMessage.LINEBREAK
								+ "("+ String.format("%.2f", currentPrice*100.0/si.getTraceablePrice()-100) +"% from Tracking Price, "
								+ "("+ String.format("%.2f",currentPrice*100.0/si.getLastPrice()-100) +"% from Last Price)"
										
							);
					}
				}else {
					actualStockItem.setLastPrice(si.getLastPrice());
				}
				actualStockItems.add(actualStockItem);
				
			}
			// save data
			storeActualData(new File(dataFileName), actualStockItems);
			
		}catch(Exception e) {
//			System.err.println(e);
			e.printStackTrace();
			telegramSendMessage.sendMessage("Error: " + e.getMessage());
		}
	}

	private static void storeActualData(File file, ArrayList<StockItem> al) throws StoreDataException {
		StringBuilder sb = new StringBuilder();
		for(StockItem si : al) {
			sb.append(si.getName());
			sb.append(",");
			sb.append(si.getTraceablePrice());
			sb.append(",");
			sb.append(si.getLastPrice());
			sb.append("\n");
		}
		FileOutputStream fos=null;
		try {
			fos = new FileOutputStream(file);
			fos.write(sb.toString().getBytes());
			fos.flush();
		} catch (Exception e) {
			throw new StocksTrackingException.StoreDataException(String.format("Couldn't store data to %s file. %s", file.getAbsoluteFile(), e.getMessage()));
		}finally {
			try {
				if(fos!=null) {
					fos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
	}

	private static String getFullNameOfStock(String response, String stockName, String urlText) throws ru.kvaga.investments.stocks.StocksTrackingException.GetFullStockNameException.ParsingResponseException {
		String REGEX_PATTERN_TEXT_TINKOFF_FULL_NAME="<meta charset=\"UTF-8\">.*" + 
				"<title data-meta-dynamic=\"true\">Купить акции (?<fullName>.*) \\("+stockName+"\\).*</title>.*" + 
				"<meta property=\"og:title\"" ;
		Pattern patternForFullName = Pattern.compile(REGEX_PATTERN_TEXT_TINKOFF_FULL_NAME);
		Matcher matcherForFullName = patternForFullName.matcher(response);
		String stockFullName;
		if(matcherForFullName.find()) {
			stockFullName=matcherForFullName.group("fullName");
		}else {
			throw new StocksTrackingException.
			GetFullStockNameException.
			ParsingResponseException(String.format("Couldn't find fullName for stock during parsing web response with "
					+ "regex pattern text [%s]. \n"
//					+ "Web response [%s]"
					,
					REGEX_PATTERN_TEXT_TINKOFF_FULL_NAME
//					,response
					), 
					urlText);
		}
		return stockFullName;
		
	}
	
	private static String getContentOfSite(String stockShortName, String urlText) throws Common, GetContentOFSiteException {
		
		
		
			URL url = null;
			HttpURLConnection con=null;
			BufferedReader br=null;
			String s;
			StringBuilder sb = new StringBuilder();
			
			try {
				url = new URL(urlText);
				con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("GET");
				br = new BufferedReader(new InputStreamReader(con.getInputStream()));
				while ((s = br.readLine()) != null) {
					sb.append(s);
				}
				return sb.toString();
			} catch (IOException e) {
				throw new StocksTrackingException.GetContentOFSiteException(e.getMessage(), stockShortName, urlText);
			}finally {
				if(br!=null) {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

	}
	
	private static double getCurrentPriceOfStock(String name, String response, String url) throws Common, ParsingResponseException {		
			String REGEX_PATTERN_TEXT_MOEXX= "\\<row SECID=\""+name+"\" PREVADMITTEDQUOTE=\"(?<lastPrice>\\d+\\.{0,1}\\d*)\" />";
			String REGEX_PATTERN_TEXT_TINKOFF=
					"<div class=\"GridColumn__column_2h5Ek GridColumn__column_hidden_on_phone_15UiO GridColumn__column_hidden_on_tabletS_G1iCc GridColumn__column_hidden_on_tabletL_3WX2Z.*"
					+ "<span class=\"Money__money_3_Tn4\" data-qa-type=\"uikit\\/money\">(?<currentPrice>\\d+.*<span>.*)[₽|$]<\\/span><\\/span><\\/span>";
	
			String regexPatternText=REGEX_PATTERN_TEXT_TINKOFF;
			//			System.out.println(response);
			Pattern pattern = Pattern.compile(regexPatternText);
			Matcher matcher = pattern.matcher(response);
			if(matcher.find()) {
//				System.out.println(String.format("Last price of %s: %s", name, matcher.group("lastPrice")));
				String str = matcher.group("currentPrice");
				str=str.replaceAll("<!-- -->", "").replaceAll("<span>", "").replaceAll(" ", "").replaceAll(",", ".");
				return Double.parseDouble(str);
			}else {
				throw new StocksTrackingException.
				GetCurrentPriceOfStockException.
				ParsingResponseException(String.format("Couldn't find lastPrice value for stock during parsing web response with "
						+ "regex pattern text [%s]. \n"
						+ "Web response [%s]",regexPatternText, response), name, url);
			}
		}

	private static ArrayList<StockItem> getListOfStocks(File file) throws StocksTrackingException{
		ArrayList<StockItem> al = new ArrayList<StockItem>();
		BufferedReader br=null;
		String s;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			while((s = br.readLine())!=null) {
				if(s.contains(",")) {
					String mas[] = s.split(",");
					if(mas.length==3) {
							StockItem si = new StockItem();
							si.setName(mas[0]);
							si.setTraceablePrice(Double.parseDouble(mas[1]));
							si.setLastPrice(Double.parseDouble(mas[2]));
							al.add(si);
					}else {
						throw new StocksTrackingException.ReadStockItemsFileException.Common(String.format("Couldn't parse the '%s' row and get "
								+ "complete information for stock. Massive length is '%s'", s, mas.length), file);
					}
				}else {
					throw new StocksTrackingException.ReadStockItemsFileException.IncorrectFormatOfRow(
							String.format("The row %s doesn't contain ',' symbol", s), file);
				}
			}
			return al;
		} catch (FileNotFoundException e) {
			throw new StocksTrackingException.ReadStockItemsFileException.ItemsFileNotFound(
					String.format("Can't find file which stores stock items"), file);
		} catch (IOException e) {
			throw new StocksTrackingException.ReadStockItemsFileException.Common(e.getMessage(), file);
		}finally {
			if(br!=null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	
	private static void getParameters(String filePath) throws FileNotFoundException, IOException {
		Properties props = new Properties();
		props.load(new FileInputStream(new File(filePath)));
		
		System.out.print(String.format("Reading information from the %s configuration file ... ", filePath));
		token=props.getProperty("token");
		System.out.print(String.format("token=%s ", token));
		channelName=props.getProperty("channelName");
		System.out.println(String.format("channelName=%s ", channelName));
	}
	
	
	private synchronized static void sendMessageToTelegram(String message) throws Exception {
		System.out.print("Sending message ... ");
		telegramSendMessage.sendMessage(message);
		System.out.println("[OK]. The message was sent");
		
	}
}
