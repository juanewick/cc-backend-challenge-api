package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.domain.port.in.GetPortfolioUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiBase.PATH + "/v1/accounts/{accountNumber}/portfolio")
public class PortfolioController {

    private final GetPortfolioUseCase getPortfolio;

    public PortfolioController(GetPortfolioUseCase getPortfolio) {
        this.getPortfolio = getPortfolio;
    }

    @GetMapping
    public PortfolioResponse get(@PathVariable String accountNumber) {
        return PortfolioResponse.from(getPortfolio.getPortfolio(accountNumber));
    }
}
