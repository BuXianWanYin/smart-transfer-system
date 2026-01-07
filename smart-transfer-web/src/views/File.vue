<template>
  <div class="file-wrapper">
    <FileList />
  </div>
</template>

<script setup>
import FileList from '@/components/file/FileList.vue'
</script>

<style lang="scss" scoped>
.file-wrapper {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style>
