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

import * as fdc3 from '@finos/fdc3';

import React, {useState} from 'react';

import axios from "axios";
import search from "./Search";

function Card({result}) {

    const [searchCard, setSearchCard] = useState(result);
    const [cardPricing, setCardPricing] = useState({});
    const handleTrades = e => {
        const sign = e.currentTarget.getAttribute('transtype')
        let updatedTrade = {
            ...searchCard,
            trades: (sign === "buy") ? searchCard.trades + 100 : (searchCard.trades === 0) ? 0 : searchCard.trades - 100
        };
        setSearchCard(updatedTrade);
        const position = {
            type: 'fdc3.position',
            instrument: {
                type: "fdc3.instrument",
                name:searchCard.name,
                id: {
                    'ticker': searchCard.id
                }
            },
            holding: updatedTrade.trades
        };

        fdc3.broadcast(position);

    }

    const findPricing = e => {
        const ticker = searchCard.id;
        const apikey = 'cEaEylhYZbVcKqSF_oZbOexAppYQRlmK';
        if (!!ticker && ticker !== "") {
            axios.get('https://api.polygon.io/v2/aggs/ticker/' + ticker + '/prev?adjusted=true&apiKey=' + apikey).then(response => {
                if (response.status === 200 && response.data.results.length>0) {
                    const prevClosePricing = response.data.results;
                    setCardPricing({...searchCard,...prevClosePricing[0]});
                }
            })
        }

    }


    return (
        <div className="tc bg-light-green dib br3 pa3 ma2 bw2 shadow-5">
            <div>
                <h3>Reference</h3>
                <h2>{searchCard.name}</h2>
                <p>{searchCard.id}</p>
                <p>{searchCard.sector}</p>
                <h3>Pricing</h3>
                <p>Trading volume {cardPricing.v}</p>
                <p>Weighted Average {cardPricing.vw}</p>
                <p>Close price {cardPricing.c}</p>
                <p>Open price {cardPricing.o}</p>
                <p>Lowest Price {cardPricing.l}</p>
                <p>Highest Price {cardPricing.h}</p>

            </div>
            <div>
                <button id="buy" transtype="buy" onClick={handleTrades} className="b--dark-green br4  ">Buy
                </button>
                <button id="find" transtype="Find Price" onClick={findPricing} className="b--dark-green br4  ">Find
                    Pricing
                </button>
                <button id="sell" transtype="sell" onClick={handleTrades} className="b--light-blue br4 ">Sell
                </button>
            </div>
        </div>
    );
}

export default Card;