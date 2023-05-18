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

import java.util.Optional;

/**
 * A system channel will be global enough to have a presence across many apps. This gives us some hints
 * to render them in a standard way. It is assumed it may have other properties too, but if it has these,
 * this is their meaning.
 */
public interface DisplayMetadata {
  /**
   * A user-readable name for this channel, e.g: `"Red"`
   */
  public Optional<String> getName();

  /**
   * The color that should be associated within this channel when displaying this channel in a UI, e.g: `0xFF0000`.
   */
  public Optional<String> getColor();

  /**
   * A URL of an image that can be used to display this channel
   */
  public Optional<String> getGlyph();
}
