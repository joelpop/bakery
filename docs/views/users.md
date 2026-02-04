# Users View

The Users view provides CRUD (Create, Read, Update, Delete) functionality for managing staff accounts. This view is typically restricted to Admin users.

## Screenshots
- User list: `legacy/images/users/Desktop, CRUD (users, products).png`
- Edit dialog: `legacy/images/users/new+edit user dialog/Desktop, CRUD, new+edit.png`

## User List

### Layout
A searchable data grid displaying all users in the system.

### Search Bar
- Text input for filtering users
- Searches across email, name fields
- "+ New user" button to add new users

### Grid Columns

| Column | Description | Sortable |
|--------|-------------|----------|
| (Avatar) | User profile photo | No |
| Email | Login email address | Yes |
| Name | Full name (First + Last) | Yes |
| Role | User role | Yes |

### Row Actions
- Click row to edit user (opens Edit User dialog)
- Edit icon button on hover for quick access

### Technical Note: User List Query
```
UserListItem {
  id: Long
  email: String
  firstName: String
  lastName: String
  role: UserRole
  photoUrl: String (optional)
}
```

---

## New/Edit User Dialog

A side panel dialog for creating or editing user accounts.

### Form Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| Photo | Image Upload | No | User profile picture with "Upload new..." link |
| Email (login) | Email Input | Yes | Unique login identifier |
| First name | Text Input | Yes | User's first name |
| Last name | Text Input | Yes | User's last name |
| Password | Password Input | Yes | Account password (with show/hide toggle) |
| Role | Dropdown | Yes | User role selection |

### Role Options

| Role | Description |
|------|-------------|
| Admin | Full system access including Users and Products management |
| Baker | Kitchen staff - Storefront and Dashboard access |
| Barista | Front-of-house - Storefront and Dashboard access |

### Actions

| Button | Description |
|--------|-------------|
| **Save** | Save changes and close dialog (primary action) |
| **Cancel** | Discard changes and close dialog |
| **Delete** | Remove user from system (confirmation required) |

### Validation Rules
- Email must be unique in the system
- Email must be valid format
- Password must meet security requirements
- First name and last name are required

### Technical Note: User Entity
```
User {
  id: Long
  email: String (unique, login identifier)
  firstName: String
  lastName: String
  password: String (hashed, never returned in queries)
  role: UserRole [Admin, Baker, Barista]
  photo: Blob (optional)
  photoContentType: String (optional)
}
```

---

## Access Control

### View Access
- Only users with **Admin** role can access the Users view
- The Users navigation item should be hidden for non-Admin users

### Self-Editing Restrictions
- Users cannot delete their own account
- Users cannot demote themselves from Admin role (if last Admin)
- Password change for own account may require current password verification

---

## Responsive Behavior

### Phone Layout
- Grid columns may be reduced (hide email, show only name and role)
- Edit dialog takes full screen
- Search and New User button at top
