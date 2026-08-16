import type { SvgImportWarning } from './types'

export function normalizeSvg(root: Element): SvgImportWarning[] {
  const warnings = normalizeViewBox(root)
  const usedEditorIds = new Set<string>()
  const generatedIdCounters = new Map<string, number>()
  const generatedNameCounters = new Map<string, number>()

  for (const element of root.querySelectorAll('*')) {
    if (element.closest('defs') || element.localName === 'tspan') {
      continue
    }

    const existingEditorId =
      element.getAttribute('data-editor-id') ?? element.getAttribute('id')
    const editorId =
      existingEditorId && !usedEditorIds.has(existingEditorId)
        ? existingEditorId
        : createEditorId(element.localName, generatedIdCounters, usedEditorIds)

    if (!existingEditorId || editorId !== existingEditorId) {
      element.setAttribute('data-editor-id', editorId)
      warnings.push({
        code: 'generated-editor-id',
        editorId,
        elementName: element.localName,
      })
    }

    usedEditorIds.add(editorId)

    if (!element.hasAttribute('data-name')) {
      const name =
        element.getAttribute('id') ??
        (!existingEditorId
          ? createLayerName(element.localName, generatedNameCounters)
          : editorId)

      element.setAttribute('data-name', name)
    }
  }

  return warnings
}

function createLayerName(
  elementName: string,
  counters: Map<string, number>,
): string {
  const index = (counters.get(elementName) ?? 0) + 1

  counters.set(elementName, index)

  return `${elementName}-${index}`
}

function normalizeViewBox(root: Element): SvgImportWarning[] {
  if (root.localName !== 'svg' || root.hasAttribute('viewBox')) {
    return []
  }

  const width = parseViewportDimension(root.getAttribute('width')) ?? 1000
  const height = parseViewportDimension(root.getAttribute('height')) ?? 1000

  root.setAttribute('viewBox', `0 0 ${width} ${height}`)

  return [{ code: 'missing-view-box', height, width }]
}

function createEditorId(
  elementName: string,
  counters: Map<string, number>,
  usedEditorIds: ReadonlySet<string>,
): string {
  const prefix =
    elementName === 'g'
      ? 'group'
      : elementName === 'text'
        ? 'text'
        : ['circle', 'ellipse', 'line', 'path', 'rect'].includes(elementName)
          ? 'shape'
          : 'unsupported'
  let index = counters.get(prefix) ?? 0
  let editorId: string

  do {
    index += 1
    editorId = `${prefix}-${index}`
  } while (usedEditorIds.has(editorId))

  counters.set(prefix, index)

  return editorId
}

function parseViewportDimension(value: string | null): number | null {
  if (!value) {
    return null
  }

  const match = value.match(/^\s*(\d+(?:\.\d+)?|\.\d+)\s*(?:px)?\s*$/)
  const dimension = match ? Number(match[1]) : Number.NaN

  return Number.isFinite(dimension) && dimension > 0 ? dimension : null
}
