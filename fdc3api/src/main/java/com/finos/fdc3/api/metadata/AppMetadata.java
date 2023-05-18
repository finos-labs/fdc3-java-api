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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.finos.fdc3.api.types.AppIdentifier;

/**
 * Extends an `AppIdentifier`, describing an application or instance of an application, with additional descriptive metadata that is usually provided by an FDC3 App Directory that the desktop agent connects to.
 *
 * The additional information from an app directory can aid in rendering UI elements, such as a launcher menu or resolver UI. This includes a title, description, tooltip and icon and screenshot URLs.
 *
 * Note that as `AppMetadata` instances are also `AppIdentifiers` they may be passed to the `app` argument of `fdc3.open`, `fdc3.raiseIntent` etc.
 */
public interface AppMetadata extends AppIdentifier
{
  /** 
      The 'friendly' app name. 
      This field was used with the `open` and `raiseIntent` calls in FDC3 <2.0, which now require an `AppIdentifier` wth `appId` set. 
      Note that for display purposes the `title` field should be used, if set, in preference to this field.
   */
  public Optional<String> getName();

  /** The Version of the application. */
  public Optional<String> getVersion();

  /** An optional set of, implementation specific, metadata fields that can be used to disambiguate instances, such as a window title or screen position. Must only be set if `instanceId` is set. */
  public Optional<Map<String, Object>> getInstanceMetadata();

  /** A more user-friendly application title that can be used to render UI elements  */
  public Optional<String> getTitle();

  /**  A tooltip for the application that can be used to render UI elements */
  public Optional<String> getTooltip();

  /** A longer, multi-paragraph description for the application that could include markup */
  public Optional<String> getDescription();

  /** A list of icon URLs for the application that can be used to render UI elements
   * @return*/
  public Optional<Collection<Icon>> getIcons();

  /** Images representing the app in common usage scenarios that can be used to render UI elements  */
  public Optional<Collection<Image>> getScreenshots();

  /** The type of output returned for any intent specified during resolution. May express a particular context type (e.g. "fdc3.instrument"), channel (e.g. "channel") or a channel that will receive a specified type (e.g. "channel<fdc3.instrument>"). */
  public Optional<String> getResultType();
}
