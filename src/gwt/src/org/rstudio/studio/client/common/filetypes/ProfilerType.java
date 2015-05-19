/*
 * ProfilerType.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.filetypes;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;

public class ProfilerType extends EditableFileType
{
   public ProfilerType()
   {
      super("r_profiler", "R Profiler",
            FileIconResources.INSTANCE.iconRdoc());
   }

   @Override
   public void openFile(FileSystemItem file, EventBus eventBus)
   {
      assert false : "Profiler doesn't operate on filesystem files";
   }
}
