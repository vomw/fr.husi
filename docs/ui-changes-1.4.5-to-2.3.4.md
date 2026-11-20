# UI changes from 1.4.5 to 2.3.4

This document compares the source trees at `0f4d6d467fe8710511ddeea042ea4ce8d193dc2f`
(version 1.4.5, version code 491) and `0822cc7b34ae3c57ee2840d553ca8cf31cf64867`
(version 2.3.4, version code 551). It records material user-visible changes
found in Compose screens, Android activities/shortcuts, manifests, resources,
and navigation. It intentionally excludes implementation-only refactors unless
they change what the user can see or do.

## Navigation and shared presentation

- The drawer-based primary navigation has been replaced with an adaptive
  navigation suite containing Configuration, Dashboard, Route, Log, and
  Settings. Screens entered from those destinations now use ordinary back
  navigation instead of reopening the drawer.
- Dashboard can display a notification badge for pending VPN-authentication and
  unread Taildrop events.
- Settings is no longer one long page: it is a landing page that opens separate
  General, System Daemon, Route, Protocol, DNS, Inbound, Miscellaneous, NTP,
  and App Update pages. Network, Backup, Debug, STUN, certificate, VPN-scanner,
  network-quality, and rule-set-match tools are reached from it.
- Screen chrome was refreshed broadly: capsule top bars and search bars, grouped
  menus, scrollable menu popups, blurred/hazy top-bar backgrounds, revised
  preference rows, reusable snackbar handling, and clearer desktop scrollbars.
- Husi is capitalized consistently in the visible app name, and the Android
  launcher icon was replaced with an adaptive/monochrome-capable icon.

## Dashboard, status, and traffic

- The old statistics bar was removed. The dashboard now presents status as a
  configurable grid of cards.
- Users can enter **Edit dashboard**, reorder visible cards, hide/show cards,
  inspect hidden cards, and reset their custom card layout. Cards cover proxy
  and direct speed, service status, OpenConnect, OpenVPN, Tailscale, exit IP,
  source addresses, Clash mode, system proxy, and network interfaces.
- Exit-IP detection was added. It can be invoked by tapping its status card,
  can run when the core is not yet created, and can optionally run on entry.
  The IP-probe settings help is only shown while editing the relevant setting.
- STUN/IP detection now reports NAT behavior, and the dashboard includes the
  resulting endpoint/source-address information.
- Proxy-set dashboard browsing gained an in-page search with a mode switch for
  searching outbounds versus proxy sets, result/empty states, ordering choices,
  nested-set expansion for URL tests, asynchronous URL-test feedback, and a
  loading indicator.
- Connection displays gained per-connection speed formatting, wrapped IPv6
  addresses, clearer empty states (all/active/closed/search/no status), and
  revised detail presentation.
- Tailscale, OpenVPN, and OpenConnect status now have dedicated dashboard
  sections/cards rather than being limited to generic service status.

## Tailscale, Taildrop, and SSH

- Tailscale and Tailcat became selectable profile types, each with a dedicated
  profile editor. Tailscale exposes endpoint and peer detail screens, exit-node
  selection, status cards, and API controls.
- Taildrop was added: users can select a target, send shared Android content to
  it, view an inbox, and access received files through an Android Documents
  Provider. Taildrop unread events drive the dashboard badge.
- Tailscale SSH was added with an SSH-login dialog and terminal screen. On
  Android it opens in its own task; desktop gets a separate SSH window.
- The Tailscale SSH server option is hidden unless Advanced features are
  enabled.

## Remote control

- A Remote Control destination was added. Users can add, edit, test, remove,
  and select remote servers using a host/port or HTTP(S) URL plus a secret.
- The app exposes the selected target in contextual menus and uses a session
  banner to show remote connection state, uptime, failures, and a Disconnect
  action. Dashboard, log viewing, and related controls can operate on the
  chosen target.

## Updates, backup, and Android integrations

- Android gained a complete app-update experience: automatic daily checks,
  pre-release selection, optional connected-only checks, immediate checks,
  GitHub-token entry/linking, release-page opening, version skipping,
  download/install progress, install-permission guidance, and optional silent
  installation through Shizuku. It also offers an Obtainium hand-off.
- Backup now supports configured WebDAV storage: configure URL/credentials,
  upload the selected export content, list remote backups, restore one, or
  select and delete several. Existing local export/import/share remains.
- The Android launcher shortcut now invokes the new quick-toggle activity.
  The quick-toggle behavior runs in the background process and is not exported
  as a general external activity.
- The Tasker integration is now explicitly opt-in in settings; enabling it
  explains that an automation app with the Tasker permission can toggle the
  tunnel.

## Logs, tools, and editors

- The log screen was rebuilt into a searchable, selectable live view with
  pause/resume, auto-scroll control, jump-to-bottom, copying/sharing, clearing,
  log-level selection, remote-target selection, and remote-session status.
  A terminal mode preserves ANSI colors, adds terminal-style scrolling and
  extra keys, and raises the retained log capacity.
- The former Speed Test screen was replaced by **Network quality**, which
  measures download/upload capacity and responsiveness (RPM) via the Apple
  `networkQuality` protocol. Its UI exposes configuration URL, serial testing,
  maximum runtime, HTTP/3, and outbound selection.
- Rule-set matching, STUN, certificate, VPN-scanning, debug, and network tools
  were retained but reorganized into the Settings tools area and refreshed with
  the shared navigation and presentation system.
- The JSON editor gained syntax highlighting that distinguishes keys, quoted
  strings, and other values, plus completion/performance improvements visible
  while editing.

## Profiles, groups, routes, and configuration

- MASQUE was added as a profile type, with its own settings screen. HTTP now
  has a version picker; certificate SHA-256 configuration is exposed where
  applicable.
- Proxy sets can now include individual profiles, groups (with a filter), and
  members of another proxy set. Their source editor gained an **Add source**
  menu and explicit dialogs for group/member sources.
- Chain and proxy-set source lists use visible drag handles for reordering;
  swipe-to-delete was removed in favor of explicit remove controls. This makes
  the reorder/remove affordances less ambiguous.
- Proxy-set controls added multi-source support, member expansion, balancer
  support, rotation interval, speed-based ordering, and more visible source
  summaries.
- Route UI terminology shifts from “bypass” to “direct” in several places.
  Route rules can show outbound/action behavior with color, include custom
  rule-set matching, direct ICMP and LAN controls, Clash-mode status, and
  clearer asset filename/path-limit errors.
- Configuration/profile editing was revised to use the new preference rows,
  scrollable action menus, state-based text fields, and safer back-stack/result
  handling. Group/profile paging and profile selection behavior were improved.

## Other visible changes

- Plugins can appear as **Not installed** entries with a link to their download
  page, instead of simply disappearing when unavailable.
- About gained a documentation/wiki entry and no longer uses its former hidden
  long-press “expert” switch. Expert mode was replaced by an explicit
  **Advanced features** setting that accepts one `key` or `key=value` per line.
- Desktop adds UI controls for system proxy management, window-size persistence,
  hotkeys, process icons, and a tray action to copy proxy environment settings.
- Android improves predictive-back behavior, Android 9 focus behavior, and
  scanner responsiveness. Authentication dialogs now cover both OpenConnect and
  OpenVPN flows.
- English resources were substantially reorganized and translations were
  updated across the shipped locales; wording changes include normalized
  “Direct”, “Settings”, “Details”, and “Husi” labels.

## Verification basis

The comparison was made with `git diff`, `git show`, and commit history across
the two supplied commits. It is a source-level UI inventory, not a visual
regression test: no APK was built or interacted with as part of this document.
