/*
 * EnvironmentResources.java
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

package org.rstudio.studio.client.workbench.views.environment.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface EnvironmentResources extends ClientBundle
{
   public static final EnvironmentResources INSTANCE =
           GWT.create(EnvironmentResources.class);

   @Source("ExpandIcon.png")
   ImageResource expandIcon();

   @Source("CollapseIcon.png")
   ImageResource collapseIcon();
   
   @Source("TracedFunction.png")
   ImageResource tracedFunction();
   
   @Source("GlobalEnvironment.png")
   ImageResource globalEnvironment();
   
   @Source("PackageEnvironment.png")
   ImageResource packageEnvironment();
   
   @Source("AttachedEnvironment.png")
   ImageResource attachedEnvironment();

   @Source("FunctionEnvironment.png")
   ImageResource functionEnvironment();
   
   @Source("ObjectListView.png")
   ImageResource objectListView();
   
   @Source("ObjectGridView.png")
   ImageResource objectGridView();
   
   @Source("EnvironmentObjects.css")
   EnvironmentStyle environmentStyle();
}

