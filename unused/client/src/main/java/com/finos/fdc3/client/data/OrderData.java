/**
 * Copyright 2023 Wellington Management Company LLP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.finos.fdc3.client.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({
    "orderId",
    "symbol",
    "side",
    "orderQty",
    "side",
    "trader"
})
@JsonIgnoreProperties(ignoreUnknown=true)
public class OrderData
{
private long orderId;
private String symbol;
private BigDecimal orderQty;
private String side;
private String trader;

public long getOrderId()
{
    return orderId;
}

public void setOrderId(long orderId)
{
    this.orderId = orderId;
}

public String getSymbol()
{
    return symbol;
}

public void setSymbol(String symbol)
{
    this.symbol = symbol;
}

public BigDecimal getOrderQty()
{
    return orderQty;
}

public void setOrderQty(BigDecimal orderQty)
{
    this.orderQty = orderQty;
}

public String getSide()
{
    return side;
}

public void setSide(String side)
{
    this.side = side;
}

public String getTrader()
{
    return trader;
}

public void setTrader(String trader)
{
    this.trader = trader;
}

@Override
public boolean equals(Object o)
{
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OrderData orderData = (OrderData) o;
    return orderId == orderData.orderId;
}

@Override
public int hashCode()
{
    return Objects.hash(orderId);
}

}
