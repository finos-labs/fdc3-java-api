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

package com.finos.fdc3.client.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.finos.fdc3.api.utils.JacksonUtilities;
import com.finos.fdc3.client.data.OrderData;
import com.finos.fdc3.client.data.SnPData;

public class SampleDataLoaderUtil
{
private static int orderCounter = 101;
public static Collection<OrderData> loadSampleData()
{
    List<OrderData> orderDataList = new ArrayList<>();

    OrderData data1 = new OrderData();
    data1.setOrderId(orderCounter++);
    data1.setTrader("Demo User");
    data1.setSide("BUY");
    data1.setSymbol("IBM");
    data1.setOrderQty(new BigDecimal(10000));
    orderDataList.add(data1);

    OrderData data2 = new OrderData();
    data2.setOrderId(orderCounter++);
    data2.setTrader("Demo User");
    data2.setSide("BUY");
    data2.setSymbol("YAHOO");
    data2.setOrderQty(new BigDecimal(10000));
    orderDataList.add(data2);

    OrderData data3 = new OrderData();
    data3.setOrderId(orderCounter++);
    data3.setTrader("Demo User");
    data3.setSide("SELL");
    data3.setSymbol("IBM");
    data3.setOrderQty(new BigDecimal(10000));
    orderDataList.add(data3);

    OrderData data4 = new OrderData();
    data4.setOrderId(orderCounter++);
    data4.setTrader("Demo User");
    data4.setSide("SELL");
    data4.setSymbol("YAHOO");
    data4.setOrderQty(new BigDecimal(10000));
    orderDataList.add(data4);

    return orderDataList;

}

public static List<SnPData> readSPData()
{
    try {
        return JacksonUtilities.deserializeListFromString(
            readData(new URL("https://pkgstore.datahub.io/core/s-and-p-500-companies/constituents_json/data/297344d8dc0a9d86b8d107449c851cc8/constituents_json.json"))
            , SnPData.class);
    }catch(Throwable t) {

    }
    return Collections.emptyList();
}

private static String readData(URL url) throws IOException
{

    try (InputStream input = url.openStream()) {
        InputStreamReader isr = new InputStreamReader(input);
        BufferedReader reader = new BufferedReader(isr);
        StringBuilder json = new StringBuilder();
        int c;
        while ((c = reader.read()) != -1) {
            json.append((char) c);
        }
        return json.toString();
    }
}
}
