package com.bijansoft.spring.service;

import com.bijansoft.spring.domain.Portfolio;
import com.bijansoft.spring.domain.PortfolioPosition;
import com.bijansoft.spring.domain.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class TradeServiceImpl implements TradeService {
    private static final Logger logger = Logger.getLogger(TradeServiceImpl.class.getName());

    private final SimpMessageSendingOperations messagingTemplate;
    private final PortfolioService portfolioService;
    private final List<TradeResult> tradeResults = new CopyOnWriteArrayList<>();

    @Autowired
    public TradeServiceImpl(SimpMessageSendingOperations messagingTemplate, PortfolioService portfolioService) {
        this.messagingTemplate = messagingTemplate;
        this.portfolioService = portfolioService;
    }

    @Override
    public void executeTrade(Trade trade) {
        Portfolio portfolio = this.portfolioService.findPortfolio(trade.getUsername());
        String ticker = trade.getTicker();
        int sharesToTrade = trade.getShares();

        PortfolioPosition newPosition = (trade.getAction() == Trade.TradeAction.Buy) ?
                portfolio.buy(ticker, sharesToTrade) : portfolio.sell(ticker, sharesToTrade);

        if (newPosition == null) {
            String payload = "Rejected trade " + trade;
            this.messagingTemplate.convertAndSendToUser(trade.getUsername(), "/queue/errors", payload);
            return;
        }

        this.tradeResults.add(new TradeResult(trade.getUsername(), newPosition));
    }

    @Scheduled(fixedDelay = 1500)
    public void sendTradeNotifications() {
        Map<String, Object> map = new HashMap<>();
        map.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON);

        this.tradeResults.stream()
                .filter(result -> System.currentTimeMillis() >= (result.timestamp + 1500))
                .forEach(result -> {
                    logger.log(Level.INFO, "Sending position update: " + result.position);
                    this.messagingTemplate.convertAndSendToUser(result.user, "/queue/position-updates",
                            result.position, map);
                    this.tradeResults.remove(result);
                });
    }

    private static class TradeResult {

        private final String user;
        private final PortfolioPosition position;
        private final long timestamp;

        public TradeResult(String user, PortfolioPosition position) {
            this.user = user;
            this.position = position;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
