package com.cc.brokerage.domain.model;

import java.math.BigDecimal;

/**
 * What the FILLED and NEW orders of one account add up to for one instrument.
 *
 * @param instrumentId         the instrument, or null for orders stored without one
 * @param netQuantity          shares bought minus shares sold, FILLED only
 * @param netInvested          pesos paid for buys minus pesos received for sells, FILLED only
 * @param reservedSellQuantity shares held by open (NEW) sells
 * @param reservedBuyAmount    pesos held by open (NEW) buys
 * @param cashFlow             pesos deposited minus pesos withdrawn, FILLED CASH_IN and CASH_OUT only
 */
public record InstrumentHoldings(
        Long instrumentId,
        long netQuantity,
        BigDecimal netInvested,
        long reservedSellQuantity,
        BigDecimal reservedBuyAmount,
        BigDecimal cashFlow
) {
}
