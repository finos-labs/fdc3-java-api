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

package com.finos.fdc3.client.publisher;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;

import com.finos.fdc3.client.AppUI;
import com.finos.fdc3.client.data.SnPData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finos.fdc3.api.DesktopAgent;
import com.finos.fdc3.api.context.Context;
import com.finos.fdc3.api.context.Instrument;
import com.finos.fdc3.api.utils.JacksonUtilities;
import com.finos.fdc3.client.data.OrderData;
import com.finos.fdc3.client.model.OrderTableModel;

public class OrderMessagePublisher implements ActionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderMessagePublisher.class);

    private AppUI appUI;

    private DesktopAgent desktopAgent;

    private JTable orderTable;

    public JTable getOrderTable() {

        return orderTable;
    }

    public void setOrderTable(JTable orderTable) {

        this.orderTable = orderTable;
    }

    public AppUI getAppUI()
    {
        return appUI;
    }

    public void setAppUI(AppUI appUI)
    {
        this.appUI = appUI;
    }

    public OrderData getSelectedRowData() {

        int row = getOrderTable().getSelectedRow();
        if (row >= 0) {
            return ((OrderTableModel) getOrderTable().getModel()).getData(row);
        }
        return null;
    }

    public DesktopAgent getDesktopAgent() {

        return desktopAgent;
    }

    public void setDesktopAgent(DesktopAgent desktopAgent) {

        this.desktopAgent = desktopAgent;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        int selectedTab = getAppUI().getOrderTabbedPane().getSelectedIndex();
        switch(selectedTab) {
            case 0: sendOrderData(); break;
            case 1: sendSnpData(); break;
            default:
        }
    }

private void sendSnpData()
{
    try {
        int row = getAppUI().getSnpTable().getSelectedRow();
        if (row < 0) {
            return;
        }
        SnPData snPData = getAppUI().getSnpTableModel().getData(row);
        if (snPData == null) {
            LOGGER.warn("snPData has not selected yet");
            return;
        }
        LOGGER.info("Selected snPData: {}", JacksonUtilities.serializeToString(snPData));
        if (getDesktopAgent() == null) {
            LOGGER.warn("FDC3 Desktop Agent is not yet initialized");
        } else {
            Context context = getApiContext(snPData);
            LOGGER.info("Broadcasting to FDC3: {}", context);
            getDesktopAgent().broadcast(context).thenRun(() -> LOGGER.debug("FDC3 broadcast complete"))
                .exceptionally(ex -> {
                    LOGGER.error("Error broadcasting to FDC3", ex);
                    return null;
                });
            //TODO raising an intent getDesktopAgent().raiseIntent("ViewQuote", context);
        }

    } catch (Exception ex) {
        LOGGER.error("Error broadcasting to FDC3", ex);
    }
}


private void sendOrderData()
    {
        try {
            OrderData selectedOrder = getSelectedRowData();
            if (selectedOrder == null) {
                LOGGER.warn("Order has not selected yet");
                return;
            }
            LOGGER.info("Selected order: {}", JacksonUtilities.serializeToString(selectedOrder));
            if (getDesktopAgent() == null) {
                LOGGER.warn("FDC3 Desktop Agent is not yet initialized");
            } else {
                Context context = getApiContext(selectedOrder);
                LOGGER.info("Broadcasting to FDC3: {}", context);
                getDesktopAgent().broadcast(context).thenRun(() -> LOGGER.debug("FDC3 broadcast complete"))
                    .exceptionally(ex -> {
                        LOGGER.error("Error broadcasting to FDC3", ex);
                        return null;
                    });
                //TODO raising an intent getDesktopAgent().raiseIntent("ViewQuote", context);
            }

        } catch (Exception ex) {
            LOGGER.error("Error broadcasting to FDC3", ex);
        }
    }


    public Context getApiContext(OrderData orderData) {
        Context context = new Instrument();
        context.setName(orderData.getSymbol());
        Map<Object, Object> id = new HashMap<>();
        id.put("ticker", orderData.getSymbol());
        context.setId(id);
        return context;
    }

public Context getApiContext(SnPData orderData) {
    Context context = new Instrument();
    context.setName(orderData.getName());
    Map<Object, Object> id = new HashMap<>();
    id.put("ticker", orderData.getSymbol());
    context.setId(id);
    return context;
}
}
