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

import axios from "axios";

async function getTickers() {
    const data = axios.get('https://pkgstore.datahub.io/core/s-and-p-500-companies/constituents_json/data/297344d8dc0a9d86b8d107449c851cc8/constituents_json.json')
    return data
}


async function initialDetails() {
    let tickers = await getTickers();
    let result = [];
    if (!!tickers.data) {
        result = tickers.data.map((tick) => {
            return {
                "id": tick.Symbol,
                "sector":
                tick.Sector,
                "name":
                tick.Name,
                "trades":0
            }
        });
    }
    return result;
}


export default initialDetails;