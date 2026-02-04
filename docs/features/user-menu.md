# User Menu

The User Menu provides access to user profile, preferences, notifications, and logout functionality.

## Screenshots
- Desktop: `legacy/images/user menu/Desktop, user menu, notifications.png`
- Phone: `legacy/images/user menu/Phone, user menu, notifications.png`

---

## Menu Trigger

### Desktop
- User avatar displayed in top-right corner
- Notification badge (red dot) when unread notifications exist
- Click to open dropdown menu

### Mobile
- Same avatar placement in header
- Notification badge visible
- Tap to open menu (may slide in from right or drop down)

---

## Menu Contents

### User Profile Section
| Element | Description |
|---------|-------------|
| Avatar | User's profile photo (or default avatar) |
| Name | User's full name (e.g., "Kate Austin") |
| Email | User's email address (e.g., "kate@cafe-sunshine.com") |

### Actions
| Action | Description |
|--------|-------------|
| **Preferences** | Link to user preferences/settings |
| **Log out** | End current session and return to login |

### Notifications Section
| Element | Description |
|---------|-------------|
| Header | "Notifications" with "View all" link |
| Notification List | Recent notifications |

---

## Notifications (Deferred)

> **Note**: User-to-user notifications are deferred for future implementation. The notification entity and UI are documented but will not be included in the initial release.

### Planned Notification Features (Future)

When implemented, notifications will include:
- **Sender name** - Who sent the notification
- **Timestamp** - When it was sent
- **Message** - Notification content
- **Unread indicator** - Visual marker for unread items
- **Notification badge** - Red dot on avatar when unread notifications exist

---

## Preferences

The Preferences link leads to a full settings page with the following sections:

### Profile Settings
| Setting | Description |
|---------|-------------|
| Profile photo | Upload or change profile picture |
| Display name | First and last name (read-only, shows current values) |
| Email | Login email (read-only) |

### Security Settings
| Setting | Description |
|---------|-------------|
| Change password | Update account password with entropy-based validation |
| Passkeys | Manage WebAuthn credentials (add/remove passkeys) |

### Notification Preferences
| Setting | Description |
|---------|-------------|
| Email notifications | Toggle email alerts for important events |
| In-app notifications | Toggle in-app notification display |

### Display Settings
| Setting | Description |
|---------|-------------|
| Theme | Light/Dark mode preference (if supported) |
| Language | UI language selection (future enhancement) |

---

## Log Out

### Behavior
1. User clicks "Log out"
2. Session is terminated
3. User is redirected to login page
4. Any unsaved work may trigger confirmation dialog

### Technical Note
```
logout():
  invalidate session
  clear auth tokens
  redirect to /login
```

---

## Access Levels

The User Menu is available to all authenticated users regardless of role. The menu content is the same for Admin, Baker, and Barista roles.

---

## Responsive Behavior

### Desktop
- Dropdown menu positioned below avatar
- Click outside to close
- Hover states on actions

### Mobile
- May use full-width slide-in panel
- Larger touch targets
- Swipe to dismiss
- Notifications may scroll if numerous
