/**
 * Copyright FINOS and its Contributors
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

package org.finos.fdc3.workbench.service;

import org.finos.fdc3.workbench.connection.ConnectionService;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Shared services for the JavaFX workbench.
 */
public class WorkbenchServices {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SystemLogService log = new SystemLogService();
    private final AgentHolder agentHolder = new AgentHolder();
    private final ConnectionService connection = new ConnectionService(agentHolder, log);
    private final ContextTemplateStore contexts = new ContextTemplateStore(mapper, log);
    private final ListenerRegistry listeners = new ListenerRegistry(log, mapper);
    private final ObservableChannelState channels = new ObservableChannelState();

    public ObjectMapper getMapper() {
        return mapper;
    }

    public SystemLogService getLog() {
        return log;
    }

    public AgentHolder getAgentHolder() {
        return agentHolder;
    }

    public ConnectionService getConnection() {
        return connection;
    }

    public ContextTemplateStore getContexts() {
        return contexts;
    }

    public ListenerRegistry getListeners() {
        return listeners;
    }

    public ObservableChannelState getChannels() {
        return channels;
    }
}
