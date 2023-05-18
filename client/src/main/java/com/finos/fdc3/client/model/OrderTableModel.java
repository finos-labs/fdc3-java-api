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

package com.finos.fdc3.client.model;

import com.finos.fdc3.client.data.OrderData;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class OrderTableModel extends AbstractTableModel
{

private List<OrderData> orderDataList = new ArrayList<OrderData>();
private String[] columnNames =  { "Order ID", "Trader", "Side", "Symbol", "Quantity"};

public OrderTableModel() {}

public OrderTableModel(List<OrderData> orderDataList) {
    this.orderDataList = orderDataList;
}

@Override
public String getColumnName(int column) {
    return columnNames[column];
}

@Override
public int getColumnCount() {
    return columnNames.length;
}

@Override
public int getRowCount() {
    return orderDataList.size();
}

@Override
public Object getValueAt(int row, int column) {
    Object OrderDataAttribute = null;
    OrderData OrderDataObject = orderDataList.get(row);
    switch(column) {
        case 0: OrderDataAttribute = OrderDataObject.getOrderId(); break;
        case 1: OrderDataAttribute = OrderDataObject.getTrader(); break;
        case 2: OrderDataAttribute = OrderDataObject.getSide(); break;
        case 3: OrderDataAttribute = OrderDataObject.getSymbol(); break;
        case 4: OrderDataAttribute = OrderDataObject.getOrderQty(); break;
        default: break;
    }
    return OrderDataAttribute;
}

public void addData(OrderData OrderData) {
    orderDataList.add(OrderData);
    fireTableDataChanged();
}
public OrderData getData(int row) {
    return orderDataList.get(row);
}
}