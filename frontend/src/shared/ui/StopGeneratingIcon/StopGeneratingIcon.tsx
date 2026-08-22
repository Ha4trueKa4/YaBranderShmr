import type { SVGProps } from 'react'

export function StopGeneratingIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      fill="none"
      height="18"
      viewBox="0 0 18 18"
      width="18"
      xmlns="http://www.w3.org/2000/svg"
      {...props}
    >
      <title>Stop generating</title>
      <rect fill="currentColor" height="9" rx="1.5" width="9" x="4.5" y="4.5" />
    </svg>
  )
}
