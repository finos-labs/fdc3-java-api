/*
 * Copyright FINOS and Contributors to the FDC3 project
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

import java.util.Map;

/**
 * Metadata that may be provided by an app when calling broadcast, open, or raiseIntent,
 * to be passed on to receiving apps.
 */
public interface AppProvidableContextMetadata extends SecurityMetadata {

    String getTraceId();

    void setTraceId(String traceId);

    Map<String, Object> getCustom();

    void setCustom(Map<String, Object> custom);
}
