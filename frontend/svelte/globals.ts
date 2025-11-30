
let fileContentReadCallback: ((content: string) => void) | null = null;

export function registerFileContentRead(callback: (content: any) => void) {
  fileContentReadCallback = callback;
}

// @ts-ignore
window.triggerFileContentRead = (content: string) => {
  if (fileContentReadCallback) {
    fileContentReadCallback(content);
    fileContentReadCallback = null;
  }
}

export async function openFileDialog(): Promise<any> {
  return new Promise(async (resolve, reject) => {
    try {
      registerFileContentRead((resp: any) => {
        resolve(resp);
      })

      const _resp = await (fetch('/open-file', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({})
      }))
    } catch (e) {
      console.error(e)
      reject(e)
    }
  })
}

let fileSavedCallback: ((filePath: string) => void) | null = null;

export function registerFileSaved(callback: (filePath: string) => void) {
  fileSavedCallback = callback;
}

// @ts-ignore
window.triggerFileSaved = (filePath: string) => {
  if (fileSavedCallback) {
    fileSavedCallback(filePath);
    fileSavedCallback = null;
  }
}

export async function openSaveFileDialog(): Promise<any> {
  return new Promise(async (resolve, reject) => {
    try {
      registerFileSaved((resp: any) => {
        resolve(resp);
      })

      const _resp = await (fetch('/save-file', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({})
      }))
    } catch (e) {
      console.error(e)
      reject(e)
    }
  })
}
