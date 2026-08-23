export const ROLES = Object.freeze({ USER: 'USER', MANAGER: 'MANAGER', ADMIN: 'ADMIN' })

export function hasRole(user, ...allowedRoles) {
  return Boolean(user?.role && allowedRoles.includes(user.role))
}

export function canAccess(user, requiredRoles = []) {
  return requiredRoles.length === 0 || hasRole(user, ...requiredRoles)
}