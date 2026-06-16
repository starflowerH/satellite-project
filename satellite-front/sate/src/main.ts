import { createApp } from 'vue'
// 引入全局电竞风 CSS 变量
import './styles/variables.css'
import App from './App.vue'
// 引入路由配置
import router from './router'
// 引入 Pinia 状态管理
import { createPinia } from 'pinia'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')