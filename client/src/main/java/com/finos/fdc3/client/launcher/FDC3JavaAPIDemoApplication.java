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

package com.finos.fdc3.client.launcher;

import com.finos.fdc3.api.context.Context;
import com.finos.fdc3.api.context.Instrument;
import com.finos.fdc3.api.metadata.ContextMetadata;
import com.finos.fdc3.api.types.ContextHandler;
import com.finos.fdc3.client.data.IncomingData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finos.fdc3.adapter.openfin.OpenFinDesktopAgent;
import com.finos.fdc3.adapter.openfin.OpenFinFDC3Config;
import com.finos.fdc3.api.DesktopAgent;
import com.finos.fdc3.client.AppUI;
import com.openfin.desktop.RuntimeConfiguration;
import com.finos.fdc3.api.context.Position;

import java.math.BigDecimal;

public class FDC3JavaAPIDemoApplication
{

private static final Logger LOGGER = LoggerFactory.getLogger(FDC3JavaAPIDemoApplication.class);

private static DesktopAgent desktopAgent;
    
public static void main(String[] arg)
{
    try {
        //1. Build UI
        AppUI ui = new AppUI();
        ui.init();
        
        // 2. FDC3 Desktop Agent
        DesktopAgent desktopAgent = createDesktopAgent();
        ui.getOrderMessagePublisher().setDesktopAgent(desktopAgent);
        ui.getSystemChannelHandler().setDesktopAgent(desktopAgent);
        desktopAgent.addContextListener(Instrument.TYPE, new ContextHandler()
        {
            @Override
            public void handleContext(Context context, ContextMetadata metadata)
            {
                LOGGER.info(String.format("Received the context :%s", context));
                try {
                    IncomingData data = new IncomingData();
                    data.setName(context.getName());
                    data.setIsin((String) context.getId().getOrDefault("ISIN", null));
                    data.setRic((String)context.getId().getOrDefault("RIC", null));
                    data.setTicker((String)context.getId().getOrDefault("ticker", null));
                    ui.getIncomingDataTableModel().addData(data);
                }catch (Throwable t) {
                    LOGGER.error(String.format("Unable to handle the request :%s ", context));
                }
            }
        });

        desktopAgent.addContextListener("fdc3.position", new ContextHandler()
        {
            @Override
            public void handleContext(Context context, ContextMetadata metadata)
            {
                LOGGER.info(String.format("Received the context :%s", context));
                try {
                    IncomingData data = new IncomingData();
                    Position position = (Position)context;
                    data.setName(position.getName());
                    Instrument instrument = position.getInstrument();
                    if(instrument!=null && instrument.getId()!=null) {

                        data.setIsin((String)context.getId().getOrDefault("ISIN", null));
                        data.setRic((String)context.getId().getOrDefault("RIC", null));
                        data.setTicker((String)context.getId().getOrDefault("ticker", null));
                    }
                    data.setHolding(position.getHolding());
                    ui.getIncomingDataTableModel().addData(data);
                }catch (Throwable t) {
                    LOGGER.error("Unable to handle the request : {}", context, t);
                }
            }
        });

    } catch (Exception t) {
        LOGGER.error("Unable to launch application", t);
    }
}

private static DesktopAgent createDesktopAgent() {

    OpenFinFDC3Config fdc3Config = new OpenFinFDC3Config();
    fdc3Config.setUuid("fdc3-java-demo");
    fdc3Config.setBrokerName("fdc3-platform");
    RuntimeConfiguration runtimeConfig = new RuntimeConfiguration();
    runtimeConfig.setRuntimeVersion("22.94.66.4");
    fdc3Config.setRuntimeConfiguration(runtimeConfig);
    desktopAgent =  new OpenFinDesktopAgent(fdc3Config);
    return desktopAgent;
}

}