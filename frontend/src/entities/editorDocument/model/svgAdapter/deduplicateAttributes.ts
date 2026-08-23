import type { SvgImportWarning } from './types'

interface DeduplicationResult {
  svg: string
  warnings: SvgImportWarning[]
}

export function deduplicateSvgAttributes(svg: string): DeduplicationResult {
  const warnings: SvgImportWarning[] = []
  let result = ''
  let cursor = 0

  while (cursor < svg.length) {
    const tagStart = svg.indexOf('<', cursor)

    if (tagStart === -1) {
      result += svg.slice(cursor)
      break
    }

    result += svg.slice(cursor, tagStart)

    const tagEnd = getTagEnd(svg, tagStart)

    if (tagEnd === -1) {
      result += svg.slice(tagStart)
      break
    }

    const tag = svg.slice(tagStart, tagEnd + 1)
    result += isOpeningTag(tag) ? deduplicateTagAttributes(tag, warnings) : tag
    cursor = tagEnd + 1
  }

  return { svg: result, warnings }
}

function getTagEnd(svg: string, tagStart: number): number {
  if (svg.startsWith('<!--', tagStart)) {
    const commentEnd = svg.indexOf('-->', tagStart + 4)

    return commentEnd === -1 ? -1 : commentEnd + 2
  }

  if (svg.startsWith('<![CDATA[', tagStart)) {
    const cdataEnd = svg.indexOf(']]>', tagStart + 9)

    return cdataEnd === -1 ? -1 : cdataEnd + 2
  }

  if (svg.startsWith('<?', tagStart)) {
    const processingInstructionEnd = svg.indexOf('?>', tagStart + 2)

    return processingInstructionEnd === -1 ? -1 : processingInstructionEnd + 1
  }

  let quote: '"' | "'" | null = null

  for (let index = tagStart + 1; index < svg.length; index += 1) {
    const character = svg[index]

    if (quote) {
      if (character === quote) {
        quote = null
      }

      continue
    }

    if (character === '"' || character === "'") {
      quote = character
      continue
    }

    if (character === '>') {
      return index
    }
  }

  return -1
}

function isOpeningTag(tag: string): boolean {
  return !/^<\s*(?:!|\?|\/)/.test(tag)
}

function deduplicateTagAttributes(
  tag: string,
  warnings: SvgImportWarning[],
): string {
  let cursor = 1

  while (isWhitespace(tag[cursor])) {
    cursor += 1
  }

  const nameStart = cursor

  while (isNameCharacter(tag[cursor])) {
    cursor += 1
  }

  const elementName = tag.slice(nameStart, cursor)

  if (!elementName) {
    return tag
  }

  const attributes: string[] = []
  const attributeNames = new Set<string>()

  while (cursor < tag.length) {
    while (isWhitespace(tag[cursor])) {
      cursor += 1
    }

    if (tag[cursor] === '/' || tag[cursor] === '>') {
      break
    }

    const attributeStart = cursor

    while (isNameCharacter(tag[cursor])) {
      cursor += 1
    }

    const attributeName = tag.slice(attributeStart, cursor)

    if (!attributeName) {
      return tag
    }

    while (isWhitespace(tag[cursor])) {
      cursor += 1
    }

    if (tag[cursor] !== '=') {
      return tag
    }

    cursor += 1

    while (isWhitespace(tag[cursor])) {
      cursor += 1
    }

    const quote = tag[cursor]

    if (quote !== '"' && quote !== "'") {
      return tag
    }

    cursor += 1
    const valueEnd = tag.indexOf(quote, cursor)

    if (valueEnd === -1) {
      return tag
    }

    cursor = valueEnd + 1

    if (attributeNames.has(attributeName)) {
      warnings.push({
        attributeName,
        code: 'ignored-duplicate-attribute',
        elementName,
      })
      continue
    }

    attributeNames.add(attributeName)
    attributes.push(tag.slice(attributeStart, cursor))
  }

  const closing = tag.slice(cursor).trimStart()

  return `<${elementName}${attributes.map((attribute) => ` ${attribute}`).join('')}${closing}`
}

function isNameCharacter(character: string | undefined): boolean {
  return Boolean(character && !/[\s=/>]/.test(character))
}

function isWhitespace(character: string | undefined): boolean {
  return Boolean(character && /\s/.test(character))
}
