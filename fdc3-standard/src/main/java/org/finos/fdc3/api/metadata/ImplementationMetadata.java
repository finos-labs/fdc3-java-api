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

package org.finos.fdc3.api.metadata;

import java.util.Optional;

/**
 * Metadata relating to the FDC3 Desktop Agent implementation and its provider.
 */
public interface ImplementationMetadata {
  /** The version number of the FDC3 specification that the implementation provides.
   *  The string must be a numeric semver version, e.g. 1.2 or 1.2.1.
   */
  String getFdc3Version();

  /** The name of the provider of the Desktop Agent implementation (e.g. Finsemble, Glue42, OpenFin etc.). */
  String getProvider();

  /** The version of the provider of the Desktop Agent implementation (e.g. 5.3.0). */
  String getProviderVersion();

  /** The calling application instance's own metadata, according to the Desktop Agent (MUST include at least the `appId` and `instanceId`). */
  AppMetadata getAppMetadata();

  /** Metadata indicating whether the Desktop Agent implements optional features of
   *  the Desktop Agent API.
   */
  OptionalFeatures getOptionalFeatures();

  /** Metadata indicating whether the Desktop Agent implements optional features of
   *  the Desktop Agent API.
   */
  interface OptionalFeatures {
    /** Used to indicate whether the exposure of 'originating app metadata' for
     *  context and intent messages is supported by the Desktop Agent.*/
    boolean isOriginatingAppMetadata();

    /** Used to indicate whether the optional `fdc3.joinUserChannel`,
     *  `fdc3.getCurrentChannel` and `fdc3.leaveCurrentChannel` are implemented by
     *  the Desktop Agent.*/
    boolean isUserChannelMembershipAPIs();

    /** Used to indicate whether Desktop Agent bridging is supported by the Desktop Agent. */
    boolean isDesktopAgentBridging();
  }
}
