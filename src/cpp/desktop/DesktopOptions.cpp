/*
 * DesktopOptions.cpp
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

#include "DesktopOptions.hpp"

#include <QtGui>

#include <core/Error.hpp>
#include <core/Random.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include "DesktopUtils.hpp"

using namespace core;

namespace desktop {

#ifdef _WIN32
// Defined in DesktopRVersion.cpp
QString binDirToHomeDir(QString binDir);
#endif

QString scratchPath;

Options& options()
{
   static Options singleton;
   return singleton;
}

void Options::initFromCommandLine(const QStringList& arguments)
{
   for (int i=1; i<arguments.size(); i++)
   {
      QString arg = arguments.at(i);
      if (arg == QString::fromAscii(kRunDiagnosticsOption))
         runDiagnostics_ = true;
   }
}

void Options::restoreMainWindowBounds(QMainWindow* win)
{
   QString key = QString::fromAscii("mainwindow/geometry");
   if (settings_.contains(key))
      win->restoreGeometry(settings_.value(key).toByteArray());
   else
   {
      QSize size = QSize(1024, 768).boundedTo(
            QApplication::desktop()->availableGeometry().size());
      if (size.width() > 800 && size.height() > 500)
      {
         // Only use default size if it seems sane; otherwise let Qt set it
         win->resize(size);
      }
   }
}

void Options::saveMainWindowBounds(QMainWindow* win)
{
   settings_.setValue(QString::fromAscii("mainwindow/geometry"),
                      win->saveGeometry());
}

QString Options::portNumber() const
{
   // lookup / generate on demand
   if (portNumber_.length() == 0)
   {
      // Use a random-ish port number to avoid collisions between different
      // instances of rdesktop-launched rsessions
      int base = std::abs(core::random::uniformRandomInteger<int>());
      portNumber_ = QString::number((base % 40000) + 8080);

      // recalculate the local peer and set RS_LOCAL_PEER so that
      // rsession and it's children can use it
      QString localPeer = QDir(QDir::tempPath()).absolutePath() +
                          QString::fromAscii("/") + portNumber_ +
                          QString::fromAscii("-rsession");
      localPeer_ = localPeer.toUtf8().constData();
      core::system::setenv("RS_LOCAL_PEER", localPeer_);
   }

   return portNumber_;
}

QString Options::newPortNumber()
{
   portNumber_.clear();
   return portNumber();
}

FilePath Options::localPeerPath() const
{
   return FilePath(localPeer_);
}


namespace {
QString findFirstMatchingFont(const QStringList& fonts,
                              QString defaultFont,
                              bool fixedWidthOnly)
{
   for (int i = 0; i < fonts.size(); i++)
   {
      QFont font(fonts.at(i));
      if (font.exactMatch())
         if (!fixedWidthOnly || isFixedWidthFont(QFont(fonts.at(i))))
            return fonts.at(i);
   }
   return defaultFont;
}
} // anonymous namespace

QString Options::proportionalFont() const
{
   static QString detectedFont;

   QString font =
         settings_.value(QString::fromAscii("font.proportional")).toString();
   if (!font.isEmpty())
   {
      return font;
   }

   if (!detectedFont.isEmpty())
      return detectedFont;

   QStringList fontList;
#if defined(_WIN32)
   fontList <<
           QString::fromAscii("Segoe UI") << QString::fromAscii("Verdana") <<  // Windows
           QString::fromAscii("Lucida Sans") << QString::fromAscii("DejaVu Sans") <<  // Linux
           QString::fromAscii("Lucida Grande") <<          // Mac
           QString::fromAscii("Helvetica");
#elif defined(__APPLE__)
   fontList <<
           QString::fromAscii("Lucida Grande") <<          // Mac
           QString::fromAscii("Lucida Sans") << QString::fromAscii("DejaVu Sans") <<  // Linux
           QString::fromAscii("Segoe UI") << QString::fromAscii("Verdana") <<  // Windows
           QString::fromAscii("Helvetica");
#else
   fontList <<
           QString::fromAscii("Ubuntu") << // Ubuntu
           QString::fromAscii("Lucida Sans") << QString::fromAscii("DejaVu Sans") <<  // Linux
           QString::fromAscii("Lucida Grande") <<          // Mac
           QString::fromAscii("Segoe UI") << QString::fromAscii("Verdana") <<  // Windows
           QString::fromAscii("Helvetica");
#endif
   return QString::fromAscii("\"") +
         findFirstMatchingFont(fontList, QString::fromAscii("sans-serif"), false) +
         QString::fromAscii("\"");
}

void Options::setFixedWidthFont(QString font)
{
   if (font.isEmpty())
      settings_.remove(QString::fromAscii("font.fixedWidth"));
   else
      settings_.setValue(QString::fromAscii("font.fixedWidth"),
                         font);
}

QString Options::fixedWidthFont() const
{
   static QString detectedFont;

   QString font =
         settings_.value(QString::fromAscii("font.fixedWidth")).toString();
   if (!font.isEmpty())
   {
      return font;
   }

   if (!detectedFont.isEmpty())
      return detectedFont;

   QStringList fontList;
   fontList <<
#if defined(Q_WS_MACX)
           QString::fromAscii("Monaco")
#elif defined (Q_WS_X11)
           QString::fromAscii("Ubuntu Mono") << QString::fromAscii("Droid Sans Mono") << QString::fromAscii("DejaVu Sans Mono") << QString::fromAscii("Monospace")
#else
           QString::fromAscii("Lucida Console") << QString::fromAscii("Consolas") // Windows;
#endif
           ;

   // The fallback font is Courier, not monospace, because QtWebKit doesn't
   // actually provide a monospace font (appears to use Helvetica)

   return detectedFont = QString::fromAscii("\"") +
         findFirstMatchingFont(fontList, QString::fromAscii("Courier"), true) +
         QString::fromAscii("\"");
}


double Options::zoomLevel() const
{
   QVariant zoom = settings_.value(QString::fromAscii("view.zoomLevel"), 1.0);
   return zoom.toDouble();
}

void Options::setZoomLevel(double zoomLevel)
{
   settings_.setValue(QString::fromAscii("view.zoomLevel"), zoomLevel);
}



#ifdef _WIN32
QString Options::rBinDir() const
{
   // HACK: If RBinDir doesn't appear at all, that means the user has never
   // specified a preference for R64 vs. 32-bit R. In this situation we should
   // accept either. We'll distinguish between this case (where preferR64
   // should be ignored) and the other case by using null for this case and
   // empty string for the other.
   if (!settings_.contains(QString::fromAscii("RBinDir")))
      return QString::null;

   QString value = settings_.value(QString::fromAscii("RBinDir")).toString();
   return value.isNull() ? QString() : value;
}

void Options::setRBinDir(QString path)
{
   settings_.setValue(QString::fromAscii("RBinDir"), path);
}

bool Options::preferR64() const
{
   if (!core::system::isWin64())
      return false;

   if (!settings_.contains(QString::fromAscii("PreferR64")))
      return true;
   return settings_.value(QString::fromAscii("PreferR64")).toBool();
}

void Options::setPreferR64(bool preferR64)
{
   settings_.setValue(QString::fromAscii("PreferR64"), preferR64);
}
#endif

FilePath Options::scriptsPath() const
{
   return scriptsPath_;
}


void Options::setScriptsPath(const FilePath& scriptsPath)
{
   scriptsPath_ = scriptsPath;
}

FilePath Options::executablePath() const
{
   if (executablePath_.empty())
   {
      Error error = core::system::executablePath(QApplication::argc(),
                                                 QApplication::argv(),
                                                 &executablePath_);
      if (error)
         LOG_ERROR(error);
   }
   return executablePath_;
}

FilePath Options::supportingFilePath() const
{
   if (supportingFilePath_.empty())
   {
      // default to install path
      core::system::installPath("..",
                                QApplication::argc(),
                                QApplication::argv(),
                                &supportingFilePath_);

      // adapt for OSX resource bundles
#ifdef __APPLE__
         if (supportingFilePath_.complete("Info.plist").exists())
            supportingFilePath_ = supportingFilePath_.complete("Resources");
#endif
   }
   return supportingFilePath_;
}

FilePath Options::wwwDocsPath() const
{
   FilePath supportingFilePath = desktop::options().supportingFilePath();
   FilePath wwwDocsPath = supportingFilePath.complete("www/docs");
   if (!wwwDocsPath.exists())
      wwwDocsPath = supportingFilePath.complete("../gwt/www/docs");
   return wwwDocsPath;
}

#ifdef _WIN32

FilePath Options::urlopenerPath() const
{
   FilePath parentDir = scriptsPath();

   // detect dev configuration
   if (parentDir.filename() == "desktop")
      parentDir = parentDir.complete("urlopener");

   return parentDir.complete("urlopener.exe");
}

FilePath Options::rsinversePath() const
{
   FilePath parentDir = scriptsPath();

   // detect dev configuration
   if (parentDir.filename() == "desktop")
      parentDir = parentDir.complete("synctex/rsinverse");

   return parentDir.complete("rsinverse.exe");
}

#endif

QStringList Options::ignoredUpdateVersions() const
{
   return settings_.value(QString::fromAscii("ignoredUpdateVersions"), QStringList()).toStringList();
}

void Options::setIgnoredUpdateVersions(const QStringList& ignoredVersions)
{
   settings_.setValue(QString::fromAscii("ignoredUpdateVersions"), ignoredVersions);
}

core::FilePath Options::scratchTempDir(core::FilePath defaultPath)
{
   core::FilePath dir(scratchPath.toUtf8().constData());

   if (!dir.empty() && dir.exists())
   {
      dir = dir.childPath("tmp");
      core::Error error = dir.ensureDirectory();
      if (!error)
         return dir;
   }
   return defaultPath;
}

void Options::cleanUpScratchTempDir()
{
   core::FilePath temp = scratchTempDir(core::FilePath());
   if (!temp.empty())
      temp.removeIfExists();
}

bool Options::webkitDevTools()
{
   return settings_.value(QString::fromAscii("webkitDevTools"), false).toBool();
}

} // namespace desktop
