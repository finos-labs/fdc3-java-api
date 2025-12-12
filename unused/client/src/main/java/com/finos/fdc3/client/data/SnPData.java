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

package com.finos.fdc3.client.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({
    "Symbol",
    "Name",
    "Sector"
})
@JsonIgnoreProperties(ignoreUnknown=true)
public class SnPData
{
private String sector;
private String name;
private String symbol;

@JsonProperty("Sector")
public String getSector()
{
    return sector;
}

public void setSector(String sector)
{
    this.sector = sector;
}

@JsonProperty("Name")
public String getName()
{
    return name;
}

public void setName(String name)
{
    this.name = name;
}

@JsonProperty("Symbol")
public String getSymbol()
{
    return symbol;
}

public void setSymbol(String symbol)
{
    this.symbol = symbol;
}

@Override
public boolean equals(Object o)
{
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SnPData snPData = (SnPData) o;
    return sector.equals(snPData.sector) && name.equals(snPData.name) && symbol.equals(snPData.symbol);
}

@Override
public int hashCode()
{
    return Objects.hash(sector, name, symbol);
}
}
