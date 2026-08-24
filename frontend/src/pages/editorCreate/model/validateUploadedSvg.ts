export const MAX_UPLOADED_SVG_LENGTH = 5000

export type UploadedSvgValidationError =
  | 'invalidFileType'
  | 'invalidSvg'
  | 'tooLong'

function getFileExtension(fileName: string) {
  return fileName.split('.').pop()?.toLowerCase()
}

export function getUploadedSvgValidationError(
  fileName: string,
  data: string,
): UploadedSvgValidationError | undefined {
  const extension = getFileExtension(fileName)

  if (extension !== 'svg' && extension !== 'txt') {
    return 'invalidFileType'
  }

  if (Array.from(data).length > MAX_UPLOADED_SVG_LENGTH) {
    return 'tooLong'
  }

  const document = new DOMParser().parseFromString(data, 'image/svg+xml')

  if (
    document.querySelector('parsererror') ||
    document.documentElement.localName.toLowerCase() !== 'svg'
  ) {
    return 'invalidSvg'
  }
}
