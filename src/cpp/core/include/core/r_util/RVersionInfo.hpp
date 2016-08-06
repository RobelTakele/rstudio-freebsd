/*
 * RVersionInfo.hpp
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

#ifndef CORE_R_UTIL_R_VERSION_INFO_HPP
#define CORE_R_UTIL_R_VERSION_INFO_HPP

#include <string>
#include <iostream>

#include <boost/regex.hpp>

#include <core/SafeConvert.hpp>

#define kRVersionDefault   "Default"

#define kRVersionArch32    "32"
#define kRVersionArch64    "64"

namespace core {
namespace r_util {

struct RVersionInfo
{
   explicit RVersionInfo(const std::string& number = kRVersionDefault,
                         const std::string& arch = std::string())
      : number(number), arch(arch)
   {
   }

   std::string number;
   std::string arch;

   bool isDefault() const { return number == kRVersionDefault; }
};

class RVersionNumber
{
public:
   static RVersionNumber parse(const std::string& number)
   {
      boost::regex re("(\\d+)\\.(\\d+)(?:.(\\d+))?");
      boost::smatch match;
      boost::match_flag_type flags = boost::match_default |
                                     boost::match_continuous;

      RVersionNumber ver;
      if (boost::regex_search(number, match, re, flags))
      {
         ver.major_ = safe_convert::stringTo<int>(match[1], 0);
         ver.minor_ = safe_convert::stringTo<int>(match[2], 0);
         std::string match3 = match[3];
         if (!match3.empty())
            ver.patch_ = safe_convert::stringTo<int>(match3, 0);
      }
      return ver;
   }

   RVersionNumber()
      : major_(0), minor_(0), patch_(0)
   {
   }

public:
   bool empty() const { return major_ != 0; }

   int major_version() const { return major_; }
   int minor_version() const { return minor_; }
   int patch_version() const { return patch_; }

   bool operator<(const RVersionNumber& other) const
   {
      if (major_version() == other.major_version() && minor_version() == other.minor_version())
         return patch_version() < other.patch_version();
      else if (major_version() == other.major_version())
         return minor_version() < other.minor_version();
      else
         return major_version() < other.major_version();
   }

   bool operator==(const RVersionNumber& other) const
   {
      return major_version() == other.major_version() &&
             minor_version() == other.minor_version() &&
             patch_version() == other.patch_version();
   }

   bool operator!=(const RVersionNumber& other) const
   {
      return !(*this == other);
   }

private:
   int major_;
   int minor_;
   int patch_;
};

inline std::ostream& operator<<(std::ostream& os, const RVersionNumber& ver)
{
   os << ver.major_version() << "." << ver.minor_version() << "." << ver.patch_version();
   return os;
}


} // namespace r_util
} // namespace core 


#endif // CORE_R_UTIL_R_VERSION_INFO_HPP

