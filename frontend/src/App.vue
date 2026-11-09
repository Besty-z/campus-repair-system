<template>
  <!-- 根组件：仅渲染当前路由匹配的页面 -->
  <router-view />
</template>

<script setup>
</script>

<style>
/* 全局基础样式 */
html,
body,
#app {
  height: 100%;
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
}
</style>
