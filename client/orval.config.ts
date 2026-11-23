import { defineConfig } from 'orval'

export default defineConfig({
  agora: {
    input: {
      target: './openapi.json',
    },
    output: {
      mode: 'tags-split',
      target: 'src/api/generated/endpoints.ts',
      schemas: 'src/api/generated/model',
      client: 'fetch',
      httpClient: 'fetch',
      mock: {
        type: 'msw',
        delay: 0,
      },
      clean: true,
      prettier: true,
    },
  },
})
