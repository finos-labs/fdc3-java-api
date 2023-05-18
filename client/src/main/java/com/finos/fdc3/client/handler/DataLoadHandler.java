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

package com.finos.fdc3.client.handler;

import com.finos.fdc3.client.data.OrderData;
import com.finos.fdc3.client.data.SnPData;
import com.finos.fdc3.client.model.OrderTableModel;
import com.finos.fdc3.client.model.SnPTableModel;
import com.finos.fdc3.client.utils.SampleDataLoaderUtil;
import com.finos.fdc3.client.utils.SwingUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

public class DataLoadHandler implements ActionListener
{

private final OrderTableModel orderTableModel;
private final SnPTableModel snPTableModel;

private int selectedIndex;

public DataLoadHandler(OrderTableModel orderTableModel, SnPTableModel snPTableModel) {
    this.orderTableModel = orderTableModel;
    this.snPTableModel = snPTableModel;
}

public OrderTableModel getOrderTableModel()
{
    return orderTableModel;
}

public SnPTableModel getSnPTableModel()
{
    return snPTableModel;
}

public int getSelectedIndex()
{
    return selectedIndex;
}

public void setSelectedIndex(int selectedIndex)
{
    this.selectedIndex = selectedIndex;
}

@Override
public void actionPerformed(ActionEvent e)
{
    SwingUtils.invokeLater(new Runnable()
    {
        @Override
        public void run()
        {
            switch (getSelectedIndex()){
                case 0: loadOrderData();break;
                case 1: loadSnPData();break;
                default:
            }
        }
    });
}



private void loadSnPData()
{
    if(getSnPTableModel().getRowCount() == 0) {
        Collection<SnPData> dataList = SampleDataLoaderUtil.readSPData();
        if (dataList != null && dataList.size() > 0) {
            dataList.forEach(o -> {
                getSnPTableModel().addData(o);
            });
        }
    }
}
private void loadOrderData()
{
    Collection<OrderData> orders =  SampleDataLoaderUtil.loadSampleData();
    if(orders!=null && orders.size() > 0) {
        orders.forEach(o->{
            getOrderTableModel().addData(o);
        });
    }
}

}
