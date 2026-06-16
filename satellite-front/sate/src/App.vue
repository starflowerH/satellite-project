<script setup lang="ts">
/**
 * App.vue - 峡谷卫星根组件
 *
 * 作为整个应用的顶层容器
 * 直接渲染 router-view，由路由控制页面内容
 */
</script>

<template>
  <router-view />
</template>

<style>
/* 全局样式重置 */
*,
*::before,
*::after {
  box-sizing: border-box;
}

#app {
  width: 100%;
  min-height: 100vh;
}
</style>