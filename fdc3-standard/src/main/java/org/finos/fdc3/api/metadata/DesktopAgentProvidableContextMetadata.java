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

import java.time.Instant;

import org.finos.fdc3.api.types.AppIdentifier;

/**
 * Metadata fields that may be provided by the Desktop Agent (optional source and timestamp).
 */
public interface DesktopAgentProvidableContextMetadata extends SecurityMetadata {

    Instant getTimestamp();

    void setTimestamp(Instant timestamp);

    AppIdentifier getSource();

    void setSource(AppIdentifier source);
}
