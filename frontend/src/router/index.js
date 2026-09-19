import { createRouter, createWebHistory } from 'vue-router'
import Tank from '../views/Tank.vue'
import Creature from '../views/Creature.vue'
import Feeding from '../views/Feeding.vue'
import Maintenance from '../views/Maintenance.vue'

const routes = [
  { path: '/', redirect: '/tanks' },
  { path: '/tanks', name: '展缸', component: Tank },
  { path: '/creatures', name: '生物', component: Creature },
  { path: '/feedings', name: '投喂', component: Feeding },
  { path: '/maintenances', name: '维生巡检', component: Maintenance }
]

export default createRouter({ history: createWebHistory(), routes })
