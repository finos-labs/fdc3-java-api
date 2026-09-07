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

package org.finos.fdc3.api.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetachedSignature {

    private String protectedHeader;
    private String signature;

    public DetachedSignature() {
    }

    public DetachedSignature(String protectedHeader, String signature) {
        this.protectedHeader = protectedHeader;
        this.signature = signature;
    }

    @JsonProperty("protected")
    public String getProtectedHeader() {
        return protectedHeader;
    }

    public void setProtectedHeader(String protectedHeader) {
        this.protectedHeader = protectedHeader;
    }

    @JsonProperty("signature")
    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
