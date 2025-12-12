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

import Select from "react-select";
import chroma from "chroma-js";
import {useQuery} from "react-query";

const dot = (color = "transparent") => ({
    alignItems: "center",
    display: "flex",

    ":before": {
        backgroundColor: color,
        borderRadius: 10,
        content: '" "',
        display: "block",
        marginRight: 8,
        height: 10,
        width: 10,
    },
});
const colourStyles = {
    control: (styles) => ({...styles, backgroundColor: "white"}),
    option: (styles, {data, isDisabled, isFocused, isSelected}) => {
        const color = chroma(data.color);
        return {
            ...styles,
            backgroundColor: isDisabled
                ? undefined
                : isSelected
                    ? data.color
                    : isFocused
                        ? color.alpha(0.1).css()
                        : undefined,
            color: isDisabled
                ? "#ccc"
                : isSelected
                    ? chroma.contrast(color, "white") > 2
                        ? "white"
                        : "black"
                    : data.color,
            cursor: isDisabled ? "not-allowed" : "default",

            ":active": {
                ...styles[":active"],
                backgroundColor: !isDisabled
                    ? isSelected
                        ? data.color
                        : color.alpha(0.3).css()
                    : undefined,
            },
        };
    },
    input: (styles) => ({...styles, ...dot()}),
    placeholder: (styles) => ({...styles, ...dot("#ccc")}),
    singleValue: (styles, {data}) => ({...styles, ...dot(data.color)}),
};

async function fetchUserChannels() {
    let channels = [];
    if (!!window.fdc3) {
        channels = await fdc3.getUserChannels();
        return channels;
    }
}

export default function ChannelDropdown(props) {
    const [channel, setChannel] = useState(props.defaultChannel);
    const {data, isLoading} = useQuery("data", fetchUserChannels);

    useEffect(() => {
        async function joinChannel() {
            await fdc3.leaveCurrentChannel();
            console.log("Leaving current channel");
            await fdc3.joinChannel(channel.value);
            console.log(`Setting channel to ${channel.value}`);
        }

        if (!!window.fdc3) {
            joinChannel();
        }
    });

    if (isLoading) return <div>Loading...</div>;

    if (!data) return <div>Error while fetching data...</div>;

    const channels = data.map((channel) => ({
        value: channel.id,
        label: channel.id,
        color: channel.displayMetadata.color,
    }));

    const defaultChannel =
        channels.find((c) => c.id === channel.value) || channels[0];

    return (
        <Select
            className="basic-single"
            classNamePrefix="select"
            styles={colourStyles}
            isClearable={true}
            isSearchable={true}
            defaultValue={defaultChannel}
            onChange={(selectedChannel) => setChannel(selectedChannel)}
            options={channels}
        />
    );
}
