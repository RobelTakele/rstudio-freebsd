/*
 * RenderRmdEvent.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

package org.rstudio.studio.client.rmarkdown.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RenderRmdEvent extends GwtEvent<RenderRmdEvent.Handler>
{  
   public interface Handler extends EventHandler
   {
      void onRenderRmd(RenderRmdEvent event);
   }

   public RenderRmdEvent(String sourceFile, 
                         int sourceLine,
                         String format,
                         String encoding,
                         boolean asTempfile,
                         boolean asShiny)
   {
      sourceFile_ = sourceFile;
      sourceLine_ = sourceLine;
      format_ = format;
      encoding_ = encoding;
      asTempfile_ = asTempfile;
      asShiny_ = asShiny;
   }

   public String getSourceFile()
   {
      return sourceFile_;
   }
   
   public int getSourceLine()
   {
      return sourceLine_;
   }
   
   public String getFormat()
   {
      return format_;
   }
   
   public String getEncoding()
   {
      return encoding_;
   }
   
   public boolean asTempfile()
   {
      return asTempfile_;
   }
    
   public boolean asShiny()
   {
      return asShiny_;
   }
    
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onRenderRmd(this);
   }
   
   private final String sourceFile_;
   private final int sourceLine_;
   private final String format_;
   private final String encoding_;
   private final boolean asTempfile_;
   private final boolean asShiny_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}