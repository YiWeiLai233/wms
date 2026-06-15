# Custom Role Permissions Design

## Context

The WMS project already has authentication, role assignment, and database tables for roles and permissions:

- `sys_user`, `sys_role`, `sys_user_role`
- `sys_permission`, `sys_role_permission`
- JWT login with role codes in the token
- A static Vue sidebar menu and a basic role helper

The current gap is that authenticated users can access any backend API, and the frontend only checks whether a user is logged in. The new permission system must prevent unauthorized operations at the backend while also hiding unavailable modules and actions in the UI.

## Goals

1. Administrators can create, edit, and delete custom roles.
2. Each role acts as a permission template.
3. Permissions support module-level access and operation-level actions.
4. Accounts can be assigned one or more roles.
5. Users cannot access unauthorized modules or call unauthorized backend operations.
6. The frontend only shows menus, routes, and buttons allowed by the user's permissions.
7. Existing default roles and user assignments remain compatible.

## Non-Goals

- Data-scope permissions such as limiting a user to one warehouse or platform.
- Field-level permissions such as hiding specific columns.
- A full audit approval workflow for role changes.

These can be added later without changing the core role-permission model.

## Permission Model

Roles are the editable templates. A role has:

- `roleCode`
- `roleName`
- `description`
- a set of permission IDs

Permissions are system-defined capabilities. They are not free-text user input. This keeps API authorization stable and prevents invalid permission codes from being assigned.

Permission codes use this format:

```text
module:action
```

Examples:

- `dashboard:view`
- `product:view`
- `product:create`
- `product:update`
- `product:delete`
- `order:view`
- `order:import`
- `outbound:confirm`
- `system:user`
- `system:role`

Module/page access is represented by `module:view` or a leaf page code such as `system:role`. Operation buttons and write endpoints use more specific actions.

`SUPER_ADMIN` is a protected built-in role and always has all permissions, even if role-permission rows are missing. This prevents administrators from locking themselves out.

## Permission Catalog

Initial permission codes should cover the current UI and backend endpoints:

```text
dashboard:view
ai:assistant
ai:knowledge
warehouse:view
warehouse:create
warehouse:update
warehouse:delete
product:view
product:create
product:update
product:delete
stock:view
stock:adjust
stock:check
stock:log
order:view
order:import
order:update
outbound:view
outbound:create
outbound:scan
outbound:confirm
outbound:cancel
return:view
return:create
return:check
return:confirm
return:cancel
express:view
express:company
express:template
express:report
system:user
system:role
system:file
system:log
system:stock-alert
system:stock-alert-template
system:platform
```

This list is intentionally module-oriented. If a future page needs finer control, add new codes without changing the role assignment model.

## Backend Design

Add a permission service that can:

- load all available permissions as a tree
- load permissions for a role
- replace a role's permission set
- load effective permission codes for a user across all assigned roles
- treat `SUPER_ADMIN` as all-permission

Add role management APIs:

```text
GET    /api/roles
GET    /api/roles/{id}
POST   /api/roles
PUT    /api/roles/{id}
DELETE /api/roles/{id}
GET    /api/permissions/tree
PUT    /api/roles/{id}/permissions
```

Keep the existing `/api/roles` list endpoint compatible for the user form.

Authorization must be enforced by the backend. Preferred implementation:

- Add an annotation such as `@RequirePermission("order:view")`.
- Add a Spring MVC interceptor that checks the current request's user ID and effective permissions.
- Return `403` using the existing `Result` response format when permission is missing.
- Allow unauthenticated public endpoints such as login, health, docs, static images, and AI internal token paths exactly as they work now.

JWT may continue storing role codes, but backend permission checks should load current permissions by user ID. This makes permission edits effective without requiring users to log in again.

## Frontend Design

Extend the user store to keep:

- `roles`
- `permissions`
- `userInfo`

Login and profile responses should include the effective permission code list.

Update menu configuration so each menu item has a `permission` field. The layout filters the sidebar from current permissions. Empty parent groups are hidden.

Update route metadata with required permissions. The route guard should:

- redirect unauthenticated users to login
- fetch the profile if permissions are not loaded
- redirect unauthorized routes to the first accessible route, or show a 403 page if no route is available

Add a permission helper:

```ts
hasPermission(code: string)
hasAnyPermission(codes: string[])
hasAllPermissions(codes: string[])
```

Use it for page action buttons, for example create, edit, delete, import, confirm, reset password, and role permission editing.

## Default Permissions

Seed permissions for the current modules:

- Dashboard
- AI assistant and knowledge base
- Warehouse management
- Product and SKU management
- Stock query, log, check, and adjustment
- Order management
- Outbound management
- Return management
- Express query, company, fee template, and fee report
- System users, roles, files, logs, stock alert, stock alert template, and platform management

Seed default role templates:

- `SUPER_ADMIN`: all permissions
- `WAREHOUSE_ADMIN`: warehouse, product view, stock, order, outbound, returns, express, dashboard
- `OPERATOR`: dashboard, stock query/log, outbound scan/confirm, returns basic actions
- `VIEWER`: read-only permissions

Existing roles should receive sane default permissions through a migration script.

## Safety Rules

- Only `SUPER_ADMIN` can create, update, delete roles or edit role permissions.
- Built-in roles cannot be deleted.
- `SUPER_ADMIN` cannot be removed from the last enabled super-admin account.
- A user cannot disable or delete their own account through the user management screen.
- Role codes must be unique and stable.
- Deleting a custom role should be logical deletion and should fail if users are still assigned to it.

## Error Handling

- Missing login token: `401`
- Authenticated but missing permission: `403`
- Missing role or permission: existing `NOT_FOUND` style response
- Duplicate role code: `BAD_REQUEST` with a clear message
- Attempting to delete protected or assigned roles: `BAD_REQUEST` with a clear message

## Testing Plan

Backend tests:

- User with no permission receives `403` for a protected endpoint.
- `SUPER_ADMIN` can access all protected endpoints.
- A custom role's permissions are replaced correctly.
- Login/profile returns effective permission codes.
- Permission edits take effect without issuing a new token.
- Protected role safety rules reject dangerous operations.

Frontend checks:

- Sidebar hides unauthorized modules.
- Direct navigation to an unauthorized route is blocked.
- Buttons hide or disable based on operation permissions.
- Role permission tree can load, save, and reload selections.

## Rollout

1. Add backend permission entities, mapper methods, service methods, interceptor, and tests.
2. Add SQL migration for permissions and default role-permission mappings.
3. Add role and permission APIs.
4. Extend login/profile responses with permissions.
5. Add frontend permission helpers, route/menu metadata, and filtering.
6. Add the role management UI for custom roles and permission templates.
7. Run backend tests and frontend build.
