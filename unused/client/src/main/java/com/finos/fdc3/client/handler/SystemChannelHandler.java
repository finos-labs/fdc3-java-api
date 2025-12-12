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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finos.fdc3.api.DesktopAgent;
import com.finos.fdc3.api.channel.Channel;

public class SystemChannelHandler implements ActionListener
{
private static final Logger LOGGER = LoggerFactory.getLogger(SystemChannelHandler.class);
private final JComboBox<String> connectionsList;
private DesktopAgent desktopAgent;

public SystemChannelHandler(JComboBox<String> connectionsList){
    this.connectionsList = connectionsList;
}

public DesktopAgent getDesktopAgent() {

    return desktopAgent;
}

public void setDesktopAgent(DesktopAgent desktopAgent) {

    this.desktopAgent = desktopAgent;
    loadChannels();
}

public JComboBox<String> getConnectionsList()
{
    return connectionsList;
}

public void actionPerformed(ActionEvent e)
{
    if(getDesktopAgent()==null) {
        LOGGER.error("Error Desktop agent is null");
       return;
    }
    joinChannel((String)getConnectionsList().getSelectedItem());
}

private void joinChannel(String channel)
{
    desktopAgent.joinUserChannel(channel).thenRun(() -> LOGGER.info("Joined {} channel", channel))
        .exceptionally(ex -> {
            LOGGER.error("Error joining FDC3 channel", ex);
            return null;
        });
}

private void loadChannels() {

    desktopAgent.getUserChannels().thenAccept(channelList -> {
        List<String> channelIds = channelList.stream().map(Channel::getId).collect(Collectors.toList());
        LOGGER.info("Loaded channels: {}", channelIds);
        SwingUtilities.invokeLater(() -> {
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addAll(channelIds);
            model.setSelectedItem(null);
            getConnectionsList().setModel(model);
        });
    }).exceptionally(ex -> {
        LOGGER.error("Error loading channels", ex);
        return null;
    });
}

}
