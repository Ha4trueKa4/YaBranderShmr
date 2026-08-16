export class SvgParseError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'SvgParseError'
  }
}
