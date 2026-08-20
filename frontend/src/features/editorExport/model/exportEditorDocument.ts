import { type EditorDocument, serializeSvg } from '@entities/editorDocument'

export type ImageExportFormat = 'png' | 'jpeg' | 'svg'

interface ExportEditorDocumentParams {
  document: EditorDocument
  fileName: string
  format: ImageExportFormat
}

function getDownloadFileName(
  fileName: string,
  format: ImageExportFormat,
): string {
  const sanitizedFileName = fileName
    .trim()
    .replace(/\.(?:png|jpe?g|svg)$/i, '')
    .replace(/[\\/:*?"<>|]/g, '-')

  return `${sanitizedFileName || 'image'}.${format}`
}

function downloadBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')

  link.download = fileName
  link.href = url
  link.click()

  URL.revokeObjectURL(url)
}

function loadSvgImage(svg: string): Promise<HTMLImageElement> {
  const svgBlob = new Blob([svg], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(svgBlob)

  return new Promise((resolve, reject) => {
    const image = new Image()

    image.onload = () => {
      URL.revokeObjectURL(url)
      resolve(image)
    }
    image.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('Unable to load SVG for rasterization'))
    }
    image.src = url
  })
}

function canvasToBlob(canvas: HTMLCanvasElement, type: string): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (blob) {
        resolve(blob)

        return
      }

      reject(new Error('Unable to create image file'))
    }, type)
  })
}

async function rasterizeSvg(
  svg: string,
  editorDocument: EditorDocument,
  format: Extract<ImageExportFormat, 'png' | 'jpeg'>,
): Promise<Blob> {
  const canvas = document.createElement('canvas')
  const context = canvas.getContext('2d')

  if (!context) {
    throw new Error('Canvas 2D context is unavailable')
  }

  canvas.width = Math.max(1, Math.round(editorDocument.width))
  canvas.height = Math.max(1, Math.round(editorDocument.height))

  const image = await loadSvgImage(svg)

  if (format === 'jpeg') {
    context.fillStyle = '#ffffff'
    context.fillRect(0, 0, canvas.width, canvas.height)
  }

  context.drawImage(image, 0, 0, canvas.width, canvas.height)

  return canvasToBlob(canvas, format === 'png' ? 'image/png' : 'image/jpeg')
}

export async function exportEditorDocument({
  document,
  fileName,
  format,
}: ExportEditorDocumentParams): Promise<void> {
  const svg = serializeSvg(document)
  const downloadFileName = getDownloadFileName(fileName, format)

  if (format === 'svg') {
    downloadBlob(
      new Blob([svg], { type: 'image/svg+xml;charset=utf-8' }),
      downloadFileName,
    )

    return
  }

  const imageBlob = await rasterizeSvg(svg, document, format)
  downloadBlob(imageBlob, downloadFileName)
}
