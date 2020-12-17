package ru.kvaga.investments.stocks;

import java.io.File;

public class StocksTrackingException extends Exception{
	private StocksTrackingException(String message) {
		super(message);
	}
	
	public static class ReadStockItemsFileException extends StocksTrackingException {
		private ReadStockItemsFileException(String message, File file) {
			super(String.format("Encoured a problem during reading the %s file process. %s", file.getAbsoluteFile(), message));
		}
		
		public static class ItemsFileNotFound extends ReadStockItemsFileException {
			public ItemsFileNotFound(String message, File file) {
				super(message, file);
			}
		}
		
		public static class IncorrectFormatOfRow extends ReadStockItemsFileException {
			public IncorrectFormatOfRow(String message, File file) {
				super(message, file);
			}
		}
		
		public static class Common extends ReadStockItemsFileException {
			public Common(String message, File file) {
				super(message, file);
			}
		}
	}
	
	public static class GetCurrentPriceOfStockException extends StocksTrackingException{
		private GetCurrentPriceOfStockException(String message, String stockName, String url) {
			super(String.format("\nAn error has occured during getting current price of stock [%s] for URL [%s] \n", stockName, url) + message);
		}
		
		public static class ParsingResponseException extends GetCurrentPriceOfStockException{
			public ParsingResponseException(String message, String stockName, String url) {
				super(message, stockName, url);
			}
		}
		
		public static class Common extends GetCurrentPriceOfStockException{
			public Common(String message, String stockName, String url) {
				super(message, stockName, url);
			}
		}
	}
	
	public static class StoreDataException extends StocksTrackingException{
		public StoreDataException(String message) {
			super(message);
		}
	}
	
}
