<script setup lang="ts">
import { ref } from 'vue'
import axios from 'axios'

const result = ref<string>('')

async function fetchBffData() {
  try {
    const response = await axios.get('/api/bff/health')
    result.value = JSON.stringify(response.data)
  } catch (error) {
    console.error('Error fetching data:', error)
    result.value = '요청 실패'
  }
}

async function fetchBeData() {
  try {
    const response = await axios.get('/api/bff/backend-health')
    result.value = JSON.stringify(response.data)
  } catch (error) {
    console.error('Error fetching data:', error)
    result.value = '요청 실패'
  }
}
</script>

<template>
  <div>
    <h1>QuestLog</h1>
    <button @click="fetchBffData">Check BFF</button>
    <button @click="fetchBeData">Check Backend</button>
    <pre>{{ result }}</pre>
  </div>
</template>