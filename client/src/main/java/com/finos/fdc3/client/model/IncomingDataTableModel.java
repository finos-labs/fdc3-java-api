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

import com.finos.fdc3.client.data.IncomingData;
import com.finos.fdc3.client.data.OrderData;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class IncomingDataTableModel extends AbstractTableModel
{

private List<IncomingData> dataList = new ArrayList<IncomingData>();
private String[] columnNames =  { "Name", "Ticker", "RIC", "ISIN" , "Stock Units (Held)"};
public IncomingDataTableModel() {}

public IncomingDataTableModel(List<IncomingData> orderDataList) {
    this.dataList = orderDataList;
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
    return dataList.size();
}

@Override
public Object getValueAt(int row, int column) {
    Object OrderDataAttribute = null;
    IncomingData data = dataList.get(row);
    switch(column) {
        case 0: OrderDataAttribute = data.getName(); break;
        case 1: OrderDataAttribute = data.getTicker(); break;
        case 2: OrderDataAttribute = data.getRic(); break;
        case 3: OrderDataAttribute = data.getIsin(); break;
        case 4: OrderDataAttribute = data.getHolding(); break;
        default: break;
    }
    return OrderDataAttribute;
}

public void addData(IncomingData data) {
    dataList.add(data);
    fireTableDataChanged();
}
public IncomingData getData(int row) {
    return dataList.get(row);
}
}