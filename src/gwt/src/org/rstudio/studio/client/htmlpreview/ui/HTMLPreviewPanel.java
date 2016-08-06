/*
 * HTMLPreviewPanel.java
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
package org.rstudio.studio.client.htmlpreview.ui;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.FindTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.AnchorableFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarLabel;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.htmlpreview.HTMLPreviewPresenter;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewResult;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.workbench.commands.Commands;

public class HTMLPreviewPanel extends ResizeComposite
                              implements HTMLPreviewPresenter.Display
{
   @Inject
   public HTMLPreviewPanel(Commands commands)
   {
      LayoutPanel panel = new LayoutPanel();
      
      Toolbar toolbar = createToolbar(commands);
      int tbHeight = toolbar.getHeight();
      panel.add(toolbar);
      panel.setWidgetLeftRight(toolbar, 0, Unit.PX, 0, Unit.PX);
      panel.setWidgetTopHeight(toolbar, 0, Unit.PX, tbHeight, Unit.PX);
      
      previewFrame_ = new AnchorableFrame();
      previewFrame_.setSize("100%", "100%");
      panel.add(previewFrame_);
      panel.setWidgetLeftRight(previewFrame_,  0, Unit.PX, 0, Unit.PX);
      panel.setWidgetTopBottom(previewFrame_, tbHeight+1, Unit.PX, 0, Unit.PX);
      
      initWidget(panel);
   }
   
   private Toolbar createToolbar(Commands commands)
   {
      Toolbar toolbar = new Toolbar();
      
      toolbar.addLeftWidget(new ToolbarLabel("Preview: "));
      fileLabel_ = new ToolbarLabel();
      fileLabel_.addStyleName(ThemeStyles.INSTANCE.subtitle());
      fileLabel_.getElement().getStyle().setMarginRight(7, Unit.PX);
      toolbar.addLeftWidget(fileLabel_);
      
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands.openHtmlExternal().createToolbarButton());
      
      showLogButtonSeparator_ = toolbar.addLeftSeparator();
      showLogButton_ = commands.showHtmlPreviewLog().createToolbarButton();
      toolbar.addLeftWidget(showLogButton_);
      
      saveHtmlPreviewAsSeparator_ = toolbar.addLeftSeparator();
      if (Desktop.isDesktop())
      { 
         saveHtmlPreviewAs_ = commands.saveHtmlPreviewAs().createToolbarButton();
         toolbar.addLeftWidget(saveHtmlPreviewAs_);
      }
      else
      {
         ToolbarPopupMenu menu = new ToolbarPopupMenu();
         menu.addItem(commands.saveHtmlPreviewAs().createMenuItem(false));
         menu.addItem(commands.saveHtmlPreviewAsLocalFile().createMenuItem(false));
      
         saveHtmlPreviewAs_ = toolbar.addLeftWidget(new ToolbarButton(
               "Save As", 
               commands.saveSourceDoc().getImageResource(),
               menu));
         
         
      }
      
      publishButtonSeparator_ = toolbar.addLeftSeparator();
      toolbar.addLeftWidget(
               publishButton_ = new RSConnectPublishButton(
                     RSConnect.CONTENT_TYPE_DOCUMENT, true, null));
      
      findTextBox_ = new FindTextBox("Find");
      findTextBox_.setIconVisible(true);
      findTextBox_.setOverrideWidth(120);
      findTextBox_.getElement().getStyle().setMarginRight(6, Unit.PX);
      toolbar.addRightWidget(findTextBox_);
      
      findTextBox_.addKeyDownHandler(new KeyDownHandler() {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            // enter key triggers a find
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
            {
               event.preventDefault();
               event.stopPropagation();
               findInTopic(findTextBox_.getValue().trim(), findTextBox_);
               findTextBox_.focus();
            }
            else if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE)
            {
               findTextBox_.setValue("");
            }       
         }
         
         private void findInTopic(String term, CanFocus findInputSource)
         {
            // get content window
            WindowEx contentWindow = previewFrame_.getWindow();
            if (contentWindow == null)
               return;
                
            if (!contentWindow.find(term, false, false, true, false))
            {
               RStudioGinjector.INSTANCE.getGlobalDisplay().showMessage(
                     MessageDialog.INFO,
                     "Find in Page", 
                     "No occurences found",
                     findInputSource);
            }     
         }
         
      });
      
      refreshButtonSeparator_ = toolbar.addRightSeparator();
      refreshButton_ = toolbar.addRightWidget(
                     commands.refreshHtmlPreview().createToolbarButton());
      
      
      return toolbar;
   }
   
   
   @Override
   public void showLog(String log)
   {
      final HTMLPreviewProgressDialog dialog = 
                              new HTMLPreviewProgressDialog("Log");
      dialog.showOutput(log);
      dialog.stopProgress();
      dialog.addClickHandler(new ClickHandler() {

         @Override
         public void onClick(ClickEvent event)
         {
            dialog.dismiss(); 
         }
      });
      
   }
   
   @Override
   public void showProgress(String caption)
   {
      closeProgress();
      activeProgressDialog_ = new HTMLPreviewProgressDialog(caption, 500);
   }
   
   @Override
   public void setProgressCaption(String caption)
   {
      activeProgressDialog_.setCaption(caption);
   }
   
   @Override
   public HandlerRegistration addProgressClickHandler(ClickHandler handler)
   {
      return activeProgressDialog_.addClickHandler(handler);
   }
   
   @Override
   public void showProgressOutput(String output)
   {
      activeProgressDialog_.showOutput(output);
   }

   @Override
   public void stopProgress()
   {
      activeProgressDialog_.stopProgress();
   }
   
   @Override
   public void closeProgress()
   {
      if (activeProgressDialog_ != null)
      {
         activeProgressDialog_.dismiss();
         activeProgressDialog_ = null;
      }
   }
   
   @Override
   public void showPreview(String url, 
                           HTMLPreviewResult result,
                           boolean enableRefresh,
                           boolean enableShowLog)
   {
      String shortFileName = StringUtil.shortPathName(
            FileSystemItem.createFile(result.getHtmlFile()), 
            ThemeStyles.INSTANCE.subtitle(), 
            300);
      fileLabel_.setText(shortFileName);
      showLogButtonSeparator_.setVisible(enableShowLog);
      showLogButton_.setVisible(enableShowLog);
      saveHtmlPreviewAsSeparator_.setVisible(result.getEnableSaveAs());
      saveHtmlPreviewAs_.setVisible(result.getEnableSaveAs());
      publishButton_.setHtmlPreview(result);
      publishButtonSeparator_.setVisible(publishButton_.isVisible());
      refreshButtonSeparator_.setVisible(enableRefresh);
      refreshButton_.setVisible(enableRefresh);
      previewFrame_.navigate(url);
   }
   
   @Override
   public void print()
   {
      WindowEx window = previewFrame_.getWindow();
      window.focus();
      window.print();
   }
   
   @Override
   public String getDocumentTitle()
   {
      return previewFrame_.getWindow().getDocument().getTitle();
   }

   @Override
   public void focusFind()
   {
      findTextBox_.focus();
   }

   private final AnchorableFrame previewFrame_;
   private ToolbarLabel fileLabel_;
   private FindTextBox findTextBox_;
   private Widget saveHtmlPreviewAsSeparator_;
   private Widget saveHtmlPreviewAs_;
   private Widget publishButtonSeparator_;
   private RSConnectPublishButton publishButton_;
   private Widget showLogButtonSeparator_;
   private ToolbarButton showLogButton_;
   private Widget refreshButtonSeparator_;
   private ToolbarButton refreshButton_;
   private HTMLPreviewProgressDialog activeProgressDialog_;
}
