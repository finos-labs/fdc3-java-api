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

package com.finos.fdc3.client;

import com.finos.fdc3.client.handler.DataLoadHandler;
import com.finos.fdc3.client.handler.SystemChannelHandler;
import com.finos.fdc3.client.model.IncomingDataTableModel;
import com.finos.fdc3.client.model.SnPTableModel;
import com.finos.fdc3.client.publisher.OrderMessagePublisher;
import com.finos.fdc3.client.model.OrderTableModel;
import com.finos.fdc3.client.utils.SwingUtils;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.logging.Logger;

public class AppUI
{
static Logger logger = Logger.getLogger(AppUI.class.getName());

private JFrame frame = null;
private final Dimension PREFERRED_SIZE = new Dimension(500, 300);

private JComboBox channelList = new JComboBox(Arrays.asList("","green","purple", "orange","red","pink","yellow").toArray());

private SystemChannelHandler systemChannelHandler = new SystemChannelHandler(channelList);

//Order Data
private OrderTableModel orderTableModel = new OrderTableModel();
private JTable orderTable = new JTable(orderTableModel);
private OrderMessagePublisher orderMessagePublisher;

//
private IncomingDataTableModel incomingDataTableModel = new IncomingDataTableModel();
private JTable incomingDataTable = new JTable(incomingDataTableModel);

//SnP Data
private SnPTableModel snpTableModel = new SnPTableModel();
private JTable snpTable = new JTable(snpTableModel);

private JTabbedPane orderTabbedPane;
private DataLoadHandler dataLoadHandler = new DataLoadHandler(orderTableModel, snpTableModel);

public void init()
{
    orderMessagePublisher = new OrderMessagePublisher();
    orderMessagePublisher.setAppUI(this);
    orderTabbedPane = new JTabbedPane();
    orderTabbedPane.addTab(
        "Blotter",
        getOrCreateOrderComponent()
    );
    orderTabbedPane.addTab(
        "S&P500",
        getOrCreateSnPComponent()
    );
    orderTabbedPane.addTab(
        "Incoming Data",
        getOrIncomingDataComponent()
    );
    orderTabbedPane.addChangeListener(new ChangeListener()
    {
        @Override
        public void stateChanged(ChangeEvent e)
        {
            dataLoadHandler.setSelectedIndex(orderTabbedPane.getSelectedIndex());
        }
    });
    // create frame
    frame = new JFrame("FDC3-Java-API-Demo-App");
    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter()
    {
        public void windowClosing(WindowEvent e)
        {
            exit();
        }
    });
    frame.setJMenuBar(getOrCreateMenuComponent());
    frame.getContentPane().setLayout(new BorderLayout());


    JPanel tabbedPanel = new JPanel();
    tabbedPanel.setLayout(SwingUtils.createVBox(tabbedPanel));
    tabbedPanel.add(orderTabbedPane);
    tabbedPanel.setPreferredSize(PREFERRED_SIZE);
    frame.getContentPane().add(tabbedPanel, BorderLayout.CENTER);

    JPanel operationalPanel = new JPanel();
    JButton loadOrders = new JButton("load Sample data");
    loadOrders.addActionListener(dataLoadHandler);
    operationalPanel.add(loadOrders);

    JButton button = new JButton("Send");
    button.addActionListener(orderMessagePublisher);
    operationalPanel.add(button);
    frame.getContentPane().add(operationalPanel, BorderLayout.SOUTH);

    frame.getContentPane().add(getSystemChannelPanel(), BorderLayout.NORTH);


    frame.pack();
    frame.setVisible(true);
    frame.toFront();
}


private JPanel getSystemChannelPanel(){
    JPanel panel = new JPanel();
    panel.add(new JLabel("System Channels "));
    panel.add(channelList);

    JButton joinBtn = new JButton("Join");
    joinBtn.addActionListener(systemChannelHandler);
    panel.add(joinBtn);

    return panel;
}

public SystemChannelHandler getSystemChannelHandler()
{
    return systemChannelHandler;
}

private OrderTableModel getOrderTableModel()
{
    return orderTableModel;
}

