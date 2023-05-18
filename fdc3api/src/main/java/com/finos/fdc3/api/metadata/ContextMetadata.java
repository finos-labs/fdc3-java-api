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

package com.finos.fdc3.api.metadata;

import com.finos.fdc3.api.types.AppIdentifier;

/**
 * Metadata relating to a context or intent and context received through the
 * `addContextListener` and `addIntentListener` functions.
 *
 * @experimental Introduced in FDC3 2.0 and may be refined by further changes outside the normal FDC3 versioning policy.
 */
public interface ContextMetadata {
  /** Identifier for the app instance that sent the context and/or intent.
   *
   *  @experimental
   */
  public AppIdentifier getSource();
}
