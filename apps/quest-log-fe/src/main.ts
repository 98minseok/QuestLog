import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import 'vuetify/styles'
import { createVuetify } from 'vuetify'
import '@mdi/font/css/materialdesignicons.css'
import './style.css'
import App from './App.vue'

const router = createRouter({
    history: createWebHistory(),
    routes :[]
})
const pinia = createPinia()
const vuetify = createVuetify()

createApp(App)
    .use(pinia)
    .use(router)
    .use(vuetify)
    .mount('#app')
