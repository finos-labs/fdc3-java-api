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

package com.finos.fdc3.api.types;

import java.util.Optional;

/**
 * Identifies an application, or instance of an application, and is used to target FDC3 API calls, such as `fdc3.open` or `fdc3.raiseIntent` at specific applications or application instances.
 *
 * Will always include at least an `appId` field, which uniquely identifies a specific app.
 *
 * If the `instanceId` field is set then the `AppMetadata` object represents a specific instance of the application that may be addressed using that Id.
 */
public interface AppIdentifier {
  /** The unique application identifier located within a specific application directory instance. An example of an appId might be 'app@sub.root' */
  public String getAppId();

  /** An optional instance identifier, indicating that this object represents a specific instance of the application described.*/
  public Optional<String> getInstanceId();
}
