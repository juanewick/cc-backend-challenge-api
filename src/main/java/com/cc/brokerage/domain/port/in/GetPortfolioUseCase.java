package com.cc.brokerage.domain.port.in;

import com.cc.brokerage.domain.model.Portfolio;

public interface GetPortfolioUseCase {

    Portfolio getPortfolio(String accountNumber);
}
