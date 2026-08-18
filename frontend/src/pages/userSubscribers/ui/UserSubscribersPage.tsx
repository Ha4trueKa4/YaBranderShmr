import { useUserSubscribersQuery } from '@entities/project'
import { UserConnections } from '@widgets'
import { useParams } from 'react-router-dom'

export function UserSubscribersPage() {
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
      emptyMessage="You have no followers yet."
      error={error}
      isError={isError}
      isPending={isPending}
      title="Followers"
    />
  )
}
