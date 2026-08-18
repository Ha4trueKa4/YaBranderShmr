import { useEffect, useState } from 'react'

const mobileLayoutMediaQuery = '(width < 640px)'

export function useIsMobileLayout() {
  const [isMobileLayout, setIsMobileLayout] = useState(
    () =>
      typeof window !== 'undefined' &&
      window.matchMedia(mobileLayoutMediaQuery).matches,
  )

  useEffect(() => {
    const mediaQuery = window.matchMedia(mobileLayoutMediaQuery)
    const handleChange = () => setIsMobileLayout(mediaQuery.matches)

    handleChange()
    mediaQuery.addEventListener('change', handleChange)

    return () => mediaQuery.removeEventListener('change', handleChange)
  }, [])

  return isMobileLayout
}
