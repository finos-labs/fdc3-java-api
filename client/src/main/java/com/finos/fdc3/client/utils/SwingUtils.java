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

package com.finos.fdc3.client.utils;

import com.finos.fdc3.client.AppGUIException;

import javax.swing.*;
import java.awt.*;

public final class SwingUtils
{

public static Component createHorizontalGlue()
{
	return Box.createHorizontalGlue();
}

public static final BoxLayout createVBox(Container container)
{
	return new BoxLayout(container, BoxLayout.Y_AXIS);
}

public static final void invokeLater(Runnable r)
{
	SwingUtilities.invokeLater(r);
}

public static final void invokeAndWait(Runnable r)
	throws AppGUIException
{
	try {
		SwingUtilities.invokeAndWait(r);
	}
	catch (Throwable t) {
		throw new AppGUIException(
			String.format(
				"error executing invokeAndWait, details: %s - %s",
				t.getMessage(),
				t.getCause()
			)
		);
	}
}


}