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

import * as fdc3 from "@finos/fdc3";

import React, {useEffect, useState} from "react";

import ChannelDropdown from "./ChannelDropdown";
import Scroll from "./Scroll";
import SearchList from "./SearchList";
import initialDetails from "../data/initialDetails";

function Search() {
    const [searchField, setSearchField] = useState("");
    const [searchShow, setSearchShow] = useState(false);
    const [tickers, setTickers] = useState([]);
    const [searchedTicker, setSearchedTicker] = useState({});

    useEffect(() => {
        initialDetails().then((data) => {
            setTickers(data);
            if (!!window.fdc3) {
                fdc3.addContextListener("fdc3.instrument", handleContext);
            }
        });
    }, []);

    useEffect(() => {
        const searchResults = tickers.filter((company) => {
            return (
                company.name.toLowerCase().includes(searchField.toLowerCase()) ||
                company.id.toLowerCase().includes(searchField.toLowerCase()) ||
                company.sector.toLowerCase().includes(searchField.toLowerCase()) ||
                company.name
                    .substring(1, 3)
                    .toLowerCase()
                    .includes(searchField.toLowerCase())
            );
        });
        setSearchedTicker(searchResults);
    }, [searchField, tickers]);

    const handleChange = (e) => {
        search(e.target.value);
    };
    const search = (value) => {
        setSearchField(value);
        if (value === "") {
            setSearchShow(false);
        } else {
            setSearchShow(true);
        }
    };

    function searchList() {
        if (searchShow) {
            return (
                <Scroll>
                    <SearchList searchResults={searchedTicker}/>
                </Scroll>
            );
        }
    }

    function handleContext(context) {
        let searchTerm = "";
        if (!!context.id.ticker) {
            searchTerm = context.id.ticker;
        } else if (!!context.name) {
            searchTerm = context.name;
        }
        setSearchField(searchTerm);
        search(searchTerm);
    }

    return (
        <section className="garamond">
            <div className="navy georgia ma0 grow">
                <h2 className="f2">Search For SP 500 Companies to trade</h2>
            </div>
            <div className="pa2">
                <input
                    className="pa3 bb br3 grow b--none bg-lightest-blue ma3"
                    type="search"
                    placeholder="Enter Name / Ticker/ Sector"
                    value={searchField}
                    onChange={handleChange}
                />
            </div>
            {searchList()}
            <ChannelDropdown defaultChannel={{value: "green", color: "#00CC88"}}/>
        </section>
    );
}

export default Search;
