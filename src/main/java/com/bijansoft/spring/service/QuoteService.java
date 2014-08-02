package com.bijansoft.spring.service;

import com.bijansoft.spring.domain.Quote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class QuoteService implements ApplicationListener<BrokerAvailabilityEvent> {
    private static final Logger logger = Logger.getLogger(QuoteService.class.getName());

    private final MessageSendingOperations<String> messagingTemplate;
    private final StockQuoteGenerator quoteGenerator = new StockQuoteGenerator();
    private AtomicBoolean brokerAvailable = new AtomicBoolean();

    @Autowired
    public QuoteService(MessageSendingOperations<String> messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onApplicationEvent(BrokerAvailabilityEvent event) {
        this.brokerAvailable.set(event.isBrokerAvailable());
    }

    @Scheduled(fixedDelay=2000)
    public void sendQuotes() {
        this.quoteGenerator.generateQuotes().stream().filter(quote -> this.brokerAvailable.get()).forEach(quote -> {
//            logger.log(Level.INFO, "Sending quote " + quote);
            this.messagingTemplate.convertAndSend("/topic/price.stock." + quote.getTicker(), quote);
        });
    }

    private static class StockQuoteGenerator {
        private static final MathContext mathContext = new MathContext(2);
        private final Random random = new Random();
        private final Map<String, String> prices = new ConcurrentHashMap<>();

        public StockQuoteGenerator() {
            this.prices.put("CTXS", "24.30");
            this.prices.put("DELL", "13.03");
            this.prices.put("EMC", "24.13");
            this.prices.put("GOOG", "893.49");
            this.prices.put("MSFT", "34.21");
            this.prices.put("ORCL", "31.22");
            this.prices.put("RHT", "48.30");
            this.prices.put("VMW", "66.98");
        }

        public Set<Quote> generateQuotes() {
            Set<Quote> quotes = new HashSet<>();
            for (String ticker : this.prices.keySet()) {
                BigDecimal price = getPrice(ticker);
                quotes.add(new Quote(ticker, price));
            }
            return quotes;
        }

        private BigDecimal getPrice(String ticker) {
            BigDecimal seedPrice = new BigDecimal(this.prices.get(ticker), mathContext);
            double range = seedPrice.multiply(new BigDecimal(0.02)).doubleValue();
            BigDecimal priceChange = new BigDecimal(String.valueOf(this.random.nextDouble() * range), mathContext);
            return seedPrice.add(priceChange);
        }
    }
}
