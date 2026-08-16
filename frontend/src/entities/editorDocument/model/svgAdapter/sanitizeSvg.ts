import type { SvgImportWarning } from './types'

const unsafeElementNames = new Set([
  'foreignObject',
  'iframe',
  'script',
  'style',
])

export function sanitizeSvg(root: Element): SvgImportWarning[] {
  const warnings: SvgImportWarning[] = []

  for (const element of Array.from(root.querySelectorAll('*'))) {
    if (unsafeElementNames.has(element.localName)) {
      warnings.push({
        code: 'removed-unsafe-element',
        elementName: element.localName,
      })
      element.remove()
      continue
    }

    for (const attribute of Array.from(element.attributes)) {
      if (isUnsafeAttribute(attribute.name, attribute.value)) {
        warnings.push({
          attributeName: attribute.name,
          code: 'removed-unsafe-attribute',
          elementName: element.localName,
        })
        element.removeAttribute(attribute.name)
      }
    }
  }

  return warnings
}

function isUnsafeAttribute(name: string, value: string): boolean {
  if (name.toLowerCase().startsWith('on')) {
    return true
  }

  if (name === 'href' || name === 'xlink:href') {
    return !value.startsWith('#') && !value.startsWith('data:image/')
  }

  return /url\(\s*['"]?(?:https?:|\/\/)/i.test(value)
}
