import { useUserSubscribersQuery } from '@entities/project'
import { UserConnections } from '@widgets'
import { useTranslation } from 'react-i18next'
import { useParams } from 'react-router-dom'

export function UserSubscribersPage() {
  const { t } = useTranslation()
  const { userId = '' } = useParams()
  const {
    data: subscribers = [],
    error,
    isError,
    isPending,
  } = useUserSubscribersQuery(userId)

  return (
    <UserConnections
      connections={subscribers}
      emptyMessage={t('You have no followers yet.')}
      error={error}
      isError={isError}
      isPending={isPending}
      title={t('Followers')}
    />
  )
}
