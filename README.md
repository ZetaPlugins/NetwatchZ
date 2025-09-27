![NetwatchZ Banner](https://cdn.modrinth.com/data/cached_images/074c056c721895b3f579093a106515ef471abf4b.png)

---

![spigot](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/spigot_vector.svg)
![paper](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/paper_vector.svg)
![purpur](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/purpur_vector.svg)
![velocity](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/velocity_vector.svg)
[![github](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/github_vector.svg)](https://github.com/ZetaPlugins/NetwatchZ)
[![modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/modrinth_vector.svg)](https://modrinth.com/plugin/netwatchz)
[![discord-plural](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/social/discord-plural_vector.svg)](https://strassburger.org/discord)
[![gitbook](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/documentation/gitbook_vector.svg)](https://docs.zetaplugins.com/netwatchz)

NetwatchZ is a Minecraft plugin for Spigot, Paper, and Velocity that helps protect your server against bots and other malicious activity through IP blocking, geoblocking, and VPN detection.

## Features

- ✅ IP lookup (e.g. Geolite2, ip-api.com)
- ✅ IP blacklists
- ✅ Dynamically fetch IP lists
- ✅ Geoblocking
- ✅ Configure any IP information service
- ✅ Automatically download and refresh Geolite2 database
- ✅ VPN blocking (e.g. vpnapi.io, proxycheck.io)

## Permissions

- `netwatchz.ipinfo` - Get the IP and IP information from a player
- `netwatchz.admin.debug` - Generate debug reports

## Configuration

You can find the configuration files in the plugins/NetwatchZ folder. The main configuration file is config.yml.

<details>
<summary>config.yml</summary>

```yml
#
#    _   _      _                 _       _       ______
#   | \ | |    | |               | |     | |     |___  /
#   |  \| | ___| |___      ____ _| |_ ___| |__      / /
#   | . ` |/ _ \ __\ \ /\ / / _` | __/ __| '_ \    / /
#   | |\  |  __/ |_ \ V  V / (_| | || (__| | | |  / /__
#   |_| \_|\___|\__| \_/\_/ \__,_|\__\___|_| |_| /_____|
#

# Set the language to any code found in the "lang" folder (don't add the .yml extension)
# You can add your own language files. Use https://github.com/ZetaPlugins/NetwatchZ/tree/main/src/main/resources/lang/en-US.yml as a template
# If you want to help translating the plugin, please refer to this article: https://docs.zetaplugins.com/localization
#  | en-US | de-DE |
lang: "en-US"

# If set to true, OP players will always be allowed to join
always_allow_ops: true

ip_list:
  # Enable or disable the IP list feature.
  enabled: true

  # If the mode is "blacklist", the IPs in the lists will be blocked.
  # If the mode is "whitelist", only the IPs in the lists will be allowed.
  mode: "blacklist"

  # Set the list of IPs or CIDR ranges to block or allow.
  # These are files inside the "plugins/NetwatchZ/ipLists" folder.
  lists:
    - "vpn_list.txt"
    # - "another_list.txt"

  fetch_jobs:
    vpn_list:
      # Set the URL to fetch the IP list from.
      url: "https://raw.githubusercontent.com/X4BNet/lists_vpn/refs/heads/main/output/vpn/ipv4.txt"

      # Set the filename to save the fetched list as. This file will be saved in the "plugins/NetwatchZ/ipLists" folder.
      filename: "vpn_list.txt"

      # Set how often the list should be fetched (in hours).
      # The minimum is 1 hour.
      update_interval_hours: 24

    # You can add more fetch jobs here. e.g.
    # another_list:
    #   url: "https://example.com/another_list.txt"
    #   filename: "another_list.txt"
    #   update_interval_hours: 12

geo_blocking:
  # Enable or disable geo-blocking.
  enabled: false

  # Wether or not the following list should be a blacklist or a whitelist.
  # If set to true, the countries in the list will be blocked.
  # If set to false, only the countries not in the list will be blocked.
  blacklist: true

  # Set the list of countries to block or allow.
  # The format of the country code depends on the IP info provider, but usually it is a two-letter country code (ISO 3166-1 alpha-2).
  # Example: ["US", "CA", "GB"]
  countries: []

ip_info_provider:
  # Set the IP info provider to use.
  # Possible values are:
  # - "ip-api" (https://ip-api.com)
  # - "ipwhois" (https://ipwhois.app)
  # - "geolite2" (MaxMind GeoLite2 databases - recommended)
  # - "custom" (Uses the custom IP info provider defined below)
  provider: "ip-api"

  geolite2:
    # Set the urls to the GeoLite2 databases. These are refreshed automatically every 7 days.
    # These can be links to tar.gz files of the official MaxMind GeoLite2 databases with your license key included or links to your own hosted databases (downloading .mmdb files directly is also supported).
    # You can get a free license key by creating an account on https://www.maxmind.com/en/geolite2/signup
    asn_url: "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-ASN&license_key=YOUR_KEY&suffix=tar.gz"
    city_url: "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-City&license_key=YOUR_KEY&suffix=tar.gz"
    country_url: "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-Country&license_key=YOUR_KEY&suffix=tar.gz"

    # Attribution: This product includes GeoLite2 data created by MaxMind, available from https://www.maxmind.com

  custom:
    # Set the custom IP info provider URL. It should return a JSON response.
    # Use %ip% as a placeholder for the IP address.
    url: "https://api.example.com/json/%ip%"

    # Set the custom IP info provider headers.
    headers:
      Authorization: "Bearer YOUR_API_KEY"

    # Set the custom IP info provider query parameters.
    parse_fields:
      # Keys are the data needed. Values are the json keys in the response.
      # If certain data is not provided by the API, you can leave it out.
      country: "country"
      countryCode: "countryCode"
      regionName: "regionName"
      region: "region"
      city: "city"
      lat: "latitude"
      lon: "longitude"
      timezone: "timezone"
      isp: "isp"
      org: "organization"
      asn: "asn"
      ip: "ip"

# The vpn_block settings are for online services that provide information about whether an IP is a VPN or proxy.
# This is independent of the ip_list feature, which can also be used to block VPNs
vpn_block:
  # Enable or disable the VPN blocking feature.
  enabled: false

  # Set the VPN info provider to use.
  # Possible values are:
  # - "vpnapi" (https://vpnapi.io)
  # - "proxycheck" (https://proxycheck.io)
  # - "custom" (Uses the custom VPN info provider defined below)
  provider: "vpnapi"

  # If using vpnapi or proxycheck, set your api key here
  api_key: ""

  custom:
    # Set the custom VPN info provider URL. It should return a JSON response.
    # Use %ip% as a placeholder for the IP address.
    url: "https://api.example.com/vpn/%ip%"

    # Set the custom VPN info provider headers.
    headers:
      Authorization: "Bearer YOUR_API_KEY"

    # Set the custom VPN info provider query parameters.
    parse_fields:
      # Keys are the data needed. Values are the json keys in the response.
      # If certain data is not provided by the API, you can leave it out.
      vpn: "security.vpn"
      proxy: "security.proxy"
      tor: "security.tor"
      relay: "security.relay"
      hosting: "security.hosting"

  # Set what to do when a certain type of VPN/proxy is detected.
  is_vpn:
    # Wether or not to block the connection if the IP is detected as a VPN.
    block: true

  is_proxy:
    block: false

  is_tor:
    block: true

  is_relay:
    block: false

  is_hosting:
    block: false
```
</details>

## Disclaimer

This plugin supports GeoLite2 for IP geolocation. This product includes GeoLite2 data created by MaxMind, available from https://www.maxmind.com. Server owners must download GeoLite2 with their own MaxMind account and license key and configure the download URL in plugin settings. The data is not used for identifying specific households or individuals.


## Support

If you need help with the setup of the plugin, or found a bug, you can join our discord [here](https://strassburger.org/discord).

[![discord-plural](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/social/discord-plural_vector.svg)](https://strassburger.org/discord)
[![gitbook](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/documentation/gitbook_vector.svg)](https://docs.zetaplugins.com/netwatchz)

---

[![Usage](https://bstats.org/signatures/bukkit/NetwatchZ.svg)](https://bstats.org/plugin/bukkit/NetwatchZ/27376)
