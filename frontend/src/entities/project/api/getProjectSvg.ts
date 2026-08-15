import { ApiError } from '@shared/api/api'

const storageOrigin = 'https://storage.yandexcloud.net'

export async function getProjectSvg(url: string): Promise<string> {
  const response = await fetch(getSvgRequestUrl(url))

  if (!response.ok) {
    throw new ApiError(response.status, null)
  }

  return response.text()
}

function getSvgRequestUrl(url: string): string {
  if (!import.meta.env.DEV) {
    return url
  }

  const svgUrl = new URL(url)

  if (svgUrl.origin !== storageOrigin) {
    return url
  }

  return `/svg-storage${svgUrl.pathname}${svgUrl.search}`
}
