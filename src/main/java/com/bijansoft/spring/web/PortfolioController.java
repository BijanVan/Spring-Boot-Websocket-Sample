package com.bijansoft.spring.web;

import com.bijansoft.spring.domain.Portfolio;
import com.bijansoft.spring.domain.PortfolioPosition;
import com.bijansoft.spring.domain.Trade;
import com.bijansoft.spring.service.PortfolioService;
import com.bijansoft.spring.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Controller
public class PortfolioController {
    private static final Logger logger = Logger.getLogger(PortfolioController.class.getName());

    private final PortfolioService portfolioService;
    private final TradeService tradeService;

    @Autowired
    public PortfolioController(PortfolioService portfolioService, TradeService tradeService) {
        this.portfolioService = portfolioService;
        this.tradeService = tradeService;
    }

    @RequestMapping("/")
    String GetIndex() {
        return "index.html";
    }

    @SubscribeMapping("/positions")
    public List<PortfolioPosition> getPositions(Principal principal) throws Exception {
        logger.log(Level.INFO, "Positions for " + principal.getName());
        Portfolio portfolio = this.portfolioService.findPortfolio(principal.getName());
        return portfolio.getPositions();
    }

    @MessageMapping("/trade")
    public void executeTrade(Trade trade, Principal principal) {
        trade.setUsername(principal.getName());
        logger.log(Level.INFO, "Trade: " + trade);
        this.tradeService.executeTrade(trade);
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception) {
        return exception.getMessage();
    }
}