public OrderMessagePublisher getOrderMessagePublisher()
{
    return orderMessagePublisher;
}

private JScrollPane getOrCreateOrderComponent()
{
    getOrderMessagePublisher().setOrderTable(orderTable);
    orderTable.setPreferredScrollableViewportSize(new Dimension(300,400));
    orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane orderScrollPane = new JScrollPane(orderTable);
    orderScrollPane.getViewport().setBackground(Color.WHITE);
    return orderScrollPane;
}
private JScrollPane getOrIncomingDataComponent()
{
    incomingDataTable.setPreferredScrollableViewportSize(new Dimension(300,400));
    incomingDataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane orderScrollPane = new JScrollPane(incomingDataTable);
    orderScrollPane.getViewport().setBackground(Color.WHITE);
    return orderScrollPane;
}

private JScrollPane getOrCreateSnPComponent()
{
    snpTable.setPreferredScrollableViewportSize(new Dimension(300,400));
    snpTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane orderScrollPane = new JScrollPane(snpTable);
    orderScrollPane.getViewport().setBackground(Color.WHITE);
    return orderScrollPane;
}

private JMenuBar getOrCreateMenuComponent()
{
    JMenuBar menuBar = new JMenuBar();
    JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic(KeyEvent.VK_F);
    JMenuItem fileRefreshAllMenuItem = new JMenuItem("Refresh All");
    fileRefreshAllMenuItem.setMnemonic(KeyEvent.VK_R);
    fileRefreshAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.CTRL_MASK));
    fileRefreshAllMenuItem.addActionListener(new ActionListener()
    {
        public void actionPerformed(ActionEvent e)
        {
            //TODO
        }
    });
    JMenuItem exitMenuItem = new JMenuItem("Exit");
    exitMenuItem.setMnemonic(KeyEvent.VK_X);
    exitMenuItem.addActionListener(new ActionListener()
    {
        public void actionPerformed(ActionEvent e)
        {
            exit();
        }
    });
    fileMenu.add(fileRefreshAllMenuItem);
    fileMenu.addSeparator();
    fileMenu.add(exitMenuItem);
    menuBar.add(fileMenu);

    JMenu actionsMenu = new JMenu("Actions");
    actionsMenu.setMnemonic(KeyEvent.VK_A);
    JMenuItem actionsRefreshAllMenuItem = new JMenuItem("Refresh All");
    actionsRefreshAllMenuItem.setMnemonic(KeyEvent.VK_R);
    actionsRefreshAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.CTRL_MASK));
    actionsRefreshAllMenuItem.addActionListener(new ActionListener()
    {
        public void actionPerformed(ActionEvent e)
        {
            //TODO
        }
    });
    actionsMenu.add(actionsRefreshAllMenuItem);
    menuBar.add(actionsMenu);

    JMenu helpMenu = new JMenu("Help");
    helpMenu.setMnemonic(KeyEvent.VK_H);
    JMenuItem aboutMenu = new JMenuItem("About");
    aboutMenu.addActionListener(new ActionListener()
    {
        public void actionPerformed(ActionEvent e)
        {
            //TODO
        }
    });
    helpMenu.add(aboutMenu);
    menuBar.add(SwingUtils.createHorizontalGlue());
    menuBar.add(helpMenu);

    return menuBar;
}

public JFrame getFrame()
{
    return frame;
}

private void exit()
{
    if (AppUI.this.showConfirmWarningDialog(
        "Are you sure you want to exit the applicaton ?") == JOptionPane.YES_OPTION) {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
        System.exit(0);
    }
}

public int showConfirmWarningDialog(String message)
{
    return JOptionPane.showConfirmDialog(
        getFrame(),
        message,
        "Confirm",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
    );
}

public IncomingDataTableModel getIncomingDataTableModel()
{
    return incomingDataTableModel;
}

public JTabbedPane getOrderTabbedPane()
{
    return orderTabbedPane;
}

public SnPTableModel getSnpTableModel()
{
    return snpTableModel;
}

public JTable getSnpTable()
{
    return snpTable;
}
}
