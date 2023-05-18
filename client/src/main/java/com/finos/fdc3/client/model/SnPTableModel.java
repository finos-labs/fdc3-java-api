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

import com.finos.fdc3.client.data.SnPData;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class SnPTableModel extends AbstractTableModel
{

private List<SnPData> dataList = new ArrayList<SnPData>();
private String[] columnNames =  { "Name", "Sector", "Symbol"};

public SnPTableModel() {}

public SnPTableModel(List<SnPData> SnPDataList) {
    this.dataList = SnPDataList;
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
    Object SnPDataAttribute = null;
    SnPData SnPDataObject = dataList.get(row);
    switch(column) {
        case 0: SnPDataAttribute = SnPDataObject.getName(); break;
        case 1: SnPDataAttribute = SnPDataObject.getSector(); break;
        case 2: SnPDataAttribute = SnPDataObject.getSymbol(); break;
        default: break;
    }
    return SnPDataAttribute;
}

public void addData(SnPData SnPData) {
    dataList.add(SnPData);
    fireTableDataChanged();
}
public SnPData getData(int row) {
    return dataList.get(row);
}

}