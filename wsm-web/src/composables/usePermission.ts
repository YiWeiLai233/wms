import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

export function usePermission() {
  const userStore = useUserStore()

  const isSuperAdmin = computed(() => userStore.roles.includes('SUPER_ADMIN'))
  const isAdmin = computed(() => userStore.roles.includes('ADMIN') || isSuperAdmin.value)

  function hasRole(role: string) {
    return userStore.roles.includes(role)
  }

  function hasAnyRole(roles: string[]) {
    return roles.some((r) => userStore.roles.includes(r))
  }

  return { isSuperAdmin, isAdmin, hasRole, hasAnyRole }
}
