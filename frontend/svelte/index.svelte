<script lang="ts">
import Button from './_button.svelte'
import {openFileDialog, openSaveFileDialog, registerFileContentRead, registerFileSaved} from "./globals";


let isLoading = false
let javaMsg = ''

async function submit() {
  isLoading = true

  try {
    const resp = await (fetch('/ask-java', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({msg: 'Hello from JS'})
    }))

    const json = await resp.json()
    javaMsg = json.response
  } catch (e) {
    console.error(e)
  } finally {
    isLoading = false
  }
}

let fileContent: string | null = null
async function openFile() {
  try {
    const resp = await openFileDialog()
    fileContent = resp.content
  } catch (e) {
    console.error(e)
  }
}

let savedFilePath: string | null = null
async function saveFile() {
  try {
    const resp = await openSaveFileDialog()
    savedFilePath = resp.filePath
  } catch (e) {
    console.error(e)
  } finally {
  }
}

</script>

<div class="container mx-auto p-8 flex flex-col gap-6">
  <div class="text-2xl font-bold">javaelectron</div>
  <p>Build cross-platform desktop apps with Java, JavaScript, HTML, and CSS</p>
  <div class="flex flex-col gap-4 border rounded p-3">
    <div>
      <Button {isLoading} onClick={submit}>
        Click to communicate with Java
      </Button>
    </div>
    {#if javaMsg}
      <div>Java said: {javaMsg}</div>
    {/if}
  </div>
  <div class="flex flex-col gap-4 border rounded p-3">
    <div>
      <Button onClick={openFile}>
        Open a file
      </Button>
    </div>
    {#if fileContent}
      <div>Content: {fileContent}</div>
    {/if}
  </div>
  <div class="flex flex-col gap-4 border rounded p-3">
    <div>
      <Button onClick={saveFile}>
        Save a file
      </Button>
    </div>
    {#if savedFilePath}
      <div>Saved: {savedFilePath}</div>
    {/if}
  </div>
</div>

<style lang="scss">
</style>
