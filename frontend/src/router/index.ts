import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true },
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/households',
      name: 'households',
      component: () => import('../views/HouseholdListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/households/:id',
      name: 'household-detail',
      component: () => import('../views/HouseholdDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/invitations/accept',
      name: 'invitation-accept',
      component: () => import('../views/InvitationAcceptView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
      meta: { guestOnly: true },
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  // Stellt persistierte Session aus localStorage wieder her, bevor Guards greifen.
  await authStore.restoreSession()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return { name: 'home' }
  }
  return true
})

export default router
