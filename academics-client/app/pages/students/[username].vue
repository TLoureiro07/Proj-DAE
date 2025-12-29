<template>
  <div v-if="student">
    <h2>Details of {{ student.username }}</h2>
    <p><strong>Name:</strong> {{ student.name }}</p>
    <p><strong>Email:</strong> {{ student.email }}</p>

    <div v-if="student.subjects && student.subjects.length">
      <h3>Enrolled in:</h3>
      <ul>
        <li v-for="subject in student.subjects" :key="subject.code">
          {{ subject.name }} — {{ subject.schoolYear }}
        </li>
      </ul>
    </div>
  </div>

  <div v-else>
    <h2>Error messages:</h2>
    <ul>
      <li v-for="message in messages" :key="message">{{ message }}</li>
    </ul>
  </div>
</template>

<script setup>
const route = useRoute()
const username = route.params.username
const config = useRuntimeConfig()
const api = config.public.apiBase

const messages = ref([])

const { data: student, error } = await useFetch(`${api}/students/${username}`)

if (error.value) {
  messages.value.push(error.value.message || 'Error fetching student data')
}
</script>
