package ru.kvaga.investments.stocks;

public class StockItem {
	private String name;
	private double traceablePrice;
	private double lastPrice;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public double getTraceablePrice() {
		return traceablePrice;
	}
	public void setTraceablePrice(double traceablePrice) {
		this.traceablePrice = traceablePrice;
	}
	public double getLastPrice() {
		return lastPrice;
	}
	public void setLastPrice(double lastPrice) {
		this.lastPrice = lastPrice;
	}
	
}
