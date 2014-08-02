package com.bijansoft.spring.domain;

public class PortfolioPosition {

    private final String company;

    private final String ticker;

    private final double price;

    private final int shares;

    private final long updateTime;


    public PortfolioPosition(String company, String ticker, double price, int shares) {
        this.company = company;
        this.ticker = ticker;
        this.price = price;
        this.shares = shares;
        this.updateTime = System.currentTimeMillis();
    }

    public PortfolioPosition(PortfolioPosition other, int sharesToAddOrSubtract) {
        this.company = other.company;
        this.ticker = other.ticker;
        this.price = other.price;
        this.shares = other.shares + sharesToAddOrSubtract;
        this.updateTime = System.currentTimeMillis();
    }

    public String getCompany() {
        return this.company;
    }

    public String getTicker() {
        return this.ticker;
    }

    public double getPrice() {
        return this.price;
    }

    public int getShares() {
        return this.shares;
    }

    public long getUpdateTime() {
        return this.updateTime;
    }

    @Override
    public String toString() {
        return "PortfolioPosition [company=" + this.company + ", ticker=" + this.ticker
                + ", price=" + this.price + ", shares=" + this.shares + "]";
    }

}

