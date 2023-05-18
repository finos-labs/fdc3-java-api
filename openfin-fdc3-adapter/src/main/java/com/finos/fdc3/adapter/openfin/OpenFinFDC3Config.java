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

package com.finos.fdc3.adapter.openfin;

import java.time.Duration;

import com.openfin.desktop.RuntimeConfiguration;

/**
 * Configuration for setting up an OpenFin FDC3 connection
 * 
 * @author Tim Jenkel
 */
public class OpenFinFDC3Config {

    private RuntimeConfiguration runtimeConfiguration;

    private String uuid;

    private String brokerName;

    private Duration timeout = Duration.ofSeconds(20);
    
    public RuntimeConfiguration getRuntimeConfiguration() {

        return runtimeConfiguration;
    }

    public void setRuntimeConfiguration(RuntimeConfiguration runtimeConfiguration) {

        this.runtimeConfiguration = runtimeConfiguration;
    }

    public String getUuid() {

        return uuid;
    }

    public void setUuid(String uuid) {

        this.uuid = uuid;
    }

    public String getBrokerName() {

        return brokerName;
    }

    public void setBrokerName(String brokerName) {

        this.brokerName = brokerName;
    }

    public Duration getTimeout() {

        return timeout;
    }

    public void setTimeout(Duration timeout) {

        this.timeout = timeout;
    }

}
