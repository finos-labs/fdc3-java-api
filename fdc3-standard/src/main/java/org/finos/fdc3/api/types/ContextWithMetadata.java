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

package org.finos.fdc3.api.types;

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.metadata.ContextMetadata;

/**
 * Context object paired with its associated metadata.
 */
public class ContextWithMetadata {

    private Context context;
    private ContextMetadata metadata;

    public ContextWithMetadata() {
    }

    public ContextWithMetadata(Context context, ContextMetadata metadata) {
        this.context = context;
        this.metadata = metadata;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public ContextMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ContextMetadata metadata) {
        this.metadata = metadata;
    }
}
