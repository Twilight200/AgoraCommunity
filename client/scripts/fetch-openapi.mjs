import { writeFile } from 'node:fs/promises'

const specUrl = process.env.SPRINGDOC_URL || 'http://localhost:8080/v3/api-docs'

try {
  const response = await fetch(specUrl, { headers: { accept: 'application/json' } })
  if (!response.ok) {
    throw new Error(`springdoc returned ${response.status} ${response.statusText}`)
  }

  const spec = await response.json()
  if (!spec.openapi && !spec.swagger) {
    throw new Error('The response is not an OpenAPI document')
  }

  await writeFile(new URL('../openapi.json', import.meta.url), `${JSON.stringify(spec, null, 2)}\n`)
  console.log(`OpenAPI document saved from ${specUrl}`)
} catch (error) {
  console.error(`Unable to fetch OpenAPI document from ${specUrl}`)
  console.error(error instanceof Error ? error.message : error)
  process.exitCode = 1
}
