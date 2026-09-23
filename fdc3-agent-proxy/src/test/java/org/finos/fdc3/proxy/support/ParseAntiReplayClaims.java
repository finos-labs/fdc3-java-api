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

package org.finos.fdc3.proxy.support;

import org.finos.fdc3.api.metadata.AntiReplayClaims;

public final class ParseAntiReplayClaims {

    private ParseAntiReplayClaims() {
    }

    public static AntiReplayClaims parse(String claims) {
        String[] parts = claims.split("/");
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                    "antiReplay claims must be three slash-separated parts (iat/exp/jti), got: " + claims);
        }
        long iat = Long.parseLong(parts[0]);
        long exp = Long.parseLong(parts[1]);
        return new AntiReplayClaims(iat, exp, parts[2]);
    }
}
